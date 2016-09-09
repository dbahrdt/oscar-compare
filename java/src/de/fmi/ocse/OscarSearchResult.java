package de.fmi.ocse;

import de.funroll_loops.oscar.GeoHierarchySubSet;

public class OscarSearchResult implements SearchResultInterface {
	private GeoHierarchySubSet m_ghs;
	private int[] m_d;
	
	//gets all
	public OscarSearchResult(GeoHierarchySubSet ghs) {
		m_ghs = ghs;
		m_d = m_ghs.flaten();
		assert(Helpers.is_strongly_monotone_ascending(m_d));
	}
	
	//gets the topk
	public OscarSearchResult(GeoHierarchySubSet ghs, int count) {
		m_ghs = ghs;
		m_d = m_ghs.topK(count);
	}
	
	@Override
	public int size() {
		return m_d.length;
	}

	@Override
	public int at(int pos) {
		return m_d[pos];
	}
}
