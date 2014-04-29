package imj2.tools;

import static java.lang.Integer.parseInt;
import static java.lang.Math.abs;
import static net.sourceforge.aprog.swing.SwingTools.horizontalSplit;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.swing.SwingTools.verticalSplit;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getOrCreate;
import static org.junit.Assert.*;
import static pixel3d.OrbiterMouseHandler.transform;
import static pixel3d.PolygonTools.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import imj2.tools.Image2DComponent.Painter;
import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.Factory;
import net.sourceforge.aprog.tools.Tools;

import org.apache.log4j.lf5.viewer.categoryexplorer.TreeModelAdapter;
import org.junit.Test;

import pixel3d.MouseHandler;
import pixel3d.OrbiterMouseHandler;
import pixel3d.OrthographicRenderer;
import pixel3d.PolygonTools;
import pixel3d.OrbiterMouseHandler.OrbiterParameters;

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
		
		clustersEditor.getModel().addTreeModelListener(new TreeModelAdapter() {
			
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
		
		private final BitSet histogram;
		
		private final OrthographicRenderer histogramRenderer;
		
		private final OrbiterMouseHandler orbiter;
		
		private final Graphics3D histogramGraphics;
		
		private BufferedImage oldImage;
		
		public HistogramView() {
			this.canvas = new Canvas().setFormat(512, 512, BufferedImage.TYPE_INT_ARGB);
			this.histogram = new BitSet(0x00FFFFFF);
			this.histogramRenderer = new OrthographicRenderer(this.canvas.getImage());
			this.orbiter = new OrbiterMouseHandler(null).addTo(this);
			this.histogramGraphics = new Graphics3D(this.histogramRenderer).setOrbiterParameters(this.orbiter.getParameters());
			
			this.setIcon(new ImageIcon(this.canvas.getImage()));
			
			new MouseHandler(this.orbiter.getUpdateNeeded()) {
				
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
		}
		
		public final void refresh() {
			this.refresh(this.oldImage);
		}
		
		public final void refresh(final BufferedImage image) {
			if (this.oldImage != image) {
				this.oldImage = image;
				final int w = image.getWidth();
				final int h = image.getHeight();
				
				this.histogram.clear();
				
				for (int y = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x) {
						final int rgb = image.getRGB(x, y);
						
						this.histogram.set(rgb & 0x00FFFFFF);
					}
				}
				
				debugPrint(this.histogram.cardinality());
			}
			
			this.histogramRenderer.clear();
			
//			final int x0 = this.canvas.getWidth() / 2;
//			final int y0 = this.canvas.getHeight() / 2;
			
			this.histogramGraphics.getOrbiterParameters()
					.setCenterX(this.canvas.getWidth() / 2)
					.setCenterY(this.canvas.getHeight() / 2);
			
			for (int rgb = 0x00000000; rgb <= 0x00FFFFFF; ++rgb) {
				if (this.histogram.get(rgb)) {
					this.histogramGraphics.drawPoint(
							((rgb >> 16) & 0xFF) + 128,
							((rgb >> 8) & 0xFF) + 128,
							((rgb >> 0) & 0xFF) - 128,
							0xFF000000 | rgb);
				}
			}
			
			this.canvas.clear(Color.GRAY);
			this.histogramRenderer.render();
			
			this.repaint();
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
		
		private final OrthographicRenderer renderer;
		
		private OrbiterParameters orbiterParameters;
		
		public Graphics3D(final OrthographicRenderer renderer) {
			this.renderer = renderer;
			this.orbiterParameters = new OrbiterParameters();
		}
		
		public final OrbiterParameters getOrbiterParameters() {
			return this.orbiterParameters;
		}
		
		public final Graphics3D setOrbiterParameters(OrbiterParameters orbiterParameters) {
			this.orbiterParameters = orbiterParameters;
			
			return this;
		}
		
		public final double getCenterX() {
			return this.getOrbiterParameters().getCenterX();
		}
		
		public final double getCenterY() {
			return this.getOrbiterParameters().getCenterY();
		}
		
		public final double getCenterZ() {
			return this.getOrbiterParameters().getCenterZ();
		}
		
		public final double getRoll() {
			return this.getOrbiterParameters().getRoll();
		}
		
		public final double getPitch() {
			return this.getOrbiterParameters().getPitch();
		}
		
		public final double getScale() {
			return this.getOrbiterParameters().getScale();
		}
		
		public final Graphics3D drawPoint(final double x, final double y, final double z, final int argb) {
			final double[] point = new double[] { x, y, z };
			
			this.transform(point);
			
			this.renderer.addPixel(point[X], point[Y], point[Z], argb);
			
			return this;
		}
		
		public final Graphics3D transform(final double... points) {
			OrbiterMouseHandler.transform(points,
					this.getRoll(), this.getPitch(), this.getScale(),
					this.getCenterX(), this.getCenterY(), this.getCenterZ());
			
			return this;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -1033925831595591034L;
		
	}
	
}
