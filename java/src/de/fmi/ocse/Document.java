package de.fmi.ocse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

import de.funroll_loops.oscar.OsmItem;
import de.funroll_loops.oscar.OsmKeyValueObjectStore;

public class Document implements Serializable {

	static final boolean lowercase_values = IndexConfig.lowercase_values;
	static final boolean lowercase_tags = IndexConfig.lowercase_tags;

	//possible field suffixes are: value, tag
	//possible field names are: all, item, region
	static final public int field_idx_item_value = 0;
	static final public int field_idx_item_tag = 1;
	static final public int field_idx_region_value = 2;
	static final public int field_idx_region_tag = 3;
	static final public int field_idx_all_value = 4;
	static final public int field_idx_all_tag = 5;
	static final public int number_of_fields = 6;
	static final public int default_field = 4;

	static final public int[] field_idx_iterate = {0, 1, 2, 3, 4, 5};
	static final public String[] field_name = {
			"item_value", "item_tag",
			"region_value", "region_tag",
			"all_value", "all_tag"
	};

	static final public int field_separator_int = 0xE000;
	static final public String field_separator_string = "\uE000";
	static final public char field_separator_char = '\uE000';
	
	public int id;
	public ArrayList<String>[] fields;
	
	@SuppressWarnings("unchecked")
	Document(IndexConfig idxCfg, OsmKeyValueObjectStore store, int _id) {
		fields = (ArrayList<String>[]) new ArrayList[number_of_fields];
		for(int i = 0; i < fields.length; ++i) {
			fields[i] = new ArrayList<String>();
		}
		id = _id;
		OsmItem item = store.at(id);
		add(idxCfg.itemImportantTags, item, false);
		int[] ancestors = item.ancestors();
		for(int ancestorId : ancestors) {
			add(idxCfg.regionImportantTags, store.at(ancestorId), true);
		}
	}
	
	public int fieldSize(int fieldIdx) {
		return fields[fieldIdx].size();
	}
	
	public int summedSize() {
		int count = 0;
		for(int i = 0; i < fields.length; ++i) {
			count += fields[i].size();
		}
		return count;
	}
	
	//This converts all of the single values of field with index fieldIdx into a single string
	//this single values are separated by the field_separator
	//(an unicode point which lies in the private use area)
	public String toString(int fieldIdx) {
		if (fields[fieldIdx].size() > 0) {
			String ret = this.fields[fieldIdx].get(0);
			for(int i = 1, s = this.fields[fieldIdx].size(); i < s; ++i) {
				ret += field_separator_string + fields[fieldIdx].get(i);
			}
			return ret;
		}
		return new String();
	}
	
	public String toString() {
		String str = "";
		boolean hasPrev = false;
		for(int i = 0; i < fields.length; ++i) {
			if (fieldSize(i) > 0) {
				if (hasPrev) {
					str += "\n";
				}
				str += field_name[i] + "=" + toString(i);
				hasPrev = true;
			}
		}
		return str;
	}

	private void add(HashSet<Integer> importantTags, OsmItem item, boolean insertAsRegion) {
		int fieldBasePos = (insertAsRegion ? field_idx_region_value : field_idx_item_value);
		for(int i = 0, s = item.size(); i < s; ++i) {
			int keyId = item.keyId(i);
			if (importantTags.contains(keyId)) {
				String v = item.value(i);
				if (v.length() > 0) {
					if (lowercase_values) {
						v = v.toLowerCase();
					}
					fields[fieldBasePos].add( v );
					fields[field_idx_all_value].add( v );
				}
			}
			{
				String kv = item.key(i) + ":" + item.value(i);
				if (kv.length() > 0) {
					if (lowercase_tags) {
						kv = kv.toLowerCase();
					}
					fields[fieldBasePos+1].add( kv );
					fields[field_idx_all_tag].add( kv );
				}
			}
		}
	}
}
