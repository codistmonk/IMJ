package imj.apps.modules;

import static imj.IMJTools.blue;
import static imj.IMJTools.green;
import static imj.IMJTools.red;
import static java.lang.Double.parseDouble;
import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class LinearColorViewFilter extends ViewFilter {
	
	private final double[] rgbCoefficients;
	
	public LinearColorViewFilter(final Context context) {
		super(context);
		this.rgbCoefficients = new double[3];
		this.getParameters().put("rgbCoefficients", ".33 .33 .33");
	}
	
	@Override
	public final int getNewValue(final int index, final int oldValue, final Channel channel) {
		final int red = red(oldValue);
		final int green = green(oldValue);
		final int blue = blue(oldValue);
		final double kr = this.rgbCoefficients[0];
		final double kg = this.rgbCoefficients[1];
		final double kb = this.rgbCoefficients[2];
		
		return (int) (kr * red + kg * green + kb * blue);
	}
	
	@Override
	protected final void doInitialize() {
		final String[] rgbCoefficients = this.getParameters().get("rgbCoefficients").trim().split("\\s++");
		
		for (int i = 0; i < 3; ++i) {
			this.rgbCoefficients[i] = parseDouble(rgbCoefficients[i]);
		}
	}
	
	@Override
	protected final ComplexFilter newComplexFilter() {
		return this.new ComplexFilter() {
			// NOP
		};
	}
	
}
