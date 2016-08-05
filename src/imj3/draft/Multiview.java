package imj3.draft;

import imj3.core.Image2D;
import imj3.tools.AwtImage2D;
import imj3.tools.Image2DComponent;
import imj3.tools.Image2DComponent.ImageChangedEvent;

import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import multij.events.EventManager;
import multij.events.EventManager.Event.Listener;
import multij.swing.MouseHandler;
import multij.swing.SwingTools;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2016-08-05)
 */
public final class Multiview {
	
	private Multiview() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String... commandLineArguments) {
		final Image2D defaultImage = new AwtImage2D("", 1, 1);
		final Image2DComponent component1 = new Image2DComponent(defaultImage).setDropImageEnabled(true);
		final Image2DComponent component2 = new Image2DComponent(defaultImage).setDropImageEnabled(true);
		
		new MouseHandler() {
			
			@Override
			public final void mousePressed(final MouseEvent event) {
				this.synchronize(event);
			}
			
			@Override
			public final void mouseDragged(final MouseEvent event) {
				this.synchronize(event);
			}
			
			@Override
			public final void mouseWheelMoved(final MouseWheelEvent event) {
				this.synchronize(event);
			}
			
			private final void synchronize(final AWTEvent event) {
				if (event.getSource() == component1) {
					component2.dispatchEvent(event);
				}
			}
			
			private static final long serialVersionUID = -3742345372787912928L;
			
		}.addTo(component1);
		
		new MouseHandler() {
			
			@Override
			public final void mouseWheelMoved(final MouseWheelEvent event) {
				this.synchronize(event);
			}
			
			private final void synchronize(final AWTEvent event) {
				if (event.getSource() == component2) {
					component1.dispatchEvent(event);
				}
			}
			
			private static final long serialVersionUID = -3082466386304526114L;
			
		}.addTo(component2);
		
		EventManager.getInstance().addListener(component1, ImageChangedEvent.class, new Object() {
			
			@Listener
			public final void imageChanged(final ImageChangedEvent event) {
				if (component2.getImage() != defaultImage) {
					SwingUtilities.invokeLater(() -> {
						component1.setViewScale(component2.getScaleX());
					});
				}
			}
			
		});
		
		EventManager.getInstance().addListener(component2, ImageChangedEvent.class, new Object() {
			
			@Listener
			public final void imageChanged(final ImageChangedEvent event) {
				if (component1.getImage() != defaultImage) {
					SwingUtilities.invokeLater(() -> {
						component2.setViewScale(component1.getScaleX());
					});
				}
			}
			
		});
		
		SwingUtilities.invokeLater(() -> {
			final JSplitPane split = SwingTools.horizontalSplit(component1, component2);
			
			split.setPreferredSize(new Dimension(1024, 512));
			
			SwingTools.show(split, Multiview.class.getSimpleName(), false).addWindowListener(new WindowAdapter() {
				
				// XXX sftp handler prevents application from closing normally
				
				@Override
				public final void windowClosed(final WindowEvent event) {
					imj3.protocol.sftp.Handler.closeAll();
					System.exit(0);
				}
				
			});
			
			split.setDividerLocation(0.5);
		});
	}

}
