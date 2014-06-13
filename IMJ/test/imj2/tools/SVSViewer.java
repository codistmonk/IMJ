package imj2.tools;

import static net.sourceforge.aprog.swing.SwingTools.getFiles;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.swing.SwingTools.show;
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
		
		private final IFormatReader reader;
		
		private final JPanel mainPanel;
		
		private final JComponent imageView;
		
		public Context() {
			this.reader = new ImageReader();
			this.mainPanel = new JPanel(new BorderLayout());
			this.mainPanel.setPreferredSize(new Dimension(512, 512));
			this.imageView = this.new ImageView();
			SwingTools.setCheckAWT(false);
			this.mainPanel.add(scrollable(this.imageView), BorderLayout.CENTER);
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
				this.getReader().setId(path);
				this.getImageView().setPreferredSize(
						new Dimension(this.getReader().getSizeX(), this.getReader().getSizeY()));
				this.getImageView().revalidate();
				this.getMainPanel().repaint();
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		}
		
		public final IFormatReader getReader() {
			return this.reader;
		}
		
		public final JPanel getMainPanel() {
			return this.mainPanel;
		}
		
		public final JComponent getImageView() {
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
			
			private final List<List<BufferedImage>> tiles;
			
			public ImageView() {
				this.tiles = new ArrayList<>();
			}
			
			@Override
			protected final void paintComponent(final Graphics g) {
				super.paintComponent(g);
				
				if (Context.this.getImagePath() == null) {
					final String string = "DROP IMAGE HERE";
					final Rectangle stringBounds = g.getFontMetrics().getStringBounds(string, g).getBounds();
					
					g.drawString(string, this.getWidth() / 2 - stringBounds.width / 2, this.getHeight() / 2 + stringBounds.height / 2);
				} else {
					// TODO
				}
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -6178755769225969039L;
			
			/**
			 * @author codistmonk (creation 2014-06-13)
			 */
			public final class Level implements Serializable {
				
				private final int width;
				
				private final int height;
				
				private final int tileWidth;
				
				private final int tileHeight;
				
				public Level(final int level) {
					final IFormatReader reader = Context.this.getReader();
					
					reader.setSeries(level);
					
					this.width = reader.getSizeX();
					this.height = reader.getSizeY();
					this.tileWidth = reader.getOptimalTileWidth();
					this.tileHeight = reader.getOptimalTileHeight();
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
					
					for (int source = shift;
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
					this.rows.set(destination, this.rows.get(source));
				}
				
				while (0 <= destination) {
					this.rows.set(destination--, null);
				}
			} else if (shift < 0) {
				int destination = 0;
				
				for (int source = shift;
						source < rowCount; ++destination, ++source) {
					this.rows.set(destination, this.rows.get(source));
				}
				
				while (destination < rowCount) {
					this.rows.set(destination++, null);
				}
			}
			
			return this;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 2447049921043312882L;
		
	}
	
}
