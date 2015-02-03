package imj3.draft.segmentation;

import static imj3.core.Channels.Predefined.blue8;
import static imj3.core.Channels.Predefined.green8;
import static imj3.core.Channels.Predefined.red8;
import static java.lang.Math.abs;
import static net.sourceforge.aprog.tools.Tools.cast;

import java.util.Arrays;
import java.util.Locale;

import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-01-16)
 */
public final class ClassifierRawPrototype extends ClassifierNode {
	
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
	
	public final String getDataAsString() {
		return Tools.join(",", Arrays.stream(this.getData()).mapToObj(
				i -> "#" + Integer.toHexString(i).toUpperCase(Locale.ENGLISH)).toArray());
	}
	
	public final ClassifierRawPrototype setData(final String dataAsString) {
		final int[] parsed = Arrays.stream(dataAsString.split(",")).mapToInt(ClassifierNode::parseARGB).toArray();
		
		System.arraycopy(parsed, 0, this.getData(), 0, this.getData().length);
		
		return this;
	}
	
	@Override
	public final ClassifierCluster getParent() {
		return (ClassifierCluster) super.getParent();
	}
	
	public final Classifier getClassifier() {
		return this.getParent().getParent();
	}
	
	public final double distanceTo(final int[] values, final double maximum) {
		final int n = values.length;
		
		if (n != this.getData().length) {
			throw new IllegalArgumentException();
		}
		
		double result = 0.0;
		
		for (int i = 0; i < n && result < maximum; ++i) {
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
	
}