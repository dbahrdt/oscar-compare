package de.fmi.ocse;
import org.apache.lucene.search.Query;

import java.util.Arrays;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;

public class LuceneSearchQuery implements SearchQueryInterface {
	Query m_q;
	
	Query query() {
		return m_q;
	}

	public String toString() {
		return m_q.toString();
	}

	@Override
	public void init(SearchQueryNode root) {
		m_q = toQuery(root);
	}
	
	Query toQuery(SearchQueryNode node) {
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
				q = new TermQuery(new Term(key, node.str));
			}
			else {
				String str = "";
				if ((node.type & SearchQueryNode.Type.SUFFIX) != 0) {
					str += "*";
				}
				str += node.str;
				if ((node.type & SearchQueryNode.Type.PREFIX) != 0) {
					str += "*";
				}
				q = new WildcardQuery(new Term(key, str));
			}
			return q;
		}
		else if ((node.type & SearchQueryNode.Type.BOOLEAN) != 0) {
			Query[] subq = new Query[node.children.length];
			for(int i = 0, s = node.children.length; i < s; ++i) {
				subq[i] = toQuery(node.children[i]);
			}
			if ((node.type & SearchQueryNode.Type.AND) != 0) {
				BooleanQuery.Builder bqb = new BooleanQuery.Builder();
				for(Query q : subq) {
					bqb.add(q, BooleanClause.Occur.MUST);
				}
				return bqb.build();
			}
			else if ((node.type & SearchQueryNode.Type.OR) != 0) {
				return new DisjunctionMaxQuery(Arrays.asList(subq), (float) 0.0);
			}
			else if ((node.type & SearchQueryNode.Type.DIFF) != 0) {
				BooleanQuery.Builder bqb = new BooleanQuery.Builder();
				bqb.add(subq[0], BooleanClause.Occur.MUST);
				bqb.add(subq[1], BooleanClause.Occur.MUST_NOT);
				return bqb.build();
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
