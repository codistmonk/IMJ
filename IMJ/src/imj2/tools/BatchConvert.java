package imj2.tools;

import java.awt.Dimension;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import loci.common.services.ServiceFactory;
import loci.formats.ImageReader;
import loci.formats.ImageWriter;
import loci.formats.services.OMEXMLService;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2014-03-30)
 */
public final class BatchConvert {
	
	private BatchConvert() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public final void run() {
				final String extensions = JOptionPane.showInputDialog("Extensions", "tiff");
				String regex = ".+";
				
				if (extensions != null) {
					regex += "(";
					for (final String extension : extensions.trim().split("\\s+")) {
						regex += "\\." + extension;
					}
					regex += ")$";
				}
				
				final String filter = regex;
				final JFrame frame = new JFrame("IMJ Batch Convert");
				final JTextArea textArea = new JTextArea();
				
				frame.setPreferredSize(new Dimension(256, 256));
				frame.add(SwingTools.scrollable(textArea));
				frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				
				textArea.setEditable(false);
				textArea.setDropTarget(new DropTarget() {
					
					@Override
					public final synchronized void drop(final DropTargetDropEvent event) {
						for (final File file : SwingTools.getFiles(event)) {
							MultiThreadTools.getExecutor().submit(new FileProcessor(filter, file, textArea));
						}
					}
					
					/**
					 * {@value}.
					 */
					private static final long serialVersionUID = -3511295868159683003L;
					
				});
				
				SwingTools.packAndCenter(frame).setVisible(true);
				
				frame.addWindowListener(new WindowAdapter() {
					
					@Override
					public final void windowClosed(final WindowEvent event) {
						MultiThreadTools.shutdownExecutor();
					}
					
				});
			}
			
		});
	}
	
	/**
	 * @author codistmonk (creation 2014-03-30)
	 */
	public static final class FileProcessor implements Runnable {
		
		private final String filter;
		
		private final File file;
		
		private final JTextArea textArea;
		
		public FileProcessor(final String filter, final File file, final JTextArea textArea) {
			this.filter = filter;
			this.file = file;
			this.textArea = textArea;
		}
		
		@Override
		public final void run() {
			if (this.file.isDirectory()) {
				for (final File file : this.file.listFiles()) {
					MultiThreadTools.getExecutor().submit(new FileProcessor(this.filter, file, this.textArea));
				}
				
				return;
			}
			
			if (!this.file.getName().matches(this.filter)) {
				return;
			}
			
			final String id = this.file.toString();
			
			this.message("Processing " + id + "...");
			
			final ImageReader reader = new ImageReader();
			final ImageWriter writer = new ImageWriter();
			
			try {
				final OMEXMLService service = new ServiceFactory().getInstance(OMEXMLService.class);
				
				reader.setMetadataStore(service.createOMEXMLMetadata());
				reader.setId(id);
				
				String outId = id.replaceFirst("\\..+$", ".png");
				
				if (!outId.endsWith(".png")) {
					outId += ".png";
				}
				
				writer.setMetadataRetrieve(service.asRetrieve(reader.getMetadataStore()));
				writer.setId(outId);
				
				this.message("Saving " + outId + "...");
				
				writer.saveBytes(0, reader.openBytes(0));
			} catch (final Exception exception) {
				this.message(exception.getLocalizedMessage());
			} finally {
				try {
					reader.close();
				} catch (final IOException exception) {
					this.message(exception.getLocalizedMessage());
				}
				try {
					writer.close();
				} catch (final IOException exception) {
					this.message(exception.getLocalizedMessage());
				}
			}
			
			this.message("Processing " + id + " done");
		}
		
		public final void message(final String message) {
			final JTextArea textArea = this.textArea;
			
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public final void run() {
					textArea.append("\n" + message);
				}
				
			});
		}
		
	}
	
}
