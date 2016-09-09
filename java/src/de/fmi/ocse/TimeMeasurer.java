package de.fmi.ocse;

import java.util.concurrent.TimeUnit;

public class TimeMeasurer {
	protected long m_startTime = -1000000;
	protected long m_endTime = 0;

	public static long time() {
		return System.nanoTime();
	}

	public void start() {
		m_startTime = time();
	}
	
	public void stop() {
		m_endTime = time();
	}

	public long elapsed(TimeUnit unit) {
		return unit.convert(elapsedNsecs(), TimeUnit.NANOSECONDS);
	}

	public long elapsedNsecs() {
		assert(m_endTime > m_startTime);
		return m_endTime - m_startTime;
	}

	public long elapsedUsecs() {
		return elapsed(TimeUnit.MICROSECONDS);
	}
	
	public long elapsedMsecs() {
		return elapsed(TimeUnit.MILLISECONDS);
	}
}
