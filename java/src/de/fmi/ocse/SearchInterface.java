package de.fmi.ocse;

import de.funroll_loops.oscar.OsmKeyValueObjectStore;
import de.funroll_loops.oscar.OsmCompleter;
import org.apache.commons.configuration.ConfigurationException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.Path;

public interface SearchInterface {

	void debugDoc(int oscarId);

	void index() throws IOException, IllegalAccessException, ConfigurationException, InvocationTargetException, InstantiationException, URISyntaxException, NoSuchMethodException, ClassNotFoundException;
	void load() throws IOException;

	//this should retrieve _all_ matches
	SearchResultInterface search(SearchQueryInterface query) throws UnsupportedQueryType;
	//this should retrieve the topk matches
	SearchResultInterface search(SearchQueryInterface query, int topk) throws UnsupportedQueryType;
	
	String name();
	int type();
}
