package imj3.draft.segmentation;

import static imj3.core.Channels.Predefined.blue8;
import static imj3.core.Channels.Predefined.green8;
import static imj3.core.Channels.Predefined.red8;
import static java.lang.Math.abs;
import static net.sourceforge.aprog.tools.Tools.cast;
import imj3.draft.segmentation.QuantizerNode.UserObject;
import imj3.draft.segmentation.QuantizerNode.Visitor;

import java.util.Arrays;
import java.util.Locale;

import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-01-16)
 */
public final class QuantizerPrototype extends QuantizerNode {
	
	private int[] data = new int[0];
	
	@Override
	public final QuantizerPrototype copy() {
		final QuantizerPrototype result = new QuantizerPrototype();
		
		result.data = this.data.clone();
		
		return this.copyChildrenTo(result).setUserObject();
	}
	
	@Override
	public final QuantizerPrototype setUserObject() {
		this.setUserObject(this.new UserObject() {
			
			@Override
			public final String toString() {
				return QuantizerPrototype.this.getDataAsString();
			}
			
			private static final long serialVersionUID = 4617070174363518324L;
			
		});
		
		return this;
	}
	
	public final int[] getData() {
		final Quantizer root = cast(Quantizer.class, this.getRoot());
		
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
	
	public final QuantizerPrototype setData(final String dataAsString) {
		final int[] parsed = Arrays.stream(dataAsString.split(",")).mapToInt(QuantizerNode::parseARGB).toArray();
		
		System.arraycopy(parsed, 0, this.getData(), 0, this.getData().length);
		
		return this;
	}
	
	@Override
	public final QuantizerCluster getParent() {
		return (QuantizerCluster) super.getParent();
	}
	
	public final Quantizer getQuantizer() {
		return this.getParent().getParent();
	}
	
	public final double distanceTo(final int[] values) {
		final int n = values.length;
		
		if (n != this.getData().length) {
			throw new IllegalArgumentException();
		}
		
		double result = 0.0;
		
		for (int i = 0; i < n; ++i) {
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