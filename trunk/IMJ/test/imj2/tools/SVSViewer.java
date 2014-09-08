package imj2.tools;

import static imj2.tools.IMJTools.awtImage;
import static imj2.tools.SimpleImageView.centered;
import static java.lang.Math.ceil;
import static java.lang.Math.min;
import static net.sourceforge.aprog.swing.SwingTools.getFiles;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2014-06-13)
 */
public final class SVSViewer {
	
	private SVSViewer() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		show(new Context().getMainPanel(), SVSViewer.class.getName(), false);
	}
	
	/**
	 * @author codistmonk (creation 2014-06-13)
	 */
	public static final class Context implements Serializable {
		
		private String imagePath;
		
		private LociBackedImage image;
		
		private final JPanel mainPanel;
		
		private final ImageView imageView;
		
		public Context() {
			this.mainPanel = new JPanel(new BorderLayout());
			this.imageView = this.new ImageView();
			SwingTools.setCheckAWT(false);
			this.mainPanel.add(scrollable(centered(this.imageView)), BorderLayout.CENTER);
			SwingTools.setCheckAWT(true);
			
			this.mainPanel.setDropTarget(new DropTarget() {
				
				@Override
				public final synchronized void drop(final DropTargetDropEvent event) {
					Context.this.loadImage(getFiles(event).get(0).getPath());
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 395341882191834544L;
				
			});
		}
		
		public final String getImagePath() {
			return this.imagePath;
		}
		
		public final void loadImage(final String path) {
			this.imagePath = path;
			
			try {
				this.image = new LociBackedImage(path);
				this.getImageView().refresh().setScale(1.0);
				this.getImageView().revalidate();
				this.getMainPanel().repaint();
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		}
		
		public final LociBackedImage getImage() {
			return this.image;
		}
		
		public final JPanel getMainPanel() {
			return this.mainPanel;
		}
		
		public final ImageView getImageView() {
			return this.imageView;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 1186037539784702031L;
		
		/**
		 * @author codistmonk (creation 2014-06-13)
		 */
		public final class ImageView extends JComponent {
			
			private String path;
			
			private final List<Level> levels;
			
			private double scale;
			
			public ImageView() {
				this.levels = new ArrayList<>();
				this.scale = 1.0;
				
				this.setPreferredSize(new Dimension(512, 512));
			}
			
			@Override
			protected final void paintComponent(final Graphics g) {
				super.paintComponent(g);
				
				if (Context.this.getImagePath() == null) {
					final String string = "DROP IMAGE HERE";
					final Rectangle stringBounds = g.getFontMetrics().getStringBounds(string, g).getBounds();
					
					g.drawString(string, this.getWidth() / 2 - stringBounds.width / 2, this.getHeight() / 2 + stringBounds.height / 2);
				} else {
					final Level level = this.getLevels().get(0).updateTiles();
					final Grid<BufferedImage> tiles = level.getTiles();
					final int tileWidth = level.getTileWidth();
					final int tileHeight = level.getTileHeight();
					final int columnCount = tiles.getColumnCount();
					final int rowCount = tiles.getRowCount();
					
					for (int tileY = level.getTopTileY(), rowIndex = 0; rowIndex < rowCount; tileY += tileHeight, ++rowIndex) {
						for (int tileX = level.getLeftTileX(), columnIndex = 0; columnIndex < columnCount; tileX += tileWidth, ++columnIndex) {
							g.drawImage(tiles.getElement(rowIndex, columnIndex), tileX, tileY, null);
							g.drawRect(tileX, tileY, tileWidth - 1, tileHeight - 1);
						}
						
					}
					
					g.drawRect(0, 0, this.getWidth() - 1, this.getHeight() - 1);
				}
			}
			
			public final ImageView refresh() {
				this.refreshLevels();
				
				return this;
			}
			
			public final double[] getViewCenterXYInLevel0() {
				final Rectangle clipping = this.getVisibleRect();
				
				return new double[] {
					this.unscale(clipping.getCenterX())
					, this.unscale(clipping.getCenterY())
				};
			}
			
			private final void refreshLevels() {
				if (!Context.this.getImagePath().equals(this.path)) {
					this.getLevels().clear();
					
					final int n = Context.this.getImage().getSeriesCount();
					final int endLevel = 2 < n ? n - 2 : n;	
					
					for (int i = 0; i < endLevel; ++i) {
						this.getLevels().add(this.new Level(i));
					}
				}
			}
			
			public final double getScale() {
				return this.scale;
			}
			
			public final void setScale(final double scale) {
				this.scale = scale;
				
				final Level level0 = this.getLevels().get(0);
				
				this.setPreferredSize(new Dimension(
						this.scale(level0.getWidth()), this.scale(level0.getHeight())));
			}
			
			public final int scale(final int value) {
				return (int) (value * this.getScale());
			}
			
			public final int unscale(final int value) {
				return (int) (value / this.getScale());
			}
			
			public final double scale(final double value) {
				return value * this.getScale();
			}
			
			public final double unscale(final double value) {
				return value / this.getScale();
			}
			
			public final List<Level> getLevels() {
				return this.levels;
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -6178755769225969039L;
			
			/**
			 * @author codistmonk (creation 2014-06-13)
			 */
			public final class Level implements Serializable {
				
				private final int level;
				
				private final int width;
				
				private final int height;
				
				private final int tileWidth;
				
				private final int tileHeight;
				
				private final int maximumHorizontalTileCount;
				
				private final int maximumVerticalTileCount;
				
				private final double scale;
				
				private final Grid<BufferedImage> tiles;
				
				private int leftTileX;
				
				private int topTileY;
				
				public Level(final int level) {
					this.level = level;
					final IFormatReader reader = Context.this.getImage().getReader();
					
					reader.setSeries(level);
					
					this.width = reader.getSizeX();
					this.height = reader.getSizeY();
					this.tileWidth = reader.getOptimalTileWidth();
					this.tileHeight = reader.getOptimalTileHeight();
					this.maximumHorizontalTileCount = (int) (ceil((double) this.width / this.tileWidth));
					this.maximumVerticalTileCount = (int) (ceil((double) this.height / this.tileHeight));
					if (level == 0) {
						this.scale = 1.0;
					} else {
						final Level previousLevel = Context.this.getImageView().getLevels().get(level - 1);
						this.scale = min((double) this.width / previousLevel.getWidth()
								, (double) this.height / previousLevel.getHeight());
					}
					this.tiles = new Grid<>();
				}
				
				public final int getLevel() {
					return this.level;
				}
				
				public final int getWidth() {
					return this.width;
				}
				
				public final int getHeight() {
					return this.height;
				}
				
				public final int getTileWidth() {
					return this.tileWidth;
				}
				
				public final int getTileHeight() {
					return this.tileHeight;
				}
				
				public final double getScale() {
					return this.scale;
				}
				
				public final Grid<BufferedImage> getTiles() {
					return this.tiles;
				}
				
				public final int getLeftTileX() {
					return this.leftTileX;
				}
				
				public final int getTopTileY() {
					return this.topTileY;
				}
				
				public final int scale(final int value) {
					return (int) (value * this.getScale());
				}
				
				public final int unscale(final int value) {
					return (int) (value / this.getScale());
				}
				
				public final double scale(final double value) {
					return value * this.getScale();
				}
				
				public final double unscale(final double value) {
					return value / this.getScale();
				}
				
				public final Level updateTiles() {
					final Rectangle clipping = ImageView.this.getVisibleRect();
					final double[] viewCenterXYInLevel0 = ImageView.this.getViewCenterXYInLevel0();
					final double x0 = viewCenterXYInLevel0[0];
					final double y0 = viewCenterXYInLevel0[1];
					
					// Consider only the case when imageView.scale is close to this.scale
					final int startTileX = ((int) (this.scale(x0) - clipping.getWidth() / 2.0) / this.getTileWidth()) * this.getTileWidth();
					final int startTileY = ((int) (this.scale(y0) - clipping.getHeight() / 2.0) / this.getTileHeight()) * this.getTileHeight();
					final int horizontalTileCount = min(this.maximumHorizontalTileCount
							, 1 + (int) ceil(clipping.getWidth() / this.getTileWidth()));
					final int verticalTileCount = min(this.maximumVerticalTileCount
							, 1 + (int) ceil(clipping.getHeight() / this.getTileHeight()));
					
					this.getTiles().shiftRight(-(startTileX - this.leftTileX) / this.getTileWidth());
					this.getTiles().shiftDown(-(startTileY - this.topTileY) / this.getTileHeight());
					this.getTiles().setSize(verticalTileCount, horizontalTileCount);
					
					this.leftTileX = startTileX;
					this.topTileY = startTileY;
					final int endTileX = min(this.getWidth(), startTileX + horizontalTileCount * this.getTileWidth());
					final int endTileY = min(this.getHeight(), startTileY + verticalTileCount * this.getTileHeight());
					LociBackedImage image = Context.this.getImage();
					
					if (this.getLevel() != image.getSeriesIndex()) {
						image = new LociBackedImage(imagePath, this.getLevel());
					}
					
					debugPrint(startTileX, startTileY, horizontalTileCount, verticalTileCount);
					
					for (int tileY = startTileY, rowIndex = 0; tileY < endTileY; tileY += this.getTileHeight(), ++rowIndex) {
						final int h = min(this.getHeight(), tileY + this.getTileHeight()) - tileY;
						
						for (int tileX = startTileX, columnIndex = 0; tileX < endTileX; tileX += this.getTileWidth(), ++columnIndex) {
							final int w = min(this.getWidth(), tileX + this.getTileWidth()) - tileX;
							
							if (this.getTiles().getElement(rowIndex, columnIndex) == null) {
								this.getTiles().setElement(rowIndex, columnIndex, awtImage(
										image, tileX, tileY, w, h));
							}
						}
					}
					
					return this;
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 8857375410170866151L;
				
			}
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2014-06-13)
	 *
	 * @param <T>
	 */
	public static final class Grid<T> implements Serializable {
		
		private final List<List<T>> rows;
		
		public Grid() {
			this.rows = new ArrayList<>();
		}
		
		public final int getRowCount() {
			return this.rows.size();
		}
		
		public final int getColumnCount() {
			return this.getRowCount() == 0 ? 0 : this.rows.get(0).size();
		}
		
		public final T getElement(final int rowIndex, final int columnIndex) {
			return this.rows.get(rowIndex).get(columnIndex);
		}
		
		public final Grid<T> setElement(final int rowIndex, final int columnIndex, final T element) {
			this.rows.get(rowIndex).set(columnIndex, element);
			
			return this;
		}
		
		public final Grid<T> setSize(final int rowCount, final int columnCount) {
			final int oldRowCount = this.getRowCount();
			
			if (rowCount < oldRowCount) {
				this.rows.subList(rowCount, oldRowCount).clear();
			}
			
			final int oldColumnCount = this.getColumnCount();
			
			if (columnCount < oldColumnCount) {
				for (final List<T> row : this.rows) {
					row.subList(columnCount, oldColumnCount).clear();
				}
			} else if (oldColumnCount < columnCount) {
				final List<T> padding = Collections.nCopies(columnCount - oldColumnCount, null);
				
				for (final List<T> row : this.rows) {
					row.addAll(padding);
				}
			}
			
			if (oldRowCount < rowCount) {
				final List<T> prototype = Collections.nCopies(columnCount, null);
				
				for (int i = oldRowCount; i < rowCount; ++i) {
					this.rows.add(new ArrayList<>(prototype));
				}
			}
			
			return this;
		}
		
		public final Grid<T> shiftRight(final int shift) {
			final int columnCount = this.getColumnCount();
			
			if (0 == columnCount) {
				return this;
			}
			
			if (0 < shift) {
				for (final List<T> row : this.rows) {
					int destination = columnCount - 1;
					
					for (int source = destination - shift;
							0 <= source; --destination, --source) {
						row.set(destination, row.get(source));
					}
					
					while (0 <= destination) {
						row.set(destination--, null);
					}
				}
			} else if (shift < 0) {
				for (final List<T> row : this.rows) {
					int destination = 0;
					
					for (int source = -shift;
							source < columnCount; ++destination, ++source) {
						row.set(destination, row.get(source));
					}
					
					while (destination < columnCount) {
						row.set(destination++, null);
					}
				}
			}
			
			return this;
		}
		
		public final Grid<T> shiftDown(final int shift) {
			final int rowCount = this.getRowCount();
			
			if (0 == rowCount) {
				return this;
			}
			
			if (0 < shift) {
				int destination = rowCount - 1;
				
				for (int source = destination - shift;
						0 <= source; --destination, --source) {
					Collections.copy(this.rows.get(destination), this.rows.get(source));
				}
				
				while (0 <= destination) {
					Collections.fill(this.rows.get(destination--), null);
				}
			} else if (shift < 0) {
				int destination = 0;
				
				for (int source = -shift;
						source < rowCount; ++destination, ++source) {
					Collections.copy(this.rows.get(destination), this.rows.get(source));
				}
				
				while (destination < rowCount) {
					Collections.fill(this.rows.get(destination++), null);
				}
			}
			
			return this;
		}
		
		public final void debugPrintHashCodes() {
			final int columnCount = this.getColumnCount();
			
			if (0 == columnCount) {
				return;
			}
			
			final int[] hashCodes = new int[columnCount];
			
			for (final List<T> row : this.rows) {
				for (int i = 0; i < columnCount; ++i) {
					hashCodes[i] = System.identityHashCode(row.get(i));
				}
				
				Tools.getDebugOutput().println(Tools.debug(Tools.DEBUG_STACK_OFFSET + 1, Arrays.toString(hashCodes)));
			}
		}
		
		public final void debugPrint() {
			final int columnCount = this.getColumnCount();
			
			if (0 == columnCount) {
				return;
			}
			
			for (final List<T> row : this.rows) {
				Tools.getDebugOutput().println(Tools.debug(Tools.DEBUG_STACK_OFFSET + 1, row));
			}
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 2447049921043312882L;
		
	}
	
}
