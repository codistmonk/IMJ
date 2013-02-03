package imj;

import static java.lang.System.currentTimeMillis;

/**
 * @author codistmonk (creation 2013-01-25)
 */
public final class TicToc {
	
	private long totalTime;
	
	private long ticTocTime;
	
	private long t0;
	
	public final long tic() {
		this.totalTime += this.ticTocTime;
		this.ticTocTime = 0L;
		return this.t0 = currentTimeMillis();
	}
	
	public final long toc() {
		this.ticTocTime = currentTimeMillis() - this.t0;
		
		return this.ticTocTime;
	}
	
	public final long getTotalTime() {
		return this.totalTime + this.ticTocTime;
	}
	
}
