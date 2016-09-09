package de.fmi.ocse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.regex.Pattern;

import de.funroll_loops.oscar.OsmKeyValueObjectStore;
import de.funroll_loops.oscar.StringTable;

public class IndexConfig {
	private Path m_itemCfgPath;
	private Path m_regionCfgPath;
	
	public HashSet<Integer> itemImportantTags;
	public HashSet<Integer> regionImportantTags;

	public static final boolean lowercase_values = true;
	public static final boolean lowercase_tags = true;
	
	Pattern regExpr(Path path) throws IOException {
		File fin = new File(path.toString());
		FileInputStream fis = new FileInputStream(fin);
		 
		//Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
	 
		String ps = "(";
		String line = null;
		int count = 0;
		while ((line = br.readLine()) != null) {
			if (count != 0) {
				ps += "|";
			}
			++count;
			ps += "(" + line + ")";
		}
		ps += ")";
	 
		br.close();
		
		return Pattern.compile(ps);
	}
	
	IndexConfig(Path itemCfgPath, Path regionCfgPath) {
		m_itemCfgPath = itemCfgPath;
		m_regionCfgPath = regionCfgPath;
		itemImportantTags = new HashSet<Integer>();
		regionImportantTags = new HashSet<Integer>();
	}
	
	void init(OsmKeyValueObjectStore store) throws IOException {
		assert(store != null);
		assert(m_itemCfgPath != null);
		assert(m_regionCfgPath != null);
		Pattern itemPattern = regExpr(m_itemCfgPath);
		Pattern regionPattern = regExpr(m_regionCfgPath);
		StringTable keyStringTable = store.keyStringTable();
		assert(keyStringTable != null);
		for(int i = 0, s = keyStringTable.size(); i < s; ++i) {
			String str = keyStringTable.at(i);
			assert(str != null);
			if (itemPattern.matcher(str).matches()) {
				itemImportantTags.add(i);
			}
			if (regionPattern.matcher(str).matches()) {
				regionImportantTags.add(i);
			}
		}
		System.out.println("Found " + itemImportantTags.size() + " matching keys for items");
		System.out.println("Found " + regionImportantTags.size() + " matching keys for regions");
	}
	
}
