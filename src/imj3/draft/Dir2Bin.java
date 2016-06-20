package imj3.draft;

import static multij.tools.Tools.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import imj3.tools.BinView;
import multij.tools.Canvas;
import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-07-15)
 */
public final class Dir2Bin {
	
	private Dir2Bin() {
		throw new IllegalInstantiationException();
	}
	
	public static final String[] EMPTY = {};
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File root = new File(arguments.get("in", ""));
		final List<String> classIds = new ArrayList<>(Arrays.asList(split(arguments.get("classIds", ""), ",")));
		final int forcedWidth = arguments.get("width", 32)[0];
		final int forcedHeight = arguments.get("height", forcedWidth)[0];
		final int channelCount = 3;
		final List<byte[]> items = new ArrayList<>();
		final Canvas itemCanvas = new Canvas().setFormat(forcedWidth, forcedHeight);
		final File outputFile = new File(arguments.get("output", root.getName() + ".bin"));
		final boolean shuffle = arguments.get("shuffle", 1)[0] != 0;
		final boolean show = arguments.get("show", 0)[0] != 0;
		
		debugPrint("root:", root);
		
		for (final File classDir : root.listFiles()) {
			final int label = classIds.indexOf(classDir.getName());
			
			if (0 <= label) {
				for (final File imageFile : classDir.listFiles()) {
					try {
						itemCanvas.getGraphics().drawImage(
								ImageIO.read(imageFile), 0, 0, forcedWidth, forcedHeight, null);
						final BufferedImage itemImage = itemCanvas.getImage();
						final byte[] item = new byte[1 + forcedWidth * forcedHeight * channelCount];
						item[0] = (byte) label;
						
						for (int y = 0; y < forcedHeight; ++y) {
							for (int x = 0; x < forcedWidth; ++x) {
								final int rgb = itemImage.getRGB(x, y);
								
								for (int channelIndex = 0; channelIndex < channelCount; ++channelIndex) {
									item[1 + channelIndex * (forcedWidth * forcedHeight) + y * forcedWidth + x] =
											(byte) (rgb >> (Byte.SIZE * (channelCount - 1 - channelIndex)));
								}
							}
						}
						
						items.add(item);
					} catch (final IOException exception) {
						debugError(imageFile, exception);
					}
				}
			}
		}
		
		debugPrint("itemCount:", items.size());
		
		if (shuffle) {
			Collections.shuffle(items);
		}
		
		{
			debugPrint("Writing", outputFile);
			
			writeBin(items, outputFile);
		}
		
		if (show) {
			BinView.main("bin", outputFile.getPath());
		}
	}
	
	public static final void writeBin(final List<byte[]> data, final File outputFile) {
		final int n = data.size();
		
		try (final OutputStream output = new FileOutputStream(outputFile)) {
			for (int i = 0; i < n; ++i) {
				output.write(data.get(i));
			}
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
	
	public static final String[] split(final String string, final String regex) {
		final String[] protoresult = string.split(regex);
		
		if (protoresult.length == 1 && protoresult[0].isEmpty()) {
			return EMPTY;
		}
		
		return protoresult;
	}
	
}
