package de.fmi.ocse;

public class OscarSearchQuery implements SearchQueryInterface {
	private String m_qstr;

	public String toString() {
		return m_qstr;
	}

	@Override
	public void init(SearchQueryNode root) {
		m_qstr = this.toQueryString(root);
	}
	
	protected String toQueryString(SearchQueryNode node) {
		return node.toString();
	}
}
