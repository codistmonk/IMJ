package imj3.tools;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.util.Arrays.fill;
import static net.sourceforge.aprog.tools.Tools.*;

import imj3.core.Channels;
import imj3.core.Image2D;
import imj3.processing.Pipeline;
import imj3.processing.Pipeline.ClassDescription;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Document;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;
import net.sourceforge.aprog.xml.XMLTools;

/**
 * @author codistmonk (creation 2015-04-04)
 */
public final class GroundTruth2Bin {
	
	private GroundTruth2Bin() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws IOException 
	 */
	public static final void main(final String[] commandLineArguments) throws IOException {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String pipelinePath = arguments.get("pipeline", "");
		// TODO later, retrieve training set from pipeline instead
		final String groundTruthPath = arguments.get("zip", "");
		final int lod = arguments.get("lod", 4)[0];
		final Pipeline pipeline = XMLSerializable.objectFromXML(new File(pipelinePath));
		final Map<Integer, Area> regions;
		
		try (final MultifileSource source = new MultifileSource(groundTruthPath);
				final InputStream input = source.getInputStream("annotations.xml")) {
			final Document xml = XMLTools.parse(input);
			
			regions = XMLSerializable.objectFromXML(xml.getDocumentElement(), new HashMap<>());
		}
		
		final String imagePath = groundTruthPath.substring(0, groundTruthPath.indexOf("_groundtruth_")) + ".zip";
		final Image2D image = IMJTools.read(imagePath, lod);
		final Channels channels = image.getChannels();
		final int channelCount = channels.getChannelCount();
		final int patchSize = 32;
		final int planeSize = patchSize * patchSize;
		final byte[] buffer = new byte[1 + planeSize * channelCount];
		final String outputPath = baseName(groundTruthPath) + ".bin";
		final Map<String, Long> counts = new LinkedHashMap<>();
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final double scale = pow(2.0, -lod);
		
		debugPrint(imageWidth, imageHeight, channels);
		
		try (final OutputStream output = new FileOutputStream(outputPath)) {
			for (final ClassDescription classDescription : pipeline.getClassDescriptions()) {
				final Area region = regions.get(classDescription.getLabel());
				
				region.transform(AffineTransform.getScaleInstance(scale, scale));
				
				final Rectangle regionBounds = region.getBounds();
				final int regionWidth = regionBounds.width;
				final int regionHeight = regionBounds.height;
				final BufferedImage mask = new BufferedImage(regionWidth, regionHeight, BufferedImage.TYPE_BYTE_BINARY);
				
				debugPrint(classDescription, regionBounds);
				
				{
					final Graphics2D graphics = mask.createGraphics();
					
					graphics.setColor(Color.WHITE);
					graphics.translate(-regionBounds.x, -regionBounds.y);
					graphics.fill(region);
					graphics.dispose();
				}
				
				buffer[0] = (byte) pipeline.getClassDescriptions().indexOf(classDescription);
				long count = 0L;
				
				for (int y = 0; y < regionHeight; ++y) {
					final int top = regionBounds.y + y - patchSize / 2;
					final int bottom = min(top + patchSize, imageHeight);
					
					for (int x = 0; x < regionWidth; ++x) {
						if ((mask.getRGB(x, y) & 0x00FFFFFF) != 0) {
							final int left = regionBounds.x + x - patchSize / 2;
							final int right = min(left + patchSize, imageWidth);
							
							fill(buffer, 1, buffer.length, (byte) 0);
							
							for (int yy = max(0, top); yy < bottom; ++yy) {
								for (int xx = max(0, left); xx < right; ++xx) {
									final long pixelValue = image.getPixelValue(xx, yy);
									
									for (int channelIndex = 0; channelIndex < channelCount; ++channelIndex) {
										buffer[1 + planeSize * channelIndex + (yy - top) * patchSize + (xx - left)] = (byte) channels.getChannelValue(pixelValue, channelIndex);
									}
								}
							}
							
							output.write(buffer);
							
							++count;
						}
					}
				}
				
				counts.put(classDescription.toString(), count);
			}
		}
		
		Tools.debugPrint(counts);
	}
	
}
