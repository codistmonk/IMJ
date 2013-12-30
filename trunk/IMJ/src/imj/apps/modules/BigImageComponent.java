package imj.apps.modules;

import static imj.apps.modules.ViewFilter.VIEW_FILTER;
import static java.lang.Double.isInfinite;
import static java.lang.Double.isNaN;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.util.Arrays.fill;
import imj.Image;
import imj.ImageWrangler;
import imj.apps.modules.Annotations.Annotation;
import imj.apps.modules.Annotations.Annotation.Region;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.events.Variable.Listener;
import net.sourceforge.aprog.events.Variable.ValueChangedEvent;

/**
 * @author codistmonk (creation 2013-02-13)
 */
public final class BigImageComponent extends JComponent {
	
	public static final String SOURCE_IMAGE = "sourceImage";

	private final Context context;
	
	private final String imageId;
	
	private int scale;
	
	private int lod;
	
	private Image image;
	
	private BufferedImage buffer1;
	
	private BufferedImage buffer2;
	
	private final Rectangle viewport;
	
	private int[] rowValues;
	
	private boolean[] rowValueReady;
	
	public BigImageComponent(final Context context, final String imageId) {
		this.context = context;
		this.imageId = imageId;
		this.scale = 1;
		this.viewport = new Rectangle();
		this.rowValues = new int[0];
		this.rowValueReady = new boolean[0];
		
		context.set("imageView", this);
		context.set("autoAdjustScale", false);
		
		this.addKeyListener(new KeyAdapter() {
			
			@Override
			public final void keyTyped(final KeyEvent event) {
				final int oldScale = BigImageComponent.this.getScale();
				final boolean adjustScale = context.get("autoAdjustScale");
				
				if ('s' == event.getKeyChar()) {
					context.set("autoAdjustScale", !adjustScale);
				} else if ('*' == event.getKeyChar()) {
					BigImageComponent.this.setScale(oldScale * 2);
				} else if ('/' == event.getKeyChar()) {
					BigImageComponent.this.setScale(oldScale / 2);
				} else {
					final int oldLod = BigImageComponent.this.getLod();
					
					if ('+' == event.getKeyChar()) {
						if (adjustScale && 0 < oldLod && 1 < oldScale) {
							BigImageComponent.this.setLodAndScale(oldLod - 1, oldScale / 2);
						} else {
							BigImageComponent.this.setLod(oldLod - 1);
						}
					} else if ('-' == event.getKeyChar()) {
						if (adjustScale) {
							BigImageComponent.this.setLodAndScale(oldLod + 1, oldScale * 2);
						} else {
							BigImageComponent.this.setLod(oldLod + 1);
						}
					}
				}
			}
			
		});
		
		final MouseAdapter mouseHandler = new MouseAdapter() {
			
			private final Point viewportInitialLocation = new Point();
			
			private final Point mousePressedLocation = new Point();
			
			@Override
			public final void mouseMoved(final MouseEvent event) {
				final int unscaledX = BigImageComponent.this.unscale(event.getX());
				final int unscaledY = BigImageComponent.this.unscale(event.getY());
				
				context.set("xy", new Point(unscaledX, unscaledY));
			}
			
			@Override
			public final void mousePressed(final MouseEvent event) {
				this.viewportInitialLocation.setLocation(BigImageComponent.this.getVisibleRect().getLocation());
				this.mousePressedLocation.setLocation(event.getLocationOnScreen());
			}
			
			@Override
			public final void mouseDragged(final MouseEvent event) {
				final Rectangle viewport = BigImageComponent.this.getVisibleRect();
				
				viewport.x = this.viewportInitialLocation.x - event.getXOnScreen() + this.mousePressedLocation.x;
				viewport.y = this.viewportInitialLocation.y - event.getYOnScreen() + this.mousePressedLocation.y;
				
				BigImageComponent.this.scrollRectToVisible(viewport);
			}
			
		};
		this.addMouseListener(mouseHandler);
		this.addMouseMotionListener(mouseHandler);
		
		final Listener<Object> fullRepaintNeeded = new Listener<Object>() {
			
			@Override
			public final void valueChanged(final ValueChangedEvent<Object, ?> event) {
				BigImageComponent.this.repaintAll();
			}
			
		};
		
		context.getVariable(VIEW_FILTER).addListener(fullRepaintNeeded);
		context.getVariable("sieve").addListener(fullRepaintNeeded);
		
		this.setFocusable(true);
		this.setDoubleBuffered(false);
		this.setLodAndScale(0, 1);
	}
	
	public final String getImageId() {
		return this.imageId;
	}
	
	public final int getLod() {
		return this.lod;
	}
	
	public final void setLod(final int lod) {
		if (0 <= lod) {
			this.lod = lod;
			
			this.context.set("lod", lod);
			
			this.refreshImage();
		}
	}
	
	public final int getScale() {
		return this.scale;
	}
	
	public final void setScale(final int scale) {
		if (1 <= scale) {
			this.scale = scale;
			
			this.context.set("scale", scale);
			
			this.refreshImage();
		}
	}
	
	public final void setLodAndScale(final int lod, final int scale) {
		if (0 <= lod && 1 <= scale) {
			this.lod = lod;
			this.scale = scale;
			
			this.context.set("lod", lod);
			this.context.set("scale", scale);
			
			this.refreshImage();
		}
	}
	
	public final int scale(final int value) {
		return value * this.getScale();
	}
	
	public final int unscale(final int value) {
		return value / this.getScale();
	}
	
	public final void refreshImage() {
		final Image sourceImage = ImageWrangler.INSTANCE.load(this.getImageId(), this.getLod());
		
		this.updateImage(sourceImage);
		this.context.set(SOURCE_IMAGE, sourceImage);
		
		final Rectangle viewport = this.getVisibleRect();
		final int columnCount = this.image.getColumnCount();
		final int rowCount = this.image.getRowCount();
		final double scaleVariation = this.scale(columnCount) / (double) this.getPreferredSize().getWidth();
		this.setPreferredSize(new Dimension(this.scale(columnCount), this.scale(rowCount)));
		
		this.buffer1 = null;
		this.buffer2 = null;
		this.viewport.setBounds(0, 0, 0, 0);
		
		if (!isInfinite(scaleVariation) && !isNaN(scaleVariation)) {
			if (1.0 < scaleVariation) {
				this.invalidate();
			}
			
			this.scrollRectToVisible(new Rectangle(
					(int) ((viewport.x + viewport.width / 2) * scaleVariation - viewport.width / 2),
					(int) ((viewport.y + viewport.height/ 2) * scaleVariation - viewport.height / 2),
					viewport.width, viewport.height));
			
			if (scaleVariation < 1.0) {
				this.invalidate();
			}
			
			this.repaint();
		}
		
		RegionOfInterest[] rois = this.context.get("rois");
		
		if (rois.length <= this.getLod()) {
			rois = Arrays.copyOf(rois, this.getLod() + 1);
		}
		
		RegionOfInterest roi = rois[this.getLod()];
		
		if (roi == null || roi.getRowCount() != rowCount || roi.getColumnCount() != columnCount) {
			roi = RegionOfInterest.newInstance(rowCount, columnCount);
		}
		
		rois[this.getLod()] = roi;
		
		this.context.set("rois", rois);
	}
	
	public final void repaintAll() {
		final Image sourceImage = this.context.get(SOURCE_IMAGE);
		this.updateImage(sourceImage);
		
		this.viewport.setSize(0, 0);
		this.repaint();
	}
	
	private final void updateImage(final Image sourceImage) {
		ViewFilter viewFilter = this.context.get(VIEW_FILTER);
		
		if (viewFilter != null) {
			this.image = viewFilter.getImage();
			
			while (viewFilter.getSource() != null) {
				viewFilter = viewFilter.getSource();
			}
			
			viewFilter.setSourceImage(sourceImage);
		} else {
			this.image = sourceImage;
		}
	}
	
	public final void refreshBuffer() {
		final Rectangle newViewport = this.getVisibleRect();
		
		if ((this.buffer1 == null || this.buffer1.getWidth() != newViewport.width || this.buffer1.getHeight() != newViewport.height) &&
				!newViewport.isEmpty()) {
			this.buffer1 = copyOf(this.buffer1, newViewport.width, newViewport.height);
			this.buffer2 = copyOf(this.buffer2, newViewport.width, newViewport.height);
		}
		
		if (this.buffer1 != null && this.image != null) {
			final int endY = newViewport.y + newViewport.height;
			final int endX = newViewport.x + newViewport.width;
			
			if (!this.viewport.isEmpty()) {
				final Graphics2D g = this.buffer2.createGraphics();
				g.drawImage(this.buffer1, this.viewport.x - newViewport.x, this.viewport.y - newViewport.y, null);
				g.dispose();
			}
			
			final RegionOfInterest[] rois = this.context.get("rois");
			final RegionOfInterest roi = this.getLod() < rois.length ? rois[this.getLod()] : null;
			final int firstUnscaledY = this.unscale(newViewport.y);
			final int lastUnscaledY = this.unscale(endY - 1);
			final int firstUnscaledX = this.unscale(newViewport.x);
			final int lastUnscaledX = this.unscale(endX - 1);
			final int n = 1 + lastUnscaledX - firstUnscaledX;
			
			if (this.rowValues.length <= n) {
				this.rowValues = new int[n];
				this.rowValueReady = new boolean[n];
			}
			
			for (int unscaledY = firstUnscaledY, y = newViewport.y; unscaledY <= lastUnscaledY || y < endY; ++unscaledY) {
				final int nextEndY = min(endY, this.scale(unscaledY + 1));
				
				if (unscaledY < 0 || this.image.getRowCount() <= unscaledY) {
					y = nextEndY;
					continue;
				}
				
				fill(this.rowValueReady, false);
				
				while (y < nextEndY) {
					final int yInBuffer = y - newViewport.y;
					
					for (int unscaledX = firstUnscaledX, x = newViewport.x, i = 0; unscaledX <= lastUnscaledX || x < endX; ++unscaledX, ++i) {
						final int nextEndX = min(endX, this.scale(unscaledX + 1));
						
						if (unscaledX < 0 || this.image.getColumnCount() <= unscaledX) {
							x = nextEndX;
							continue;
						}
						
						while (x < nextEndX) {
							if (this.viewport.contains(x, y)) {
								++x;
								continue;
							}
							
							if (!this.rowValueReady[i]) {
								this.rowValueReady[i] = true;
								this.rowValues[i] = roi == null || roi.get(unscaledY, unscaledX) ?
										this.image.getValue(unscaledY, unscaledX) : 0;
							}
							
							final int xInBuffer = x - newViewport.x;
							
							this.buffer2.setRGB(xInBuffer, yInBuffer, this.rowValues[i]);
							
							++x;
						}
					}
					
					++y;
				}
			}
			
			{
				final BufferedImage tmp = this.buffer1;
				this.buffer1 = this.buffer2;
				this.buffer2 = tmp;
			}
		}
		
		this.viewport.setBounds(newViewport);
	}
	
	public final Image getImage() {
		return this.image;
	}
	
	@Override
	protected final void paintComponent(final Graphics g) {
		super.paintComponent(g);
		
		this.refreshBuffer();
		
		final Rectangle viewport = this.getVisibleRect();
		
		g.drawImage(this.buffer1, viewport.x, viewport.y, null);
		
		this.drawAnnotations(this.context, (Graphics2D) g);
	}
	
	public static final void drawAnnotations(final Context context, final Graphics2D g) {
		final Annotations annotations = context.get("annotations");
		final TreePath[] selectedPaths = context.get("selectedAnnotations");
		final int scale = context.get("scale");
		final int lod = context.get("lod");
		
		drawAnnotations(annotations, selectedPaths, scale, lod, g);
	}
	
	public static final void drawAnnotations(final Annotations annotations, final TreePath[] selectedPaths, final int scale, final int lod, final Graphics2D g) {
		final Collection<Object> selection = new ArrayList<Object>(selectedPaths == null ? 0 : selectedPaths.length);
		
		if (selectedPaths != null) {
			for (final TreePath path : selectedPaths) {
				selection.add(path.getLastPathComponent());
			}
		}
		
		final double s = scale * pow(2.0, -lod);
		final float[] dash = { (float) (5.0 / s), (float) (5.0 / s) };
		
		g.scale(s, s);
		
		try {
			for (final Annotation annotation : annotations.getAnnotations()) {
				if (!annotation.isVisible()) {
					continue;
				}
				
				g.setColor(annotation.getLineColor());
				
				final boolean annotationSelected = selection.contains(annotation);
				
				for (final Region region : annotation.getRegions()) {
					final float strokeWidth = annotationSelected || selection.contains(region) ? 3F / (float) s : 1F;
					
					if (region.isNegative()) {
						g.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 1F, dash, 0F));
					} else {
						g.setStroke(new BasicStroke(strokeWidth));
					}
					
					drawRegionOutline(region, g);
				}
			}
		} finally {
			g.scale(1.0 / s, 1.0 / s);
		}
	}
	
	public static final void drawRegionOutline(final Region region, final Graphics2D g) {
		final int n = region.getVertices().size();
		final int[] xs = new int[n];
		final int[] ys = new int[n];
		int i = 0;
		
		for (final Point2D.Float vertex : region.getVertices()) {
			xs[i] = (int) vertex.x;
			ys[i] = (int) vertex.y;
			++i;
		}
		
		g.drawPolyline(xs, ys, n);
	}
	
	public static final void fillRegion(final Region region, final Graphics2D g) {
		final int n = region.getVertices().size();
		final int[] xs = new int[n];
		final int[] ys = new int[n];
		int i = 0;
		
		for (final Point2D.Float vertex : region.getVertices()) {
			xs[i] = (int) vertex.x;
			ys[i] = (int) vertex.y;
			++i;
		}
		
		g.fillPolygon(xs, ys, n);
	}
	
	public static final BufferedImage copyOf(final BufferedImage image, final int newWidth, final int newHeight) {
		if (image == null) {
			return new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_3BYTE_BGR);
		}
		
		final BufferedImage result = new BufferedImage(newWidth, newHeight, image.getType());
		final Graphics2D g = result.createGraphics();
		
		g.drawImage(image, 0, 0, null);
		
		g.dispose();
		
		return result;
	}
	
}
