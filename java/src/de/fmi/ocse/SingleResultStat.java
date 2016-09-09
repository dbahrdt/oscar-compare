package de.fmi.ocse;


public class SingleResultStat {
	SingleResultStat(SearchResultInterface result, long elapsedTime) {
		this.size = result.size();
		this.time = elapsedTime;
	}
	public int size;
	public long time;
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Time [ms]: " + this.time/(1000*1000) + "\n");
		sb.append("Size: " +  this.size);
		return sb.toString();
	}
}
