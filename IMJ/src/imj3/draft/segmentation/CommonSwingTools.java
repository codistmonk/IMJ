package imj3.draft.segmentation;

import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;

import imj3.draft.segmentation.CommonTools.Property;

import java.awt.Component;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-01-20)
 */
public final class CommonSwingTools {
	
	private CommonSwingTools() {
		throw new IllegalInstantiationException();
	}
	
	public static final void showEditDialog(final String title, final Runnable ifOkClicked, final Property... properties) {
		final JDialog dialog = new JDialog((Window) null, title);
		final Box box = Box.createVerticalBox();
		final Map<Property, Supplier<String>> newValues = new IdentityHashMap<>();
		
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		for (final Property property : properties) {
			final JTextField field = new JTextField("" + property.getGetter().get());
			
			box.add(horizontalBox(new JLabel(property.getName()), field));
			
			newValues.put(property, field::getText);
		}
		
		box.add(horizontalBox(
				Box.createHorizontalGlue(),
				new JButton(new AbstractAction("Ok") {
					
					@Override
					public final void actionPerformed(final ActionEvent event) {
						for (final Property property : properties) {
							property.getParser().apply(newValues.get(property).get());
						}
						
						dialog.dispose();
						
						ifOkClicked.run();
					}
					
					private static final long serialVersionUID = -1250254465599248142L;
				
				}),
				new JButton(new AbstractAction("Cancel") {
					
					@Override
					public final void actionPerformed(final ActionEvent event) {
						dialog.dispose();
					}
					
					private static final long serialVersionUID = -1250254465599248142L;
					
				})
		));
		
		dialog.add(box);
		
		SwingTools.packAndCenter(dialog).setVisible(true);
	}
	
	public static final JPanel center(final Component component) {
		final JPanel result = new JPanel(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		
		c.anchor = GridBagConstraints.CENTER;
		
		result.add(component, c);
		
		return result;
	}
	
	public static final JMenuItem item(final String text, final ActionListener action) {
		final JMenuItem result = new JMenuItem(text);
		
		result.addActionListener(action);
		
		return result;
	}
	
	public static final void repeat(final Window window, final int delay, final ActionListener action) {
		final Timer timer = new Timer(delay, action) {
			
			@Override
			public final void stop() {
				try {
					action.actionPerformed(null);
				} finally {
					super.stop();
				}
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 4028342893634389147L;
			
		};
		
		timer.setRepeats(true);
		timer.start();
		
		window.addWindowListener(new WindowAdapter() {
			
			@Override
			public final void windowClosing(final WindowEvent event) {
				timer.stop();
			}
			
		});
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
	
}
