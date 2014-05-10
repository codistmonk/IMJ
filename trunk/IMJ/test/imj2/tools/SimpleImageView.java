package imj2.tools;

import static net.sourceforge.aprog.swing.SwingTools.getFiles;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import imj2.tools.Image2DComponent.Painter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sourceforge.aprog.swing.SwingTools;

/**
 * @author codistmonk (creation 2014-01-22)
 */
public final class SimpleImageView extends JPanel {
	
	private final JLabel imageHolder;
	
	private BufferedImage image;
	
	private final Canvas buffer;
	
	private final List<Painter<SimpleImageView>> painters;
	
	private int bufferType;
	
	public SimpleImageView() {
		super(new BorderLayout());
		this.imageHolder = new JLabel();
		this.buffer = new Canvas();
		this.painters = new ArrayList<Painter<SimpleImageView>>();
		
		this.setPreferredSize(new Dimension(256, 256));
		
		SwingTools.setCheckAWT(false);
		this.add(scrollable(centered(this.imageHolder)), BorderLayout.CENTER);
		SwingTools.setCheckAWT(true);
		
		this.setDropTarget(new DropTarget(this, new DropTargetAdapter() {
			
			@Override
			public final void drop(final DropTargetDropEvent event) {
				final List<File> files = getFiles(event);
				
				try {
					SimpleImageView.this.setImage(ImageIO.read(files.get(0)));
				} catch (final IOException exception) {
					throw unchecked(exception);
				}
			}
			
		}));
	}
	
	public final int getBufferType() {
		return this.bufferType;
	}
	
	public final SimpleImageView setBufferType(final int bufferType) {
		this.bufferType = bufferType;
		
		return this;
	}
	
	public final JLabel getImageHolder() {
		return this.imageHolder;
	}
	
	public final BufferedImage getImage() {
		return this.image;
	}
	
	public final Canvas getBuffer() {
		return this.buffer;
	}
	
	public final BufferedImage getBufferImage() {
		return this.getBuffer().getImage();
	}
	
	public final Graphics2D getBufferGraphics() {
		return this.getBuffer().getGraphics();
	}
	
	public final void setImage(final BufferedImage image) {
		this.image = image;
		this.getBuffer().setFormat(image.getWidth(), image.getHeight(),
				this.getBufferType() != 0 ? this.getBufferType() : image.getType());
		
		this.getImageHolder().setIcon(new ImageIcon(this.getBuffer().getImage()));
		
		this.refreshBuffer();
	}
	
	public final List<Painter<SimpleImageView>> getPainters() {
		return this.painters;
	}
	
	public final void refreshBuffer() {
		final Graphics2D g = this.getBuffer().getGraphics();
		
		if (g == null) {
			return;
		}
		
		g.drawImage(this.getImage(), 0, 0, null);
		
		for (final Painter<SimpleImageView> painter : this.getPainters()) {
			painter.paint(g, this, this.getBuffer().getWidth(), this.getBuffer().getHeight());
		}
		
		this.repaint();
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -7264772728114165458L;
	
	public static final JPanel centered(final Component component) {
		SwingTools.checkAWT();
		
		final JPanel result = new JPanel(new GridBagLayout());
		
		result.add(component, new GridBagConstraints());
		
		return result;
	}
	
}
