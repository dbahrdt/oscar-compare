package de.fmi.ocse;

import it.unimi.di.big.mg4j.document.AbstractDocument;
import it.unimi.di.big.mg4j.document.AbstractDocumentCollection;
import it.unimi.di.big.mg4j.document.AbstractDocumentFactory;
import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.document.DocumentCollectionBuilder;
import it.unimi.di.big.mg4j.document.DocumentFactory;
import it.unimi.di.big.mg4j.index.*;
import it.unimi.di.big.mg4j.index.cluster.IndexCluster;
import it.unimi.di.big.mg4j.io.IOFactories;
import it.unimi.di.big.mg4j.io.IOFactory;
import it.unimi.di.big.mg4j.tool.Combine;
import it.unimi.di.big.mg4j.tool.Concatenate;
import it.unimi.di.big.mg4j.tool.IndexBuilder;
import it.unimi.di.big.mg4j.tool.Scan;
import it.unimi.di.big.mg4j.tool.VirtualDocumentResolver;
import it.unimi.dsi.big.util.ImmutableExternalPrefixMap;
import it.unimi.dsi.big.util.StringMap;
import it.unimi.dsi.big.util.StringMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.io.WordReader;
import it.unimi.dsi.logging.ProgressLogger;
import org.apache.commons.configuration.ConfigurationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public class MG4JSearchBase {

	//use this char to separate tokens of a field
	static final protected char field_separator_char = '\uE000';

	//A document is made up of fields which themselves are made up of tokens
	//There is no processing of the tokens when putting them into the index
	public interface Document extends Serializable {
		//the oscar id!
		int id();
		List<String> field(int index);
		void close();
	}

	public interface SourceDocumentCollection {
		int size();
		int number_of_fields();
		String fieldName(int index);
		de.fmi.ocse.MG4JSearchBase.Document document(long index);
		SourceDocumentCollection copy();
	}

	private class MyWordReader extends FastBufferedReader {
		@Override
		protected boolean isWordConstituent(char c) {
			return c != field_separator_char;
		}
	}


	//used by MyDocument, thread-safe
	private transient Pattern m_replaceRegex = Pattern.compile("\n|\r");

	private class MyDocument extends AbstractDocument {
		private Document m_d;

		MyDocument(Document bd) {
			m_d = bd;
		}

		//concatenate the tokens into a single string separated with the field_separator_char
		private String concat(List<String> tokens) {
			if (tokens.size() > 0) {
				StringBuilder sb = new StringBuilder();
				sb.append( m_replaceRegex.matcher(tokens.get(0)).replaceAll("_") );
				for (int i = 1, s = tokens.size(); i < s; ++i) {
					sb.append(field_separator_char);
					String token = tokens.get(i);
					java.util.regex.Matcher matcher = m_replaceRegex.matcher(token);
					if (matcher.find()) {
						sb.append(matcher.replaceAll("_"));
					}
					else {
						sb.append(token);
					}
				}
				return sb.toString();
			}
			return "";
		}

		@Override
		public CharSequence title() {
			return "" + m_d.id();
		}

		@Override
		public CharSequence uri() {
			return title();
		}

		@Override
		public Object content(int i) throws IOException {
			String concatString = this.concat(m_d.field(i));
			assert(!m_replaceRegex.matcher(concatString).find());
			return new StringReader(concatString);
		}

		@Override
		public WordReader wordReader(int i) {
			return new MyWordReader();
		}

		@Override
		public void close() throws IOException {
			m_d.close();
			super.close();
		}
	}

	private class MyDocumentFactory extends AbstractDocumentFactory {
		private HashMap<String, Integer> m_fieldName2FieldIdx = new HashMap<>();
		private SourceDocumentCollection m_sdc;
		private long m_begin;
		private long m_end;

		public MyDocumentFactory(SourceDocumentCollection sdc) {
			m_sdc = sdc;
			m_begin = 0;
			m_end = m_sdc.size();
			for(int i = 0, s = m_sdc.number_of_fields(); i < s; ++i) {
				m_fieldName2FieldIdx.put(m_sdc.fieldName(i), i);
			}
		}

		public MyDocumentFactory(SourceDocumentCollection sdc, long begin, long end) {
			m_sdc = sdc;
			m_begin = begin;
			m_end = end;
			for(int i = 0, s = m_sdc.number_of_fields(); i < s; ++i) {
				m_fieldName2FieldIdx.put(m_sdc.fieldName(i), i);
			}
		}

		@Override
		public int numberOfFields() {
			return m_sdc.number_of_fields();
		}

		@Override
		public String fieldName(int i) {
			return m_sdc.fieldName(i);
		}

		@Override
		public int fieldIndex(String s) {
			return m_fieldName2FieldIdx.get(s);
		}

		@Override
		public FieldType fieldType(int i) {
			return FieldType.TEXT;
		}

		@Override
		public it.unimi.di.big.mg4j.document.Document getDocument(InputStream inputStream, Reference2ObjectMap<Enum<?>, Object> reference2ObjectMap) throws IOException {
			ObjectInputStream ois = new ObjectInputStream(inputStream);

			Document doc = null;
			try {
				doc = (Document) ois.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				assert(false);
			}

			return new MyDocument(doc);
		}

		@Override
		public it.unimi.di.big.mg4j.document.DocumentFactory copy() {
			return new MyDocumentFactory(m_sdc.copy(), m_begin, m_end);
		}
	}

	private class MyDocumentCollection extends AbstractDocumentCollection {
		private SourceDocumentCollection m_sdc;
		private long m_begin;
		private long m_end;

		public MyDocumentCollection(SourceDocumentCollection sdc) {
			m_sdc = sdc;
			m_begin = 0;
			m_end = m_sdc.size();
		}

		public MyDocumentCollection(SourceDocumentCollection sdc, long begin, long end) {
			m_sdc = sdc;
			m_begin = begin;
			m_end = end;
			assert(begin > 0 && begin <= end && end <= m_sdc.size());
		}

		@Override
		public long size() {
			return m_end-m_begin;
		}

		@Override
		public it.unimi.di.big.mg4j.document.Document document(long index) throws IOException {
			return new MyDocument(m_sdc.document(index+m_begin));
		}

		@Override
		public InputStream stream(long index) throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);

			Document doc = m_sdc.document(index);
			oos.writeObject(doc);

			oos.flush();
			oos.close();

			return new ByteArrayInputStream(baos.toByteArray());
		}

		@Override
		public Reference2ObjectMap<Enum<?>, Object> metadata(long index) throws IOException {
			return null;
		}

		@Override
		public DocumentCollection copy() {
			return new MyDocumentCollection(m_sdc.copy(), m_begin, m_end);
		}

		@Override
		public DocumentFactory factory() {
			return new MyDocumentFactory(m_sdc, m_begin, m_end);
		}

		MyDocumentCollection slice(long begin, long end) {
			assert(begin > 0 && begin < end && end <= m_end);
			return new MyDocumentCollection(m_sdc.copy(), begin, end);
		}
	}

	//wrap the index builder to easily change options
	private class MyIndexBuilder {

		private class IndexBuilderCfg {
			private String path;
			private MyDocumentCollection collection;
			private IOFactory ioFactory = IOFactory.FILESYSTEM_FACTORY;
			private TermProcessor termProcessor = NullTermProcessor.getInstance();
			public Class<? extends StringMap<? extends CharSequence>> termMapClass = ImmutableExternalPrefixMap.class;

			//we only want to store pointers
			private Map<CompressionFlags.Component, CompressionFlags.Coding> idxWriterFlags = new EnumMap<CompressionFlags.Component,CompressionFlags.Coding>( CompressionFlags.Component.class );
			private final Scan.Completeness completeness = Scan.Completeness.POINTERS;

			private boolean skips = true;
			private Combine.IndexType indexType = Combine.IndexType.QUASI_SUCCINCT;
			private int quantum = BitStreamIndex.DEFAULT_QUANTUM;
			private int height = BitStreamIndex.DEFAULT_HEIGHT;

			private int scanBufferSize = Scan.DEFAULT_BUFFER_SIZE;
			private int combineBufferSize = Combine.DEFAULT_BUFFER_SIZE;
			private int skipBufferSize = SkipBitStreamIndexWriter.DEFAULT_TEMP_BUFFER_SIZE;
			private int documentsPerBatch = Scan.DEFAULT_BATCH_SIZE;
			private int maxTerms = Scan.DEFAULT_MAX_TERMS;

			private long logInterval = ProgressLogger.DEFAULT_LOG_INTERVAL;

			private String batchDirName = null;

			private DocumentFactory factory;
			private DocumentCollectionBuilder builder = null;

			private final String[] basenameField = new String[de.fmi.ocse.Document.field_idx_iterate.length];

			// Create gap array and dummy virtual document resolvers
			private final int[] virtualDocumentGap = new int[de.fmi.ocse.Document.field_idx_iterate.length];
			private final VirtualDocumentResolver[] virtualDocumentResolver = new VirtualDocumentResolver[ de.fmi.ocse.Document.field_idx_iterate.length ];

			IndexBuilderCfg(String path, MyDocumentCollection collection) {
				this.path = path;
				this.collection = collection;
				this.factory = this.collection.factory();

				idxWriterFlags.put( CompressionFlags.Component.POINTERS, null );

				for(int fieldId : de.fmi.ocse.Document.field_idx_iterate) {
					basenameField[fieldId] = path + "-" + factory.fieldName( fieldId );
				}
				Arrays.fill(virtualDocumentGap, Scan.DEFAULT_VIRTUAL_DOCUMENT_GAP);
			}
		}

		private class SubIndexBuilder implements Runnable {
			IndexBuilderCfg m_cfg;
			SubIndexBuilder(IndexBuilderCfg cfg) {
				m_cfg = cfg;
			}

			public IndexBuilderCfg cfg() { return m_cfg; }

			@Override
			public void run() {
				try {
					Scan.run( m_cfg.ioFactory,
							m_cfg.path,
							m_cfg.collection,
							m_cfg.completeness,
							m_cfg.termProcessor,
							m_cfg.builder,
							m_cfg.scanBufferSize,
							m_cfg.documentsPerBatch,
							m_cfg.maxTerms,
							de.fmi.ocse.Document.field_idx_iterate,
							m_cfg.virtualDocumentResolver,
							m_cfg.virtualDocumentGap,
							null,
							m_cfg.logInterval,
							null);
				} catch (Exception e) {
					e.printStackTrace();
					assert(false);
				}
				for (int fieldId : de.fmi.ocse.Document.field_idx_iterate) {
					assert(m_cfg.factory.fieldType(fieldId) == DocumentFactory.FieldType.TEXT );
					final String[] inputBasename;
					try {
						inputBasename = IOFactories.loadProperties(m_cfg.ioFactory, m_cfg.basenameField[fieldId] + Scan.CLUSTER_PROPERTIES_EXTENSION).getStringArray(IndexCluster.PropertyKeys.LOCALINDEX);
						Combine combine = null;
						combine = new Concatenate(m_cfg.ioFactory, m_cfg.basenameField[fieldId], inputBasename, false, m_cfg.combineBufferSize, m_cfg.idxWriterFlags, m_cfg.indexType, m_cfg.skips, m_cfg.quantum, m_cfg.height, m_cfg.skipBufferSize, m_cfg.logInterval);
						combine.run();
						Scan.cleanup(m_cfg.ioFactory, m_cfg.basenameField[fieldId], inputBasename.length, null);
					} catch (Exception e) {
						e.printStackTrace();
						assert(false);
					}
				}

				for(int fieldId : de.fmi.ocse.Document.field_idx_iterate) {
					try {
						IOFactories.storeObject(
								m_cfg.ioFactory,
								StringMaps.synchronize( m_cfg.termMapClass.getConstructor( Iterable.class ).newInstance(
										IOFactories.fileLinesCollection(
												m_cfg.ioFactory, m_cfg.basenameField[fieldId] + DiskBasedIndex.TERMS_EXTENSION, "UTF-8"
										)
								) ),
								m_cfg.basenameField[fieldId] + DiskBasedIndex.TERMMAP_EXTENSION  );
					} catch (Exception e) {
						e.printStackTrace();
						assert(false);
					}
				}
			}
		}

		private MyDocumentCollection m_collection;
		private IndexBuilder m_idxBuilder;
		MyIndexBuilder(MyDocumentCollection collection) {
			m_collection = collection;
		}
		private void buildWithIdxBuilder(Path path) {
			Map<CompressionFlags.Component, CompressionFlags.Coding> idxWriterFlags = new EnumMap<CompressionFlags.Component,CompressionFlags.Coding>( CompressionFlags.Component.class );
			idxWriterFlags.put( CompressionFlags.Component.POINTERS, null );

			m_idxBuilder = new IndexBuilder(path.toString(), m_collection);
			m_idxBuilder.termProcessor( DowncaseTermProcessor.getInstance() );
			m_idxBuilder.keepBatches(false);
			m_idxBuilder.quasiSuccinctWriterFlags(idxWriterFlags);
			try {
				m_idxBuilder.run();
			} catch (Exception e) {
				e.printStackTrace();
				assert(false);
			}
		}

		public void build_own(Path path) {
			int cores = Runtime.getRuntime().availableProcessors();
			if (m_collection.size() < cores*10) {
				cores = 1;
			}

			Thread threads[]= new Thread[cores];
			SubIndexBuilder builders[] = new SubIndexBuilder[cores];
			long blockSize = m_collection.size()/cores;
			for(int i = 0; i < cores; ++i) {
				long blockBegin = blockSize*i;
				long blockEnd = blockBegin+blockSize;
				blockEnd = blockEnd < m_collection.size() ? blockEnd : m_collection.size();
				System.out.println("Collection slice for thread" + i + " is " + blockBegin + "->" + blockEnd);
				MyDocumentCollection myCollection = m_collection.slice(blockBegin, blockEnd);
				IndexBuilderCfg myCfg = new IndexBuilderCfg(path.toString() + i, myCollection);
				builders[i] = new SubIndexBuilder(myCfg);
				threads[i] = new Thread(builders[i]);
				threads[i].start();
			}

			for(int i = 0; i < cores;) {
				try {
					threads[i].join();
					++i;
				}
				catch (InterruptedException e) {
					e.printStackTrace();
					assert(false);
				}
			}

			System.out.println("Subindexing complete. Merging...");

			IndexBuilderCfg cfg = new IndexBuilderCfg(path.toString(), m_collection);

			//we now need to concatenate the sub indices
			for (int fieldId : de.fmi.ocse.Document.field_idx_iterate) {
				System.out.println("Merging field " + de.fmi.ocse.Document.field_name[fieldId]);

				final String[] inputBasename = new String[cores];
				for(int i = 0; i < cores; ++i) {
					inputBasename[i] = builders[i].cfg().basenameField[fieldId];
				}

				try {
					Combine combine = null;
					combine = new Concatenate(cfg.ioFactory, cfg.basenameField[fieldId], inputBasename, false, cfg.combineBufferSize, cfg.idxWriterFlags, cfg.indexType, cfg.skips, cfg.quantum, cfg.height, cfg.skipBufferSize, cfg.logInterval);
					combine.run();
					Scan.cleanup(cfg.ioFactory, cfg.basenameField[fieldId], inputBasename.length, null);
				} catch (Exception e) {
					e.printStackTrace();
					assert(false);
				}
			}


			for(int fieldId : de.fmi.ocse.Document.field_idx_iterate) {
				try {
					IOFactories.storeObject(
							cfg.ioFactory,
							StringMaps.synchronize( cfg.termMapClass.getConstructor( Iterable.class ).newInstance(
									IOFactories.fileLinesCollection(
											cfg.ioFactory, cfg.basenameField[fieldId] + DiskBasedIndex.TERMS_EXTENSION, "UTF-8"
									)
							) ),
							cfg.basenameField[fieldId] + DiskBasedIndex.TERMMAP_EXTENSION  );
				} catch (Exception e) {
					e.printStackTrace();
					assert(false);
				}
			}

			System.out.println("Deleting sub indices");
			for (int i = 0; i < cores; ++i) {
				IOFactory ioFactory = builders[i].cfg().ioFactory;
				System.out.println("Deleting sub index " + i);
				for(int fieldId : de.fmi.ocse.Document.field_idx_iterate) {
					String fieldBaseName = builders[i].cfg().basenameField[fieldId];
					System.out.println("Deleting sub index " + i + " file:" + fieldBaseName);
					try {
						ioFactory.delete(fieldBaseName + DiskBasedIndex.FREQUENCIES_EXTENSION);
						ioFactory.delete(fieldBaseName + DiskBasedIndex.OCCURRENCIES_EXTENSION);
						ioFactory.delete(fieldBaseName + DiskBasedIndex.INDEX_EXTENSION);
						ioFactory.delete(fieldBaseName + DiskBasedIndex.OFFSETS_EXTENSION);
						ioFactory.delete(fieldBaseName + DiskBasedIndex.SIZES_EXTENSION);
						ioFactory.delete(fieldBaseName + DiskBasedIndex.STATS_EXTENSION);
						ioFactory.delete(fieldBaseName + DiskBasedIndex.PROPERTIES_EXTENSION);
						ioFactory.delete(fieldBaseName + DiskBasedIndex.POSITIONS_NUMBER_OF_BITS_EXTENSION);
						ioFactory.delete(fieldBaseName + DiskBasedIndex.TERMS_EXTENSION);
						ioFactory.delete(fieldBaseName + DiskBasedIndex.SUMS_MAX_POSITION_EXTENSION);
						ioFactory.delete(fieldBaseName + DiskBasedIndex.UNSORTED_TERMS_EXTENSION);
						ioFactory.delete(fieldBaseName + DiskBasedIndex.POINTERS_EXTENSIONS);
						ioFactory.delete(fieldBaseName + DiskBasedIndex.TERMMAP_EXTENSION);
						ioFactory.delete(fieldBaseName + ".pointersoffsets");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		public void build(Path path) {
//			buildWithIdxBuilder(path);
			build_own(path);
		}

	}

	private MyDocumentCollection m_collection;

	///you have to call init() afterwards
	MG4JSearchBase() {}

	protected void init(SourceDocumentCollection sdc) {
		m_collection = new MyDocumentCollection(sdc);
	}

	public void index(Path path) {
		MyIndexBuilder idxBuilder = new MyIndexBuilder(m_collection);
		idxBuilder.build(path);
	}

}
