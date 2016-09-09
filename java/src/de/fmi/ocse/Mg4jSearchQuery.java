package de.fmi.ocse;
import it.unimi.di.big.mg4j.query.nodes.Query;
import it.unimi.di.big.mg4j.query.nodes.And;
import it.unimi.di.big.mg4j.query.nodes.Or;
import it.unimi.di.big.mg4j.query.nodes.Difference;
import it.unimi.di.big.mg4j.query.nodes.Prefix;
import it.unimi.di.big.mg4j.query.nodes.Select;
import it.unimi.di.big.mg4j.query.nodes.Term;

public class Mg4jSearchQuery implements SearchQueryInterface {

	private Query m_q;

	public Query q() { return m_q; }

	@Override
	public String toString() {
		return m_q.toString();
	}

	@Override
	public void init(SearchQueryNode root) throws UnsupportedQueryType {
		m_q = toQuery(root);
	}

	private Query toQuery(SearchQueryNode node) throws UnsupportedQueryType {
		if ((node.type & (SearchQueryNode.Type.STR | SearchQueryNode.Type.TAG)) != 0) {
			Query q;
			String key;
			String fieldSuffix;
			if ((node.type & SearchQueryNode.Type.STR) != 0) {
				fieldSuffix = "value";
			}
			else {
				fieldSuffix = "tag";
			}
			if ((node.type & (SearchQueryNode.Type.ITEM | SearchQueryNode.Type.REGION)) != 0)  {
				key = "all_" + fieldSuffix;
			}
			else if ((node.type & SearchQueryNode.Type.ITEM) != 0) {
				key = "item_" + fieldSuffix;
			}
			else if ((node.type & SearchQueryNode.Type.REGION) != 0) {
				key = "region_" + fieldSuffix;
			}
			else {
				key = "all_" + fieldSuffix;
			}
			if ((node.type & SearchQueryNode.Type.EXACT) != 0) {
				q = new Term(node.str);
			}
			else {
				String str = "";
				if ((node.type & SearchQueryNode.Type.SUFFIX) != 0) {
					throw new UnsupportedQueryType("Mg4j does not support suffix search");
				}
				q = new Prefix(node.str);
			}
			return new Select(key, q);
		}
		else if ((node.type & SearchQueryNode.Type.BOOLEAN) != 0) {
			Query[] subq = new Query[node.children.length];
			for(int i = 0, s = node.children.length; i < s; ++i) {
				subq[i] = toQuery(node.children[i]);
			}
			if ((node.type & SearchQueryNode.Type.AND) != 0) {
				return new And(subq);
			}
			else if ((node.type & SearchQueryNode.Type.OR) != 0) {
				return new Or(subq);
			}
			else if ((node.type & SearchQueryNode.Type.DIFF) != 0) {
				throw new UnsupportedQueryType("Mg4j does not support set difference operation");
				//Difference is only supported if frequencies are there
//				return new Difference(subq[0], subq[1]);
			}
			else {
				return null;
			}
		}
		else {
			return null;
		}
	}
}
