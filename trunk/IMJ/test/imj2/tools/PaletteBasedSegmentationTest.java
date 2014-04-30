package imj2.tools;

import static java.lang.Integer.parseInt;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.util.Arrays.copyOfRange;
import static net.sourceforge.aprog.swing.SwingTools.horizontalSplit;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.swing.SwingTools.verticalSplit;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getOrCreate;
import static pixel3d.PolygonTools.*;
import imj2.tools.Image2DComponent.Painter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import jgencode.primitivelists.DoubleList;
import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.Factory;
import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;

import pixel3d.MouseHandler;
import pixel3d.OrbiterMouseHandler;
import pixel3d.OrthographicRenderer;
import pixel3d.OrbiterMouseHandler.OrbiterParameters;
import pixel3d.Renderer;
import pixel3d.TiledRenderer;

/**
 * @author codistmonk (creation 2014-04-23)
 */
public final class PaletteBasedSegmentationTest {
	
	@Test
	public final void test() {
		SwingTools.useSystemLookAndFeel();
		SwingTools.setCheckAWT(false);
		
		final SimpleImageView imageView = new SimpleImageView();
		final HistogramView histogramView = new HistogramView();
		final GenericTree clustersEditor = new GenericTree("Clusters");
		final JSplitPane splitPane = horizontalSplit(imageView, verticalSplit(clustersEditor, histogramView));
		
		SwingTools.setCheckAWT(true);
		
		clustersEditor.getModel().addTreeModelListener(new TreeModelListener() {
			
			@Override
			public final void treeNodesChanged(final TreeModelEvent event) {
				imageView.refreshBuffer();
			}
			
			@Override
			public final void treeNodesInserted(final TreeModelEvent event) {
				imageView.refreshBuffer();
			}
			
			@Override
			public final void treeNodesRemoved(final TreeModelEvent event) {
				imageView.refreshBuffer();
			}
			
			@Override
			public final void treeStructureChanged(final TreeModelEvent event) {
				imageView.refreshBuffer();
			}
			
		});
		
		imageView.getPainters().add(new Painter<SimpleImageView>() {
			
			@Override
			public final void paint(final Graphics2D g, final SimpleImageView component,
					final int width, final int height) {
				final Map<Integer, Collection<Integer>> clusters = getClusters(clustersEditor);
				
				debugPrint(clusters);
				
				final BufferedImage image = imageView.getImage();
				final BufferedImage buffer = imageView.getBufferImage();
				final int w = image.getWidth();
				final int h = image.getHeight();
				
				for (int y = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x) {
						final int rgb = image.getRGB(x, y);
						int bestCluster = rgb;
						int bestDistance = Integer.MAX_VALUE;
						
						for (final Map.Entry<Integer, Collection<Integer>> entry : clusters.entrySet()) {
							for (final Integer prototype : entry.getValue()) {
								final int distance = distance1(rgb, prototype);
								
								if (distance < bestDistance) {
									bestDistance = distance;
									bestCluster = entry.getKey();
								}
							}
						}
						
						buffer.setRGB(x, y, bestCluster);
					}
				}
				
				histogramView.refresh(image);
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 646058874106526093L;
			
		});
		
		show(splitPane, this.getClass().getSimpleName(), true);
	}
	
	public static final int distance1(final int rgb1, final int rgb2) {
		int result = 0;
		
		result += abs(((rgb1 >> 16) & 0xFF) - ((rgb2 >> 16) & 0xFF));
		result += abs(((rgb1 >> 8) & 0xFF) - ((rgb2 >> 8) & 0xFF));
		result += abs(((rgb1 >> 0) & 0xFF) - ((rgb2 >> 0) & 0xFF));
		
		return result;
	}
	
	public static final Map<Integer, Collection<Integer>> getClusters(final GenericTree clustersEditor) {
		final Map<Integer, Collection<Integer>> result = new TreeMap<Integer, Collection<Integer>>();
		final DefaultTreeModel model = clustersEditor.getModel();
		final Object root = model.getRoot();
		
		for (int i = 0; i < model.getChildCount(root); ++i) {
			final Object cluster = model.getChild(root, i);
			final Integer clusterRGB = stringsToRGB(cluster);
			
			for (int j = 0; j < model.getChildCount(cluster); ++j) {
				getOrCreate((Map) result, clusterRGB, Factory.DefaultFactory.TREE_SET_FACTORY)
				.add(stringsToRGB(model.getChild(cluster, j)));
			}
		}
		
		return result;
	}
	
	public static final int stringsToRGB(final Object object) {
		final String[] strings = object.toString().split("\\s+");
		
		return 0xFF000000 | ((parseInt(strings[0]) & 0xFF) << 16) | ((parseInt(strings[1]) & 0xFF) << 8) | ((parseInt(strings[2]) & 0xFF) << 0); 
	}
	
	/**
	 * @author codistmonk (creation 2014-04-23)
	 */
	public static final class GenericTree extends JTree {
		
		public GenericTree(final Object rootData) {
			super(new DefaultMutableTreeNode(rootData));
			
			this.setEditable(true);
			
			this.addMouseListener(new MouseAdapter() {
				
				@Override
				public final void mousePressed(final MouseEvent event) {
					this.mousePressedOrReleased(event);
				}
				
				@Override
				public final void mouseReleased(final MouseEvent event) {
					this.mousePressedOrReleased(event);
				}
				
				final void mousePressedOrReleased(final MouseEvent event) {
					if (event.isPopupTrigger()) {
						final int row = GenericTree.this.getRowForLocation(event.getX(), event.getY());
						
						if (row < 0) {
							return;
						}
						
						GenericTree.this.setSelectionRow(row);
						final DefaultMutableTreeNode node = (DefaultMutableTreeNode) GenericTree.this.getSelectionPath().getLastPathComponent();
						
						final JPopupMenu popup = new JPopupMenu();
						
						popup.add(new AbstractAction("Add...") {

							@Override
							public final void actionPerformed(final ActionEvent event) {
								final Object childData = JOptionPane.showInputDialog("New child:");
								
								if (childData != null) {
									GenericTree.this.getModel().insertNodeInto(new DefaultMutableTreeNode(childData), node, node.getChildCount());
								}
							}
							
							/**
							 * {@value}.
							 */
							private static final long serialVersionUID = 3476173857535861284L;
							
						});
						
						if (0 < row) {
							popup.addSeparator();
							
							popup.add(new AbstractAction("Remove") {
								
								@Override
								public final void actionPerformed(final ActionEvent event) {
									GenericTree.this.getModel().removeNodeFromParent(node);
								}
								
								/**
								 * {@value}.
								 */
								private static final long serialVersionUID = 7037260191035026652L;
								
							});
						}
						
						popup.show(event.getComponent(), event.getX(), event.getY());
					}
				}
				
			});
		}
		
		public GenericTree() {
			this("");
		}
		
		@Override
		public DefaultTreeModel getModel() {
			return (DefaultTreeModel) super.getModel();
		}
		
		@Override
		public final void setModel(final TreeModel newModel) {
			super.setModel((DefaultTreeModel) newModel);
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 2264597555092857049L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-04-28)
	 */
	public static final class HistogramView extends JLabel {
		
		private final Canvas canvas;
		
		private final Canvas idCanvas;
		
		private final BitSet histogram;
		
		private double[] histogramPoints;
		
		private int[] histogramARGBs;
		
		private final Renderer histogramRenderer;
		
		private final Renderer idRenderer;
		
		private final OrbiterMouseHandler orbiter;
		
		private final Graphics3D histogramGraphics;
		
		private final Graphics3D idGraphics;
		
		private final DoubleList userPoints;
		
		private BufferedImage oldImage;
		
		private final int[] idUnderMouse;
		
		private final int[] lastTouchedId;
		
		public HistogramView() {
			new MouseHandler(null) {
				
				private final Point lastMouseLocation = new Point();
				
				@Override
				public final void mousePressed(final MouseEvent event) {
					this.lastMouseLocation.setLocation(event.getPoint());
					lastTouchedId[0] = idUnderMouse[0] = idCanvas.getImage().getRGB(event.getX(), event.getY()) & 0x00FFFFFF;
					
					HistogramView.this.refresh();
				}
				
				@Override
				public final void mouseClicked(final MouseEvent event) {
					if (event.getButton() == 1 && event.getClickCount() == 2) {
						final double[] point = this.getPoint(event, 0.0);
						
						userPoints.addAll(point);
						idUnderMouse[0] = userPoints.size() / 3;
						
						HistogramView.this.refresh();
					}
				}
				
				@Override
				public final void mouseDragged(final MouseEvent event) {
					if (0 < idUnderMouse[0]) {
						final int offset = (idUnderMouse[0] - 1) * 3;
						final double[] tmp = copyOfRange(userPoints.toArray(), offset, offset + 3);
						
						orbiter.transform(tmp);
						
						System.arraycopy(this.getPoint(event, tmp[Z]), 0, userPoints.toArray(), offset, 3);
						
						event.consume();
					}
				}
				
				@Override
				public final void mouseMoved(final MouseEvent event) {
					idUnderMouse[0] = idCanvas.getImage().getRGB(event.getX(), event.getY()) & 0x00FFFFFF;
					
					HistogramView.this.refresh();
				}
				
				public final double[] getPoint(final MouseEvent event, final double z) {
					final double[] result = { event.getX(), (canvas.getHeight() - 1 - event.getY()), z };
					
					orbiter.inverseTransform(result);
					
					return result;
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 2866844463835255393L;
				
			}.addTo(this);
			
			this.orbiter = new OrbiterMouseHandler(null).addTo(this);
			
			new MouseHandler(null) {
				
				@Override
				public final void mouseWheelMoved(final MouseWheelEvent event) {
					HistogramView.this.refresh();
				}
				
				@Override
				public final void mouseDragged(final MouseEvent event) {
					HistogramView.this.refresh();
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 465287425693150361L;
				
			}.addTo(this);
			
			this.canvas = new Canvas().setFormat(512, 512, BufferedImage.TYPE_INT_ARGB);
			this.idCanvas = new Canvas().setFormat(this.canvas.getWidth(), this.canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
			this.histogram = new BitSet(0x00FFFFFF);
			this.histogramRenderer = new TiledRenderer(OrthographicRenderer.FACTORY).setCanvas(this.canvas.getImage());
			this.idRenderer = new TiledRenderer(OrthographicRenderer.FACTORY).setCanvas(this.idCanvas.getImage());
			this.histogramGraphics = new Graphics3D(this.histogramRenderer).setOrbiterParameters(this.orbiter.getParameters());
			this.idGraphics = new Graphics3D(this.idRenderer).setOrbiterParameters(this.orbiter.getParameters());
			this.userPoints = new DoubleList();
			this.idUnderMouse = new int[1];
			this.lastTouchedId = new int[1];
			
			this.setIcon(new ImageIcon(this.canvas.getImage()));
		}
		
		public final void refresh() {
			this.refresh(this.oldImage);
		}
		
		public final void refresh(final BufferedImage image) {
			if (image == null) {
				this.oldImage = null;
				
				return;
			}
			
			final double x0 = this.canvas.getWidth() / 2;
			final double y0 = this.canvas.getHeight() / 2;
			final double z0 = 0.0;
			final double tx = x0 - 128.0;
			final double ty = y0 - 128.0;
			final double tz = z0 - 128.0;
			final TicToc timer = new TicToc();
			final DoubleList times = new DoubleList();
			
			this.histogramGraphics.getOrbiterParameters().setCenterX(x0).setCenterY(y0);
			
			timer.tic();
			
			if (this.oldImage != image) {
				this.oldImage = image;
				final int w = image.getWidth();
				final int h = image.getHeight();
				
				this.histogram.clear();
				
				for (int y = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x) {
						this.histogram.set(image.getRGB(x, y) & 0x00FFFFFF);
					}
				}
				
				final int n = this.histogram.cardinality();
				
				debugPrint(n);
				
				this.histogramPoints = new double[n * 3];
				this.histogramARGBs = new int[n];
				
				for (int rgb = 0x00000000, i = 0, j = 0; rgb <= 0x00FFFFFF; ++rgb) {
					if (this.histogram.get(rgb)) {
						this.histogramPoints[i++] = ((rgb >> 16) & 0xFF) + tx;
						this.histogramPoints[i++] = ((rgb >> 8) & 0xFF) + ty;
						this.histogramPoints[i++] = ((rgb >> 0) & 0xFF) + tz;
						this.histogramARGBs[j++] = 0xFF000000 | rgb;
					}
				}
			}
			
			times.add(tocTic(timer));
			
			this.histogramRenderer.clear();
			this.idRenderer.clear();
			
			this.histogramGraphics.setPointSize(0).transformAndDrawPoints(
					this.histogramPoints.clone(), this.histogramARGBs);
			
			times.add(tocTic(timer));
			
			this.drawBox(tx, ty, tz);
			
			times.add(tocTic(timer));
			
			{
				final double[] userPoints = this.userPoints.toArray();
				final int n = userPoints.length;
				
				this.histogramGraphics.setPointSize(2);
				this.idGraphics.setPointSize(2);
				
				for (int i = 0, id = 1; i < n; i += 3, ++id) {
					final double x = userPoints[i + X];
					final double y = userPoints[i + Y];
					final double z = userPoints[i + Z];
					
					this.histogramGraphics.drawPoint(x, y, z, idUnderMouse[0] == id ? 0xFFFFFF00 : 0xFF0000FF);
					this.idGraphics.drawPoint(x, y, z, 0xFF000000 | id);
				}
			}
			
			times.add(tocTic(timer));
			
			this.canvas.clear(Color.GRAY);
			this.idCanvas.clear(Color.BLACK);
			
			this.histogramRenderer.render();
			this.idRenderer.render();
			
			times.add(tocTic(timer));
			
			if (false) {
				debugPrint(times);
			}
			
			this.repaint();
		}
		
		public static final long tocTic(final TicToc timer) {
			final long result = timer.toc();
			
			timer.tic();
			
			return result;
		}
		
		private final void drawBox(final double tx, final double ty, final double tz) {
			this.histogramGraphics.drawSegment(
					0.0 + tx, 0.0 + ty, 0.0 + tz,
					255.0 + tx, 0.0 + ty, 0.0 + tz,
					0xFFFF0000);
			this.histogramGraphics.drawSegment(
					0.0 + tx, 0.0 + ty, 0.0 + tz,
					0.0 + tx, 255.0 + ty, 0.0 + tz,
					0xFF00FF00);
			this.histogramGraphics.drawSegment(
					0.0 + tx, 0.0 + ty, 0.0 + tz,
					0.0 + tx, 0.0 + ty, 255.0 + tz,
					0xFF0000FF);
			this.histogramGraphics.drawSegment(
					0.0 + tx, 255.0 + ty, 255.0 + tz,
					255.0 + tx, 255.0 + ty, 255.0 + tz,
					0xFFFFFFFF);
			this.histogramGraphics.drawSegment(
					255.0 + tx, 0.0 + ty, 255.0 + tz,
					255.0 + tx, 255.0 + ty, 255.0 + tz,
					0xFFFFFFFF);
			this.histogramGraphics.drawSegment(
					255.0 + tx, 255.0 + ty, 0.0 + tz,
					255.0 + tx, 255.0 + ty, 255.0 + tz,
					0xFFFFFFFF);
			this.histogramGraphics.drawSegment(
					0.0 + tx, 0.0 + ty, 255.0 + tz,
					255.0 + tx, 0.0 + ty, 255.0 + tz,
					0xFFFFFFFF);
			this.histogramGraphics.drawSegment(
					0.0 + tx, 0.0 + ty, 255.0 + tz,
					0.0 + tx, 255.0 + ty, 255.0 + tz,
					0xFFFFFFFF);
			this.histogramGraphics.drawSegment(
					0.0 + tx, 255.0 + ty, 0.0 + tz,
					255.0 + tx, 255.0 + ty, 0.0 + tz,
					0xFFFFFFFF);
			this.histogramGraphics.drawSegment(
					0.0 + tx, 255.0 + ty, 0.0 + tz,
					0.0 + tx, 255.0 + ty, 255.0 + tz,
					0xFFFFFFFF);
			this.histogramGraphics.drawSegment(
					255.0 + tx, 0.0 + ty, 0.0 + tz,
					255.0 + tx, 255.0 + ty, 0.0 + tz,
					0xFFFFFFFF);
			this.histogramGraphics.drawSegment(
					255.0 + tx, 0.0 + ty, 0.0 + tz,
					255.0 + tx, 0.0 + ty, 255.0 + tz,
					0xFFFFFFFF);
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 8150020673886684998L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-04-29)
	 */
	public static final class Graphics3D implements Serializable {
		
		private final Renderer renderer;
		
		private OrbiterParameters orbiterParameters;
		
		private int pointSize;
		
		public Graphics3D(final Renderer renderer) {
			this.renderer = renderer;
			this.orbiterParameters = new OrbiterParameters();
		}
		
		public final OrbiterParameters getOrbiterParameters() {
			return this.orbiterParameters;
		}
		
		public final Graphics3D setOrbiterParameters(final OrbiterParameters orbiterParameters) {
			this.orbiterParameters = orbiterParameters;
			
			return this;
		}
		
		public final int getPointSize() {
			return this.pointSize;
		}
		
		public final Graphics3D setPointSize(final int pointSize) {
			this.pointSize = pointSize;
			
			return this;
		}
		
		public final Graphics3D drawSegment(final double x1, final double y1, final double z1,
				final double x2, final double y2, final double z2, final int argb) {
			final double[] extremities = { x1, y1, z1, x2, y2, z2 };
			
			this.transform(extremities);
			
			final double dx = extremities[3 + X] - extremities[0 + X];
			final double dy = extremities[3 + Y] - extremities[0 + Y];
			final double dz = extremities[3 + Z] - extremities[0 + Z];
			final int d = 1 + (int) max(abs(dx), abs(dy));
			
			for (int i = 0; i < d; ++i) {
				this.renderer.addPixel(
						extremities[0 + X] + i * dx / d,
						extremities[0 + Y] + i * dy / d,
						extremities[0 + Z] + i * dz / d,
						argb);
			}
			
			return this;
		}
		
		public final Graphics3D transformAndDrawPoints(final double[] points, final int[] argbs) {
			this.transform(points);
			
			for (int i = 0, j = 0; i < points.length; i += 3, ++j) {
				for (int ty = -this.pointSize; ty <= this.pointSize; ++ty) {
					for (int tx = -this.pointSize; tx <= this.pointSize; ++tx) {
						this.renderer.addPixel(points[i + X] + tx, points[i + Y] + ty, points[i + Z], argbs[j]);
					}
				}
			}
			
			return this;
		}
		
		public final Graphics3D drawPoint(final double x, final double y, final double z, final int argb) {
			return this.transformAndDrawPoints(new double[] { x, y, z }, new int[] { argb });
		}
		
		public final Graphics3D transform(final double... points) {
			OrbiterMouseHandler.transform(points, this.getOrbiterParameters());
			
			return this;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -1033925831595591034L;
		
	}
	
}
