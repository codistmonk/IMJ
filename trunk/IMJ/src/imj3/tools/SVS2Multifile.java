package imj3.tools;

import static net.sourceforge.aprog.tools.Tools.unchecked;
import imj3.core.Channels;
import imj3.tools.CommonTools.FileProcessor;

import java.io.File;
import java.util.Locale;

import loci.formats.FormatTools;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.RegexFilter;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2014-11-30)
 */
public final class SVS2Multifile {
	
	private SVS2Multifile() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File inRoot = new File(arguments.get("in", ""));
		final RegexFilter filter = new RegexFilter(arguments.get("filter", ".*\\.svs"));
		final File outRoot = new File(arguments.get("out", ""));
		
		IMJTools.toneDownBioFormatsLogger();
		
		FileProcessor.deepForEachFileIn(inRoot, new FileProcessor() {

			@Override
			public final void process(final File file) {
				if (filter.accept(file.getParentFile(), file.getName())) {
					if (file.getPath().contains("/old/")) {
						Tools.debugError("Ignoring", file);
					} else {
						Tools.debugPrint(file);
						
						try {
							final IFormatReader reader = newImageReader(file.getPath());
							
							Tools.debugPrint(reader.getSizeX(), reader.getSizeY(), predefinedChannelsFor(reader));
						} catch (final Exception exception) {
							Tools.debugError(exception);
						}
					}
				}
			}
			
			private static final long serialVersionUID = 7631423500885984364L;
			
		});
	}
	
	public static final IFormatReader newImageReader(final String id) {
		final IFormatReader reader = new ImageReader();
		
		try {
			reader.setId(id);
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
		
		if ("portable gray map".equals(reader.getFormat().toLowerCase(Locale.ENGLISH))) {
			// XXX This fixes a defect in Bio-Formats PPM loading, but is it always OK?
			reader.getCoreMetadata()[0].interleaved = true;
		}
		
		reader.setSeries(0);
		
		return reader;
	}
	
	public static final int packPixelValue(final byte[][] channelTables, final int colorIndex) {
		int result = 0;
		
		for (final byte[] channelTable : channelTables) {
			result = (result << 8) | (channelTable[colorIndex] & 0x000000FF);
		}
		
		return result;
	}
	
	public static final int packPixelValue(final short[][] channelTables, final int colorIndex) {
		int result = 0;
		
		for (final short[] channelTable : channelTables) {
			result = (result << 16) | (channelTable[colorIndex] & 0x0000FFFF);
		}
		
		return result;
	}
	
	public static final Channels predefinedChannelsFor(final IFormatReader lociImage) {
		if (lociImage.isIndexed()) {
			return Channels.Predefined.C3_U8;
		}
		
		switch (lociImage.getRGBChannelCount()) {
		case 1:
			switch (FormatTools.getBytesPerPixel(lociImage.getPixelType()) * lociImage.getRGBChannelCount()) {
			case 1:
				return 1 == lociImage.getBitsPerPixel() ?
						Channels.Predefined.C1_U1 : Channels.Predefined.C1_U8;
			case 2:
				return Channels.Predefined.C1_U16;
			default:
				return Channels.Predefined.C1_S32;
			}
		case 2:
			return Channels.Predefined.C2_U16;
		case 3:
			return Channels.Predefined.C3_U8;
		case 4:
			return Channels.Predefined.C4_U8;
		default:
			throw new IllegalArgumentException();
		}
	}
	
}
