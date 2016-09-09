package de.fmi.ocse;

import java.nio.file.Path;

public class Config {
	static public class SearchType {
		static public final int Lucene = 1;
		static public final int Mg4j = 2;
		static public final int Oscar = 4;
		static public final int Last = Oscar;
		static public int[] iterate = {
				Lucene, Mg4j, Oscar
		};
		static public String[] names = {
			"INVALID", //0
			"Lucene", //1
			"Mg4j", //2
			"INVALID", //3
			"Oscar" //4
		};
	}
	
	public Path oscarPath;
	public Path lucenePath;
	public Path mg4jPath;
	public Path queriesPath;
	public Path statsOutPath;
	int activeSearches = 0;
	int buildSearches = 0;
	boolean calcDiffs = false;
	IndexConfig idxConfig;
	
	public static void help() {
		System.out.println("prg");
		System.out.println("--lucene --lucene-build --lucene-path dir");
		System.out.println("--mg4j --mg4j-build --mg4j-path dir");
		System.out.println("--oscar");
		System.out.println("--help -h");
		System.out.println("-q <file>\tQueries to process");
		System.out.println("-qd\tCalculate difference to oscar");
		System.out.println("-i item-cfg-file -r region-cfg-file");
		System.out.println("--stats-out-prefix <path>");
		System.out.println("oscar-dir");
	}
	
	public void print() {
		String activeS = "";
		String buildS = "";
		int anyActive = buildSearches | activeSearches;
		if ((activeSearches & SearchType.Lucene) != 0) {
			activeS += "lucene ";
		}
		if ((activeSearches & SearchType.Mg4j) != 0) {
			activeS += "mg4j ";
		}
		if ((activeSearches & SearchType.Oscar) != 0) {
			activeS += "oscar";
		}
		if ((buildSearches & SearchType.Lucene) != 0) {
			buildS += "lucene ";
		}
		if ((buildSearches & SearchType.Mg4j) != 0) {
			buildS += "mg4j ";
		}
		if ((buildSearches & SearchType.Oscar) != 0) {
			buildS += "oscar";
		}
		System.out.println("Base dir: " + oscarPath.toString());
		System.out.println("Active searches: " + activeS);
		System.out.println("Build searches: " + buildS);
		if ((anyActive & SearchType.Lucene) != 0) {
			System.out.println("Lucene dir: " + lucenePath.toString());
		}
		if ((anyActive & SearchType.Mg4j) != 0) {
			System.out.println("Mg4j dir: " + mg4jPath.toString());
		}
		if (queriesPath != null) {
			System.out.println("Queries file: " + queriesPath.toString());
		}
		if (statsOutPath != null) {
			System.out.println("Stats out path:" + statsOutPath.toString());
		}
	}
	
	public Config() {
	}
		
	public int parse(String[] args) {
		
		if (args.length == 0) {
			help();
			return -1;
		}
		Path itemCfgPath = null;
		Path regionCfgPath = null;
		
		for(int i = 0; i < args.length; ++i) {
			String arg = args[i];
			if (arg.equals("--lucene")) {
				activeSearches |= SearchType.Lucene;
			}
			else if(arg.equals("--mg4j")) {
				activeSearches |= SearchType.Mg4j;
			}
			else if(arg.equals("--oscar")) {
				activeSearches |= SearchType.Oscar;
			}
			else if (arg.equals("--lucene-build")) {
				buildSearches |= SearchType.Lucene;
			}
			else if(arg.equals("--mg4j-build")) {
				buildSearches |= SearchType.Mg4j;
			}
			else if(arg.equals("--oscar-build")) {
				buildSearches |= SearchType.Oscar;
			}
			else if (arg.equals("--lucene-path") && i+1 < args.length) {
				lucenePath = java.nio.file.Paths.get(args[i+1]);
				++i;
			}
			else if (arg.equals("--mg4j-path") && i+1 < args.length) {
				mg4jPath = java.nio.file.Paths.get(args[i+1]);
				++i;
			}
			else if (arg.equals("-i") && i+1 < args.length) {
				itemCfgPath = java.nio.file.Paths.get(args[i+1]);
				++i;
			}
			else if (arg.equals("-r") && i+1 < args.length) {
				regionCfgPath = java.nio.file.Paths.get(args[i+1]);
				++i;
			}
			else if (arg.equals("-q") && i+1 < args.length) {
				queriesPath = java.nio.file.Paths.get(args[i+1]);
				++i;
			}
			else if (arg.equals("--stats-out-prefix") && i+1 < args.length) {
				statsOutPath = java.nio.file.Paths.get(args[i+1]);
				++i;
			}
			else if (arg.equals("-qd")) {
				calcDiffs = true;
			}
			else if (arg.equals("--help") || arg.equals("-h")) {
				help();
				oscarPath = null;
				return -1;
			}
			else {
				oscarPath = java.nio.file.Paths.get(arg);
			}
		}
		if (calcDiffs && activeSearches > 0) {
			activeSearches |= SearchType.Oscar;
		}
		if (buildSearches > 0) {
			if (itemCfgPath == null && regionCfgPath == null) {
				oscarPath = null;
				System.out.println("No itemCfg or regionCfg given");
				return -1;
			}
			else if (itemCfgPath == null) {
				itemCfgPath = regionCfgPath;
			}
			else if (regionCfgPath == null) {
				regionCfgPath = itemCfgPath;
			}
			idxConfig = new IndexConfig(itemCfgPath, regionCfgPath);
		}
		return 0;
	}
}
