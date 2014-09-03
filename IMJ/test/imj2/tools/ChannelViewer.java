package imj2.tools;

import static imj2.tools.IMJTools.*;
import static java.lang.Integer.parseInt;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static net.sourceforge.aprog.swing.SwingTools.show;

import imj2.tools.ColorSeparationTest.RGBTransformer;
import imj2.tools.Image2DComponent.Painter;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Locale;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2014-08-03)
 */
public final class ChannelViewer {
	
	private ChannelViewer() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param arguments
	 * <br>Unused
	 */
	public static final void main(final String[] arguments) {
		SwingTools.useSystemLookAndFeel();
		
		final JTextField channelsSpecifier = new JTextField("r g b");
		final SimpleImageView imageView = new SimpleImageView();
		final JPanel mainPanel = new JPanel(new BorderLayout());
		final RGBTransformer[] transformer = { RGBTransformer.Predefined.ID };
		
		mainPanel.add(channelsSpecifier, BorderLayout.NORTH);
		mainPanel.add(imageView, BorderLayout.CENTER);
		
		imageView.getPainters().add(new Painter<SimpleImageView>() {
			
			@Override
			public final void paint(final Graphics2D g, final SimpleImageView component,
					final int width, final int height) {
				final BufferedImage buffer = component.getBufferImage();
				final int w = buffer.getWidth();
				final int h = buffer.getHeight();
				
				for (int y = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x) {
						buffer.setRGB(x, y, transformer[0].transform(buffer.getRGB(x, y)));
					}
				}
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -1045988659583024746L;
			
		});
		
		channelsSpecifier.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public final void removeUpdate(final DocumentEvent event) {
				this.update(event);
			}
			
			@Override
			public final void insertUpdate(final DocumentEvent event) {
				this.update(event);
			}
			
			@Override
			public final void changedUpdate(final DocumentEvent event) {
				this.update(event);
			}
			
			private final void update(final DocumentEvent event) {
				try {
					final String text = channelsSpecifier.getText().toUpperCase(Locale.ENGLISH);
					final String[] channelsSpecification = text.trim().split("\\s+");
					transformer[0] = new Multiplexer(Arrays.stream(channelsSpecification)
							.map(ChannelViewer::parse).toArray(RGBTransformer[]::new));
					
					Tools.debugPrint((Object[]) channelsSpecification);
					
					imageView.refreshBuffer();
				} catch (final Exception exception) {
					Tools.debugError(exception);
				}
			}
			
		});
		
		show(mainPanel, ChannelViewer.class.getSimpleName(), false);
	}
	
	public static final int clamp(final int value, final int minimum, final int maximum) {
		return max(0, min(value, 255));
	}
	
	public static final int digitize(final float channelValue) {
		return clamp(round(channelValue * 255F), 0, 255);
	}
	
	public static final RGBTransformer parse(final String string) {
		try {
			return new Gray(parseInt(string));
		} catch (final Exception exception) {
			return Channel2Gray.valueOf(string);
		}
	}
	
	/**
	 * @author codistmonk (creation 2014-08-03)
	 */
	public static final class Multiplexer implements RGBTransformer {
		
		private final RGBTransformer[] transformers;
		
		public Multiplexer(final RGBTransformer[] transformers) {
			this.transformers = transformers;
		}
		
		@Override
		public final int transform(final int rgb) {
			int result = 0xFF000000;
			int mask = 0x00FFFFFF;
			
			for (final RGBTransformer transformer : this.transformers) {
				result = (result & ~mask) | (transformer.transform(rgb) & mask);
				mask >>= Byte.SIZE;
			}
			
			return result;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 4394160211395945075L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-08-03)
	 */
	public static final class Gray implements RGBTransformer {
		
		private final int value;
		
		public Gray(final int value) {
			this.value = 0xFF000000 | (clamp(value, 0, 255) * 0x00010101);
		}
		
		@Override
		public final int transform(final int rgb) {
			return this.value;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -6796486661098501344L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-08-03)
	 */
	public static enum Channel2Gray implements RGBTransformer {
		
		R {
			
			@Override
			public final int transform(final int rgb) {
				return 0xFF000000 | (red8(rgb) * 0x00010101);
			}
			
		}, G {
			
			@Override
			public final int transform(final int rgb) {
				return 0xFF000000 | (green8(rgb) * 0x00010101);
			}
			
		}, B {
			
			@Override
			public final int transform(final int rgb) {
				return 0xFF000000 | (blue8(rgb) * 0x00010101);
			}
			
		}, Y {
			
			@Override
			public final int transform(final int rgb) {
				final float r = red8(rgb) / 255F;
				final float g = green8(rgb) / 255F;
				final float b = blue8(rgb) / 255F;
				final float y = 0.299F * r + 0.587F * g + 0.114F * b;
				
				return 0xFF000000 | (digitize(y) * 0x00010101);
			}
			
		}, U {
			
			@Override
			public final int transform(final int rgb) {
				final float r = red8(rgb) / 255F;
				final float g = green8(rgb) / 255F;
				final float b = blue8(rgb) / 255F;
				final float uMax = 0.436F;
				final float u = (-0.14713F * r - 0.28886F * g + uMax * b + uMax) / uMax;
				
				return 0xFF000000 | (digitize(u) * 0x00010101);
			}
			
		}, V {
			
			@Override
			public final int transform(final int rgb) {
				final float r = red8(rgb) / 255F;
				final float g = green8(rgb) / 255F;
				final float b = blue8(rgb) / 255F;
				final float vMax = 0.615F;
				final float v = (vMax * r - 0.51499F * g - 0.10001F * b + vMax) / vMax;
				
				return 0xFF000000 | (digitize(v) * 0x00010101);
			}
			
		};
		
	}
	
}
