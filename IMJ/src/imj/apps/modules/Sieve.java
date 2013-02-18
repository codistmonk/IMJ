package imj.apps.modules;

import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public abstract class Sieve extends Plugin {
	
	protected Sieve(final Context context) {
		super(context);
	}
	
	public abstract boolean accept(int x, int y, int value);
	
}
