package imj3.tools;

import static imj3.tools.CommonTools.classForName;
import static imj3.tools.CommonTools.fieldValue;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static multij.tools.Tools.array;
import static multij.tools.Tools.invoke;

import imj3.core.IMJCoreTools;
import imj3.core.Image2D;

import multij.tools.IllegalInstantiationException;
import multij.tools.Tools;

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
	
}
