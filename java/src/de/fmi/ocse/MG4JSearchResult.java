package de.fmi.ocse;

import it.unimi.di.big.mg4j.index.Index;
import it.unimi.di.big.mg4j.query.SelectedInterval;
import it.unimi.di.big.mg4j.search.score.DocumentScoreInfo;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;

public class MG4JSearchResult implements SearchResultInterface {
	private static final int debug_cost = Helpers.DebugCost.expensive;
	private static final int debug_type = Helpers.DebugType.constant;

	int m_offset;
	ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> m_d;

	MG4JSearchResult(int offset, ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> d) {
		m_offset = offset;
		m_d = d;
		if (debug_cost >= Helpers.DebugCost.expensive) {
			int[] tmp = new int[size()];
			for(int i = 0; i < size(); ++i) {
				tmp[i] = at(i);
			}
			if (!Helpers.is_strongly_monotone_ascending(tmp)) {
				throw new RuntimeException("MG4JSearchResult: data is not strongly monotone ascending!");
			}
		}
	}

	@Override
	public int size() {
		return m_d.size();
	}

	@Override
	public int at(int pos) {
		//BUG:possible bug here, the same as for lucene?
		// though this should be correct here since the index needs the correct collection
		//it should return the document ids of the collection associated with the index
		return (int) m_d.get(pos).document + m_offset;
	}

}
