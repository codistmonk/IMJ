package imj3.draft;

import static java.lang.Math.log;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.tools.MathTools.square;
import static net.sourceforge.aprog.tools.Tools.*;
import imj3.core.Channels;
import imj3.core.Image2D;
import imj3.core.Image2D.Pixel2DProcessor;
import imj3.tools.IMJTools;
import imj3.tools.Image2DComponent;
import imj3.tools.PackedImage2D;

import java.awt.geom.Point2D;
import java.io.Serializable;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.Factory.DefaultFactory;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-04-24)
 */
public final class Register {
	
	private Register() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final int lod = arguments.get("lod", 5)[0];
		final Image2D source = IMJTools.read(arguments.get("source", ""), lod);
		
		debugPrint("sourceId:", source.getId());
		debugPrint("sourceWidth:", source.getWidth(), "sourceHeight:", source.getHeight(), "sourceChannels:", source.getChannels());
		
		final Image2D target = IMJTools.read(arguments.get("target", ""), lod);
		
		debugPrint("targetId:", target.getId());
		debugPrint("targetWidth:", target.getWidth(), "targetHeight:", target.getHeight(), "targetChannels:", target.getChannels());
		
		final WarpField warpField = new WarpField(100, 100);
		
		debugPrint("score:", warpField.score(source, target));
		
		show(new Image2DComponent(source), "source", false);
		show(new Image2DComponent(target), "target", false);
		show(new Image2DComponent(warpField.warp(source, target, null)), "warped", false);
	}
	
	/**
	 * @author codistmonk (creation 2015-04-24)
	 */
	public static final class WarpField implements Serializable {
		
		private final int width;
		
		private final int height;
		
		private final Point2D[] grid;
		
		public WarpField(final int width, final int height) {
			this.width = width;
			this.height = height;
			this.grid = instances(width * height, new DefaultFactory<>(Point2D.Double.class));
		}
		
		public final int getWidth() {
			return this.width;
		}
		
		public final int getHeight() {
			return this.height;
		}
		
		public final Point2D get(final int x, final int y) {
			return this.grid[y * this.getWidth() + x];
		}
		
		public final long warp(final Image2D source, final Image2D target, final int targetX, final int targetY) {
			final int sourceWidth = source.getWidth();
			final int sourceHeight = source.getHeight();
			final int targetWidth = target.getWidth();
			final int targetHeight = target.getHeight();
			final int x = targetX * this.getWidth() / targetWidth;
			final int y = targetY * this.getHeight() / targetHeight;
			final Point2D delta = this.get(x, y);
			final int sourceX = (int) (targetX * sourceWidth / targetWidth + delta.getX() * sourceWidth);
			final int sourceY = (int) (targetY * sourceHeight / targetHeight + delta.getY() * sourceHeight);
			
			return source.getPixelValue(sourceX, sourceY);
		}
		
		public final double score(final Image2D source, final Image2D target) {
			final Channels sourceChannels = source.getChannels();
			final Channels targetChannels = target.getChannels();
			final int channelCount = sourceChannels.getChannelCount();
			
			if (channelCount != targetChannels.getChannelCount()) {
				return Double.NaN;
			}
			
			final double[] result = { 0.0 };
			
			target.forEachPixel(new Pixel2DProcessor() {
				
				@Override
				public final boolean pixel(final int x, final int y) {
					final long targetValue = target.getPixelValue(x, y);
					final long sourceValue = WarpField.this.warp(source, target, x, y);
					
					for (int channelIndex = 0; channelIndex < channelCount; ++channelIndex) {
						result[0] += square(sourceChannels.getChannelValue(sourceValue, channelIndex)
								- targetChannels.getChannelValue(targetValue, channelIndex));
					}
					
					return true;
				}
				
				private static final long serialVersionUID = 7888357168588108112L;
				
			});
			
			return result[0];
		}
		
		public final Image2D warp(final Image2D source, final Image2D target, final Image2D result) {
			final Image2D actualResult = result != null ? result : new PackedImage2D(
					baseName(source.getId()) + "_warped_lod" + (int) (-log(target.getScale()) / log(2.0)),
					target.getWidth(), target.getHeight(), source.getChannels());
			
			target.forEachPixel(new Pixel2DProcessor() {
				
				@Override
				public final boolean pixel(final int x, final int y) {
					actualResult.setPixelValue(x, y, WarpField.this.warp(source, target, x, y));
					
					return true;
				}
				
				private static final long serialVersionUID = 4284446331825700908L;
				
			});
			
			return actualResult;
		}
		
		private static final long serialVersionUID = -9073706319287807440L;
		
	}
	
}
