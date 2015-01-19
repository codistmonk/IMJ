package imj3.draft;

import static imj3.core.Channels.Predefined.blue8;
import static imj3.core.Channels.Predefined.green8;
import static imj3.core.Channels.Predefined.red8;
import static imj3.draft.VisualSegmentation.getSharedProperty;
import static imj3.draft.VisualSegmentation.newItem;
import static imj3.draft.VisualSegmentation.property;
import static imj3.draft.VisualSegmentation.repeat;
import static imj3.draft.VisualSegmentation.setSharedProperty;
import static imj3.draft.VisualSegmentation.showEditDialog;
import static java.lang.Math.abs;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.baseName;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.ignore;
import imj2.pixel3d.MouseHandler;
import imj2.tools.Canvas;
import imj3.draft.TrainableSegmentation.ImageComponent.Painter;
import imj3.tools.AwtImage2D;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Pair;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;
import net.sourceforge.aprog.xml.XMLTools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author codistmonk (creation 2015-01-16)
 */
public final class TrainableSegmentation {
	
	private TrainableSegmentation() {
		throw new IllegalInstantiationException();
	}
	
	public static final String PALETTE_XML = "palette.xml";
	
	static final Preferences preferences = Preferences.userNodeForPackage(TrainableSegmentation.class);
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		
		SwingTools.useSystemLookAndFeel();
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public final void run() {
				final JFrame mainFrame = new JFrame();
				final JToolBar toolBar = new JToolBar();
				final JTree tree = newQuantizerTree();
				final Component[] view = { null };
				
				toolBar.add(new JComboBox<>(array("Train and classify", "classify")));
				toolBar.add(new JButton(new AbstractAction("Run") {
					
					@Override
					public final void actionPerformed(final ActionEvent event) {
						final Quantizer quantizer = (Quantizer) tree.getModel().getRoot();
						final BufferedImage image = ((ImageComponent) getSharedProperty(mainFrame, "view")).getImage();
						final Canvas groundTruth = getSharedProperty(mainFrame, "groundtruth");
						final Canvas labels = getSharedProperty(mainFrame, "labels");
						// TODO Auto-generated method stub
						Tools.debugPrint("TODO");
					}
					
				}));
				toolBar.addSeparator();
				toolBar.add(new JLabel(" TP: "));
				toolBar.add(new JLabel("-------"));
				toolBar.add(new JLabel(" FP: "));
				toolBar.add(new JLabel("-------"));
				toolBar.add(new JLabel(" TN: "));
				toolBar.add(new JLabel("-------"));
				toolBar.add(new JLabel(" FN: "));
				toolBar.add(new JLabel("-------"));
				toolBar.addSeparator();
				toolBar.add(new JLabel(" F1: "));
				toolBar.add(new JLabel("---"));
				toolBar.add(new JLabel(" F1°: "));
				toolBar.add(new JLabel("---"));
				
				tree.addTreeExpansionListener(new TreeExpansionListener() {
					
					@Override
					public final void treeExpanded(final TreeExpansionEvent event) {
						mainFrame.validate();
					}
					
					@Override
					public final void treeCollapsed(final TreeExpansionEvent event) {
						mainFrame.validate();
					}
					
				});
				
				mainFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				
				mainFrame.setPreferredSize(new Dimension(512, 512));
				mainFrame.add(toolBar, BorderLayout.NORTH);
				mainFrame.add(scrollable(tree), BorderLayout.WEST);
				
				setSharedProperty(mainFrame, "tree", tree);
				
				mainFrame.setDropTarget(new DropTarget() {
					
					@Override
					public final synchronized void drop(final DropTargetDropEvent event) {
						final File file = SwingTools.getFiles(event).get(0);
						
						setView(mainFrame, view, file);
						
						preferences.put("filePath", file.getPath());
					}
					
					/**
					 * {@value}.
					 */
					private static final long serialVersionUID = 5442000733451223725L;
					
				});
				
				SwingTools.packAndCenter(mainFrame).setVisible(true);
				
				{
					final String filePath = preferences.get("filePath", "");
					
					if (!filePath.isEmpty()) {
						setView(mainFrame, view, new File(filePath));
					}
				}
				
				repeat(mainFrame, 30_000, e -> {
					final DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
					
					try (final OutputStream output = new FileOutputStream(PALETTE_XML)) {
						synchronized (model) {
							writePaletteXML((Quantizer) model.getRoot(), output);
							final File imageFile = getSharedProperty(mainFrame, "file");
							final File groundtruthFile = groundtruthFile(imageFile);
							Tools.debugPrint("Writing", groundtruthFile);
							ImageIO.write(((Canvas) getSharedProperty(mainFrame, "groundtruth")).getImage(),
									"png", groundtruthFile);
						}
					} catch (final IOException exception) {
						exception.printStackTrace();
					}
				});
			}
			
		});
	}
	
	public static final File groundtruthFile(final File imageFile) {
		return new File(baseName(imageFile.getPath()) + "_groundtruth.png");
	}
	
	public static final void writePaletteXML(final Quantizer quantizer, final OutputStream output) {
		XMLTools.write(quantizer.accept(new ToXML()), output, 0);
	}
	
	/**
	 * @author codistmonk (creation 2015-01-19)
	 */
	public static final class InvertComposite implements Composite {
		
		@Override
		public final CompositeContext createContext(final ColorModel srcColorModel,
				final ColorModel dstColorModel, final RenderingHints hints) {
			return new CompositeContext() {
				
				@Override
				public final void dispose() {
					// NOP
				}
				
				@Override
				public final void compose(final Raster src, final Raster dstIn, final WritableRaster dstOut) {
					final Rectangle inBounds = dstIn.getBounds();
					final Rectangle outBounds = dstOut.getBounds();
					final int[] buffer = dstIn.getPixels(inBounds.x, inBounds.y, inBounds.width, inBounds.height, (int[]) null);
					int n = buffer.length;
					
					for (int i = 0; i < n; ++i) {
						if (((i + 1) % 4) != 0) {
							buffer[i] ^= ~0;
						}
					}
					
					dstOut.setPixels(outBounds.x, outBounds.y, outBounds.width, outBounds.height, buffer);
				}
				
			};
		}
		
	}

	/**
	 * @author codistmonk (creation 2015-01-18)
	 */
	public static final class ToXML implements QuantizerNode.Visitor<Node> {
		
		private final Document xml = XMLTools.newDocument();
		
		@Override
		public Element visit(final Quantizer quantizer) {
			final Element result = (Element) this.xml.appendChild(this.xml.createElement("palette"));
			final int n = quantizer.getChildCount();
			
			result.setAttribute("scale", quantizer.getScaleAsString());
			
			for (int i = 0; i < n; ++i) {
				result.appendChild(((QuantizerCluster) quantizer.getChildAt(i)).accept(this));
			}
			
			return result;
		}
		
		@Override
		public final Element visit(final QuantizerCluster cluster) {
			final Element result = this.xml.createElement("cluster");
			final int n = cluster.getChildCount();
			
			result.setAttribute("name", cluster.getName());
			result.setAttribute("label", cluster.getLabelAsString());
			result.setAttribute("minimumSegmentSize", cluster.getMinimumSegmentSizeAsString());
			result.setAttribute("maximumSegmentSize", cluster.getMaximumSegmentSizeAsString());
			
			for (int i = 0; i < n; ++i) {
				result.appendChild(((QuantizerPrototype) cluster.getChildAt(i)).accept(this));
			}
			
			return result;
		}
		
		@Override
		public final Element visit(final QuantizerPrototype prototype) {
			final Element result = this.xml.createElement("prototype");
			
			result.setTextContent(prototype.getDataAsString());
			
			return result;
		}
		
		private static final long serialVersionUID = -8012834350224027358L;
		
	}
	
	public static final QuantizerCluster getSelectedCluster(final JTree tree) {
		final TreePath selectionPath = tree.getSelectionPath();
		
		return selectionPath == null ? null : cast(QuantizerCluster.class, selectionPath.getLastPathComponent());
	}
	
	public static final void setView(final JFrame mainFrame, final Component[] view, final File file) {
		final BufferedImage image = AwtImage2D.awtRead(file.getPath());
		final Canvas filtered = new Canvas().setFormat(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		final Canvas groundtruth = new Canvas().setFormat(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		final Canvas labels = new Canvas().setFormat(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		
		{
			final File groundtruthFile = groundtruthFile(file);
			
			try {
				groundtruth.getGraphics().drawImage(ImageIO.read(groundtruthFile), 0, 0, null);
			} catch (final IOException exception) {
				Tools.debugError(exception);
			}
		}
		
		setSharedProperty(mainFrame, "groundtruth", groundtruth);
		setSharedProperty(mainFrame, "labels", labels);
		setSharedProperty(mainFrame, "file", file);
		final BufferedImage mask = VisualSegmentation.newMaskFor(image);
		final ImageComponent newView = new ImageComponent(filtered.getImage());
		final JTree tree = getSharedProperty(mainFrame, "tree");
		final int[] xys = { -1, 0, 16 };
		
		newView.getBuffer().getSecond().add(new Painter() {
			
			@Override
			public final void paint(final Canvas canvas) {
				canvas.getGraphics().drawImage(groundtruth.getImage(), 0, 0, null);
				
				VisualSegmentation.outlineSegments(labels.getImage(), labels.getImage(), mask, canvas.getImage());
				
				final QuantizerCluster cluster = getSelectedCluster(tree);
				
				if (0 <= xys[0]) {
					final int x = xys[0];
					final int y = xys[1];
					final int s = xys[2];
					final Composite savedComposite = canvas.getGraphics().getComposite();
					
					if (cluster == null) {
						canvas.getGraphics().setComposite(new InvertComposite());
					} else {
						canvas.getGraphics().setColor(new Color(cluster.getLabel()));
					}
					
					canvas.getGraphics().drawOval(x - s / 2, y - s / 2, s, s);
					
					if (cluster == null) {
						canvas.getGraphics().setComposite(savedComposite);
					}
				}
			}
			
			private static final long serialVersionUID = -891880936915736755L;
			
		});
		
		new MouseHandler(null) {
			
			@Override
			public final void mouseDragged(final MouseEvent event) {
				final int oldX = xys[0];
				final int oldY = xys[1];
				final int x = xys[0] = event.getX();
				final int y = xys[1] = event.getY();
				final int s = xys[2];
				final QuantizerCluster cluster = getSelectedCluster(tree);
				final Composite savedComposite = groundtruth.getGraphics().getComposite();
				final Stroke savedStroke = groundtruth.getGraphics().getStroke();
				
				if (cluster == null) {
					groundtruth.getGraphics().setComposite(AlphaComposite.Clear);
				} else {
					groundtruth.getGraphics().setColor(new Color(cluster.getLabel()));
				}
				
				groundtruth.getGraphics().setStroke(new java.awt.BasicStroke(s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				groundtruth.getGraphics().drawLine(oldX, oldY, x, y);
				groundtruth.getGraphics().setStroke(savedStroke);
				
				if (cluster == null) {
					groundtruth.getGraphics().setComposite(savedComposite);
				}
				
				newView.repaint();
			}
			
			@Override
			public final void mouseMoved(final MouseEvent event) {
				xys[0] = event.getX();
				xys[1] = event.getY();
				newView.repaint();
			}
			
			@Override
			public final void mouseWheelMoved(final MouseWheelEvent event) {
				if (event.getWheelRotation() < 0) {
					xys[2] = Math.max(1, xys[2] - 1);
				} else {
					++xys[2];
				}
				newView.repaint();
			}
			
			@Override
			public final void mouseExited(final MouseEvent event) {
				xys[0] = -1;
				newView.repaint();
			}
			
			@Override
			public final void mouseClicked(final MouseEvent event) {
				if (event.getClickCount() == 2) {
					final TreePath selectionPath = tree.getSelectionPath();
					
					if (selectionPath != null) {
						final QuantizerPrototype node = cast(QuantizerPrototype.class,
								selectionPath.getLastPathComponent());
						
						if (node != null) {
							Quantizer.extractValues(image, event.getX(), event.getY(),
									node.getQuantizer().getScale(), node.getData());
							tree.getModel().valueForPathChanged(selectionPath, node.renewUserObject());
							mainFrame.validate();
						}
					}
				}
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 6325908040300578842L;
			
		}.addTo(newView);
		
		final Semaphore modelChanged = new Semaphore(1);
		final Thread updater = new Thread() {
			
			@Override
			public final void run() {
				while (!this.isInterrupted()) {
					try {
						modelChanged.acquire();
						modelChanged.drainPermits();
					} catch (final InterruptedException exception) {
						break;
					}
					
					filtered.getGraphics().drawImage(image, 0, 0, null);
					
					final TicToc timer = new TicToc();
//					final Map<Integer, List<Pair<Point, Integer>>> labelCells = extractCells(
//							file, image, mask, labels, segments.getImage(), (PaletteRoot) treeModel.getRoot());
//					
//					outlineSegments(segments.getImage(), labels.getImage(), null, filtered.getImage());
//					
//					Tools.debugPrint(labelCells.size());
					// TODO
					Tools.debugPrint(timer.toc());
					
					newView.repaint();
				}
			}
			
		};
		
		updater.start();
		
		tree.getModel().addTreeModelListener(new TreeModelListener() {
			
			@Override
			public final void treeStructureChanged(final TreeModelEvent event) {
				this.modelChanged(event);
			}
			
			@Override
			public final void treeNodesRemoved(final TreeModelEvent event) {
				this.modelChanged(event);
			}
			
			@Override
			public final void treeNodesInserted(final TreeModelEvent event) {
				this.modelChanged(event);
			}
			
			@Override
			public final void treeNodesChanged(final TreeModelEvent event) {
				this.modelChanged(event);
			}
			
			private final void modelChanged(final TreeModelEvent event) {
				ignore(event);
				modelChanged.release();
				mainFrame.validate();
			}
			
		});
		
		newView.addHierarchyListener(new HierarchyListener() {
			
			@Override
			public final void hierarchyChanged(final HierarchyEvent event) {
				if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !newView.isShowing()) {
					updater.interrupt();
				}
			}
			
		});
		
		setSharedProperty(mainFrame, "view", newView);
		
		VisualSegmentation.setView(mainFrame, view, scrollable(VisualSegmentation.center(newView)), file.getName());
	}
	
	public static final JTree newQuantizerTree() {
		final DefaultTreeModel treeModel = loadModel();
		final JTree result = new JTree(treeModel);
		
		result.addMouseListener(new MouseAdapter() {
			
			private final JPopupMenu rootPopup;
			
			private final JPopupMenu clusterPopup;
			
			private final JPopupMenu prototypePopup;
			
			private final TreePath[] currentPath;
			
			{
				this.rootPopup = new JPopupMenu();
				this.clusterPopup = new JPopupMenu();
				this.prototypePopup = new JPopupMenu();
				this.currentPath = new TreePath[1];
				
				this.rootPopup.add(newItem("Add cluster", e -> {
					final Quantizer currentNode = (Quantizer) this.currentPath[0].getLastPathComponent();
					final QuantizerCluster newNode = new QuantizerCluster().setName("cluster").setLabel(1).setUserObject();
					
					treeModel.insertNodeInto(newNode, currentNode, currentNode.getChildCount());
					result.getRootPane().validate();
				}));
				this.clusterPopup.add(newItem("Edit cluster properties...", e -> {
					final QuantizerCluster currentNode = (QuantizerCluster) this.currentPath[0].getLastPathComponent();
					
					showEditDialog("Edit cluster properties",
							() -> {
								treeModel.valueForPathChanged(this.currentPath[0], currentNode.renewUserObject());
								result.getRootPane().validate();
							},
							property("name:", currentNode::getName, currentNode::setName),
							property("label:", currentNode::getLabelAsString, currentNode::parseLabel),
							property("minimumSegmentSize:", currentNode::getMinimumSegmentSize, currentNode::parseMinimumSegmentSize),
							property("maximumSegmentSize:", currentNode::getMaximumSegmentSize, currentNode::parseMaximumSegmentSize)
					);
					
				}));
				this.clusterPopup.add(newItem("Add prototype", e -> {
					final QuantizerCluster currentNode = (QuantizerCluster) this.currentPath[0].getLastPathComponent();
					final QuantizerNode newNode = new QuantizerPrototype().setUserObject();
					
					treeModel.insertNodeInto(newNode, currentNode, currentNode.getChildCount());
					result.getRootPane().validate();
				}));
				this.clusterPopup.add(newItem("Remove cluster", e -> {
					final QuantizerCluster currentNode = (QuantizerCluster) this.currentPath[0].getLastPathComponent();
					treeModel.removeNodeFromParent(currentNode);
					result.getRootPane().validate();
				}));
				this.prototypePopup.add(newItem("Remove prototype", e -> {
					final QuantizerPrototype currentNode = (QuantizerPrototype) this.currentPath[0].getLastPathComponent();
					treeModel.removeNodeFromParent(currentNode);
					result.getRootPane().validate();
				}));
			}
			
			@Override
			public final void mouseClicked(final MouseEvent event) {
				this.mouseUsed(event);
			}
			
			@Override
			public final void mousePressed(final MouseEvent event) {
				this.mouseUsed(event);
			}
			
			@Override
			public final void mouseReleased(final MouseEvent event) {
				this.mouseUsed(event);
			}
			
			private final void mouseUsed(final MouseEvent event) {
				if (event.isPopupTrigger()) {
					final TreePath path = result.getPathForLocation(event.getX(), event.getY());
					this.currentPath[0] = path;
					
					if (path != null) {
						final Object node = path.getLastPathComponent();
						JPopupMenu popup = null;
						
						if (node instanceof Quantizer) {
							popup = this.rootPopup;
						} else if (node instanceof QuantizerCluster) {
							popup = this.clusterPopup;
						} else if (node instanceof QuantizerPrototype) {
							popup = this.prototypePopup;
						}
						
						if (popup != null) {
							popup.show(event.getComponent(), event.getX(), event.getY());
						}
					}
				}
			}
			
		});
		
		return result;
	}
	
	public static final DefaultTreeModel loadModel() {
		try {
			return new DefaultTreeModel(fromXML(Tools.getResourceAsStream(PALETTE_XML)));
		} catch (final Exception exception) {
			exception.printStackTrace();
		}
		
		return new DefaultTreeModel(new Quantizer().setUserObject());
	}
	
	public static final Quantizer fromXML(final InputStream input) {
		final Document xml = XMLTools.parse(input);
		final Quantizer result = new Quantizer().setUserObject();
		
		result.parseScale(((Element) XMLTools.getNode(xml, "palette")).getAttribute("scale"));
		
		for (final Node clusterNode : XMLTools.getNodes(xml, "palette/cluster")) {
			final Element clusterElement = (Element) clusterNode;
			final QuantizerCluster cluster = new QuantizerCluster()
				.setName(clusterElement.getAttribute("name"))
				.parseLabel(clusterElement.getAttribute("label"))
				.parseMinimumSegmentSize(clusterElement.getAttribute("minimumSegmentSize"))
				.parseMaximumSegmentSize(clusterElement.getAttribute("maximumSegmentSize"))
				.setUserObject();
			
			result.add(cluster);
			
			for (final Node prototypeNode : XMLTools.getNodes(clusterNode, "prototype")) {
				final QuantizerPrototype prototype = new QuantizerPrototype();
				
				cluster.add(prototype);
				
				prototype.parseData(prototypeNode.getTextContent()).setUserObject();
			}
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2015-01-16)
	 */
	public static final class ImageComponent extends JComponent {
		
		private final BufferedImage image;
		
		private final Pair<Canvas, List<Painter>> buffer;
		
		public ImageComponent(final BufferedImage image) {
			this.image = image;
			final int imageWidth = image.getWidth();
			final int imageHeight = image.getHeight();
			this.buffer = new Pair<>(
					new Canvas().setFormat(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB), new ArrayList<>());
			
			this.setMinimumSize(new Dimension(imageWidth, imageHeight));
			this.setMaximumSize(new Dimension(imageWidth, imageHeight));
			this.setPreferredSize(new Dimension(imageWidth, imageHeight));
			this.setSize(new Dimension(imageWidth, imageHeight));
		}
		
		public final BufferedImage getImage() {
			return this.image;
		}
		
		public final Pair<Canvas, List<Painter>> getBuffer() {
			return this.buffer;
		}
		
		@Override
		protected final void paintComponent(final Graphics g) {
			super.paintComponent(g);
			
			final Pair<Canvas, List<Painter>> buffer = this.getBuffer();
			final Canvas canvas = buffer.getFirst();
			
			canvas.getGraphics().drawImage(this.getImage(), 0, 0, null);
			buffer.getSecond().forEach(p -> p.paint(canvas));
			
			g.drawImage(canvas.getImage(), 0, 0, null);
		}
		
		private static final long serialVersionUID = 1260599901446126551L;
		
		public static final Color CLEAR = new Color(0, true);
		
		/**
		 * @author codistmonk (creation 2015-01-16)
		 */
		public static abstract interface Painter extends Serializable {
			
			public abstract void paint(Canvas canvas);
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-01-16)
	 */
	public static abstract class QuantizerNode extends DefaultMutableTreeNode {
		
		public abstract <V> V accept(Visitor<V> visitor);
		
		public abstract QuantizerNode setUserObject();
		
		public final UserObject renewUserObject() {
			return (UserObject) this.setUserObject().getUserObject();
		}
		
		private static final long serialVersionUID = 7636724853656189383L;
		
		public static final int parseARGB(final String string) {
			return string.startsWith("#") ? (int) Long.parseLong(string.substring(1), 16) : Integer.parseInt(string);
		}
		
		/**
		 * @author codistmonk (creation 2015-01-18)
		 */
		public abstract class UserObject implements Serializable, Cloneable {
			
			private static final long serialVersionUID = 1543313797613503533L;
			
		}
		
		/**
		 * @author codistmonk (creation 2015-01-16)
		 */
		public static abstract interface Visitor<V> extends Serializable {
			
			public abstract V visit(Quantizer quantizer);
			
			public abstract V visit(QuantizerCluster cluster);
			
			public abstract V visit(QuantizerPrototype prototype);
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-01-16)
	 */
	public static final class Quantizer extends QuantizerNode {
		
		private int[] buffer = new int[1];
		
		@Override
		public final Quantizer setUserObject() {
			this.setUserObject(this.new UserObject() {
				
				@Override
				public final String toString() {
					return "scale: " + Quantizer.this.getScaleAsString();
				}
				
				private static final long serialVersionUID = 948766593376210016L;
				
			});
			
			return this;
		}
		
		public final int getScale() {
			return this.buffer.length;
		}
		
		public final void setScale(final int scale) {
			if (scale <= 0) {
				throw new IllegalArgumentException();
			}
			
			if (scale != this.getScale()) {
				this.buffer = new int[scale];
			}
		}
		
		public final String getScaleAsString() {
			return Integer.toString(this.getScale());
		}
		
		public final void parseScale(final String scaleAsString) {
			this.setScale(Integer.parseInt(scaleAsString));
		}
		
		public final QuantizerCluster quantize(final BufferedImage image, final int x, final int y) {
			extractValues(image, x, y, this.getScale(), this.buffer);
			final int n = this.getChildCount();
			QuantizerCluster result = null;
			double bestDistance = Double.POSITIVE_INFINITY;
			
			for (int i = 0; i < n; ++i) {
				final QuantizerCluster cluster = (QuantizerCluster) this.getChildAt(i);
				final double distance = cluster.distanceTo(this.buffer);
				
				if (distance < bestDistance) {
					result = cluster;
					bestDistance = distance;
				}
			}
			
			return result;
		}
		
		@Override
		public final <V> V accept(final Visitor<V> visitor) {
			return visitor.visit(this);
		}
		
		private static final long serialVersionUID = 3228746395868315788L;
		
		public static final int[] extractValues(final BufferedImage image, final int x, final int y, final int patchSize, final int[] result) {
			Arrays.fill(result, 0);
			
			final int width = image.getWidth();
			final int height = image.getHeight();
			final int s = patchSize / 2;
			final int left = x - s;
			final int right = left + patchSize;
			final int top = y - s;
			final int bottom = top + patchSize;
			
			for (int yy = top, i = 0; yy < bottom; ++yy) {
				if (0 <= yy && yy < height) {
					for (int xx = left; xx < right; ++xx, ++i) {
						if (0 <= xx && xx < width) {
							result[i] = image.getRGB(xx, yy);
						}
					}
				} else {
					i += patchSize;
				}
			}
			
			return result;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-01-16)
	 */
	public static final class QuantizerCluster extends QuantizerNode {
		
		private String name = "cluster";
		
		private int label = 1;
		
		private int minimumSegmentSize = 0;
		
		private int maximumSegmentSize = Integer.MAX_VALUE;
		
		@Override
		public final QuantizerCluster setUserObject() {
			this.setUserObject(this.new UserObject() {
				
				@Override
				public final String toString() {
					final boolean showMaximum = QuantizerCluster.this.getMaximumSegmentSize() != Integer.MAX_VALUE;
					
					return QuantizerCluster.this.getName() + " ("
							+ QuantizerCluster.this.getLabelAsString() + " "
							+ QuantizerCluster.this.getMinimumSegmentSizeAsString() + ".."
							+ (showMaximum ? QuantizerCluster.this.getMaximumSegmentSizeAsString() : "")
							+ ")";
				}
				
				private static final long serialVersionUID = 1507012060737286549L;
				
			});
			
			return this;
		}
		
		public final String getName() {
			return this.name;
		}
		
		public final QuantizerCluster setName(final String name) {
			this.name = name;
			
			return this;
		}
		
		@Override
		public final Quantizer getParent() {
			return (Quantizer) super.getParent();
		}
		
		public final Quantizer getQuantizer() {
			return this.getParent();
		}
		
		public final int getLabel() {
			return this.label;
		}
		
		public final QuantizerCluster setLabel(final int label) {
			this.label = label;
			
			return this;
		}
		
		public final String getLabelAsString() {
			return "#" + Integer.toHexString(this.label).toUpperCase(Locale.ENGLISH);
		}
		
		public final QuantizerCluster parseLabel(final String labelAsString) {
			this.setLabel(parseARGB(labelAsString));
			
			return this;
		}
		
		public final int getMinimumSegmentSize() {
			return this.minimumSegmentSize;
		}
		
		public final QuantizerCluster setMinimumSegmentSize(final int minimumSegmentSize) {
			this.minimumSegmentSize = minimumSegmentSize;
			
			return this;
		}
		
		public final String getMinimumSegmentSizeAsString() {
			return Integer.toString(this.getMinimumSegmentSize());
		}
		
		public final QuantizerCluster parseMinimumSegmentSize(final String minimumSegmentSizeAsString) {
			this.setMinimumSegmentSize(Integer.parseInt(minimumSegmentSizeAsString));
			
			return this;
		}
		
		public final int getMaximumSegmentSize() {
			return this.maximumSegmentSize;
		}
		
		public final QuantizerCluster setMaximumSegmentSize(final int maximumSegmentSize) {
			this.maximumSegmentSize = maximumSegmentSize;
			
			return this;
		}
		
		public final String getMaximumSegmentSizeAsString() {
			return Integer.toString(this.getMaximumSegmentSize());
		}
		
		public final QuantizerCluster parseMaximumSegmentSize(final String maximumSegmentSizeAsString) {
			this.setMaximumSegmentSize(Integer.parseInt(maximumSegmentSizeAsString));
			
			return this;
		}
		
		public final double distanceTo(final int[] values) {
			final int n = this.getChildCount();
			double result = Double.POSITIVE_INFINITY;
			
			for (int i = 0; i < n; ++i) {
				final double distance = ((QuantizerPrototype) this.getChildAt(i)).distanceTo(values);
				
				if (distance < result) {
					result = distance;
				}
			}
			
			return result;
		}
		
		@Override
		public final <V> V accept(final Visitor<V> visitor) {
			return visitor.visit(this);
		}
		
		private static final long serialVersionUID = -3727849715989585298L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-01-16)
	 */
	public static final class QuantizerPrototype extends QuantizerNode {
		
		private int[] data = new int[0];
		
		@Override
		public final QuantizerPrototype setUserObject() {
			this.setUserObject(this.new UserObject() {
				
				@Override
				public final String toString() {
					return QuantizerPrototype.this.getDataAsString();
				}
				
				private static final long serialVersionUID = 4617070174363518324L;
				
			});
			
			return this;
		}
		
		public final int[] getData() {
			final Quantizer root = cast(Quantizer.class, this.getRoot());
			
			if (root == null) {
				return null;
			}
			
			if (this.data.length != root.getScale()) {
				this.data = new int[root.getScale()];
			}
			
			return this.data;
		}
		
		@Override
		public final QuantizerCluster getParent() {
			return (QuantizerCluster) super.getParent();
		}
		
		public final Quantizer getQuantizer() {
			return this.getParent().getParent();
		}
		
		public final String getDataAsString() {
			return Tools.join(",", Arrays.stream(this.getData()).mapToObj(
					i -> "#" + Integer.toHexString(i).toUpperCase(Locale.ENGLISH)).toArray());
		}
		
		public final QuantizerPrototype parseData(final String dataAsString) {
			final int[] parsed = Arrays.stream(dataAsString.split(",")).mapToInt(QuantizerNode::parseARGB).toArray();
			
			System.arraycopy(parsed, 0, this.getData(), 0, this.getData().length);
			
			return this;
		}
		
		public final double distanceTo(final int[] values) {
			final int n = values.length;
			
			if (n != this.data.length) {
				throw new IllegalArgumentException();
			}
			
			double result = 0.0;
			
			for (int i = 0; i < n; ++i) {
				final int thisRGB = this.data[i];
				final int thatRGB = values[i];
				result += abs(red8(thisRGB) - red8(thatRGB))
						+ abs(green8(thisRGB) - green8(thatRGB))
						+ abs(blue8(thisRGB) - blue8(thatRGB));
						
			}
			
			return result;
		}
		
		@Override
		public final <V> V accept(final Visitor<V> visitor) {
			return visitor.visit(this);
		}
		
		private static final long serialVersionUID = 946728342547485375L;
		
	}
	
}
