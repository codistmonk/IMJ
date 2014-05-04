package imj2.tools;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.sourceforge.aprog.tools.Tools.debugError;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import imj2.core.Image.Channels;
import imj2.core.Image.Monochannel;
import imj2.core.Image2D;
import imj2.core.LinearPackedGrayImage;
import imj2.core.TiledImage2D;
import imj2.tools.IMJTools.TileProcessor;
import imj2.tools.Image2DComponent.Painter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Window;
import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2014-05-03)
 */
public final class InteractiveClassifier {
	
	private InteractiveClassifier() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		if (true) {
			final Window window = Image2DComponent.showDefaultImage();
			final Image2DComponent imageView = IMJTools.findComponent(window, Image2DComponent.class);
			
			imageView.getPainters().add(new Painter<Image2DComponent>() {
				
				private DelimitersBuilder delimitersBuilder;
				
				private LinearPackedGrayImage delimiters;
				
				@Override
				public final void paint(final Graphics2D g, final Image2DComponent component
						, final int width, final int height) {
					final Image2D image = component.getImage();
					
					if (this.delimitersBuilder == null || this.delimitersBuilder.getImage() != image) {
						this.delimitersBuilder = new DelimitersBuilder(image, 32);
						this.delimiters = this.delimitersBuilder.newEmptyDelimiters();
					}
					
					final Rectangle box = component.getVisibleBoxInImage();
					
					debugPrint(box);
					
					this.delimitersBuilder.setDelimiters(this.delimiters
							, box.x, box.y, box.x + box.width, box.y + box.height);
					
					final int tx = -component.getXInImage(0);
					final int ty = -component.getYInImage(0);
					
					this.delimitersBuilder.processSegments(box.x, box.y
							, box.x + box.width, box.y + box.height, new SegmentProcessor() {
						
						@Override
						public final void segment(final int x, final int y, final int row, final int column) {
							final int northY = delimitersBuilder.getSegmentNorthY(delimiters, x, y, row, column);
							final int westX = delimitersBuilder.getSegmentWestX(delimiters, x, y, row, column);
							final int eastX = delimitersBuilder.getSegmentEastX(delimiters, x, y, row, column);
							final int southY = delimitersBuilder.getSegmentSouthY(delimiters, x, y, row, column);
							
							g.setColor(Color.GREEN);
							g.drawOval(x - 2 + tx, northY - 2 + ty, 4, 4);
							g.drawOval(westX - 2 + tx, y - 2 + ty, 4, 4);
							g.drawOval(eastX - 2 + tx, y - 2 + ty, 4, 4);
							g.drawOval(x - 2 + tx, southY - 2 + ty, 4, 4);
							
							g.setColor(Color.RED);
							g.drawOval(x - 2 + tx, y - 2 + ty, 4, 4);
							
							final Polygon polygon = segmentToPolygon(delimiters, delimitersBuilder, x, y, row, column);
							
							polygon.translate(tx, ty);
							
							g.drawPolygon(polygon);
						}
						
						/**
						 * {value}.
						 */
						private static final long serialVersionUID = 3851222531284298589L;
						
					});
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = -7467299626996751246L;
				
			});
		}
		
		if (false) {
			final TicToc timer = new TicToc();
			
			debugPrint("Loading image started...", new Date(timer.tic()));
			
//			final TiledImage2D image = new LociBackedImage("../Libraries/images/svs/SYS08_A10_7414-005.svs");
			final TiledImage2D image = (TiledImage2D) new MultifileImage(
					"../Libraries/images/jpg/SYS08_A10_7414-005", "SYS08_A10_7414-005_lod0", null).getLODImage(6);
			
			debugPrint("Loading image done in", timer.toc(), "ms");
			debugPrint(image.getWidth(), image.getHeight(), image.getPixelCount());
			
			debugPrint("Analyzing image started...", new Date(timer.tic()));
			
			final DelimitersBuilder delimitersBuilder = new DelimitersBuilder(image, 32);
			
			delimitersBuilder.buildDelimitersFor(image);
			
			debugPrint("Analyzing image done in", timer.toc(), "ms");
		}
	}
	
	public static final Polygon segmentToPolygon(
			final LinearPackedGrayImage delimiters, final DelimitersBuilder delimitersBuilder
			, final int x, final int y, final int row, final int column) {
		final Polygon result = new Polygon();
		
		result.addPoint(x, delimitersBuilder.getSegmentNorthY(delimiters, x, y, row, column));
		result.addPoint(delimitersBuilder.getSegmentWestX(delimiters, x, y, row, column), y);
		result.addPoint(x, delimitersBuilder.getSegmentSouthY(delimiters, x, y, row, column));
		result.addPoint(delimitersBuilder.getSegmentEastX(delimiters, x, y, row, column), y);
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2014-05-03)
	 */
	public static final class DelimitersBuilder implements Serializable {
		
		private final Image2D image;

		private final int w;

		private final int h;
		
		private final int s;

		private final int delimitersPerRow;

		private final int rows;

		private final long horizontalDelimiters;

		private final int delimitersPerColumn;

		private final int columns;

		private final long verticalDelimiters;

		private final long allDelimiters;
		
		private final AtomicLong delimitersDone;
		
		public DelimitersBuilder(final Image2D image, final int s) {
			this.image = image;
			
			this.w = image.getWidth();
			this.h = image.getHeight();
			this.s = s;
			
			/*
			 * <-------------------w------------------->
			 * <---s/2--><-----s-----><-----s----->
			 * *---------0---*--------0------*-----0---*
			 * 
			 * <-----------------------w----------------------->
			 * <---s/2--><-----s-----><-----s-----><-----s----->
			 * *---------0---*--------0------*-----0-----------*
			 * 
			 * numberOfDelimiters = 1+ceiling((w-s/2)/s)
			 */
			
			this.delimitersPerRow = 1 + ceiling(this.w - s / 2, s);
			this.rows = 2 + ceiling(this.h - s / 2, s);
			this.horizontalDelimiters = (long) this.delimitersPerRow * this.rows;
			this.delimitersPerColumn = 1 + ceiling(this.h - s / 2, s);
			this.columns = 2 + ceiling(this.w - s / 2, s);
			this.verticalDelimiters = (long) this.delimitersPerColumn * this.columns;
			this.allDelimiters = this.horizontalDelimiters + this.verticalDelimiters;
			final LinearPackedGrayImage delimiters = this.newEmptyDelimiters();
			this.delimitersDone = new AtomicLong();
			
			debugPrint("delimitersPerRow:", this.delimitersPerRow, "rows:", this.rows
					, "delimitersPerColumn:", this.delimitersPerColumn, "columns:", this.columns
					, "delimiters:", delimiters.getPixelCount());
		}
		
		public final Image2D getImage() {
			return this.image;
		}
		
		public final LinearPackedGrayImage buildDelimitersFor(final TiledImage2D image) {
			final LinearPackedGrayImage result = this.newEmptyDelimiters();
			
			IMJTools.forEachTileIn(image, new TileProcessor() {
				
				@Override
				public final void pixel(final Info info) {
					debugPrint("tile:", info.getTileX(), info.getTileY(), info.getActualTileWidth(), info.getActualTileHeight());
					
					final int xStart = info.getTileX() + info.getPixelXInTile();
					final int yStart = info.getTileY() + info.getPixelYInTile();
					final int xEnd = info.getTileX() + info.getActualTileWidth();
					final int yEnd = info.getTileY() + info.getActualTileHeight();
					
					DelimitersBuilder.this.setDelimiters(result, xStart, yStart, xEnd, yEnd);
				}
				
				@Override
				public final void endOfTile() {
					// NOP
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 8075249791734409997L;
				
			});
			
			this.check(result);
			
			return result;
		}
		
		public final LinearPackedGrayImage newEmptyDelimiters() {
			return new LinearPackedGrayImage(""
					, this.allDelimiters, new Monochannel(Long.SIZE - Long.numberOfLeadingZeros(this.s - 1L)));
		}
		
		public final void setDelimiters(final LinearPackedGrayImage delimiters
				, final int xStart, final int yStart, final int xEnd, final int yEnd) {
			this.processSegments(xStart, yStart, xEnd, yEnd, new SegmentProcessor() {
				
				@Override
				public final void segment(final int x, final int y, final int row, final int column) {
//					debugPrint("segment:", x, y, row, column);
					
					DelimitersBuilder.this.setHorizontalDelimiters(delimiters, y, row, x, column);
					DelimitersBuilder.this.setVerticalDelimiters(delimiters, y, row, x, column);
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 7972837075735571744L;
				
			});
		}
		
		public final void processSegments(final int xStart, final int yStart
				, final int xEnd, final int yEnd, final SegmentProcessor process) {
			/*
			 * y0 <= s/2 + k s
			 * <- y0 - s/2 <= k s
			 * <- (y0 - s/2)/s <= k
			 * <- k = ceiling(y0-s/2,s)
			 */
			
			for (int y = this.s / 2 + this.s * ceiling(yStart - this.s / 2, this.s); y < yEnd; y += this.s) {
				final int row = 1 + ceiling(y - this.s / 2, this.s);
				
				for (int x = this.s / 2 + this.s * ceiling(xStart - this.s / 2, this.s); x < xEnd; x += this.s) {
					final int column = 1 + ceiling(x - this.s / 2, this.s);
					
					process.segment(x, y, row, column);
				}
			}
		}
		
		public final int getSegmentNorthY(final LinearPackedGrayImage delimiters, final int x, final int y, final int row, final int column) {
			if (row == 1) {
				return 0;
			}
			
			final long delimiterIndex = this.horizontalDelimiters + column * this.delimitersPerColumn + row - 1L;
			
			return y - this.s + delimiters.getPixelValue(delimiterIndex);
		}
		
		public final int getSegmentWestX(final LinearPackedGrayImage delimiters, final int x, final int y, final int row, final int column) {
			if (column == 1) {
				return 0;
			}
			
			final long delimiterIndex = row * this.delimitersPerRow + column - 1L;
			
			return x - this.s + delimiters.getPixelValue(delimiterIndex);
		}
		
		public final int getSegmentEastX(final LinearPackedGrayImage delimiters, final int x, final int y, final int row, final int column) {
			final long delimiterIndex = row * this.delimitersPerRow + column;
			
			return x + delimiters.getPixelValue(delimiterIndex);
		}
		
		public final int getSegmentSouthY(final LinearPackedGrayImage delimiters, final int x, final int y, final int row, final int column) {
			final long delimiterIndex = this.horizontalDelimiters + column * this.delimitersPerColumn + row;
			
			return y + delimiters.getPixelValue(delimiterIndex);
		}
		
		public final void check(final LinearPackedGrayImage delimiters) {
			if (this.delimitersDone.get() != delimiters.getPixelCount()) {
				debugError("expectedDelimiters:", delimiters.getPixelCount(), "actualDelimiters:", this.delimitersDone.get());
			}
			
			{
				long errors = 0L;
				
				for (long i = 0L; i < this.horizontalDelimiters; ++i) {
					final int delimiterValue = delimiters.getPixelValue(i);
					
					if (i % this.delimitersPerRow == 0) {
						if (delimiterValue != 0) {
							++errors;
						}
					} else if (delimiterValue == 0) {
						++errors;
					}
				}
				
				for (long i = this.horizontalDelimiters; i < this.allDelimiters; ++i) {
					final int delimiterValue = delimiters.getPixelValue(i);
					
					if ((i - this.horizontalDelimiters) % this.delimitersPerColumn == 0) {
						if (delimiterValue != 0) {
							++errors;
						}
					} else if (delimiterValue == 0) {
						++errors;
					}
				}
				
				if (0 < errors) {
					debugError("erroneousDelimiters:", errors);
				}
			}
		}
		
		final void setHorizontalDelimiters(final LinearPackedGrayImage delimiters
				, final int y, final int row, final int x, final int column) {
			if (y == this.s / 2) {
				{
					final long delimiterIndex = (row - 1L) * this.delimitersPerRow + column - 1L;
					
					if (column == 1) {
						delimiters.setPixelValue(delimiterIndex, 0);
					} else {
						delimiters.setPixelValue(delimiterIndex
								, getHorizontalOffsetOfLargestGradient(this.getImage(), x - this.s, 0, this.s));
					}
					
					this.delimitersDone.incrementAndGet();
				}
				
				if (this.w <= x + this.s) {
					final long delimiterIndex = (row - 1L) * this.delimitersPerRow + column;
					delimiters.setPixelValue(delimiterIndex, this.w - 1 - x);
					this.delimitersDone.incrementAndGet();
				}
			}
			
			{
				final long delimiterIndex = row * this.delimitersPerRow + column - 1L;
				
				if (column == 1) {
					delimiters.setPixelValue(delimiterIndex, 0);
				} else {
					delimiters.setPixelValue(delimiterIndex
							, getHorizontalOffsetOfLargestGradient(this.getImage(), x - this.s, y, this.s));
				}
				
				this.delimitersDone.incrementAndGet();
			}
			
			if (this.w <= x + this.s) {
				final long delimiterIndex = row * this.delimitersPerRow + column;
				delimiters.setPixelValue(delimiterIndex, this.w - 1 - x);
				this.delimitersDone.incrementAndGet();
			}
			
			if (this.h <= y + this.s) {
				{
					final long delimiterIndex = (row + 1L) * this.delimitersPerRow + column - 1L;
					
					if (column == 1) {
						delimiters.setPixelValue(delimiterIndex, 0);
					} else {
						delimiters.setPixelValue(delimiterIndex
								, getHorizontalOffsetOfLargestGradient(this.getImage(), x - this.s, this.h - 1, this.s));
					}
					
					this.delimitersDone.incrementAndGet();
				}
				
				if (this.w <= x + this.s) {
					final long delimiterIndex = (row + 1L) * this.delimitersPerRow + column;
					delimiters.setPixelValue(delimiterIndex, this.w - 1 - x);
					this.delimitersDone.incrementAndGet();
				}
			}
		}
		
		final void setVerticalDelimiters(final LinearPackedGrayImage delimiters
				, final int y, final int row, final int x, final int column) {
			if (x == this.s / 2) {
				{
					final long delimiterIndex = this.horizontalDelimiters + (column - 1L) * this.delimitersPerColumn + row - 1L;
					
					if (row == 1) {
						delimiters.setPixelValue(delimiterIndex, 0);
					} else {
						delimiters.setPixelValue(delimiterIndex
								, getHorizontalOffsetOfLargestGradient(this.getImage(), 0, y - this.s, this.s));
					}
					
					this.delimitersDone.incrementAndGet();
				}
				
				if (this.h <= y + this.s) {
					final long delimiterIndex = this.horizontalDelimiters + (column - 1L) * this.delimitersPerColumn + row;
					delimiters.setPixelValue(delimiterIndex, this.h - 1 - y);
					this.delimitersDone.incrementAndGet();
				}
			}
			
			{
				final long delimiterIndex = this.horizontalDelimiters + column * this.delimitersPerColumn + row - 1L;
				
				if (row == 1) {
					delimiters.setPixelValue(delimiterIndex, 0);
				} else {
					delimiters.setPixelValue(delimiterIndex
							, getVerticalOffsetOfLargestGradient(this.getImage(), x, y - this.s, this.s));
				}
				
				this.delimitersDone.incrementAndGet();
			}
			
			if (this.h <= y + this.s) {
				final long delimiterIndex = this.horizontalDelimiters + column * this.delimitersPerColumn + row;
				delimiters.setPixelValue(delimiterIndex, this.h - 1 - y);
				this.delimitersDone.incrementAndGet();
			}
			
			if (this.w <= x + this.s) {
				{
					final long delimiterIndex = this.horizontalDelimiters + (column + 1L) * this.delimitersPerColumn + row - 1L;
					
					if (row == 1) {
						delimiters.setPixelValue(delimiterIndex, 0);
					} else {
						delimiters.setPixelValue(delimiterIndex
								, getVerticalOffsetOfLargestGradient(this.getImage(), this.w - 1, y - this.s, this.s));
					}
					
					this.delimitersDone.incrementAndGet();
				}
				
				if (this.h <= y + this.s) {
					final long delimiterIndex = this.horizontalDelimiters + (column + 1L) * this.delimitersPerColumn + row;
					delimiters.setPixelValue(delimiterIndex, this.h - 1 - y);
					this.delimitersDone.incrementAndGet();
				}
			}
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 2680059399437526583L;
		
		public static final int ceiling(final int a, final int b) {
			return (a + b - 1) / b;
		}
		
		public static final int getHorizontalOffsetOfLargestGradient(final Image2D image
				, final int x, final int y, final int s) {
			final int w = image.getWidth();
			final int xStart = x + 1;
			final int xEnd = min(w, x + s);
			int largestGradient = getGradient(image, xStart, y);
			int result = 1;
			
			for (int xx = xStart; xx < xEnd; ++xx) {
				final int gradient = getGradient(image, xx, y);
				
				if (largestGradient < gradient) {
					largestGradient = gradient;
					result = xx - xStart;
				}
			}
			
			return result;
		}
		
		public static final int getVerticalOffsetOfLargestGradient(final Image2D image
				, final int x, final int y, final int s) {
			final int h = image.getHeight();
			final int yStart = y + 1;
			final int yEnd = min(h, y + s);
			int largestGradient = getGradient(image, x, yStart);
			int result = 1;
			
			for (int yy = yStart; yy < yEnd; ++yy) {
				final int gradient = getGradient(image, x, yy);
				
				if (largestGradient < gradient) {
					largestGradient = gradient;
					result = yy - yStart;
				}
			}
			
			return result;
		}
		
		public static final int getGradient(final Image2D image, final int x, final int y) {
			final int pixelValue = image.getPixelValue(x, y);
			final Channels channels = image.getChannels();
			final int channelCount = channels.getChannelCount();
			final int w = image.getWidth();
			final int h = image.getHeight();
			final int xStart = max(0, x - 1);
			final int xEnd = min(w, x + 1);
			final int yStart = max(0, y - 1);
			final int yEnd = min(h, y + 1);
			int result = 0;
			
			for (int yi = yStart; yi < yEnd; ++yi) {
				for (int xi = xStart; xi < xEnd; ++xi) {
					if (xi != x || yi != y) {
						final int neighborValue = image.getPixelValue(xi, yi);
						
						for (int channel = 0; channel < channelCount; ++channel) {
							result += abs(channels.getChannelValue(pixelValue, channel)
									- channels.getChannelValue(neighborValue, channel));
						}
					}
				}
			}
			
			return result;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2014-05-04)
	 */
	public static abstract interface SegmentProcessor extends Serializable {
		
		public abstract void segment(int x, int y, int row, int column);
		
	}
	
}
