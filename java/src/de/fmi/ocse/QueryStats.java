package de.fmi.ocse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class QueryStats {
	private class Stats {
		double min;
		double max;
		double median;
		double mean;
		double sum;
		Stats() {
			min = 1000*1000*1000;
			max = 0;
			median = 0;
			mean = 0;
			sum = 0;
		}
		void normalize(double divisor) {
			min /= divisor;
			max /= divisor;
			median /= divisor;
			mean /= divisor;
			sum /= divisor;
		}
		public String toString(String prefix) {
			StringBuilder sb = new StringBuilder();
			sb.append(prefix + "min: " + min + "\n");
			sb.append(prefix + "max: " + max + "\n");
			sb.append(prefix + "sum: " + sum + "\n");
			sb.append(prefix + "mean: " + mean + "\n");
			sb.append(prefix + "median: " + median + "\n");
			return sb.toString();
		}
	}
	ArrayList<SingleResultStat>[] m_stats; 
	
	@SuppressWarnings("unchecked")
	QueryStats() {
		m_stats = new ArrayList[Config.SearchType.Last+1];
		for(int type : Config.SearchType.iterate) {
			m_stats[type] = new ArrayList<SingleResultStat>();
		}
	}
	
	public void add(int type, SingleResultStat stat) {
		m_stats[type].add(stat);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(int type : Config.SearchType.iterate) {
			if (m_stats[type].size() == 0) {
				continue;
			}
			double[] time = new double[m_stats[type].size()];
			double[] size = new double[m_stats[type].size()];
			int i = 0;
			for(SingleResultStat stat : m_stats[type]) {
				time[i] = stat.time;
				size[i] = stat.size;
				++i;
			}
			Stats timeStats = stats(time);
			Stats sizeStats = stats(size);
			timeStats.normalize(1000*1000);
			sb.append(Config.SearchType.names[type] + ":\n");
			sb.append("  Time [ms]:\n");
			sb.append(timeStats.toString("    "));
			sb.append("  Size:\n");
			sb.append(sizeStats.toString("    "));
		}
		return sb.toString();
	}
	public void export(String prefix) throws IOException {
		for(int type : Config.SearchType.iterate) {
			if (m_stats[type].size() == 0) {
				continue;
			}
			export(type, prefix + "." + Config.SearchType.names[type] + ".stats.raw");
		}
	}
	public void export(int type, String filename) throws IOException {
		if (m_stats.length <= type || m_stats[type].size() == 0) {
			System.out.println("No data for type=" + type);
			return;
		}
		File file = new File(filename);
		if (file.exists()) {
			System.out.println("Will overwrite " + filename);
		}
		else if (!file.createNewFile()) {
			throw new IOException("Could not create file " + filename);
		}
		if (!file.canWrite()) {
			throw new IOException("Unable to write to file " + filename);
		}
		FileWriter fw = new FileWriter(file);
		BufferedWriter writer = new BufferedWriter(fw);
		writer.write("Id;Time[us];Size[1]");
		writer.newLine();
		for(int i = 0, s = m_stats[type].size(); i < s; ++i) {
			writer.write(i + ";" + m_stats[type].get(i).time/1000 + ";" + m_stats[type].get(i).size);
			writer.newLine();
		}
		writer.flush();
		writer.close();
	}
	private Stats stats(double[] d) {
		Arrays.sort(d);
		Stats res = new Stats();
		for(double v : d) {
			res.sum += v;
			res.min = Math.min(res.min, v);
			res.max = Math.max(res.max, v);
		}
		if (d.length > 0) {
			res.mean = res.sum / d.length;
			res.median = d[d.length/2];
		} 
		return res;
	}
}
