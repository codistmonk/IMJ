package imj3.draft;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.tools.Tools.baseName;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import imj3.core.Image2D;
import imj3.core.Image2D.Pixel2DProcessor;
import imj3.draft.Register.WarpField;
import imj3.tools.DoubleImage2D;
import imj3.tools.IMJTools;
import imj3.tools.Image2DComponent;

import java.awt.geom.Point2D;
import java.util.Arrays;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.MathTools.VectorStatistics;

/**
 * @author codistmonk (creation 2015-04-27)
 */
public final class Register2 {
	
	private Register2() {
		throw new IllegalInstantiationException();
	}
	
	static final Point2D ZERO = new Point2D.Double();
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final int lod = arguments.get("lod", 5)[0];
		final Image2D source = IMJTools.read(arguments.get("source", ""), lod);
		final int iterations = arguments.get("iterations", 200)[0];
		final int patchSize0 = arguments.get("patchSize", 25)[0];
		final int regularization = arguments.get("regularization", 20)[0];
		final String outputPrefix = arguments.get("outputPrefix", baseName(source.getId()));
		final boolean show = arguments.get("show", 0)[0] != 0;
		
		debugPrint("sourceId:", source.getId());
		debugPrint("sourceWidth:", source.getWidth(), "sourceHeight:", source.getHeight(), "sourceChannels:", source.getChannels());
		
		final Image2D target = IMJTools.read(arguments.get("target", ""), lod);
		
		debugPrint("targetId:", target.getId());
		debugPrint("targetWidth:", target.getWidth(), "targetHeight:", target.getHeight(), "targetChannels:", target.getChannels());
		
		final WarpField warpField = new WarpField(target.getWidth() / 2, target.getHeight() / 2);
		
		debugPrint("score:", warpField.score(source, target));
		
		if (true) {
			show(new Image2DComponent(scalarize(source)), "scalarized source", false);
			show(new Image2DComponent(scalarize(target)), "scalarized target", false);
			
			return;
		}
		
	}
	
	public static final DoubleImage2D scalarize(final Image2D image) {
		final DoubleImage2D result = new DoubleImage2D(image.getId() + "_scalarized", image.getWidth(), image.getHeight(), 1);
		final int n = image.getChannels().getChannelCount();
		final int patchSize = 5;
		
		result.forEachPixel(new Pixel2DProcessor() {
			
			private final VectorStatistics statistics = new VectorStatistics(n);
			
			private final double[] inputValue = new double[n];
			
			private final double[] outputValue = new double[1];
			
			@Override
			public final boolean pixel(final int x, final int y) {
				this.statistics.reset();
				
				final int imageWidth = image.getWidth();
				final int imageHeight = image.getHeight();
				final int left = max(0, x - patchSize / 2);
				final int right = min(left + patchSize, imageWidth);
				final int top = max(0, y - patchSize / 2);
				final int bottom = min(top + patchSize, imageHeight);
				
				for (int yy = top; yy < bottom; ++yy) {
					for (int xx = left; xx < right; ++xx) {
						this.statistics.addValues(image.getPixelValue(xx, yy, this.inputValue));
					}
				}
				
				this.outputValue[0] = Arrays.stream(this.statistics.getVariances()).max().getAsDouble();
				
				result.setPixelValue(x, y, this.outputValue);
				
				return true;
			}
			
			private static final long serialVersionUID = 3324647706518224181L;
			
		});
		
		return result;
	}
	
}
