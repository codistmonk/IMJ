package imj2.core;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author codistmonk (creation 2013-08-04)
 */
public abstract interface Image extends Serializable {
	
	public abstract String getId();
	
	public abstract long getPixelCount();
	
	public abstract Channels getChannels();
	
	public abstract int getPixelValue(long pixelIndex);
	
	public abstract void setPixelValue(long pixelIndex, int pixelValue);
	
	public abstract Image[] newParallelViews(int n);
	
	public abstract Image getSource();
	
	public abstract AtomicLong getTimestamp();
	
	/**
	 * @author codistmonk (creation 2013-08-04)
	 */
	public static abstract interface Channels extends Serializable {
		
		public abstract int getChannelCount();
		
		public abstract int getChannelBitCount();
		
		public abstract int getChannelValue(int pixelValue, int channelIndex);
		
	}
	
	/**
	 * @author codistmonk (creation 2013-08-07)
	 */
	public static abstract interface Process extends Serializable {
		
		public abstract void endOfPatch();
		
	}
	
	/**
	 * @author codistmonk (creation 2013-08-07)
	 */
	public static interface Traversing<I extends Image, P extends Process> extends Serializable {
		
		public abstract void forEachPixelIn(I image, P process);
		
	}
	
	/**
	 * @author codistmonk (creation 2014-05-03)
	 */
	public static final class Monochannel implements Channels {
		
		private final int bitCount;
		
		public Monochannel(final int bitCount) {
			this.bitCount = bitCount;
		}
		
		@Override
		public final int getChannelCount() {
			return 1;
		}
		
		@Override
		public final int getChannelBitCount() {
			return this.bitCount;
		}
		
		@Override
		public final int getChannelValue(final int pixelValue, final int channelIndex) {
			return PredefinedChannels.C1_U1.getChannelValue(pixelValue, channelIndex);
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -7375032451555035569L;
		
	}
	
	/**
	 * @author codistmonk (creation 2013-08-04)
	 */
	public static enum PredefinedChannels implements Channels {
		
		C1_U1 {
			
			@Override
			public final int getChannelCount() {
				return 1;
			}
			
			@Override
			public final int getChannelBitCount() {
				return 1;
			}
			
			@Override
			public final int getChannelValue(final int pixelValue, final int channelIndex) {
				return channelIndex == 0 ? pixelValue : 0;
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
			
			@Override
			public final int getChannelValue(final int pixelValue, final int channelIndex) {
				return C1_U1.getChannelValue(pixelValue, channelIndex);
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
			
			@Override
			public final int getChannelValue(final int pixelValue, final int channelIndex) {
				return C1_U1.getChannelValue(pixelValue, channelIndex);
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
			
			@Override
			public final int getChannelValue(final int pixelValue, final int channelIndex) {
				return C1_U1.getChannelValue(pixelValue, channelIndex);
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
			
			@Override
			public final int getChannelValue(final int pixelValue, final int channelIndex) {
				return this.defaultGetChannelValue(pixelValue, channelIndex);
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
			
			@Override
			public final int getChannelValue(final int pixelValue, final int channelIndex) {
				return this.defaultGetChannelValue(pixelValue, channelIndex);
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
			
			@Override
			public final int getChannelValue(final int pixelValue, final int channelIndex) {
				return this.defaultGetChannelValue(pixelValue, channelIndex);
			}
			
		};
		
		public final boolean isIndexValid(final int index) {
			return 0 <= index && index < this.getChannelCount();
		}
		
		protected final int defaultGetChannelValue(final int pixelValue, final int channelIndex) {
			return this.isIndexValid(channelIndex) ? (pixelValue >> (this.getChannelBitCount() * channelIndex)) & bitmask(this.getChannelBitCount()) : 0;
		}
		
		public static final int bitmask(final int lowBitCount) {
			return ~((~0) << lowBitCount);
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-11-12)
	 */
	public static abstract class Abstract implements Image {
		
		private final AtomicLong timestamp;
		
		public Abstract() {
			this.timestamp = new AtomicLong(Long.MIN_VALUE);
		}
		
		@Override
		public final AtomicLong getTimestamp() {
			return this.timestamp;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -1522957828451615762L;
		
	}
	
}
