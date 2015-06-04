package imj3.core;

import static java.lang.Math.max;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2014-11-29)
 */
public abstract interface Channels extends Serializable {
	
	public abstract int getChannelCount();
	
	public abstract int getChannelBitCount();
	
	public default int getValueBitCount() {
		return this.getChannelCount() * this.getChannelBitCount();
	}
	
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
			
		}, C1_U1 {
			
			@Override
			public final int getChannelCount() {
				return 1;
			}
			
			@Override
			public final int getChannelBitCount() {
				return 1;
			}
			
		}, C1_U8 {
			
			@Override
			public final int getChannelCount() {
				return 1;
			}
			
			@Override
			public final int getChannelBitCount() {
				return 8;
			}
			
		}, C1_U16 {
			
			@Override
			public final int getChannelCount() {
				return 1;
			}
			
			@Override
			public final int getChannelBitCount() {
				return 16;
			}
			
		}, C1_S32 {
			
			@Override
			public final int getChannelCount() {
				return 1;
			}
			
			@Override
			public final int getChannelBitCount() {
				return 32;
			}
			
		}, C2_U16 {
			
			@Override
			public final int getChannelCount() {
				return 2;
			}
			
			@Override
			public final int getChannelBitCount() {
				return 16;
			}
			
		}, C3_U8 {
			
			@Override
			public final int getChannelCount() {
				return 3;
			}
			
			@Override
			public final int getChannelBitCount() {
				return 8;
			}
			
		}, C4_U8 {
			
			@Override
			public final int getChannelCount() {
				return 4;
			}
			
			@Override
			public final int getChannelBitCount() {
				return 8;
			}
			
		};
		
		public static final int uint8(final long value) {
			return (int) (value & 0xFF);
		}
		
		public static final int a8r8g8b8(final int alpha8, final int red8, final int green8, final int blue8) {
			return (alpha8 << 24) | (red8 << 16) | (green8 << 8) | (blue8 << 0);
		}
		
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
		
		public static final int lightness(final long pixelValue) {
			return max(max(red8(pixelValue), green8(pixelValue)), blue8(pixelValue));
		}
		
	}
	
}
