package imj2.tools;

import java.io.Serializable;
import java.net.URLConnection;

/**
 * @author codistmonk (creation 2013-11-06)
 */
public abstract interface ConnectionConfigurator extends Serializable {
	
	public abstract void configureConnection(URLConnection connection);
	
	public abstract void connectionFailed(URLConnection connection, Exception exception);
	
}

