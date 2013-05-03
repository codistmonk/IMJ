package imj.database;

import imj.IMJTools.PixelProcessor;
import imj.Image;

/**
 * @author codistmonk (creation 2013-05-03)
 */
public abstract interface Segmenter {
	
	public abstract void process(Image image, PixelProcessor processor);
	
}

