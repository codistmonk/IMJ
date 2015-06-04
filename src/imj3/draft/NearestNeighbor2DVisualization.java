package imj3.draft;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.swing.JColorChooser;
import javax.swing.JComponent;

import multij.swing.MouseHandler;
import multij.swing.SwingTools;
import multij.tools.Canvas;
import multij.tools.IllegalInstantiationException;
import multij.tools.Pair;

/**
 * @author codistmonk (creation 2015-05-27)
 */
public final class NearestNeighbor2DVisualization {
	
	private NearestNeighbor2DVisualization() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		SwingTools.show(new NNCanvas(), NearestNeighbor2DVisualization.class.getName(), false);
	}
	
	/**
	 * @author codistmonk (creation 2015-05-27)
	 */
	public static final class NNCanvas extends JComponent {
		
		private final List<Vertex> vertices = new ArrayList<>();
		
		private final List<Color> availableColors = new ArrayList<>();
		
		private final Canvas components = new Canvas();
		
		private final Canvas colors = new Canvas();
		
		private boolean edgesVisible;
		
		{
			this.setPreferredSize(new Dimension(640, 480));
			
			final Vertex[] vertexUnderMouse = { null };
			
			new MouseHandler() {
				
				private final Point point = new Point();
				
				private int colorIndex;
				
				@Override
				public final void mouseClicked(final MouseEvent event) {
					this.mouseMoved(event);
					
					if (event.getClickCount() == 2 && event.getButton() == MouseEvent.BUTTON1) {
						final Vertex newVertex = new Vertex().setLocation(event.getX(), event.getY());
						
						NNCanvas.this.getVertices().add(newVertex);
						NNCanvas.this.updateAvailableColors(newVertex.getColor());
						
						this.mouseMoved(event);
					} else if (event.getClickCount() == 1 && event.getButton() != MouseEvent.BUTTON1 && vertexUnderMouse[0] != null) {
						final Color newColor = JColorChooser.showDialog(event.getComponent(), "Color", vertexUnderMouse[0].getColor());
						
						if (newColor != null) {
							vertexUnderMouse[0].setColor(newColor);
							
							NNCanvas.this.updateAvailableColors(newColor);
						}
					}
				}
				
				@Override
				public final void mouseMoved(final MouseEvent event) {
					this.point.setLocation(event.getX(), event.getY());
					
					final Pair<Integer, Double> nearest = findNearest(this.point);
					
					vertexUnderMouse[0] = nearest.getSecond() < 8.0 ? getVertex(nearest.getFirst()) : null;
				}
				
				@Override
				public final void mouseDragged(final MouseEvent event) {
					if (vertexUnderMouse[0] != null) {
						vertexUnderMouse[0].setLocation(event.getX(), event.getY());
						repaint();
					}
				}
				
				@Override
				public final void mouseWheelMoved(final MouseWheelEvent event) {
					this.mouseMoved(event);
					
					final int n = NNCanvas.this.availableColors.size();
					
					if (vertexUnderMouse[0] != null && 0 < n) {
						this.colorIndex %= n;
						vertexUnderMouse[0].setColor(NNCanvas.this.availableColors.get(this.colorIndex));
						this.colorIndex = (this.colorIndex + n + (event.getWheelRotation() < 0 ? -1 : 1)) % n;
						repaint();
					}
				}
				
				private static final long serialVersionUID = -3681411842554328167L;
				
			}.addTo(this);
			
			this.addKeyListener(new KeyAdapter() {
				
				@Override
				public final void keyPressed(final KeyEvent event) {
					if (event.getKeyCode() == KeyEvent.VK_B) {
						setEdgesVisible(!isEdgesVisible());
					} else if (event.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
						getVertices().remove(vertexUnderMouse[0]);
						updateAvailableColors(null);
					}
				}
				
			});
			
			this.setFocusable(true);
			this.requestFocusInWindow();
		}
		
		final void updateAvailableColors(final Color newColor) {
			if (newColor != null && !this.availableColors.contains(newColor)) {
				this.availableColors.add(newColor);
			}
			
			remove_unused_colors:
			for (final Iterator<Color> i = this.availableColors.iterator(); i.hasNext();) {
				final Color c = i.next();
				
				for (final Vertex vertex : getVertices()) {
					if (vertex.getColor().equals(c)) {
						continue remove_unused_colors;
					}
				}
				
				i.remove();
			}
			
			NNCanvas.this.repaint();
		}
		
		public final boolean isEdgesVisible() {
			return this.edgesVisible;
		}
		
		public final NNCanvas setEdgesVisible(final boolean edgesVisible) {
			this.edgesVisible = edgesVisible;
			this.repaint();
			
			return this;
		}
		
		public final List<Vertex> getVertices() {
			return this.vertices;
		}
		
		public final Vertex getVertex(final int index) {
			return this.getVertices().get(index);
		}
		
		public final Pair<Integer, Double> findNearest(final Point point) {
			int bestIndex = -1;
			double bestDistance = Double.POSITIVE_INFINITY;
			final List<Vertex> vertices = this.getVertices();
			final int n = vertices.size();
			
			for (int index = 0; index < n; ++index) {
				final double distance = point.distance(vertices.get(index).getLocation());
				
				if (distance < bestDistance) {
					bestIndex = index;
					bestDistance = distance;
				}
			}
			
			return new Pair<>(bestIndex, bestDistance);
		}
		
		@Override
		protected final void paintComponent(final Graphics graphics) {
			super.paintComponent(graphics);
			
			final int width = this.getWidth();
			final int height = this.getHeight();
			
			if (0 < width && 0 < height) {
				this.components.setFormat(width, height);
				this.colors.setFormat(width, height);
				
				final Point point = new Point();
				
				for (int y = 0; y < height; ++y) {
					for (int x = 0; x < width; ++x) {
						point.setLocation(x, y);
						
						final int nearest = this.findNearest(point).getFirst();
						
						if (0 <= nearest) {
							this.components.getImage().setRGB(x, y, nearest);
							this.colors.getImage().setRGB(x, y, this.getVertex(nearest).getColor().getRGB());
						}
					}
				}
				
				if (this.isEdgesVisible()) {
					for (int y = 0; y < height; ++y) {
						for (int x = 0; x < width; ++x) {
							final int component = this.components.getImage().getRGB(x, y);
							final int northComponent = 0 < y ? this.components.getImage().getRGB(x, y - 1) : component;
							final int westComponent = 0 < x ? this.components.getImage().getRGB(x - 1, y) : component;
							final int eastComponent = x + 1 < width ? this.components.getImage().getRGB(x + 1, y) : component;
							final int southComponent = y + 1 < height ? this.components.getImage().getRGB(x, y + 1) : component;
							
							if (component != northComponent || component != westComponent || component != eastComponent || component != southComponent) {
								this.colors.getImage().setRGB(x, y, 0xFF000000);
							}
						}
					}
				}
				
				final int r = 2;
				final Graphics2D canvasGraphics = this.colors.getGraphics();
				
				canvasGraphics.setColor(Color.BLACK);
				
				this.vertices.forEach(vertex -> canvasGraphics.fillOval(vertex.getLocation().x - r, vertex.getLocation().y - r, 2 * r, 2 * r));
				
				graphics.drawImage(this.colors.getImage(), 0, 0, null);
			}
		}
		
		private static final long serialVersionUID = 4782134068163833188L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-05-27)
	 */
	public static final class Vertex implements Serializable {
		
		private final Point location = new Point();
		
		private Color color = new Color(new Random().nextInt(1 << 24));
		
		public final Color getColor() {
			return this.color;
		}
		
		public final Vertex setColor(final Color color) {
			this.color = color;
			
			return this;
		}
		
		public final Point getLocation() {
			return this.location;
		}
		
		public final Vertex setLocation(final int x, final int y) {
			this.getLocation().setLocation(x, y);
			
			return this;
		}
		
		private static final long serialVersionUID = 7802533716319504591L;
		
	}
	
}
