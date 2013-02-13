package imj.apps;

import static java.lang.Double.isInfinite;
import static java.lang.Double.isNaN;
import static net.sourceforge.aprog.af.AFTools.newAboutItem;
import static net.sourceforge.aprog.af.AFTools.newPreferencesItem;
import static net.sourceforge.aprog.af.AFTools.newQuitItem;
import static net.sourceforge.aprog.i18n.Messages.setMessagesBase;
import static net.sourceforge.aprog.swing.SwingTools.menuBar;
import static net.sourceforge.aprog.swing.SwingTools.packAndCenter;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.swing.SwingTools.useSystemLookAndFeel;
import static net.sourceforge.aprog.swing.SwingTools.I18N.menu;
import static net.sourceforge.aprog.tools.Tools.getThisPackagePath;
import imj.Image;
import imj.ImageWrangler;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.sourceforge.aprog.af.AFConstants;
import net.sourceforge.aprog.af.AFMainFrame;
import net.sourceforge.aprog.af.AFTools;
import net.sourceforge.aprog.af.AbstractAFAction;
import net.sourceforge.aprog.af.MacOSXTools;
import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-02-13)
 */
public final class Show {
	
	private Show() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * {@value}.
	 */
	public static final String APPLICATION_NAME = "IMJ Show";
	
	/**
	 * {@value}.
	 */
	public static final String APPLICATION_VERSION = "0.1.0";
	
	/**
	 * {@value}.
	 */
	public static final String APPLICATION_COPYRIGHT = "(c) 2013 Codist Monk";
	
	/**
	 * {@value}.
	 */
	public static final String APPLICATION_ICON_PATH = "imj/apps/thumbnail.png";
	
	public static final Context newContext() {
		final Context result = AFTools.newContext();
		
		result.set(AFConstants.Variables.APPLICATION_NAME, APPLICATION_NAME);
		result.set(AFConstants.Variables.APPLICATION_VERSION, APPLICATION_VERSION);
		result.set(AFConstants.Variables.APPLICATION_COPYRIGHT, APPLICATION_COPYRIGHT);
		result.set(AFConstants.Variables.APPLICATION_ICON_PATH, APPLICATION_ICON_PATH);
		
		new AbstractAFAction(result, AFConstants.Variables.ACTIONS_QUIT) {
			
			@Override
			public final void perform() {
				final AFMainFrame mainFrame = result.get(AFConstants.Variables.MAIN_FRAME);
				
				mainFrame.dispose();
			}
			
		};
		
		result.set(AFConstants.Variables.MAIN_MENU_BAR, menuBar(
				menu("Application",
						newAboutItem(result),
						null,
						newPreferencesItem(result),
						null,
						newQuitItem(result))));
		
		return result;
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		if (commandLineArguments.length != 2) {
			System.out.println("Arguments: file <imageId>");
			
//			return;
		}
		
		MacOSXTools.setupUI(APPLICATION_NAME, APPLICATION_ICON_PATH);
		useSystemLookAndFeel();
		setMessagesBase(getThisPackagePath() + "i18n/Messages");
		
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
//		final String imageId = arguments.get("file", "");
		final String imageId = arguments.get("file", "../Libraries/images/16088.svs");
		
		ImageWrangler.INSTANCE.load(imageId);
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public final void run() {
				final AFMainFrame frame = AFMainFrame.newMainFrame(newContext());
				
				frame.setPreferredSize(new Dimension(800, 600));
				
				frame.add(scrollable(centered(new BigImageComponent(imageId))));
				
				packAndCenter(frame).setVisible(true);
			}
			
		});
	}
	
	public static final JComponent centered(final Component component) {
		final JComponent result = new JPanel(new GridBagLayout());
		
		result.add(component);
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2013-02-13)
	 */
	public static final class BigImageComponent extends JComponent {
		
		private final String imageId;
		
		private int lod;
		
		private Image image;
		
		private BufferedImage buffer1;
		
		private BufferedImage buffer2;
		
		private final Rectangle viewport;
		
		public BigImageComponent(final String imageId) {
			this.imageId = imageId;
			this.viewport = new Rectangle();
			
			this.refreshImage();
			
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
				
				this.refreshImage();
			}
		}
		
		public final void refreshImage() {
			this.image = ImageWrangler.INSTANCE.load(this.getImageId(), this.getLod());
			final Rectangle viewport = this.getVisibleRect();
			final double scaleVariation = this.image.getColumnCount() / (double) this.getPreferredSize().getWidth();
			
			this.setPreferredSize(new Dimension(this.image.getColumnCount(), this.image.getRowCount()));
			
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
			}
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
				
				for (int y = newViewport.y; y < endY; ++y) {
					for (int x = newViewport.x; x < endX; ++x) {
						if (!this.viewport.contains(x, y)) {
							this.buffer2.setRGB(x - newViewport.x, y - newViewport.y, this.image.getValue(y, x));
						}
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
		
	}
	
}
