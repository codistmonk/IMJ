package imj2.core;

/**
 * @author codistmonk (creation 2013-08-04)
 */
public abstract interface Image2D extends Image {
	
	public abstract int getWidth();
	
	public abstract int getHeight();
	
	public abstract int getPixelValue(int x, int y);
	
	public abstract void setPixelValue(int x, int y, int value);
	
}
