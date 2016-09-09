package de.fmi.ocse;

import de.funroll_loops.oscar.OsmKeyValueObjectStore;

import it.unimi.di.big.mg4j.document.*;
import it.unimi.di.big.mg4j.index.*;

import it.unimi.di.big.mg4j.index.cluster.IndexCluster;
import it.unimi.di.big.mg4j.io.IOFactories;
import it.unimi.di.big.mg4j.io.IOFactory;
import it.unimi.di.big.mg4j.query.QueryEngine;
import it.unimi.di.big.mg4j.query.SelectedInterval;
import it.unimi.di.big.mg4j.query.nodes.Query;
import it.unimi.di.big.mg4j.query.nodes.QueryBuilderVisitorException;
import it.unimi.di.big.mg4j.search.DocumentIteratorBuilderVisitor;
import it.unimi.di.big.mg4j.search.score.DocumentScoreInfo;
import it.unimi.di.big.mg4j.tool.*;
import it.unimi.dsi.big.util.ImmutableExternalPrefixMap;
import it.unimi.dsi.big.util.StringMap;
import it.unimi.dsi.big.util.StringMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.logging.ProgressLogger;
import org.apache.commons.collections.ArrayStack;
import org.apache.commons.configuration.ConfigurationException;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.LongConsumer;

import static java.lang.Math.min;
import static java.util.stream.LongStream.range;


class MG4JSearch extends MG4JSearchBase implements SearchInterface {

	private class MyDocument implements Document {
		de.fmi.ocse.Document m_d;
		MyDocument(de.fmi.ocse.Document doc) {
			m_d = doc;
		}

		@Override
		public int id() {
			return m_d.id;
		}

		@Override
		public List<String> field(int index) {
			return m_d.fields[index];
		}

		@Override
		public void close() {
			m_d = null;
		}
	}

	private class BufferedDocumentCreator {
		static final int buffer_size = 1000;
		private class MyDocAdder implements LongConsumer {
			@Override
			public void accept(long id) {
				m_documents[(int) (id-m_offset)] = new MyDocument(new de.fmi.ocse.Document(m_idxCfg, m_store, (int) id));
			}
		};

		private OsmKeyValueObjectStore m_store;
		private IndexConfig m_idxCfg;
		private long m_offset = 0;
		private long m_bufferSize = 0;
		private long m_size = 0;
		private MyDocument m_documents[] = new MyDocument[buffer_size];
		private MyDocAdder m_docAdder = new MyDocAdder();

		BufferedDocumentCreator(OsmKeyValueObjectStore store, IndexConfig idxCfg) {
			m_store = store;
			m_idxCfg = idxCfg;
			m_size = m_store.size();
		}

		BufferedDocumentCreator(BufferedDocumentCreator other) {
			m_store = other.m_store;
			m_idxCfg = other.m_idxCfg;
			m_offset = other.m_offset;
			m_size = other.m_size;
		}

		public synchronized Document document(long realIdx) {
			if (m_offset+m_bufferSize <= realIdx) {
				long beginId = realIdx;
				long endId = min(m_size, realIdx+buffer_size);
				m_offset = beginId;
				m_bufferSize = endId-beginId;
//				range(beginId, endId).parallel().forEach(m_docAdder);
				for(long id = beginId; id < endId; ++id) {
					m_docAdder.accept(id);
				}
//				System.out.println("Buffering documents " + beginId + " to " + endId);
			}
//			System.out.println("Returning document" + realIdx);
			return m_documents[(int) (realIdx-m_offset)];
		}

		BufferedDocumentCreator copy() {
			return new BufferedDocumentCreator(this);
		}
	}

	private class MySourceDocumentCollection implements SourceDocumentCollection {
		private BufferedDocumentCreator m_dc;
		private OsmKeyValueObjectStore m_store;

		public long begin() {
			return m_store.numberOfRegions();
		}
		public long end() {
			return m_store.size();
		}

		MySourceDocumentCollection(OsmKeyValueObjectStore store, IndexConfig idxCfg) {
			m_store = store;
			m_dc = new BufferedDocumentCreator(m_store, idxCfg);
		}

		MySourceDocumentCollection(MySourceDocumentCollection other) {
			m_store = other.m_store;
			m_dc = other.m_dc.copy();
		}

		@Override
		public int size() {
			return (int) (end()-begin());
		}

		@Override
		public int number_of_fields() {
			return de.fmi.ocse.Document.number_of_fields;
		}

		@Override
		public String fieldName(int index) {
			return de.fmi.ocse.Document.field_name[index];
		}

		@Override
		public Document document(long index) {
			long realIdx = begin()+index;
			return m_dc.document(realIdx);
		}

		@Override
		public SourceDocumentCollection copy() {
			return new MySourceDocumentCollection(this);
		}
	}

	private Path m_path;
	private MySourceDocumentCollection m_collection;

	//stuff for query
	Object2ReferenceOpenHashMap<String,Index> m_indexMap = new Object2ReferenceOpenHashMap<String,Index>();
	Object2ReferenceOpenHashMap<String, TermProcessor> m_termProcessors = new Object2ReferenceOpenHashMap<String, TermProcessor>();
	DocumentIteratorBuilderVisitor m_docIterator;
	QueryEngine m_queryEngine;

	MG4JSearch(OsmKeyValueObjectStore store, IndexConfig idxCfg, Path path) {
		super();
		m_path = path;
		m_collection = new MySourceDocumentCollection(store, idxCfg);
		init(m_collection);
	}

	@Override
	public void debugDoc(int oscarId) {
		System.out.println(m_collection.toString());
	}

	@Override
	public void index() throws IOException, IllegalAccessException, ConfigurationException, InvocationTargetException, InstantiationException, URISyntaxException, NoSuchMethodException, ClassNotFoundException {
		index(m_path);
	}

	@Override
	public void load() {
		try {
			for (int fieldId : de.fmi.ocse.Document.field_idx_iterate) {
				String fieldName = de.fmi.ocse.Document.field_name[fieldId];
				Index idx = Index.getInstance(m_path.toString() + "-" + fieldName, false, false);
				m_indexMap.put(fieldName, idx);
				m_termProcessors.put(fieldName, idx.termProcessor);
			}
			Index defaultIdx = m_indexMap.get(de.fmi.ocse.Document.field_name[de.fmi.ocse.Document.default_field]);
			m_docIterator = new DocumentIteratorBuilderVisitor(m_indexMap, defaultIdx, 1000*1000*1000 );
			m_queryEngine = new QueryEngine(null, m_docIterator, m_indexMap);
		}
		catch (Exception e) {
			e.printStackTrace();
			assert(false);
		}
	}

	@Override
	public SearchResultInterface search(SearchQueryInterface query) throws UnsupportedQueryType {
		return search(query, m_collection.size());
	}

	@Override
	public SearchResultInterface search(SearchQueryInterface query, int topk) throws UnsupportedQueryType {
		Query q = ((Mg4jSearchQuery) query).q();
		ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> result =
				new ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index,SelectedInterval[]>>>();
		try {
			m_queryEngine.process(q, 0, topk, result);
		} catch (java.lang.UnsupportedOperationException e) {
			throw new UnsupportedQueryType(e.toString());
		}
		catch (Exception e) {
			e.printStackTrace();
			assert(false);
		}

		return new MG4JSearchResult((int) m_collection.begin(), result);
	}

	@Override
	public String name() {
		return "Mg4j";
	}

	@Override
	public int type() {
		return Config.SearchType.Mg4j;
	}
	
}