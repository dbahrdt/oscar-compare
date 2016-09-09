package de.fmi.ocse;

import java.io.Reader;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexOptions;

public final class LuceneTextField extends Field {
	public static final FieldType TYPE_NOT_STORED = new FieldType();
	public static final FieldType TYPE_STORED = new FieldType();

	public LuceneTextField(String name, Reader reader) {
		super(name, reader, TYPE_NOT_STORED);
	}

	public LuceneTextField(String name, String value, Store store) {
		super(name, value, store == Store.YES?TYPE_STORED:TYPE_NOT_STORED);
	}

	public LuceneTextField(String name, TokenStream stream) {
		super(name, stream, TYPE_NOT_STORED);
	}

	static {
		TYPE_NOT_STORED.setIndexOptions(IndexOptions.DOCS);
		TYPE_NOT_STORED.setTokenized(true);
		TYPE_NOT_STORED.freeze();
		TYPE_STORED.setIndexOptions(IndexOptions.DOCS);
		TYPE_STORED.setTokenized(true);
		TYPE_STORED.setStored(true);
		TYPE_STORED.freeze();
	}
}
