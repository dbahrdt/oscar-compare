//@formatter:off

package de.fmi.ocse;

import org.apache.commons.configuration.ConfigurationException;import java.io.IOException;import java.lang.reflect.InvocationTargetException;import java.net.URISyntaxException;

class Main {
	static {
		try {
			System.loadLibrary("joscar");
		}
		catch (UnsatisfiedLinkError e) {
			System.err.println(e);
			String libPath = System.getProperty("java.library.path");
			System.err.println("java.library.path=" + libPath + "\n");
			System.exit(1);
		}
	}

	public static void main(String[] args)throws IllegalAccessException, URISyntaxException, InstantiationException, ConfigurationException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, IOException {
		Config cfg = new Config();
		if (cfg.parse(args) < 0) {
			System.out.println("Error parsing command line options");
			System.exit(1);
		}
		
		if (cfg.oscarPath == null) {
			System.out.println("No input file specified");
			System.exit(1);
		}

		System.out.println("---------Config--------");
		cfg.print();
		System.out.println("-----------------------");

		Worker w = new Worker();
		
		System.out.println("Init worker");
		try {
			w.init(cfg);
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		if (cfg.queriesPath != null) {
			System.out.println("Loading queries");
			w.loadQueries();
			System.out.println("Loaded " + w.queryCount() + " queries");
		}
		System.out.println("Indexing");
		try {
			w.index();
		}
		catch (IOException e) {
			e.printStackTrace();
			return;
		}
		System.out.println("Indexing completed");
		if (cfg.activeSearches > 0) {
			if (cfg.calcDiffs) {
				System.out.println("Calculating query differences");
				w.queryDifferenceToOscar();
				System.out.println("Calculating query differences completed");
			}
			else {
				System.out.println("Querying");
				QueryStats qs = null;
				try {
					qs = w.query();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println("Querying completed");
				System.out.println(qs.toString());
				if (cfg.statsOutPath != null) {
					qs.export(cfg.statsOutPath.toString());
				}
			}
		}
	}
}