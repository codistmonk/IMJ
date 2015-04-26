package imj3.draft;

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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;

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
	
	static final Point2D ZERO = new Point2D.Double();
	
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
		
		final WarpField warpField = new WarpField(target.getWidth() / 4, target.getHeight() / 4);
		
		debugPrint("score:", warpField.score(source, target));
		
		for (int i = 0; i < 1000; ++i) {
			update(warpField, source, target, 5);
			debugPrint("score:", warpField.score(source, target));
			regularize(warpField);
			debugPrint("score:", warpField.score(source, target));
		}
		
		show(new Image2DComponent(source), "source", false);
		show(new Image2DComponent(target), "target", false);
		show(new Image2DComponent(warpField.warp(source, target, null)), "warped", false);
	}
	
	public static final void regularize(final WarpField warpField) {
		final int fieldWidth = warpField.getWidth();
		final int fieldHeight = warpField.getHeight();
		
		for (int i = 0; i < fieldHeight; ++i) {
			for (int j = 0; j < fieldWidth; ++j) {
				final Point2D normalizedDelta = warpField.get(j, i);
				final Point2D sum = new Point2D.Double();
				int n = 0;
				
				if (0 < j && j + 1 < fieldWidth) {
					add(warpField.get(j - 1, i), sum);
					add(warpField.get(j + 1, i), sum);
					n += 2;
				}
				
				if (0 < i && i + 1 < fieldHeight) {
					add(warpField.get(j, i - 1), sum);
					add(warpField.get(j, i + 1), sum);
					n += 2;
				}
				
				if (0 < n) {
					normalizedDelta.setLocation(middle(normalizedDelta.getX(), sum.getX() / n),
							middle(normalizedDelta.getY(), sum.getY() / n));
				}
			}
		}
	}
	
	public static final double middle(final double a, final double b) {
		return (a + b) / 2.0;
	}
	
	public static final void add(final Point2D source, final Point2D destination) {
		destination.setLocation(destination.getX() + source.getX(), destination.getY() + source.getY());
	}
	
	public static final void update(final WarpField warpField, final Image2D source, final Image2D target, final int patchSize) {
		final int fieldWidth = warpField.getWidth();
		final int fieldHeight = warpField.getHeight();
		final int sourceWidth = source.getWidth();
		final int sourceHeight = source.getHeight();
		final int targetWidth = target.getWidth();
		final int targetHeight = target.getHeight();
		final Channels sourceChannels = source.getChannels();
		final Channels targetChannels = target.getChannels();
		final Point2D[] patchOffsets = newPatchOffsets(patchSize);
		final Point2D warped = new Point2D.Double();
		
		for (int i = 0; i < fieldHeight; ++i) {
			final int targetY = i * targetHeight / fieldHeight;
			
			for (int j = 0; j < fieldWidth; ++j) {
				final int targetX = j * targetWidth / fieldWidth;
				final long targetValue = target.getPixelValue(targetX, targetY);
				final Point2D normalizedDelta = warpField.get(j, i);
				
				WarpField.warp(sourceWidth, sourceHeight, targetWidth, targetHeight, targetX, targetY, normalizedDelta, warped);
				
				double bestScore = Double.POSITIVE_INFINITY;
				Point2D bestPatchOffset = null;
				
				for (final Point2D patchOffset : patchOffsets) {
					final int sourceX = (int) (warped.getX() + patchOffset.getX());
					final int sourceY = (int) (warped.getY() + patchOffset.getY());
					final long sourceValue = getPixelValue(source, sourceX, sourceY);
					final double score = WarpField.score(sourceValue, sourceChannels, targetValue, targetChannels);
					
					if (score < bestScore) {
						bestScore = score;
						bestPatchOffset = patchOffset;
					}
				}
				
				normalizedDelta.setLocation(normalizedDelta.getX() + bestPatchOffset.getX() / sourceWidth,
						normalizedDelta.getY() + bestPatchOffset.getY() / sourceHeight);
			}
		}
	}
	
	public static final long getPixelValue(final Image2D image, final int x, final int y) {
		return x < 0 || image.getWidth() <= x || y < 0 || image.getHeight() <= y ? 0L : image.getPixelValue(x, y);
	}
	
	public static final Point2D[] newPatchOffsets(final int patchSize) {
		final Point2D[] result = new Point2D[patchSize * patchSize];
		
		for (int y = 0, i = 0; y < patchSize; ++y) {
			for (int x = 0; x < patchSize; ++x, ++i) {
				result[i] = new Point2D.Float(x - patchSize / 2, y - patchSize / 2);
			}
		}
		
		Arrays.sort(result, new Comparator<Point2D>() {
			
			@Override
			public final int compare(final Point2D o1, final Point2D o2) {
				return Double.compare(o1.distance(ZERO), o2.distance(ZERO));
			}
			
		});
		
		return result;
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
			if (x < 0 || this.getWidth() <= x || y < 0 || this.getHeight() <= y) {
				return new Point2D.Double();
			}
			
			return this.grid[y * this.getWidth() + x];
		}
		
		public final Point2D get(final double x, final double y) {
			final int left = (int) x;
			final int top = (int) y;
			final int right = left + 1;
			final int bottom = top + 1;
			final double rx = x - left;
			final double ry = y - top;
			final Point2D topLeft = this.get(left, top);
			
			if (rx == 0.0 && ry == 0.0) {
				return topLeft;
			}
			
			final double topLeftWeight = (1.0 - rx) * (1.0 - ry);
			final double topRightWeight = rx * (1.0 - ry);
			final double bottomLeftWeight = (1.0 - rx) * ry;
			final double bottomRightWeight = rx * ry;
			final Point2D topRight = topRightWeight == 0.0 ? topLeft : this.get(right, top);
			final Point2D bottomLeft = bottomLeftWeight == 0.0 ? topLeft : this.get(left, bottom);
			final Point2D bottomRight = bottomRightWeight == 0.0 ? topLeft : this.get(right, bottom);
			
			return new Point2D.Double(
					topLeft.getX() * topLeftWeight
					+ topRight.getX() * topRightWeight
					+ bottomLeft.getX() * bottomLeftWeight
					+ bottomRight.getX() * bottomRightWeight,
					topLeft.getY() * topLeftWeight
					+ topRight.getY() * topRightWeight
					+ bottomLeft.getY() * bottomLeftWeight
					+ bottomRight.getY() * bottomRightWeight);
		}
		
		public final long warp(final Image2D source, final int targetWidth, final int targetHeight, final int targetX, final int targetY) {
			final int sourceWidth = source.getWidth();
			final int sourceHeight = source.getHeight();
			final Point2D delta = this.getNormalizedDelta(targetWidth, targetHeight, targetX, targetY);
			final int sourceX = (int) ((double) targetX * sourceWidth / targetWidth + delta.getX() * sourceWidth);
			final int sourceY = (int) ((double) targetY * sourceHeight / targetHeight + delta.getY() * sourceHeight);
			
			if (sourceX < 0 || sourceWidth <= sourceX || sourceY < 0 || sourceHeight <= sourceY) {
				return 0L;
			}
			
			return source.getPixelValue(sourceX, sourceY);
		}
		
		public final Point2D getNormalizedDelta(final int targetWidth, final int targetHeight, final int targetX, final int targetY) {
			return this.get((double) targetX * this.getWidth() / targetWidth, (double) targetY * this.getHeight() / targetHeight);
		}
		
		public final double score(final Image2D source, final Image2D target) {
			final Channels sourceChannels = source.getChannels();
			final Channels targetChannels = target.getChannels();
			
			if (sourceChannels.getChannelCount() != targetChannels.getChannelCount()) {
				return Double.NaN;
			}
			
			final double[] result = { 0.0 };
			final int targetWidth = target.getWidth();
			final int targetHeight = target.getHeight();
			
			target.forEachPixel(new Pixel2DProcessor() {
				
				@Override
				public final boolean pixel(final int x, final int y) {
					final long targetValue = target.getPixelValue(x, y);
					final long sourceValue = WarpField.this.warp(source, targetWidth, targetHeight, x, y);
					
					result[0] += score(sourceValue, sourceChannels, targetValue, targetChannels);
					
					return true;
				}
				
				private static final long serialVersionUID = 7888357168588108112L;
				
			});
			
			return result[0];
		}
		
		public final Image2D warp(final Image2D source, final Image2D target, final Image2D result) {
			final int targetWidth = target.getWidth();
			final int targetHeight = target.getHeight();
			
			if (result == null) {
				return this.new WarpedImage2D(source, targetWidth, targetHeight);
			}
			
			target.forEachPixel(new Pixel2DProcessor() {
				
				@Override
				public final boolean pixel(final int x, final int y) {
					result.setPixelValue(x, y, WarpField.this.warp(source, targetWidth, targetHeight, x, y));
					
					return true;
				}
				
				private static final long serialVersionUID = 4284446331825700908L;
				
			});
			
			return result;
		}

		private static final long serialVersionUID = -9073706319287807440L;
		
		public static final double score(final long sourceValue, final Channels sourceChannels,
				final long targetValue, final Channels targetChannels) {
			final int channelCount = sourceChannels.getChannelCount();
			
			if (channelCount != targetChannels.getChannelCount()) {
				return Double.NaN;
			}
			
			double result = 0.0;
			
			for (int channelIndex = 0; channelIndex < channelCount; ++channelIndex) {
				result += square(sourceChannels.getChannelValue(sourceValue, channelIndex)
						- targetChannels.getChannelValue(targetValue, channelIndex));
			}
			
			return result;
		}
		
		public static final Point2D warp(final int sourceWidth, final int sourceHeight, final int targetWidth, final int targetHeight,
				final int targetX, final int targetY, final Point2D normalizedDelta, final Point2D result) {
			result.setLocation((double) targetX * sourceWidth / targetWidth + normalizedDelta.getX() * sourceWidth,
					(double) targetY * sourceHeight / targetHeight + normalizedDelta.getY() * sourceHeight);
			
			return result;
		}
		
		/**
		 * @author codistmonk (creation 2015-04-24)
		 */
		public final class WarpedImage2D implements Image2D {
			
			private final Image2D source;
			
			private final int width;
			
			private final int height;
			
			private final Map<Thread, TileHolder> tileHolders;
			
			public WarpedImage2D(final Image2D source, final int width, final int height) {
				this.source = source;
				this.width = width;
				this.height = height;
				this.tileHolders = new WeakHashMap<>();
			}
			
			public final Image2D getSource() {
				return this.source;
			}
			
			@Override
			public final Map<String, Object> getMetadata() {
				debugPrint();
				return null;
			}
			
			@Override
			public final String getId() {
				return this.getSource().getId() + "_scale" + this.getScale() + "_warped";
			}
			
			@Override
			public final Channels getChannels() {
				return this.getSource().getChannels();
			}
			
			@Override
			public final int getWidth() {
				return this.width;
			}
			
			@Override
			public final int getHeight() {
				return this.height;
			}
			
			@Override
			public final int getOptimalTileWidth() {
				return 512;
			}
			
			@Override
			public final int getOptimalTileHeight() {
				return this.getOptimalTileWidth();
			}
			
			@Override
			public final TileHolder getTileHolder() {
				return this.tileHolders.computeIfAbsent(Thread.currentThread(), t -> new TileHolder());
			}
			
			@Override
			public final Image2D getTile(final int tileX, final int tileY) {
				final Image2D result = new PackedImage2D(this.getTileKey(tileX, tileY), this.getTileWidth(tileX), this.getTileHeight(tileY), this.getChannels());
				
				result.forEachPixel(new Pixel2DProcessor() {
					
					@Override
					public final boolean pixel(final int x, final int y) {
						result.setPixelValue(x, y, WarpedImage2D.this.getPixelValue(tileX + x, tileY + y));
						
						return true;
					}
					
					private static final long serialVersionUID = 7780780132286145982L;
					
				});
				
				return result;
			}
			
			@Override
			public final long getPixelValue(final int x, final int y) {
				return WarpField.this.warp(this.getSource(), this.getWidth(), this.getHeight(), x, y);
			}
			
			@Override
			public final double getScale() {
				return this.getSource().getScale();
			}
			
			@Override
			public final WarpedImage2D getScaledImage(final double scale) {
				final Image2D newSource = this.getSource().getScaledImage(scale);
				
				if (newSource == this.getSource()) {
					return this;
				}
				
				return new WarpedImage2D(newSource,
						(int) (this.getWidth() * newSource.getScale() / this.getScale()),
						(int) (this.getHeight() * newSource.getScale() / this.getScale()));
			}
			
			private static final long serialVersionUID = 4508491053506310207L;
			
		}
		
	}
	
}
