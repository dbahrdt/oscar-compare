package de.fmi.ocse;

import de.funroll_loops.oscar.OsmKeyValueObjectStore;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.CollectionTerminatedException;
import org.apache.lucene.analysis.Tokenizer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Comparator;
import java.util.function.IntConsumer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.document.Document;

import static java.util.stream.IntStream.range;

class LuceneSearch implements SearchInterface {

	private final int debug_cost = Helpers.DebugCost.none;
	private final int debug_type = Helpers.DebugType.constant;
	
	private interface MyCollectorDestinationInterface {
		public void collect(int arg0);
		public int size();
	}
	
	private class MyCollectorDestination implements MyCollectorDestinationInterface {
		private ArrayList<Integer> m_d = new ArrayList<Integer>();

		MyCollectorDestination() {
		}

		public ArrayList<Integer> result() {
			return m_d;
		}
		public void collect(int arg0) {
			m_d.add(arg0);
		}
		@Override
		public int size() {
			return m_d.size();
		}
	}
	
	private abstract class MyCollector extends SimpleCollector {
		private MyCollectorDestinationInterface m_dest;
		int m_docId = -1;
		
		public MyCollectorDestinationInterface dest() {
			return m_dest;
		}
		
		public void dest(MyCollectorDestinationInterface _dest) {
			m_dest = _dest;
		}
		
		public MyCollector() {
			m_dest = null;
		}
		
		public boolean needsScores() {
			return false;
		}

		public boolean acceptsDocsOutOfOrder() {
			return true;
		}

		@Override
		public void collect(int arg0) throws IOException {
			doCollect(this.m_docId+arg0);
		}

		protected abstract void doCollect(int luceneId) throws IOException;

		@Override
		protected void doSetNextReader(LeafReaderContext context) throws IOException {
			super.doSetNextReader(context);
			m_docId = context.docBase;
		}
	}
	
	private class MyAllCollector extends MyCollector {
		@Override
		public void doCollect(int arg0) throws IOException {
			dest().collect(arg0);
		}
	}
	
	private class MyTopKCollector extends MyCollector {
		int m_topK;
		
		public MyTopKCollector(int topK) {
			m_topK = topK;
		}
		
		@Override
		public void doCollect(int arg0) throws IOException {
			if (dest().size() < m_topK) {
				dest().collect(arg0);
			}
			else {
				throw new CollectionTerminatedException();
			}
		}
	}
	
	private class MyTokenizer extends Tokenizer {
		protected CharTermAttribute charTermAttribute = this.addAttribute(CharTermAttribute.class);
		protected PositionIncrementAttribute positionIncrementAttribute = this.addAttribute(PositionIncrementAttribute.class);
		protected String[] m_d;
		protected int m_pos;
		
		MyTokenizer() {
			m_d = new String[0];
			m_pos = 0;
		}
		
		//read all the tokens of our input stream at once
		protected void init() {
	        int numChars;
	        char[] buffer = new char[1024];
	        StringBuilder stringBuilder = new StringBuilder();
	        try {
	            while ((numChars = input.read(buffer, 0, buffer.length)) != -1) {
	                stringBuilder.append(buffer, 0, numChars);
	            }
	        }
	        catch (IOException e) {
	            throw new RuntimeException(e);
	        }
	        String tmp = stringBuilder.toString();
	        //split string into the single tokens
	        if (tmp.length() > 0) {
	        	m_d = tmp.split(de.fmi.ocse.Document.field_separator_string);
				for(int i = 0; i < m_d.length; ++i) {
					if (m_d[i].length() > IndexWriter.MAX_TERM_LENGTH) {
						System.out.println("LuceneSearch::MyTokenizer::init: had to cut off parts of a term: " + m_d[i].substring(IndexWriter.MAX_TERM_LENGTH, m_d[i].length()));
						m_d[i] = m_d[i].substring(0, IndexWriter.MAX_TERM_LENGTH);
					}
				}
	        }
		}
		
		@Override
		public void reset() throws IOException {
			super.reset();
			this.m_d = new String[0];
			this.m_pos = 0;
			this.charTermAttribute.setEmpty();
		}
		
		@Override
		public boolean incrementToken() throws IOException {
			if (m_pos >= m_d.length) { //possibly setReader was called, so we have a new stream
				init();
			}
			if (m_pos >= m_d.length) { // end of stream
				return false;
			}
			this.charTermAttribute.setEmpty();
			this.charTermAttribute.append(m_d[m_pos]);
			this.positionIncrementAttribute.setPositionIncrement(1);
			++m_pos;
			return true;
		}
		
		@Override
		public void close() throws IOException {
			super.close();
			m_d = null;
			this.charTermAttribute.setEmpty();
		}
	}
	
	private class MyAnalyzer extends Analyzer {
		@Override
		protected TokenStreamComponents createComponents(String arg0) {
			return new TokenStreamComponents(new MyTokenizer());
		}
		
	}

	private class MyDocumentAdder implements IntConsumer {
		IndexWriter idxWriter;
		int num_added = 0;

		MyDocumentAdder(IndexWriter idxWriter) {
			this.idxWriter = idxWriter;
		}

		@Override
		public void accept(int i) {
			addDocument(idxWriter, i);
			inc();
		}
		private synchronized void inc() {
			++num_added;
			if (num_added % 1000 == 0) {
				System.out.print("\r" + num_added);
			}
		}
	};
	
	private Directory m_dir;
	private MyAnalyzer m_analyzer;
	private DirectoryReader m_dirReader;
	private IndexSearcher m_idxSearcher;
	private Vector<Integer> m_oscarId2LuceneId = new Vector<Integer>();

	private IndexConfig m_idxCfg;
	private OsmKeyValueObjectStore m_store;
	
	private Document toDoc(int id) {
		de.fmi.ocse.Document bd = new de.fmi.ocse.Document(m_idxCfg, m_store, id);
		if ((debug_type & Helpers.DebugType.print_debug_msg) != 0) {
			System.out.println("LuceneSearch::toDoc::bd=" + bd.toString());
		}
		Document doc = new Document();
		doc.add( new StoredField("id", id) );
		int num_fields = 0;
		for(int i = 0; i < de.fmi.ocse.Document.number_of_fields; ++i) {
			if (bd.fieldSize(i) > 0) {
				++num_fields;
				String fieldStr;
				if ((debug_type & Helpers.DebugType.modifies_output) != 0) {
					fieldStr ="oscarid=" + id + de.fmi.ocse.Document.field_separator_string + bd.toString(i);
				}
				else {
					fieldStr = bd.toString(i);
				}
				doc.add( new LuceneTextField(de.fmi.ocse.Document.field_name[i], fieldStr, Field.Store.NO) );
			}
		}
		if ((debug_type & Helpers.DebugType.print_debug_msg) != 0) {
			System.out.println("LuceneSearch::toDoc::num_fields=" + num_fields);
			System.out.println("LuceneSearch::toDoc::doc=" + doc.toString());
		}
		return doc;
	}

	private void initOscarId2LuceneId() {
		try {
			for(int i = 0, s = m_idxSearcher.getIndexReader().numDocs(); i < s; ++i) {
				int oscarId = m_idxSearcher.doc(i).getField("id").numericValue().intValue();
				if (oscarId+1 > m_oscarId2LuceneId.size()) {
					m_oscarId2LuceneId.setSize(oscarId+1);
				}
				m_oscarId2LuceneId.set(oscarId, i);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void debugDoc(int oscarId) {
		try {
			int luceneId = m_oscarId2LuceneId.get(oscarId);
			Document doc = null;
			doc = m_idxSearcher.doc(luceneId);
			TokenStream ts = new MyTokenizer();
			IndexableField field = doc.getField("all_value");
			if (field != null) {
				ts = field.tokenStream(m_analyzer, ts);
				ts.reset();
				ts.incrementToken();
//				System.out.println(ts);
				ts.close();
			}
			else {
				System.out.println("Document luceneId=" + luceneId + " oscarId=" + oscarId + " has no field all_value");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public LuceneSearch(OsmKeyValueObjectStore store, IndexConfig idxCfg, Path path) throws IOException {
		m_idxCfg = idxCfg;
		m_dir = FSDirectory.open(path);
		m_analyzer = new MyAnalyzer();
		m_store = store;
	}

	private void addDocument(IndexWriter idxWriter, int i) {
		//don't index regions since oscar behaves a bit different for regions
		if (m_store.at(i).isRegion()) {
			return;
		}
		if ((debug_type & Helpers.DebugType.print_debug_msg) != 0) {
			System.out.print("LuceneSearch::index: begin adding document " + i + "\n");
		}
		Document doc = toDoc(i);
		try {
			idxWriter.addDocument(doc);
		}
		catch (IOException e) {
			e.printStackTrace();
			assert(false);
			return;
		}
		if ((debug_type & Helpers.DebugType.print_debug_msg) != 0) {
			System.out.print("LuceneSearch::index: end adding document " + i + "\n\n");
		}
	}
	
	@Override
	public void index() throws IOException {
		IndexWriterConfig idxWriterCfg = new IndexWriterConfig(m_analyzer);
		IndexWriter idxWriter = null;
		try {
			idxWriter = new IndexWriter(m_dir, idxWriterCfg);
		}
		catch (IOException e1) {
			e1.printStackTrace();
			assert(false);
			return;
		}

		range(0, m_store.size()).parallel().forEach(new MyDocumentAdder(idxWriter));

//		for(int i = 0, s = m_store.size(); i < s; ++i) {
//			addDocument(idxWriter, i);
//		}
		System.out.print("\n");
		try {
			idxWriter.close();
		}
		catch (IOException e) {
			e.printStackTrace();
			throw new IOException("Lucene could not write index");
		}
	}

	@Override
	public void load() throws IOException {
		try {
			m_dirReader = DirectoryReader.open(m_dir);
		}
		catch (IOException e) {
			e.printStackTrace();
			throw new IOException("Lucene could not load index");
		}
		m_idxSearcher = new IndexSearcher(m_dirReader);
		if (debug_cost >= Helpers.DebugCost.expensive) {
			initOscarId2LuceneId();
		}
	}

	public SearchResultInterface search(SearchQueryInterface query, MyCollector collector) {
		MyCollectorDestination dest = new MyCollectorDestination();
		collector.dest(dest);
		try {
			m_idxSearcher.search( ((LuceneSearchQuery) query).query(), collector);
		} catch (IOException e) {
			e.printStackTrace();
			assert(false);
		}
		//transform to real ids
		ArrayList<Integer> oscarIds = new ArrayList<Integer>();
		for(int luceneId : dest.result()) {
			int oscarId = -1;
			try {
				oscarId = m_idxSearcher.doc(luceneId).getField("id").numericValue().intValue();
			} catch (IOException e) {
				e.printStackTrace();
			}
			oscarIds.add(oscarId);
		}
		oscarIds.sort(new Comparator<Integer>() {
			@Override
			public int compare(Integer a, Integer b) {
				return a-b;
			}
		});
		oscarIds = Helpers.make_unique(oscarIds);
		assert( Helpers.is_strongly_monotone_ascending(oscarIds) );
		return new LuceneSearchResult(oscarIds, dest.result());
	}
	
	@Override
	public SearchResultInterface search(SearchQueryInterface query) {
		return search(query, new MyAllCollector());
	}

	@Override
	public SearchResultInterface search(SearchQueryInterface query, int topk) {
		return search(query, new MyTopKCollector(topk));
	}

	@Override
	public String name() {
		return "Lucene";
	}

	@Override
	public int type() {
		return Config.SearchType.Lucene;
	}
}
