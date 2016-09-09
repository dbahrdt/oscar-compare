package de.fmi.ocse;

/**
 * Created by daniel on 30.09.16.
 */
public class EmptySearchResult implements SearchResultInterface {
	@Override
	public int size() {
		return 0;
	}

	@Override
	public int at(int pos) {
		return 0;
	}
}
