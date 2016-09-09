package de.fmi.ocse;

public interface SearchQueryInterface {
	public String toString();
	public void init(SearchQueryNode root) throws UnsupportedQueryType;
}
