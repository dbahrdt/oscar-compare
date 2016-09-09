package de.fmi.ocse;

import de.funroll_loops.oscar.OsmKeyValueObjectStore;
import de.funroll_loops.oscar.OsmCompleter;

import java.nio.file.Path;

public class OscarSearch implements SearchInterface {
	protected OsmCompleter m_cmp;

	OscarSearch(OsmCompleter cmp) {
		m_cmp = cmp;
	}

	@Override
	public void debugDoc(int oscarId) {
		;
	}

	@Override
	public void index() {}

	@Override
	public void load() {}

	@Override
	public SearchResultInterface search(SearchQueryInterface query) {
		return new OscarSearchResult( m_cmp.clusteredComplete(query.toString(), false) );
	}

	@Override
	public SearchResultInterface search(SearchQueryInterface query, int topk) {
		return new OscarSearchResult( m_cmp.clusteredComplete(query.toString(), false), topk );
	}

	@Override
	public String name() {
		return "Oscar";
	}

	@Override
	public int type() {
		return Config.SearchType.Oscar;
	}
	
}
