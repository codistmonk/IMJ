package imj.apps.modules;

import static java.lang.Double.isInfinite;
import static java.lang.Double.isNaN;
import static java.lang.Math.min;
import static java.util.Arrays.fill;
import imj.Image;
import imj.ImageWrangler;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.JComponent;

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.events.Variable.Listener;
import net.sourceforge.aprog.events.Variable.ValueChangedEvent;

/**
 * @author codistmonk (creation 2013-02-13)
 */
public final class BigImageComponent extends JComponent {
	
	private final Context context;
	
	private final String imageId;
	
	private int scale;
	
	private int lod;
	
	private FilteredImage image;
	
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
		
		this.setLod(0);
		this.setScale(1);
		
		this.setFocusable(true);
		
		context.set("autoAdjustScale", false);
		
		this.addKeyListener(new KeyAdapter() {
			
			@Override
			public final void keyTyped(final KeyEvent event) {
				final int oldScale = BigImageComponent.this.getScale();
				
				if ('s' == event.getKeyChar()) {
					context.set("autoAdjustScale", false);
				} else if ('S' == event.getKeyChar()) {
					context.set("autoAdjustScale", true);
				} else if ('*' == event.getKeyChar()) {
					BigImageComponent.this.setScale(oldScale * 2);
				} else if ('/' == event.getKeyChar()) {
					BigImageComponent.this.setScale(oldScale / 2);
				} else {
					final int oldLod = BigImageComponent.this.getLod();
					final boolean adjustScale = context.get("autoAdjustScale");
					
					if ('+' == event.getKeyChar()) {
						BigImageComponent.this.setLod(oldLod - 1);
						
						if (adjustScale && 0 < oldLod) {
							BigImageComponent.this.setScale(oldScale / 2);
						}
					} else if ('-' == event.getKeyChar()) {
						BigImageComponent.this.setLod(oldLod + 1);
						
						if (adjustScale) {
							BigImageComponent.this.setScale(oldScale * 2);
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
		
		context.getVariable("viewFilter").addListener(fullRepaintNeeded);
		context.getVariable("sieve").addListener(fullRepaintNeeded);
		
		this.setDoubleBuffered(false);
	}
	
	@Override
	protected final void paintComponent(final Graphics g) {
		super.paintComponent(g);
		
		this.refreshBuffer();
		
		final Rectangle viewport = this.getVisibleRect();
		
		g.drawImage(this.buffer1, viewport.x, viewport.y, null);
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
	
	public final int scale(final int value) {
		return value * this.getScale();
	}
	
	public final int unscale(final int value) {
		return value / this.getScale();
	}
	
	public final void refreshImage() {
		this.image = new FilteredImage(ImageWrangler.INSTANCE.load(this.getImageId(), this.getLod()));
		this.image.setFilter((ViewFilter) this.context.get("viewFilter"));
		
		this.context.set("image", this.image);
		
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
			roi = new RegionOfInterest(rowCount, columnCount);
		}
		
		rois[this.getLod()] = roi;
		
		this.context.set("rois", rois);
	}
	
	public final void repaintAll() {
		this.image.setFilter((ViewFilter) this.context.get("viewFilter"));
		this.viewport.setSize(0, 0);
		this.repaint();
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
				if (unscaledY < 0 || this.image.getRowCount() <= unscaledY) {
					continue;
				}
				
				fill(this.rowValueReady, false);
				
				final int nextEndY = min(endY, this.scale(unscaledY + 1));
				
				while (y < nextEndY) {
					final int yInBuffer = y - newViewport.y;
					
					for (int unscaledX = firstUnscaledX, x = newViewport.x, i = 0; unscaledX <= lastUnscaledX || x < endX; ++unscaledX, ++i) {
						if (unscaledX < 0 || this.image.getColumnCount() <= unscaledX) {
							continue;
						}
						
						final int nextEndX = min(endX, this.scale(unscaledX + 1));
						
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
