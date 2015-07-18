package imj3.draft;

import static multij.tools.Tools.*;
import imj3.core.Image2D;
import imj3.tools.IMJTools;
import imj3.tools.Image2DComponent;
import imj3.tools.Image2DComponent.Overlay;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.concurrent.atomic.AtomicInteger;

import multij.swing.MouseHandler;
import multij.swing.SwingTools;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-07-18)
 */
public final class VisualPatchExtractor {
	
	private VisualPatchExtractor() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final Image2DComponent component = new Image2DComponent(IMJTools.read(commandLineArguments[0]));
		final Point mouseLocation = new Point();
		final AtomicInteger patchSize = new AtomicInteger(32);
		final AtomicInteger patchStride = new AtomicInteger(1);
		
		new MouseHandler() {
			
			@Override
			public final void mouseExited(final MouseEvent event) {
				mouseLocation.x = -1;
				component.repaint();
			}
			
			@Override
			public final void mouseMoved(final MouseEvent event) {
				mouseLocation.setLocation(event.getX(), event.getY());
				component.repaint();
			}
			
			@Override
			public final void mouseDragged(final MouseEvent event) {
				this.mouseMoved(event);
			}
			
			@Override
			public final void mouseWheelMoved(final MouseWheelEvent event) {
				this.mouseMoved(event);
			}
			
			private static final long serialVersionUID = 4116418318352589515L;
			
		}.addTo(component);
		
		component.setOverlay(new Overlay() {
			
			private final Point tmp = new Point();
			
			@Override
			public final void update(final Graphics2D graphics, final Rectangle region) {
				if (0 <= mouseLocation.x) {
					try {
						final int size = patchSize.get();
						final int stride = patchStride.get();
						final AffineTransform view = component.getView();
						final Image2D image = component.getImage();
						final Image2D image0 = image.getScaledImage(1.0);
						final double imageScale = image.getScale();
						final int scaledPatchSize = (int) (size * view.getScaleX() / imageScale);
						
						view.inverseTransform(mouseLocation, this.tmp);
						
						if (0 <= this.tmp.x && this.tmp.x < image0.getWidth()
								&& 0 <= this.tmp.y && this.tmp.y < image0.getHeight()) {
							this.tmp.x = patchStart(this.tmp.x, stride, size, imageScale);
							this.tmp.y = patchStart(this.tmp.y, stride, size, imageScale);
							
							view.transform(this.tmp, this.tmp);
						
							graphics.setColor(Color.RED);
							graphics.drawRect(this.tmp.x, this.tmp.y, scaledPatchSize, scaledPatchSize);
						}
					} catch (final NoninvertibleTransformException exception) {
						exception.printStackTrace();
					}
				}
			}
			
			private static final long serialVersionUID = -6646527468467687096L;
			
		});
		
		SwingTools.show(component, commandLineArguments[0], false);
	}
	
	public static final int patchStart(final int coordinate, final int patchStride, final int patchSize, final double imageScale) {
		return (int) (adjust(coordinate, (int) (patchStride / imageScale)) - patchSize / imageScale / 2.0);
	}
	
	public static final int adjust(final int coordinate, final int patchStride) {
		return coordinate / patchStride * patchStride + patchStride / 2;
	}
	
}
