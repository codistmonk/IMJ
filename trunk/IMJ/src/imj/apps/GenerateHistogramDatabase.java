package imj.apps;

import static imj.ImageOfBufferedImage.rgb;
import static java.lang.Integer.parseInt;
import static net.sourceforge.aprog.tools.Tools.usedMemory;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2013-04-11)
 */
public final class GenerateHistogramDatabase {
	
	private GenerateHistogramDatabase() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static final void main(final String[] commandLineArguments) throws FileNotFoundException, IOException {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String rootPath = arguments.get("classes", "");
		final String outPath = arguments.get("out", "db.jo");
		final int shift = arguments.get("shift", 3)[0];
		final int channelBitCount = 8 - shift;
		final File root = new File(rootPath);
		final Map<Object, Object> database = new LinkedHashMap<Object, Object>();
		final List<Object[]> table = new ArrayList<Object[]>();
		final File[] classes = root.listFiles();
		int classId = 0;
		final TicToc timer = new TicToc();
		
		System.out.println("Collecting data... " + new Date(timer.tic()));
		
		for (final File classFolder : classes) {
			final String className = classFolder.getName();
			final File[] samples = classFolder.listFiles();
			int sampleId = 0;
			
			for (final File region : samples) {
				System.out.printf("%3.1f %%     \r", 100.0 * ((double) classId / classes.length + (double) sampleId / samples.length / classes.length));
				
				final String[] regionMetadata = region.getName().split("\\.");
				final int lod = parseInt(regionMetadata[1].substring(3));
				
				if (lod <= 5) {
					final String imageId = regionMetadata[0];
					final int[] histogram = new int[1 << (3 * channelBitCount)];
					final BufferedImage image = ImageIO.read(region);
					final int width = image.getWidth();
					final int height = image.getHeight();
					final WritableRaster raster = image.getRaster();
					
					for (int y = 0; y < height; ++y) {
						for (int x = 0; x < width; ++x) {
							++histogram[shiftRightRGB(rgb(raster.getDataElements(x, y, null)) & 0x00FFFFFF, shift)];
						}
					}
					
					table.add(new Object[] { className, imageId, lod, histogram });
				}
				
				++sampleId;
			}
			
			++classId;
		}
		
		database.put("shift", shift);
		database.put("table", table);
		
		System.out.println("Collecting data done" + " time: " + timer.toc() + " memory: " + usedMemory());
		
		System.out.println("tableSize: " + table.size());
		
		System.out.println("Writing data... " + new Date(timer.tic()));
		
		final ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outPath));
		
		try {
			out.writeObject(database);
		} finally {
			out.close();
		}
		
		System.out.println("Writing data done" + " time: " + timer.toc() + " memory: " + usedMemory());
	}
	
	public static final int shiftRightRGB(final int rgb, final int shift) {
		final int r = (rgb >> (3 * shift));
		final int g = (rgb >> (2 * shift));
		final int b = (rgb >> (1 * shift));
		
		return (r & 0x00FF0000) | (g & 0x0000FF00) & (b & 0x000000FF);
	}
	
}
