package imj3.draft.segmentation;

import static imj3.core.Channels.Predefined.a8r8g8b8;
import static imj3.core.Channels.Predefined.blue8;
import static imj3.core.Channels.Predefined.green8;
import static imj3.core.Channels.Predefined.red8;
import static java.lang.Math.abs;
import static net.sourceforge.aprog.tools.Tools.cast;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Locale;

import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-01-16)
 */
public final class ClassifierRawPrototype extends ClassifierPrototype {
	
	private int[] data = new int[0];
	
	@Override
	public final ClassifierRawPrototype copy() {
		final ClassifierRawPrototype result = new ClassifierRawPrototype();
		
		result.data = this.data.clone();
		
		return this.copyChildrenTo(result).setUserObject();
	}
	
	@Override
	public final ClassifierRawPrototype setUserObject() {
		this.setUserObject(this.new UserObject() {
			
			@Override
			public final String toString() {
				return ClassifierRawPrototype.this.getDataAsString();
			}
			
			private static final long serialVersionUID = 4617070174363518324L;
			
		});
		
		return this;
	}
	
	@Override
	public final int[] getData() {
		final Classifier root = cast(Classifier.class, this.getRoot());
		
		if (root == null) {
			return null;
		}
		
		final int n = root.getScale() * root.getScale();
		
		if (this.data.length != n) {
			this.data = new int[n];
		}
		
		return this.data;
	}
	
	@Override
	public final ClassifierPrototype setData(final double[] elements) {
		final int[] data = this.getData();
		
		for (int k = 0; k < data.length; ++k) {
			data[k] = a8r8g8b8(0xFF,
					(int) elements[3 * k + 0],
					(int) elements[3 * k + 1],
					(int) elements[3 * k + 2]);
		}
		
		return this;
	}
	
	@Override
	public final String getDataAsString() {
		return Tools.join(",", Arrays.stream(this.getData()).mapToObj(
				i -> "#" + Integer.toHexString(i).toUpperCase(Locale.ENGLISH)).toArray());
	}
	
	@Override
	public final ClassifierRawPrototype setData(final String dataAsString) {
		final int[] parsed = Arrays.stream(dataAsString.split(",")).mapToInt(ClassifierNode::parseARGB).toArray();
		
		System.arraycopy(parsed, 0, this.getData(), 0, this.getData().length);
		
		return this;
	}
	
	@Override
	public final double distanceTo(final int[] values, final double limit) {
		final int n = values.length;
		
		if (n != this.getData().length) {
			throw new IllegalArgumentException();
		}
		
		double result = 0.0;
		
		for (int i = 0; i < n && result < limit; ++i) {
			final int thisRGB = this.data[i];
			final int thatRGB = values[i];
			result += abs(red8(thisRGB) - red8(thatRGB))
					+ abs(green8(thisRGB) - green8(thatRGB))
					+ abs(blue8(thisRGB) - blue8(thatRGB));
		}
		
		return result;
	}
	
	@Override
	public final <V> V accept(final Visitor<V> visitor) {
		return visitor.visit(this);
	}
	
	private static final long serialVersionUID = 946728342547485375L;
	
	public static final Factory FACTORY = new Factory();
	
	/**
	 * @author codistmonk (creation 2015-02-04)
	 */
	public static final class Factory implements ClassifierPrototype.Factory {
		
		@Override
		public final ClassifierRawPrototype newPrototype() {
			return new ClassifierRawPrototype();
		}
		
		@Override
		public final int[] allocateDataBuffer(final int scale, final int[] old) {
			final int n = scale * scale;
			
			return old == null || old.length != n ? new int[n] : old;
		}
		
		@Override
		public final void extractData(final BufferedImage image, final int x, final int y, final int scale,
				final int[] result) {
			Arrays.fill(result, 0);
			
			final int width = image.getWidth();
			final int height = image.getHeight();
			final int s = scale / 2;
			final int left = x - s;
			final int right = left + scale;
			final int top = y - s;
			final int bottom = top + scale;
			
			for (int yy = top, i = 0; yy < bottom; ++yy) {
				if (0 <= yy && yy < height) {
					for (int xx = left; xx < right; ++xx, ++i) {
						if (0 <= xx && xx < width) {
							result[i] = image.getRGB(xx, yy);
						}
					}
				} else {
					i += scale;
				}
			}
		}
		
		private static final long serialVersionUID = 2715848951797691503L;
		
	}
	
}