package imj2.core;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2013-08-04)
 */
public abstract interface Image extends Serializable {
	
	public abstract String getId();
	
	public abstract long getPixelCount();
	
	public abstract Channels getChannels();
	
	public abstract int getPixelValue(long pixelIndex);
	
	public abstract void setPixelValue(long pixelIndex, int pixelValue);
	
	/**
	 * @author codistmonk (creation 2013-08-04)
	 */
	public static abstract interface Channels extends Serializable {
		
		public abstract int getChannelCount();
		
		public abstract int getChannelValue(int pixelValue, int channelIndex);
		
	}
	
	/**
	 * @author codistmonk (creation 2013-08-04)
	 */
	public static enum PredefinedChannels implements Channels {
		
		C1 {
			
			@Override
			public final int getChannelCount() {
				return 1;
			}
			
			@Override
			public final int getChannelValue(final int pixelValue, final int channelIndex) {
				return channelIndex == 0 ? pixelValue : 0;
			}
			
		}, C2 {
			
			@Override
			public final int getChannelCount() {
				return 2;
			}
			
			@Override
			public final int getChannelValue(final int pixelValue, final int channelIndex) {
				return (channelIndex == 0 ? pixelValue : (pixelValue >> 16)) & 0x0000FFFF;
			}
			
		}, C4 {
			
			@Override
			public final int getChannelCount() {
				return 4;
			}
			
			@Override
			public final int getChannelValue(final int pixelValue, final int channelIndex) {
				return (pixelValue >> (channelIndex * 8)) & 0x000000FF;
			}
			
		};
		
	}
	
}
