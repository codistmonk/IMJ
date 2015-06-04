package imj.apps;

import static imj.IMJTools.BLUE_MASK;
import static imj.IMJTools.GREEN_MASK;
import static imj.IMJTools.RED_MASK;
import static imj.ImageOfBufferedImage.rgb;
import static java.lang.Integer.parseInt;
import static multij.tools.Tools.usedMemory;

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

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;
import multij.tools.TicToc;

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
					final int colorCount = 1 << (3 * channelBitCount);
					final float[] histogram = new float[colorCount];
					final BufferedImage image = ImageIO.read(region);
					final int width = image.getWidth();
					final int height = image.getHeight();
					final WritableRaster raster = image.getRaster();
					int pixelCount = 0;
					
					for (int y = 0; y < height; ++y) {
						for (int x = 0; x < width; ++x) {
							final int rgb = rgb(raster.getDataElements(x, y, null));
							
							if (rgb != 0) {
								++histogram[shiftRightRGB(rgb & 0x00FFFFFF, shift)];
								++pixelCount;
							}
						}
					}
					
					if (pixelCount != 0) {
						for (int i = 0; i < colorCount; ++i) {
							histogram[i] /= pixelCount;
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
		final int r = (((rgb & RED_MASK) >> shift) & RED_MASK) >> (2 * shift);
		final int g = (((rgb & GREEN_MASK) >> shift) & GREEN_MASK) >> (1 * shift);
		final int b = (((rgb & BLUE_MASK) >> shift) & BLUE_MASK) >> (0 * shift);
		
		return r | g | b;
	}
	
}
