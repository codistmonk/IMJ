package imj3.core;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2014-11-29)
 */
public abstract interface Channels extends Serializable {
	
	public abstract int getChannelCount();
	
	public abstract int getChannelBitCount();
	
	public default long getChannelValue(final long pixelValue, final int channelIndex) {
		final int channelBitCount = this.getChannelBitCount();
		final long mask1 = ~((~0L) << channelBitCount);
		final int offset = channelIndex * channelBitCount;
		
		return (pixelValue >> offset) & mask1;
	}
	
	public default long setChannelValue(final long pixelValue, final int channelIndex, final long channelValue) {
		final int channelBitCount = this.getChannelBitCount();
		final int offset = channelIndex * channelBitCount;
		final long mask1 = (~((~0L) << channelBitCount)) << offset;
		
		return (pixelValue & ~mask1) | ((channelValue << offset) & mask1);
	}
	
	/**
	 * @author codistmonk (creation 2014-11-29)
	 */
	public static final class Default implements Channels {
		
		private final int channelCount;
		
		private final int channelBitCount;
		
		public Default(final int channelCount, final int channelBitCount) {
			this.channelCount = channelCount;
			this.channelBitCount = channelBitCount;
		}
		
		@Override
		public final int getChannelCount() {
			return this.channelCount;
		}
		
		@Override
		public final int getChannelBitCount() {
			return this.channelBitCount;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -8255656054588359355L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-11-29)
	 */
	public static enum Predefined implements Channels {
		
		A8R8G8B8 {
			
			@Override
			public final int getChannelCount() {
				return 4;
			}
			
			@Override
			public final int getChannelBitCount() {
				return 8;
			}
			
		};
		
		public static final int alpha8(final long pixelValue) {
			return (int) A8R8G8B8.getChannelValue(pixelValue, 3);
		}
		
		public static final int red8(final long pixelValue) {
			return (int) A8R8G8B8.getChannelValue(pixelValue, 2);
		}
		
		public static final int green8(final long pixelValue) {
			return (int) A8R8G8B8.getChannelValue(pixelValue, 1);
		}
		
		public static final int blue8(final long pixelValue) {
			return (int) A8R8G8B8.getChannelValue(pixelValue, 0);
		}
		
	}
	
}
