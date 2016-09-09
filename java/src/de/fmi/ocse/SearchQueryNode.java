package de.fmi.ocse;

import java.util.ArrayList;
import org.w3c.dom.NodeList;

public class SearchQueryNode {
	static public class Type {
		public static final int AND = 1;
		public static final int OR = 2;
		public static final int DIFF = 4;
		public static final int BOOLEAN = 7;
		public static final int STR = 8;
		public static final int TAG = 16;
		public static final int PREFIX = 32;
		public static final int SUFFIX = 64;
		public static final int EXACT = 128;
		public static final int ITEM = 256;
		public static final int REGION = 512;
	}
	public int type;
	public String str;
	public SearchQueryNode[] children;
	static private boolean isTrue(org.w3c.dom.NamedNodeMap nnm, String name) {
		org.w3c.dom.Node node = nnm.getNamedItem(name);
		return node != null && (node.getNodeValue().equals("true") || node.getNodeValue().equals("True"));
	}
	static public SearchQueryNode fromXml(org.w3c.dom.Node node) {
		assert(node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE);
		String type = node.getNodeName();
		SearchQueryNode res = new SearchQueryNode();
		if (type == "str" || type == "tag") {
			org.w3c.dom.NamedNodeMap attrs = node.getAttributes();
			res.str = node.getAttributes().getNamedItem("q").getNodeValue();
			if (type == "str") {
				res.type = SearchQueryNode.Type.STR;
			}
			else {
				res.type = SearchQueryNode.Type.TAG;
			}
			if (isTrue(attrs, "exact")) {
				res.type |= SearchQueryNode.Type.EXACT;
			}
			if (isTrue(attrs, "prefix")) {
				res.type |= SearchQueryNode.Type.PREFIX;
			}
			if (isTrue(attrs, "suffix")) {
				res.type |= SearchQueryNode.Type.SUFFIX;
			}
			if (isTrue(attrs, "item")) {
				res.type |= SearchQueryNode.Type.ITEM;
			}
			if (isTrue(attrs, "region")) {
				res.type |= SearchQueryNode.Type.REGION;
			}
			if ((res.type & SearchQueryNode.Type.TAG) != 0) {
				res.type = res.type & ~Type.SUFFIX;
				if (IndexConfig.lowercase_tags) {
					res.str = res.str.toLowerCase();
				}
			}
			else if (IndexConfig.lowercase_values) {
				res.str = res.str.toLowerCase();
			}
		}
		else {
			ArrayList<SearchQueryNode> children = new ArrayList<SearchQueryNode>();
			NodeList nodeChildren = node.getChildNodes();
			res.children = new SearchQueryNode[node.getChildNodes().getLength()];
			for(int i = 0, s = nodeChildren.getLength(); i < s; ++i) {
				org.w3c.dom.Node childNode = nodeChildren.item(i);
				if (childNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
					children.add( fromXml(childNode) );
				}
			}
			res.children = children.toArray(new SearchQueryNode[children.size()]);
			if (type == "and") {
				res.type = SearchQueryNode.Type.AND;
			}
			else if (type == "or") {
				res.type = SearchQueryNode.Type.OR;
			}
			else if (type == "diff") {
				res.type = SearchQueryNode.Type.DIFF;
				if (res.children.length != 2) {
					throw new java.lang.RuntimeException("DIFF only allows 2 operands!");
				}
			}
			else {
				throw new java.lang.RuntimeException("Found invalid node type=" + type);
			}
		}
		return res;
	}
	///returns this node as string with syntax for oscar
	public String toString() {
		if ((type & (Type.AND | Type.OR | Type.DIFF)) != 0) {
			StringBuilder sb = new StringBuilder();
			char sep = '0';
			if ((type & Type.AND) != 0) {
				sep = '/';
			}
			else if ((type & Type.OR) != 0) {
				sep = '+';
			}
			else if ((type & Type.DIFF) != 0) {
				sep = '-';
			}
			sb.append('(');
			sb.append(' ');
			boolean hasPrev = false;
			for(int i = 0; i < children.length; ++i) {
				if (hasPrev) {
					sb.append(' ');
					sb.append(sep);
					sb.append(' ');
				}
				sb.append(children[i].toString());
				hasPrev = true;
			}
			sb.append(' ');
			sb.append(')');
			return sb.toString();
		}
		else if ((type & (Type.STR | Type.TAG)) != 0) {
			String tmp = "";
			if ((type & (Type.ITEM | Type.REGION)) != 0) {
				;
			}
			else if ((type & Type.ITEM) != 0) {
				tmp += "!";
			}
			else if ((type & Type.REGION) != 0) {
				tmp += "#";
			}
			if ((type & Type.SUFFIX) != 0) {
				tmp += "?";
			}
			tmp += "\"";
			if ((type & Type.TAG) != 0) {
				tmp += "@";
			}
			tmp += str;
			tmp += "\"";
			if ((type & Type.PREFIX) != 0) {
				tmp += "?";
			}
			return "(" + tmp + ")";
		}
		else {
			return "INVALID";
		}
	}
}
