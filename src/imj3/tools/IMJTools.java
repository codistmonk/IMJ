package imj3.tools;

import static imj3.tools.CommonTools.classForName;
import static imj3.tools.CommonTools.fieldValue;
import static imj3.tools.SVS2Multifile.newMetadata;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static multij.tools.Tools.array;
import static multij.tools.Tools.invoke;

import imj2.tools.BigBitSet;
import imj3.core.IMJCoreTools;
import imj3.core.Image2D;
import imj3.core.Image2D.Pixel2DProcessor;

import java.awt.Rectangle;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import multij.primitivelists.LongList;
import multij.tools.IllegalInstantiationException;
import multij.tools.Tools;
import multij.xml.XMLTools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author codistmonk (creation 2014-11-29)
 */
public final class IMJTools {
	
	private IMJTools() {
		throw new IllegalInstantiationException();
	}
	
	public static Image2D read(final String path) {
		return read(path, 0);
	}
	
	public static Image2D read(final String path, final int lod) {
		return IMJCoreTools.cache(path, () -> {
			try {
				return new MultifileImage2D(new MultifileSource(path), 0);
			} catch (final Exception exception1) {
				if (!exception1.getMessage().endsWith("metadata.xml")) {
					Tools.debugError(exception1);
				}
				
				try {
					return new AwtImage2D(path);
				} catch (final Exception exception2) {
					Tools.debugError(exception2);
					
					IMJTools.toneDownBioFormatsLogger();
					
					return new BioFormatsImage2D(path);
				}
			}
		}).getScaledImage(pow(2.0, -lod));
	}
	
	public static final void toneDownBioFormatsLogger() {
		try {
			final Class<?> loggerFactory = classForName("org.slf4j.LoggerFactory");
			final Object logLevel = fieldValue(classForName("ch.qos.logback.classic.Level"), "WARN");
			
			for (final String className : array(
					"loci.formats.tiff.TiffParser"
					, "loci.formats.tiff.TiffCompression"
					, "loci.formats.in.MinimalTiffReader"
					, "loci.formats.in.BaseTiffReader"
					, "loci.formats.FormatTools"
					, "loci.formats.FormatHandler"
					, "loci.formats.FormatReader"
					, "loci.formats.ImageReader"
					, "loci.common.services.ServiceFactory"
					)) {
				try {
					invoke(invoke(loggerFactory, "getLogger", classForName(className)), "setLevel", logLevel);
				} catch (final Exception exception) {
					Tools.debugError(exception);
				}
			}
		} catch (final Exception exception) {
			exception.printStackTrace();
		}
	}
	
	public static final void forEachTileIn(final Image2D image, final Image2D.Pixel2DProcessor tileProcessor) {
		forEachTile(image.getWidth(), image.getHeight(), image.getOptimalTileWidth(), image.getOptimalTileHeight(), tileProcessor);
	}
	
	public static final void forEachTile(final int imageWidth, final int imageHeight,
			final int tileWidth, final int tileHeight, final Image2D.Pixel2DProcessor tileProcessor) {
		for (int tileY = 0; tileY < imageHeight; tileY += tileHeight) {
			for (int tileX = 0; tileX < imageWidth; tileX += tileWidth) {
				if (!tileProcessor.pixel(tileX, tileY)) {
					return;
				}
				
				tileProcessor.endOfPatch();
			}
		}
	}
	
	public static final void forEachPixelInEachTileIn(final Image2D image, final Image2D.Pixel2DProcessor process) {
		forEachPixelInEachTile(image.getWidth(), image.getHeight(), image.getOptimalTileWidth(), image.getOptimalTileHeight(), process);
	}
	
	public static final void forEachPixelInEachTile(final int imageWidth, final int imageHeight,
			final int tileWidth, final int tileHeight, final Image2D.Pixel2DProcessor process) {
		forEachTile(imageWidth, imageHeight, tileWidth, tileHeight, new Image2D.Pixel2DProcessor() {
			
			@Override
			public final boolean pixel(final int tileX, final int tileY) {
				final int w = min(tileWidth, imageWidth - tileX);
				final int h = min(tileHeight, imageHeight - tileY);
				
				for (int y = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x) {
						if (!process.pixel(tileX + x, tileY + y)) {
							return false;
						}
					}
				}
				
				return true;
			}
			
			@Override
			public final void endOfPatch() {
				process.endOfPatch();
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 3431410834328760116L;
			
		});
	}
	
	public static final void forEachPixelInEachComponent4(final Image2D image, final Rectangle bounds, final ComponentComembership comembership, final Pixel2DProcessor process) {
		final int xStart = bounds.x;
		final int xEnd = bounds.x + bounds.width;
		final int yStart = bounds.y;
		final int yEnd = bounds.y + bounds.height;
		final BigBitSet done = new BigBitSet(image.getPixelCount());
		final LongList todo = new LongList();
		
		for (int y = yStart; y < yEnd; ++y) {
			for (int x = xStart; x < xEnd; ++x) {
				final long pixel = image.getPixel(x, y);
				
				if (!done.get(pixel)) {
					todo.add(pixel);
					
					done.set(pixel, true);
					
					while (!todo.isEmpty()) {
						final long p = todo.remove(0);
						final int xx = image.getX(p);
						final int yy = image.getY(p);
						
						if (!process.pixel(xx, yy)) {
							return;
						}
						
						if (yStart < yy) {
							final long neighbor = image.getPixel(xx, yy - 1);
							
							if (!done.get(neighbor) && comembership.test(image, p, neighbor)) {
								todo.add(neighbor);
								done.set(neighbor, true);
							}
						}
						
						if (xStart < xx) {
							final long neighbor = image.getPixel(xx - 1, yy);
							
							if (!done.get(neighbor) && comembership.test(image, p, neighbor)) {
								todo.add(neighbor);
								done.set(neighbor, true);
							}
						}
						
						if (xx + 1 < xEnd) {
							final long neighbor = image.getPixel(xx + 1, yy);
							
							if (!done.get(neighbor) && comembership.test(image, p, neighbor)) {
								todo.add(neighbor);
								done.set(neighbor, true);
							}
						}
						
						if (yy + 1 < yEnd) {
							final long neighbor = image.getPixel(xx, yy + 1);
							
							if (!done.get(neighbor) && comembership.test(image, p, neighbor)) {
								todo.add(neighbor);
								done.set(neighbor, true);
							}
						}
					}
					
					process.endOfPatch();
				}
			}
		}
	}
	
	public static final void createZipImage(final String path, final int width0, final int height0,
			final int optimalTileWidth, final int optimalTileHeight, final String tileFormat, final double micronsPerPixel,
			final TileGenerator generator) {
		final long t0 = System.currentTimeMillis();
		
		try (final ZipOutputStream output = new ZipOutputStream(new FileOutputStream(path))) {
			output.setLevel(ZipOutputStream.STORED);
			generator.setOutput(output);
			
			final Document xml = newMetadata(width0, height0,
					optimalTileWidth, optimalTileHeight,
					tileFormat, "" + micronsPerPixel, new int[] { 0 });
			
			output.putNextEntry(new ZipEntry("metadata.xml"));
			XMLTools.write(xml, output, 0);
			output.closeEntry();
			final List<Element> elements = XMLTools.getElements(xml, "//image");
			
			for (final Element imageElement : elements) {
				generator.setElement(imageElement);
				
				final int w = XMLTools.getNumber(imageElement, "@width").intValue();
				final int h = XMLTools.getNumber(imageElement, "@height").intValue();
				
				forEachTile(w, h, optimalTileWidth, optimalTileHeight, generator);
			}
			
			SVS2Multifile.includeHTMLViewer(output, width0, height0, optimalTileWidth, elements.size() - 1, tileFormat);
			
			Tools.debugPrint("Created", path, "in", System.currentTimeMillis() - t0, "ms");
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
	
	/**
	 * @author codistmonk (creation 2016-06-07)
	 */
	public static abstract interface TileGenerator extends Pixel2DProcessor {
		
		public abstract void setOutput(ZipOutputStream output);
		
		public abstract void setElement(Element imageElement);
		
	}
	
	/**
	 * @author codistmonk (creation 2016-02-24)
	 */
	public static abstract interface ComponentComembership extends Serializable {
		
		public abstract boolean test(Image2D image, long pixel, long otherPixel);
		
		/**
		 * @author codistmonk (creation 2016-02-24)
		 */
		public static final class Default implements ComponentComembership {
			
			@Override
			public final boolean test(final Image2D image, final long pixel, final long otherPixel) {
				return image.getPixelValue(pixel) == image.getPixelValue(otherPixel);
			}
			
			private static final long serialVersionUID = 3705129683034846142L;
			
			public static final Default INSTANCE = new Default();
			
		}
		
	}
	
}
