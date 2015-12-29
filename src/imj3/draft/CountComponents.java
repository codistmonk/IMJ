package imj3.draft;

import static imj3.tools.IMJTools.read;
import static multij.tools.Tools.debugPrint;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import imj3.core.Image2D;
import imj3.core.Image2D.Pixel2DProcessor;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-12-29)
 */
public final class CountComponents {
	
	private CountComponents() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String... commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imagePath = arguments.get("file", "");
		final boolean buildHistogram = arguments.get("histogram", 0)[0] != 0;
		final long[] count = { 0L };
		final Map<Long, AtomicLong> histogram = new TreeMap<>();
		final Image2D image = read(imagePath);
		
		ExtractComponents.forEachPixelInEachComponent4(image, new Pixel2DProcessor() {
			
			private Long pixelValue;
			
			@Override
			public final boolean pixel(final int x, final int y) {
				if (buildHistogram && this.pixelValue == null) {
					this.pixelValue = image.getPixelValue(x, y);
				}
				
				return true;
			}
			
			@Override
			public final void endOfPatch() {
				++count[0];
				
				if (buildHistogram) {
					histogram.computeIfAbsent(this.pixelValue, v -> new AtomicLong()).incrementAndGet();
				}
				
				this.pixelValue = null;
			}
			
			private static final long serialVersionUID = 2045154224638453864L;
			
		});
		
		debugPrint("count:", count[0]);
		
		if (buildHistogram) {
			debugPrint("histogram:", histogram);
		}
	}
	
}
