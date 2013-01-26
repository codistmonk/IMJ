package imj;

import static java.lang.System.currentTimeMillis;

/**
 * @author codistmonk (creation 2013-01-25)
 */
public final class TicToc {
	
	private long t0;
	
	public final long tic() {
		return this.t0 = currentTimeMillis();
	}
	
	public final long toc() {
		return currentTimeMillis() - this.t0;
	}
	
}
