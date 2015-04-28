package imj3.draft;

import static imj3.draft.Register.add;
import static imj3.draft.Register.lerp;
import static imj3.draft.Register.newPatchOffsets;
import static java.lang.Math.*;
import static java.util.Arrays.stream;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.tools.Tools.*;
import imj3.core.Channels;
import imj3.core.Image2D;
import imj3.core.Image2D.Pixel2DProcessor;
import imj3.draft.Register.WarpField;
import imj3.tools.DoubleImage2D;
import imj3.tools.IMJTools;
import imj3.tools.Image2DComponent;
import imj3.tools.Image2DComponent.TileOverlay;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.ConsoleMonitor;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Factory.DefaultFactory;
import net.sourceforge.aprog.tools.MathTools.VectorStatistics;
import net.sourceforge.aprog.tools.Tools;

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
		final Warping warping = new Warping(16, 16);
		
		debugPrint("score:", warpField.score(source, target));
		
		if (true) {
			final Image2DComponent sourceComponent = new Image2DComponent(scalarize(source));
			final Image2DComponent targetComponent = new Image2DComponent(scalarize(target));
			
			focus(warping.getTargetGrid(), target, 200, 16, 5);
			copyTo(warping.getSourceGrid(), warping.getTargetGrid());
			focus(warping.getSourceGrid(), source, 400, 16, 5);
			
			overlay(warping.getSourceGrid(), sourceComponent);
			overlay(warping.getTargetGrid(), targetComponent);
			
			show(sourceComponent, "scalarized source", false);
			show(targetComponent, "scalarized target", false);
			
			return;
		}
	}
	
	public static final void copyTo(final ParticleGrid destination, final ParticleGrid source) {
		final int w = destination.getWidth();
		final int h = destination.getHeight();
		
		if (w != source.getWidth() || h != source.getHeight()) {
			throw new IllegalArgumentException();
		}
		
		destination.forEach((x, y) -> {
			destination.get(x, y).setLocation(source.get(x, y));
			
			return true;
		});
	}
	
	public static final void focusAndRegularize(final ParticleGrid grid, final Image2D image, final int iterations, final int windowSize, final int patchSize, final int regularization) {
		final ConsoleMonitor monitor = new ConsoleMonitor(10_000L);
		
		for (int i = 0; i < iterations; ++i) {
			monitor.ping(i + "/" + iterations + "\r");
			
			focus(grid, image, windowSize - i * (windowSize - 1) / iterations, patchSize);
			
			for (int j = 0; j < regularization; ++j) {
				regularize(grid);
			}
		}
		
		monitor.pause();
	}
	
	public static final void focus(final ParticleGrid grid, final Image2D image, final int iterations, final int windowSize, final int patchSize) {
		final ConsoleMonitor monitor = new ConsoleMonitor(10_000L);
		
		for (int i = 0; i < iterations; ++i) {
			debugPrint(i);
			monitor.ping(i + "/" + iterations + "\r");
			
			focus(grid, image, windowSize - i * (windowSize - 1) / iterations, patchSize);
		}
		
		monitor.pause();
	}
	
	public static final void overlay(final ParticleGrid grid, final Image2DComponent component) {
		final AtomicBoolean showParticles = new AtomicBoolean(true);
		
		component.setTileOverlay(new TileOverlay() {
			
			@Override
			public final void update(final Graphics2D graphics, final Point tileXY, final Rectangle region) {
				if (!showParticles.get()) {
					return;
				}
				
				final int tileX = tileXY.x;
				final int tileY = tileXY.y;
				final Image2D image = component.getImage();
				final int tileWidth = image.getTileWidth(tileX);
				final int tileHeight = image.getTileHeight(tileY);
				final int w = grid.getWidth();
				final int h = grid.getHeight();
				final int r = 2;
				final int imageWidth = image.getWidth();
				final int imageHeight = image.getHeight();
				
				for (int i = 0; i < h; ++i) {
					for (int j = 0; j < w; ++j) {
						final Point2D normalizedPoint = grid.get(j, i);
						final int x0 = j * imageWidth / w;
						final int y0 = i * imageHeight / h;
						final int x1 = (int) (normalizedPoint.getX() * imageWidth);
						final int y1 = (int) (normalizedPoint.getY() * imageHeight);
						
						if (tileX <= x1 && x1 < tileX + tileWidth && tileY <= y1 && y1 < tileY + tileHeight) {
							final int rx0 = (int) lerp(region.x, region.x + region.width, (double) (x0 - tileX) / tileWidth);
							final int ry0 = (int) lerp(region.y, region.y + region.height, (double) (y0 - tileY) / tileHeight);
							final int rx1 = (int) lerp(region.x, region.x + region.width, (double) (x1 - tileX) / tileWidth);
							final int ry1 = (int) lerp(region.y, region.y + region.height, (double) (y1 - tileY) / tileHeight);
							
							graphics.setColor(Color.GREEN);
							graphics.drawLine(rx0, ry0, rx1, ry1);
							graphics.setColor(Color.RED);
							graphics.drawOval(rx1 - r, ry1 - r, 2 * r, 2 * r);
						}
					}
				}
			}
			
			private static final long serialVersionUID = -993700290668202661L;
			
		});
		
		component.addKeyListener(new KeyAdapter() {
			
			@Override
			public final void keyPressed(final KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.VK_F) {
					showParticles.set(!showParticles.get());
					component.repaint();
				}
			}
			
		});
	}
	
	public static final void regularize(final ParticleGrid grid) {
		final WarpField tmp = new WarpField(grid.getWidth(), grid.getHeight());
		final int fieldWidth = grid.getWidth();
		final int fieldHeight = grid.getHeight();
		
		for (int i = 0; i < fieldHeight; ++i) {
			for (int j = 0; j < fieldWidth; ++j) {
				final Point2D normalizedDelta = grid.get(j, i);
				final Point2D sum = new Point2D.Double();
				int n = 0;
				
				if (0 < j && j + 1 < fieldWidth) {
					add(grid.get(j - 1, i), sum);
					add(grid.get(j + 1, i), sum);
					n += 2;
				}
				
				if (0 < i && i + 1 < fieldHeight) {
					add(grid.get(j, i - 1), sum);
					add(grid.get(j, i + 1), sum);
					n += 2;
				}
				
				if (i == 0) {
					if (j == 0) {
						add(grid.get(j + 1, i), sum);
						add(grid.get(j, i + 1), sum);
						n += 2;
					} else if (j + 1 == fieldWidth) {
						add(grid.get(j - 1, i), sum);
						add(grid.get(j, i + 1), sum);
						n += 2;
					}
				} else if (i + 1 == fieldHeight) {
					if (j == 0) {
						add(grid.get(j, i - 1), sum);
						add(grid.get(j + 1, i), sum);
						n += 2;
					} else if (j + 1 == fieldWidth) {
						add(grid.get(j, i - 1), sum);
						add(grid.get(j - 1, i), sum);
						n += 2;
					}
				}
				
				if (0 < n) {
					final double k = 0.5;
					
					tmp.get(j, i).setLocation(lerp(normalizedDelta.getX(), sum.getX() / n, k),
							lerp(normalizedDelta.getY(), sum.getY() / n, k));
				} else {
					tmp.get(j, i).setLocation(normalizedDelta);
				}
			}
		}
		
		for (int i = 0; i < fieldHeight; ++i) {
			for (int j = 0; j < fieldWidth; ++j) {
				grid.get(j, i).setLocation(tmp.get(j, i));
			}
		}
	}
	
	public static final void focus(final ParticleGrid grid, final Image2D image, final int windowSize, final int patchSize) {
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final Point2D[] patchOffsets = newPatchOffsets(windowSize);
		
		grid.forEach((x, y) -> {
			final Point2D normalizedPixel = grid.get(x, y);
			double bestSaliency = 0.0;
			Point2D bestOffset = null;
			
			for (final Point2D offset : patchOffsets) {
				final double saliency = saliency(image,
						(int) (normalizedPixel.getX() * imageWidth + offset.getX()),
						(int) (normalizedPixel.getY() * imageHeight + offset.getY()), patchSize);
				
				if (bestSaliency < saliency) {
					bestSaliency = saliency;
					bestOffset = offset;
				}
			}
			
			if (0 < x && x + 1 < grid.getWidth() && 0 < y && y + 1 < grid.getHeight()) {
				for (int i = 0; i <= 3; ++i) {
					final int i0 = i & 1;
					final int i1 = (i >> 1) & 1;
					final int dx = i1 - i0;
					final int dy = i0 + i1 - 1;
					
					move(grid, x + dx, y + dy, bestOffset, imageWidth, imageHeight);
				}
			}
			
			if (0 < x && x + 1 < grid.getWidth() && 0 < y && y + 1 < grid.getHeight()) {
				checkInducedTopology(grid, x, y, imageWidth, imageHeight);
			}
				
			move(grid, x, y, bestOffset, imageWidth, imageHeight);
			
			if (0 < x && x + 1 < grid.getWidth() && 0 < y && y + 1 < grid.getHeight()) {
				checkInducedTopology(grid, x, y, imageWidth, imageHeight);
			}
			
			return true;
		});
	}
	
	public static final void move(final ParticleGrid grid, final int x, final int y,
			final Point2D offset, final int imageWidth, final int imageHeight) {
		if (offset != null) {
			final double d = offset.distance(ZERO);
			
			if (0.0 < d) {
				final double nx = offset.getX() / d;
				final double ny = offset.getY() / d;
				final Point2D normalizedPixel = grid.get(x, y);
				double closestNeighborDistance = Double.POSITIVE_INFINITY;
				
				{
					for (int i = 0; i <= 3; ++i) {
						final int i0 = i & 1;
						final int i1 = (i >> 1) & 1;
						final int dx = i1 - i0;
						final int dy = i0 + i1 - 1;
						final Point2D neighbor = grid.get(x + dx, y + dy);
//						final double neighborDistance = normalizedPixel.distance(neighbor);
						final double neighborDistance = neighbor.getX() * nx + neighbor.getY() * ny;
						
						if (0.0 < neighborDistance && neighborDistance < closestNeighborDistance) {
							closestNeighborDistance = neighborDistance;
						}
					}
				}
				
				final double k = min(d, closestNeighborDistance) / d / 8.0;
				
				if (1.0 < k) {
					debugError(k, d, closestNeighborDistance);
				}
				
				normalizedPixel.setLocation(
						normalizedPixel.getX() + k * offset.getX() / imageWidth,
						normalizedPixel.getY() + k * offset.getY() / imageHeight);
			}
		}
	}
	
	public static void checkInducedTopology(final ParticleGrid grid, final int x, final int y,
			final int imageWidth, final int imageHeight) {
		final Point2D normalizedPixel = grid.get(x, y);
		
		for (int i = 0; i <= 3; ++i) {
			final int i0 = i & 1;
			final int i1 = (i >> 1) & 1;
			final int dx = i1 - i0;
			final int dy = i0 + i1 - 1;
			final Point2D neighbor = grid.get(x + dx, y + dy);
			
			xs[i] = (int) (neighbor.getX() * imageWidth);
			ys[i] = (int) (neighbor.getY() * imageHeight);
		}
		
		swap(xs, 2, 3);
		swap(ys, 2, 3);
		final Polygon polygon = new Polygon(xs, ys, xs.length);
		
		if (!polygon.contains(normalizedPixel.getX() * imageWidth, normalizedPixel.getY() * imageHeight)) {
			debugError(Arrays.toString(xs));
			debugError(Arrays.toString(ys));
			debugError(normalizedPixel, (int) (normalizedPixel.getX() * imageWidth), (int) (normalizedPixel.getY() * imageHeight));
			debugError();
			debugError(grid.get(x, y - 1));
			debugError(grid.get(x - 1, y));
			debugError(grid.get(x, y + 1));
			debugError(grid.get(x + 1, y));
			throw new IllegalStateException();
		}
	}
	
	static final int[] xs = new int[4];
	
	static final int[] ys = new int[4];
	
	public static final double saliency(final Image2D image, final int x, final int y, final int patchSize) {
		final Channels channels = image.getChannels();
		final int n = channels.getChannelCount();
		final VectorStatistics statistics = new VectorStatistics(n);
		final int left = max(0, x - patchSize / 2);
		final int right = min(left + patchSize, image.getWidth());
		final int top = max(0, y - patchSize / 2);
		final int bottom = min(top + patchSize, image.getHeight());
		final double[] pixelValue = new double[n];
		
		for (int yy = top; yy < bottom; ++yy) {
			for (int xx = left; xx < right; ++xx) {
				statistics.addValues(image.getPixelValue(xx, yy, pixelValue));
			}
		}
		
		return stream(statistics.getVariances()).sum();
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
				
				this.outputValue[0] = Arrays.stream(this.statistics.getVariances()).sum();
				
				result.setPixelValue(x, y, this.outputValue);
				
				return true;
			}
			
			private static final long serialVersionUID = 3324647706518224181L;
			
		});
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2015-04-27)
	 */
	public static final class ParticleGrid implements Serializable {
		
		private final int width;
		
		private final int height;
		
		private final Point2D[] grid;
		
		public ParticleGrid(final int width, final int height) {
			this.width = width;
			this.height = height;
			this.grid = instances(width * height, new DefaultFactory<>(Point2D.Double.class));
			
			this.forEach((x, y) -> {
				this.get(x, y).setLocation((double) x / width, (double) y / height);
				
				return true;
			});
		}
		
		public final int getWidth() {
			return this.width;
		}
		
		public final int getHeight() {
			return this.height;
		}
		
		public final void forEach(final Pixel2DProcessor process) {
			final int w = this.getWidth();
			final int h = this.getHeight();
			
			for (int i = 0; i < h; ++i) {
				for (int j = 0; j < w; ++j) {
					if (!process.pixel(j, i)) {
						return;
					}
				}
			}
		}
		
		public final Point2D get(final int x, final int y) {
			return this.grid[max(0, min(y, this.getHeight() - 1)) * this.getWidth() + max(0, min(x, this.getWidth() - 1))];
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
		
		private static final long serialVersionUID = -1320819035669324144L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-04-27)
	 */
	public static final class Warping implements Serializable {
		
		private final ParticleGrid sourceGrid;
		
		private final ParticleGrid targetGrid;

		public Warping(final int wwidth, final int height) {
			this.sourceGrid = new ParticleGrid(wwidth, height);
			this.targetGrid = new ParticleGrid(wwidth, height);
		}
		
		public final ParticleGrid getSourceGrid() {
			return this.sourceGrid;
		}
		
		public final ParticleGrid getTargetGrid() {
			return this.targetGrid;
		}
		
		private static final long serialVersionUID = 7301482635789363102L;
		
	}
	
}
