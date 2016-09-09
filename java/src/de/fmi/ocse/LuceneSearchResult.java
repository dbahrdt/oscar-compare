package de.fmi.ocse;

import java.util.ArrayList;

public class LuceneSearchResult implements SearchResultInterface {
	private ArrayList<Integer> m_d;
	private ArrayList<Integer> m_luceneIds;
	
	LuceneSearchResult(ArrayList<Integer> results) {
		m_d = results;
	}
	LuceneSearchResult(ArrayList<Integer> results, ArrayList<Integer> luceneIds) {
		m_d = results;
		m_luceneIds = luceneIds;
	}
	
	@Override
	public int size() {
		return m_d.size();
	}

	@Override
	public int at(int pos) {
		return m_d.get(pos);
	}

}
