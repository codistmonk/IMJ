package imj3.tools;

import static imj3.tools.CommonTools.classForName;
import static imj3.tools.CommonTools.fieldValue;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.invoke;

import imj3.core.Image2D;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

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
		try {
			return new MultifileImage2D(new MultifileSource(path), lod);
		} catch (final Exception exception1) {
			try {
				return new AwtImage2D(path);
			} catch (final Exception exception2) {
				IMJTools.toneDownBioFormatsLogger();
				
				return new BioFormatsImage2D(path);
			}
		}
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
	
}
