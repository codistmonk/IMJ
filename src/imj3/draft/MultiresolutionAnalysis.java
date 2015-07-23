package imj3.draft;

import static imj3.tools.CommonSwingTools.*;
import static java.lang.Math.abs;
import static java.lang.Math.log;
import static java.lang.Math.min;
import static multij.swing.SwingTools.*;
import static multij.tools.Tools.*;

import imj3.core.Channels;
import imj3.core.Image2D;
import imj3.tools.IMJTools;
import imj3.tools.Image2DComponent;
import imj3.tools.PackedImage2D;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import multij.swing.MouseHandler;
import multij.swing.SwingTools;
import multij.tools.Canvas;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-07-22)
 */
public final class MultiresolutionAnalysis {
	
	private MultiresolutionAnalysis() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		useSystemLookAndFeel();
		
		SwingUtilities.invokeLater(() -> {
			SwingTools.show(new AnalysisPanel(), MultiresolutionAnalysis.class.getName(), false);
		});
	}
	
	/**
	 * @author codistmonk (creation 2015-07-22)
	 */
	public static final class AnalysisPanel extends JPanel {
		
		private final JTree datasetsView;
		
		private final Image2DComponent imageView;
		
		private final JLabel w0View, h0View, x0View, y0View, scaleView;
		
		public AnalysisPanel() {
			super(new BorderLayout());
			this.datasetsView = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode("Datasets")));
			this.imageView = new Image2DComponent(new PackedImage2D(this.getClass().getName() + ".defaultImage", 1, 1, Channels.Predefined.C1_U1));
			this.w0View = new JLabel(Integer.toString(this.imageView.getImage().getWidth()));
			this.h0View = new JLabel(Integer.toString(this.imageView.getImage().getHeight()));
			this.x0View = new JLabel("");
			this.y0View = new JLabel("");
			this.scaleView = new JLabel(Double.toString(this.imageView.getView().getScaleX()));
			
			this.setup();
		}
		
		public final JTree getDatasetsView() {
			return this.datasetsView;
		}
		
		public final Image2DComponent getImageView() {
			return this.imageView;
		}
		
		public final JLabel getW0View() {
			return this.w0View;
		}
		
		public final JLabel getH0View() {
			return this.h0View;
		}
		
		public final JLabel getX0View() {
			return this.x0View;
		}
		
		public final JLabel getY0View() {
			return this.y0View;
		}
		
		public final JLabel getScaleView() {
			return this.scaleView;
		}
		
		final Point2D getLocationInImage0(final Point2D locationInView) {
			final Point2D result = new Point2D.Double();
			
			try {
				this.getImageView().getView().inverseTransform(locationInView, result);
				
				return result;
			} catch (final NoninvertibleTransformException exception) {
				exception.printStackTrace();
				return null;
			}
		}
		
		final void updateStatusBox(final Point mouseLocationInView) {
			final AffineTransform imageToView = this.getImageView().getView();
			
			if (mouseLocationInView == null || mouseLocationInView.x < 0) {
				this.getX0View().setText("");
				this.getY0View().setText("");
			} else {
				final Point2D mouseLocationInImage0 = this.getLocationInImage0(mouseLocationInView);
				
				if (mouseLocationInImage0 != null) {
					this.getX0View().setText(Integer.toString((int) mouseLocationInImage0.getX()));
					this.getY0View().setText(Integer.toString((int) mouseLocationInImage0.getY()));
				}
			}
			
			final Image2D image0 = this.getImageView().getImage().getScaledImage(1.0);
			
			this.getW0View().setText(Integer.toString(image0.getWidth()));
			this.getH0View().setText(Integer.toString(image0.getHeight()));
			this.getScaleView().setText(Double.toString(imageToView.getScaleX()));
		}
		
		final void setImage(final Image2D image) {
			this.getImageView().setImage(image);
		}
		
		final String getFeatureVectorAsString(final Point locationInView) {
			final Point2D mouseLocationInImage = getLocationInImage0(locationInView);
			
			if (mouseLocationInImage == null) {
				return null;
			}
			
			final StringBuilder resultBuilder = new StringBuilder();
			Image2D image = getImageView().getImage().getScaledImage(1.0);
			
			while (1 < image.getWidth() && 1 < image.getHeight()) {
				final long pixelValue = image.getPixelValue((int) mouseLocationInImage.getX(), (int) mouseLocationInImage.getY());
				
				resultBuilder.append('#').append(String.format("%06x", pixelValue & 0x00FFFFFFL));
				
				image = image.getScaledImage(image.getScale() / 2.0);
				mouseLocationInImage.setLocation(
						min(mouseLocationInImage.getX() / 2.0, image.getWidth() - 1),
						min(mouseLocationInImage.getY() / 2.0, image.getHeight() - 1));
			}
			
			return resultBuilder.toString();
		}
		
		final double[] getFeatureVector(final int x0, final int y0, final double[] result) {
			int x = x0;
			int y = y0;
			final double currentScale = getImageView().getImage().getScale();
			Image2D image = getImageView().getImage().getScaledImage(1.0);
			int offset = 0;
			
			while (1 < image.getWidth() && 1 < image.getHeight()) {
				if (currentScale < image.getScale()) {
					result[offset++] = Double.NaN;
					result[offset++] = Double.NaN;
					result[offset++] = Double.NaN;
				} else {
					final long pixelValue = image.getPixelValue(x, y);
					
					result[offset++] = (pixelValue >> 16) & 0xFFL;
					result[offset++] = (pixelValue >> 8) & 0xFFL;
					result[offset++] = (pixelValue >> 0) & 0xFFL;
				}
				
				image = image.getScaledImage(image.getScale() / 2.0);
				x = min(x / 2, image.getWidth() - 1);
				y = min(y / 2, image.getHeight() - 1);
			}
			
			return result;
		}
		
		private final void setup() {
			this.getDatasetsView().setEditable(true);
			
			this.getImageView().setPreferredSize(new Dimension(512, 512));
			
			this.add(horizontalSplit(
					verticalSplit(
							verticalBox(
									horizontalBox(new JLabel("w0:"), this.getW0View()),
									horizontalBox(new JLabel("h0:"), this.getH0View()),
									horizontalBox(new JLabel("x0:"), this.getX0View()),
									horizontalBox(new JLabel("y0:"), this.getY0View()),
									horizontalBox(new JLabel("scale:"), this.getScaleView())
							),
							scrollable(this.getDatasetsView())),
					this.getImageView()), BorderLayout.CENTER);
			
			new MouseHandler() {
				
				@Override
				public final void mouseExited(final MouseEvent event) {
					updateStatusBox(null);
				}
				
				@Override
				public final void mouseWheelMoved(final MouseWheelEvent event) {
					updateStatusBox(event.getPoint());
				}
				
				@Override
				public final void mouseMoved(final MouseEvent event) {
					updateStatusBox(event.getPoint());
				}
				
				@Override
				public final void mouseClicked(final MouseEvent event) {
					if (!event.isPopupTrigger() && event.getClickCount() == 2) {
						final TreePath datasetsSelectionPath = getDatasetsView().getSelectionPath();
						
						if (datasetsSelectionPath != null) {
							final DefaultTreeModel datasetsModel = (DefaultTreeModel) getDatasetsView().getModel();
							
							if (datasetsSelectionPath.getPathCount() == 3) {
								final DefaultMutableTreeNode classNode = (DefaultMutableTreeNode) datasetsSelectionPath.getLastPathComponent();
								datasetsModel.insertNodeInto(new DefaultMutableTreeNode(getFeatureVectorAsString(event.getPoint())), classNode, classNode.getChildCount());
							} else if (datasetsSelectionPath.getPathCount() == 4) {
								datasetsModel.valueForPathChanged(datasetsSelectionPath, getFeatureVectorAsString(event.getPoint()));
							}
						}
					}
				}
				
				private static final long serialVersionUID = -2087632277415594403L;
				
			}.addTo(this.getImageView());
			
			this.getImageView().setDropTarget(new DropTarget() {
				
				@Override
				public final synchronized void drop(final DropTargetDropEvent event) {
					String path = null;
					
					try {
						path = getFiles(event).get(0).getPath();
					} catch (final IllegalArgumentException exception) {
						debugError(exception);
						
						if ("Comparison method violates its general contract!".equals(exception.getMessage())) {
							// FIXME this doesn't seem to actually work :-(
							System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
							path = getFiles(event).get(0).getPath();
							debugPrint(":-)");
						}
					}
					
					try {
						getImageView().setImage(IMJTools.read(path));
						updateStatusBox(null);
					} catch (final Exception exception) {
						exception.printStackTrace();
					}
				}
				
				private static final long serialVersionUID = -6903603783164650228L;
				
			});
			
			final Canvas overlayCanvas = new Canvas();
			
			this.getImageView().setOverlay((g, r) -> {
				final TreePath selectionPath = getDatasetsView().getSelectionPath();
				
				if (selectionPath != null && 1 < selectionPath.getPathCount()) {
					final Image2D image0 = getImageView().getImage().getScaledImage(1.0);
					final int w0 = image0.getWidth();
					final int h0 = image0.getHeight();
					final int n = 3 * (int) (1.0 + log(min(w0, h0)) / log(2.0));
					final DefaultMutableTreeNode datasetNode = (DefaultMutableTreeNode) selectionPath.getPathComponent(1);
					final int classCount = datasetNode.getChildCount();
					final List<List<double[]>> prototypes = new ArrayList<>(classCount);
					final Pattern element = Pattern.compile("#?([0-9a-fA-F]{2})");
					final int[] classRGBs = new int[classCount];
					int usableClasses = 0;
					
					for (int i = 0; i < classCount; ++i) {
						final DefaultMutableTreeNode classNode = (DefaultMutableTreeNode) datasetNode.getChildAt(i);
						final int prototypeCount = classNode.getChildCount();
						final List<double[]> classPrototypes = new ArrayList<>(prototypeCount);
						
						{
							final String classDescription = classNode.getUserObject().toString();
							classRGBs[i] = Long.decode(classDescription.substring(classDescription.lastIndexOf("#"))).intValue();
						}
						
						for (int j = 0; j < prototypeCount; ++j) {
							final double[] prototype = new double[n];
							final String prototypeAsString = ((DefaultMutableTreeNode) classNode.getChildAt(j)).getUserObject().toString();
							final Matcher matcher = element.matcher(prototypeAsString);
							int k = 0;
							
							while (matcher.find()) {
								prototype[k++] = Integer.parseInt(matcher.group(1), 16);
							}
							
							classPrototypes.add(prototype);
						}
						
						prototypes.add(classPrototypes);
						
						if (!classPrototypes.isEmpty()) {
							++usableClasses;
						}
					}
					
					if (2 <= usableClasses) {
						overlayCanvas.setFormat(r.width, r.height);
						overlayCanvas.clear(new Color(0, true));
						
						final Point2D topLeftInImage0 = new Point2D.Double(0.0, 0.0);
						final Point2D deltasInImage0 = new Point2D.Double(1.0, 1.0);
						final double[] featureVector = new double[n];
						
						try {
							getImageView().getView().inverseTransform(topLeftInImage0, topLeftInImage0);
							getImageView().getView().inverseTransform(deltasInImage0, deltasInImage0);
							
							deltasInImage0.setLocation(deltasInImage0.getX() - topLeftInImage0.getX(), deltasInImage0.getY() - topLeftInImage0.getY());
							
							for (int y = 0; y < r.height; ++y) {
								final int y0 = (int) (topLeftInImage0.getY() + y * deltasInImage0.getY());
								
								for (int x = 0; x < r.width; ++x) {
									final int x0 = (int) (topLeftInImage0.getX() + x * deltasInImage0.getX());
									
									if (0 <= x0 && x0 < w0 && 0 <= y0 && y0 < h0) {
										getFeatureVector(x0, y0, featureVector);
										int bestClassId = -1;
										double bestScore = Double.POSITIVE_INFINITY;
										
										for (int i = 0; i < classCount; ++i) {
											for (final double[] prototype : prototypes.get(i)) {
												double score = 0.0;
												
												for (int j = 0; j < n; ++j) {
													if (!Double.isNaN(featureVector[j])) {
														score += abs(featureVector[j] - prototype[j]) * 1.0 / (1.0 + j / 3);
													}
												}
												
												if (score < bestScore) {
													bestClassId = i;
													bestScore = score;
												}
											}
										}
										
										if (0 <= bestClassId) {
											overlayCanvas.getImage().setRGB(x, y, classRGBs[bestClassId]);
										}
									}
								}
							}
							
							g.drawImage(overlayCanvas.getImage(), 0, 0, null);
						} catch (final NoninvertibleTransformException exception) {
							exception.printStackTrace();
						}
					}
				}
			});
			
			this.getDatasetsView().addKeyListener(new KeyAdapter() {
				
				@Override
				public final void keyPressed(final KeyEvent event) {
					final TreePath selectionPath = getDatasetsView().getSelectionPath();
					
					if (selectionPath != null) {
						final DefaultTreeModel datasetsModel = (DefaultTreeModel) getDatasetsView().getModel();
						final DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
						
						if (event.getKeyCode() == KeyEvent.VK_ENTER) {
							switch (selectionPath.getPathCount()) {
							case 1:
								final DefaultMutableTreeNode newDatasetNode = new DefaultMutableTreeNode("Dataset");
								datasetsModel.insertNodeInto(newDatasetNode, lastPathComponent, lastPathComponent.getChildCount());
								break;
							case 2:
								final DefaultMutableTreeNode newClassNode = new DefaultMutableTreeNode("Class#" + Integer.toHexString(newRandomColor().getRGB()));
								datasetsModel.insertNodeInto(newClassNode, lastPathComponent, lastPathComponent.getChildCount());
								break;
							default:
								debugError();
							}
						} else if (event.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
							switch (selectionPath.getPathCount()) {
							case 2:
							case 3:
							case 4:
								datasetsModel.removeNodeFromParent(lastPathComponent);
								break;
							default:
								debugError(selectionPath.getPathCount());
							}
						}
					}
				}
				
			});
		}
		
		private static final long serialVersionUID = -7426011797849966607L;
		
	}
	
}
