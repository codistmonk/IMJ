package imj3.draft;

import static imj3.tools.IMJTools.read;

import imj3.core.Channels;
import imj3.core.Image2D;
import imj3.draft.ExtractComponents.ComponentPixelsProcessor;
import imj3.tools.Image2DComponent;
import imj3.tools.PackedImage2D;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import jgencode.primitivelists.LongList;
import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-05-10)
 */
public final class Process2D {
	
	private Process2D() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws IOException 
	 */
	public static final void main(final String[] commandLineArguments) throws IOException {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String inputPath = arguments.get("file", "");
		final String outputPath = arguments.get("output", "");
		final Image2D input = read(inputPath);
		final Image2DProcessor[] pipeline = Arrays.stream(arguments.get("pipeline", "").split("\\|")).map(Process2D::decode).toArray(Image2DProcessor[]::new);
		Image2D result = input;
		
		for (final Image2DProcessor processor : pipeline) {
			result = processor.process(result, null);
		}
		
		if (outputPath.isEmpty()) {
			SwingTools.show(new Image2DComponent(input), inputPath, false);
			SwingTools.show(new Image2DComponent(result), inputPath + "(" + arguments.get("pipeline", "") + ")", false);
		} else {
			ImageIO.write((RenderedImage) result.toAwt(), "png", new File(outputPath));
		}
	}
	
	public static final Image2DProcessor decode(final String specification) {
		Image2DProcessor candidate = FilterComponentsBySize.decode(specification);
		
		if (candidate != null) {
			return candidate;
		}
		
		candidate = Label.decode(specification);
		
		if (candidate != null) {
			return candidate;
		}
		
		candidate = Copy.decode(specification);
		
		if (candidate != null) {
			return candidate;
		}
		
		throw new IllegalArgumentException();
	}
	
	/**
	 * @author codistmonk (creation 2015-05-10)
	 */
	public static abstract interface Image2DProcessor extends Serializable {
		
		public abstract Image2D process(Image2D input, Image2D result);
		
	}
	
	/**
	 * @author codistmonk (creation 2015-05-10)
	 */
	public static final class Copy implements Image2DProcessor {
		
		@Override
		public final Image2D process(final Image2D input, final Image2D result) {
			final Image2D actualResult = result != null ? result : new PackedImage2D("", input.getWidth(), input.getHeight(), input.getChannels());
			
			if (input != actualResult) {
				input.forEachPixel(p -> {
					actualResult.setPixelValue(p, input.getPixelValue(p));
					
					return true;
				});
			}
			
			return actualResult;
		}
		
		private static final long serialVersionUID = -7005951447048464000L;
		
		public static final Copy decode(final String specification) {
			if (specification.trim().isEmpty()) {
				return new Copy();
			}
			
			return null;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-05-10)
	 */
	public static final class FilterComponentsBySize implements Image2DProcessor {
		
		private final int minimum, maximum;
		
		public FilterComponentsBySize(final int minimum) {
			this(minimum, Integer.MAX_VALUE);
		}
		
		public FilterComponentsBySize(final int minimum, final int maximum) {
			this.minimum = minimum;
			this.maximum = maximum;
		}
		
		public final int getMinimum() {
			return this.minimum;
		}
		
		public final int getMaximum() {
			return this.maximum;
		}
		
		@Override
		public final Image2D process(final Image2D input, final Image2D result) {
			final Image2D actualResult = result != null ? result : new PackedImage2D("", input.getWidth(), input.getHeight(), input.getChannels());
			
			ExtractComponents.forEachPixelInEachComponent4(input, new ComponentPixelsProcessor() {
				
				@Override
				protected final void protectedEndOfPatch() {
					final LongList pixels = this.getPixels();
					final int n = pixels.size();
					
					if (n < getMinimum() || getMaximum() < n) {
						pixels.forEach(p -> {
							actualResult.setPixelValue(getX(p), getY(p), 0L);
							
							return true;
						});
					} else {
						pixels.forEach(p -> {
							final int x = getX(p);
							final int y = getY(p);
							
							actualResult.setPixelValue(x, y, input.getPixelValue(x, y));
							
							return true;
						});
					}
				}
				
				private static final long serialVersionUID = -8453450963080143988L;
				
			});
			
			return actualResult;
		}
		
		private static final long serialVersionUID = -3937970757786402430L;
		
		public static final Pattern PATTERN = Pattern.compile("\\s*components\\s+of\\s+size\\s+above\\s+([0-9]+)(\\s+and\\s+below\\s+([0-9]+))?\\s*");
		
		public static final FilterComponentsBySize decode(final String specification) {
			final Matcher matcher = PATTERN.matcher(specification);
			
			if (matcher.matches()) {
				final int minimum = Integer.decode(matcher.group(1));
				final String maximumSpecification = matcher.group(3);
				final int maximum = maximumSpecification == null ? Integer.MAX_VALUE : Integer.decode(maximumSpecification);
				
				return new FilterComponentsBySize(minimum, maximum);
			}
			
			return null;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-05-10)
	 */
	public static final class Label implements Image2DProcessor {
		
		@Override
		public final Image2D process(final Image2D input, final Image2D result) {
			final Image2D actualResult = result != null ? result : new PackedImage2D("", input.getWidth(), input.getHeight(), Channels.Predefined.C1_S32);
			
			ExtractComponents.forEachPixelInEachComponent4(input, new ComponentPixelsProcessor() {
				
				private int id = 0;
				
				@Override
				protected final boolean accept(final int x, final int y) {
					return (input.getPixelValue(x, y) & 0x00FFFFFFL) != 0L;
				}
				
				@Override
				protected final void protectedEndOfPatch() {
					++this.id;
					
					this.getPixels().forEach(p -> {
						final int x = getX(p);
						final int y = getY(p);
						
						actualResult.setPixelValue(x, y, this.id);
						
						return true;
					});
				}
				
				private static final long serialVersionUID = 8848718141692783114L;
				
			});
			
			return actualResult;
		}
		
		private static final long serialVersionUID = -1496430541654618127L;
		
		public static final Label decode(final String specification) {
			if (specification.trim().equals("labels")) {
				return new Label();
			}
			
			return null;
		}
		
	}
	
}
