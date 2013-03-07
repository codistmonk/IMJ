package imj.apps.modules;

import static imj.IMJTools.alpha;
import static imj.IMJTools.argb;
import static imj.IMJTools.blue;
import static imj.IMJTools.green;
import static imj.IMJTools.red;
import static java.lang.Double.parseDouble;
import imj.apps.modules.FilteredImage.StructuringElementFilter;
import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class RegionViewFilter extends ViewFilter.FromFilter {
	
	public RegionViewFilter(final Context context) {
		super(context);
		
		this.getParameters().remove("channels");
		this.getParameters().put("structuringElement", "disk 1 chessboard");
		this.getParameters().put("rgbCoefficients", "2 -6 4");
		this.getParameters().put("threshold", "128");
		this.getParameters().put("rejectionModulation", "0.5");
	}
	
	@Override
	protected final boolean splitInputChannels() {
		return false;
	}
	
	@Override
	protected final void doInitialize() {
		super.doInitialize();
		
		final String[] rgbCoefficientAsStrings = this.getParameters().get("rgbCoefficients").trim().split("\\s++");
		final double[] rgbCoefficients = new double[3];
		
		for (int i = 0; i < 3; ++i) {
			rgbCoefficients[i] = parseDouble(rgbCoefficientAsStrings[i]);
		}
		
		final double threshold = parseDouble(this.getParameters().get("threshold").trim());
		final double rejectionModulation = parseDouble(this.getParameters().get("rejectionModulation").trim());
		
		final int[] structuringElement = parseStructuringElement(this.getParameters().get("structuringElement"));
		
		this.setFilter(new RegionFilter(structuringElement, rgbCoefficients, threshold, rejectionModulation));
	}
	
	/**
	 * @author codistmonk (creation 2013-03-07)
	 */
	public static final class RegionFilter extends StructuringElementFilter {
		
		private final double[] rgbCoefficients;
		
		private final double threshold;
		
		private final double rejectionModulation;
		
		private int smallestDistance;
		
		private int newValue;
		
		public RegionFilter(final int[] structuringElement, final double[] rgbCoefficients,
				final double threshold, final double rejectionModulation) {
			super(structuringElement);
			this.rgbCoefficients = rgbCoefficients;
			this.threshold = threshold;
			this.rejectionModulation = rejectionModulation;
		}
		
		@Override
		protected final void reset(final int index, final int oldChannelValue) {
			this.smallestDistance = Integer.MAX_VALUE;
			this.newValue = oldChannelValue;
		}
		
		@Override
		protected final void processNeighbor(final int index, final int oldChannelValue,
				final int neighborIndex, final int neighborChannelValue) {
			final int r1 = index / this.getImage().getColumnCount();
			final int c1 = index % this.getImage().getColumnCount();
			final int r2 = neighborIndex / this.getImage().getColumnCount();
			final int c2 = neighborIndex % this.getImage().getColumnCount();
			final int dr = r2 - r1;
			final int dc = c2 - c1;
			final int d2 = dc * dc + dr * dr;
			final double kr = this.rgbCoefficients[0];
			final double kg = this.rgbCoefficients[1];
			final double kb = this.rgbCoefficients[2];
			
			if (d2 < this.smallestDistance) {
				final int red = red(neighborChannelValue);
				final int green = green(neighborChannelValue);
				final int blue = blue(neighborChannelValue);
				
				if (this.threshold <= (int) (kr * red + kg * green + kb * blue)) {
					this.smallestDistance = d2;
					this.newValue = neighborChannelValue;
				}
			}
			
			if (this.smallestDistance == Integer.MAX_VALUE) {
				final int alpha = (int) (alpha(oldChannelValue) * this.rejectionModulation);
				final int red = (int) (red(oldChannelValue) * this.rejectionModulation);
				final int green = (int) (green(oldChannelValue) * this.rejectionModulation);
				final int blue = (int) (blue(oldChannelValue) * this.rejectionModulation);
				
				this.newValue = argb(alpha, red, green, blue);
			}
		}
		
		@Override
		protected final int getResult(final int index, final int oldChannelValue) {
			return this.newValue;
		}
		
	}
	
}
