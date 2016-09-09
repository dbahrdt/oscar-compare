package de.fmi.ocse;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MG4JSearchDebug extends MG4JSearchBase {

	static final int number_of_fields = 6;
	static final int min_string_length = 0;
	static final int max_string_length = 30;
	static final int min_field_size = 0;
	static final int max_field_size = 30;
	static final String[] field_names = {"field0", "field1", "field2", "field3", "field4", "field5"};

	private String[] possible_field_values;
	private Random m_rnd = new Random();

	private class MyDocument implements Document {
		int m_id;
		public List<String>[] m_d;

		MyDocument(int id) {
			m_id = id;
			m_d = new ArrayList[number_of_fields];
			for(int i = 0; i < m_d.length; ++i) {
				m_d[i] = new ArrayList<String>();
			}
			for(int i = 0; i < number_of_fields; ++i) {
				int fieldSize = m_rnd.nextInt(max_field_size);
				for(int j = 0; j < fieldSize || j < min_field_size; ++j) {
					int strId = m_rnd.nextInt(possible_field_values.length);
					m_d[i].add(possible_field_values[strId]);
				}
			}
		}

		@Override
		public int id() {
			return m_id;
		}

		@Override
		public List<String> field(int index) {
			return m_d[index];
		}

		@Override
		public void close() {
			m_d = null;
		}
	}

	private class MySourceDocumentCollection implements SourceDocumentCollection {
		List<MyDocument> m_docs = new ArrayList<MyDocument>();

		public void populate(int numDocs) {
			for(int i = 0; i < numDocs; ++i) {
				m_docs.add(new MyDocument(i));
			}
		}

		@Override
		public int size() {
			return m_docs.size();
		}

		@Override
		public int number_of_fields() {
			return MG4JSearchDebug.number_of_fields;
		}

		@Override
		public String fieldName(int index) {
			return MG4JSearchDebug.field_names[index];
		}

		@Override
		public Document document(long index) {
			return m_docs.get((int) index);
		}

		@Override
		public SourceDocumentCollection copy() { return this; }
	}

	private MySourceDocumentCollection m_docs = new MySourceDocumentCollection();

	MG4JSearchDebug() {
		possible_field_values = new String[10240];
		String charStr = "abcdefghijklmnopqrstuvwxyz+-_:.;#'~*1234567890\n";
		char[] chars = charStr.toCharArray();
		for(int i = 0; i < possible_field_values.length; ++i) {
			StringBuilder sb = new StringBuilder();
			int len = m_rnd.nextInt(max_string_length);
			for(int j = 0; j < min_string_length || j < len; ++j) {
				sb.append( chars[ m_rnd.nextInt(chars.length) ] );
			}
			possible_field_values[i] = sb.toString();
		}
	}

	public void start(int numDocs, Path path) {

		System.out.println("Populating document collection");
		m_docs.populate(numDocs);
		System.out.println("Initializing base document collection");
		this.init(m_docs);
		System.out.println("Starting indexing phase");
		this.index(path);
	}

	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("prg <number of docs> <path to index folder>");
			return;
		}
		int numDocs = new Integer(args[0]);
		Path path = java.nio.file.Paths.get(args[1]);

		System.out.println("Creating a document collection with " + numDocs + " documents");
		System.out.println("Index folder: " + path.toString());

		MG4JSearchDebug	worker = new MG4JSearchDebug();

		worker.start(numDocs, path);
	}
}
