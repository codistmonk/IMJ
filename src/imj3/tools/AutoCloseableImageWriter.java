package imj3.tools;

import static multij.tools.Tools.unchecked;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;

/**
 * @author codistmonk (creation 2014-05-25)
 */
public final class AutoCloseableImageWriter implements AutoCloseable, Serializable {
	
	private final ImageWriter writer;
	
	private final ImageWriteParam outputParameters;
	
	public AutoCloseableImageWriter(final String format) {
		this.writer = ImageIO.getImageWritersByFormatName(format).next();
		this.outputParameters = this.writer.getDefaultWriteParam();
	}
	
	public final ImageWriter getWriter() {
		return this.writer;
	}
	
	public final ImageWriteParam getOutputParameters() {
		return this.outputParameters;
	}
	
	public final AutoCloseableImageWriter setCompressionQuality(final float quality) {
		this.getOutputParameters().setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		this.getOutputParameters().setCompressionQuality(quality);
		
		return this;
	}
	
	public AutoCloseableImageWriter setOutput(final OutputStream output) {
		try {
			this.getWriter().setOutput(ImageIO.createImageOutputStream(output));
		} catch (final IOException exception) {
			throw unchecked(exception);
		}
		
		return this;
	}
	
	public final AutoCloseableImageWriter write(final RenderedImage image) {
		try {
			this.getWriter().write(null, new IIOImage(image, null, null), this.outputParameters);
		} catch (final IOException exception) {
			throw unchecked(exception);
		}
		
		return this;
	}
	
	@Override
	public final void close() throws Exception {
		this.getWriter().dispose();
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -3450450668038280563L;
	
	public static final void write(final RenderedImage image, final String format, final float quality, final String outputPath) {
		write(image, format, quality, new File(outputPath));
	}
	
	public static final void write(final RenderedImage image, final String format, final float quality, final File outputFile) {
		try {
			write(image, format, quality, new FileOutputStream(outputFile));
		} catch (final FileNotFoundException exception) {
			throw unchecked(exception);
		}
	}
	
	public static final void write(final RenderedImage image, final String format, final float quality, final OutputStream output) {
		try (final AutoCloseableImageWriter writer = new AutoCloseableImageWriter(format)) {
			writer.setCompressionQuality(quality).setOutput(output).write(image);
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
}