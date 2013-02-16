package imj.apps.modules;

import static java.lang.Double.isInfinite;
import static java.lang.Double.isNaN;

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

/**
 * @author codistmonk (creation 2013-02-13)
 */
public final class BigImageComponent extends JComponent {
	
	private final Context context;
	
	private final String imageId;
	
	private int lod;
	
	private Image image;
	
	private BufferedImage buffer1;
	
	private BufferedImage buffer2;
	
	private final Rectangle viewport;
	
	public BigImageComponent(final Context context, final String imageId) {
		this.context = context;
		this.imageId = imageId;
		this.viewport = new Rectangle();
		
		this.setLod(0);
		
		this.setFocusable(true);
		
		this.addKeyListener(new KeyAdapter() {
			
			@Override
			public final void keyTyped(final KeyEvent event) {
				if ('+' == event.getKeyChar()) {
					BigImageComponent.this.setLod(BigImageComponent.this.getLod() - 1);
				} else if ('-' == event.getKeyChar()) {
					BigImageComponent.this.setLod(BigImageComponent.this.getLod() + 1);
				}
			}
			
		});
		final MouseAdapter mouseHandler = new MouseAdapter() {
			
			private final Point viewportInitialLocation = new Point();
			
			private final Point mousePressedLocation = new Point();
			
			@Override
			public final void mouseMoved(final MouseEvent event) {
				context.set("xy", event.getPoint());
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
	
	public final void refreshImage() {
		this.image = ImageWrangler.INSTANCE.load(this.getImageId(), this.getLod());
		
		this.context.set("image", this.image);
		
		final Rectangle viewport = this.getVisibleRect();
		final int columnCount = this.image.getColumnCount();
		final int rowCount = this.image.getRowCount();
		final double scaleVariation = columnCount / (double) this.getPreferredSize().getWidth();
		this.setPreferredSize(new Dimension(columnCount, rowCount));
		
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
			
			RegionOfInterest roi = this.getLod() < rois.length ? rois[this.getLod()] : null;
			
			for (int y = newViewport.y; y < endY; ++y) {
				if (y < 0 || this.image.getRowCount() <= y) {
					continue;
				}
				
				final int yInBuffer = y - newViewport.y;
				
				for (int x = newViewport.x; x < endX; ++x) {
					if (x < 0 || this.image.getColumnCount() <= x || this.viewport.contains(x, y)) {
						continue;
					}
					
					final int xInBuffer = x - newViewport.x;
					
					this.buffer2.setRGB(xInBuffer, yInBuffer, roi == null || roi.get(y, x) ? this.image.getValue(y, x) : 0);
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
