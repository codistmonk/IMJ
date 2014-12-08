package jocltest;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static jocltest.JOCLConvolution2D.Denormalize.denormalizeTo;
import static net.sourceforge.aprog.tools.Tools.ignore;

import imj.cl.CLContext;
import imj.cl.CLKernel;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import javax.imageio.ImageIO;

import org.jocl.CL;
import org.jocl.Sizeof;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;
import net.sourceforge.aprog.tools.MathTools.Statistics;

/**
 * @author codistmonk (creation 2014-12-07)
 */
public final class JOCLConvolution2D {
	
	private JOCLConvolution2D() {
		throw new IllegalInstantiationException();
	}
	
    public static final String PROGRAM_SOURCE =
            "__kernel void "
            + "convolution2D(__global const float * input,"
            + "             __global const int * inputDimensions,"
            + "             __global const int * step,"
            + "             __global const float * convolutions,"
            + "             __global const int * convolutionsDimensions,"
            + "             __global float * output)"
            + "{\n"
            + "	const int gid = get_global_id(0);\n"
            + "	const int inputChannels = inputDimensions[0];\n"
            + "	const int inputWidth = inputDimensions[1];\n"
            + "	const int inputHeight = inputDimensions[2];\n"
            + "	const int inputPlaneSize = inputWidth * inputHeight;\n"
            + "	const int s = *step;\n"
            + "	const int convolutionCount = convolutionsDimensions[0];\n"
            + "	const int convolutionWidth = convolutionsDimensions[1];\n"
            + "	const int convolutionHeight = convolutionsDimensions[2];\n"
            + "	const int convolutionSize = convolutionWidth * convolutionHeight * inputChannels;\n"
            + "	const int x = s * (gid % (inputWidth / s));\n"
            + "	const int y = s * (gid / (inputWidth / s));\n"
            + "	const int virtualLeft = x + 1 - (convolutionWidth / 2);\n"
            + "	const int virtualRight = virtualLeft + convolutionWidth;\n"
            + "	const int virtualTop = y + 1 - (convolutionHeight / 2);\n"
            + "	const int virtualBottom = virtualTop + convolutionHeight;\n"
            + "	const int left = max(0, virtualLeft);\n"
            + "	const int right = min(virtualRight, inputWidth) - 1;\n"
            + "	const int top = max(0, virtualTop);\n"
            + "	const int bottom = min(virtualBottom, inputHeight) - 1;\n"
            + "	const int cleft = left - virtualLeft;\n"
            + "	const int cright = (virtualRight - 1) - right;\n"
            + "	const int ctop = top - virtualTop;\n"
            + "	const int cbottom = (virtualBottom - 1) - bottom;\n"
            + "	const int outputPlaneSize = (inputWidth / s) * (inputHeight / s);\n"
            + "	for (int o = 0; o < convolutionCount; ++o)\n"
            + "	{\n"
            + "		output[outputPlaneSize * o + gid] = 0;\n"
            + "	}\n"
            + "	for (int yy = top, cy = ctop; yy <= bottom; ++yy, ++cy)\n"
            + "	{\n"
            + "		for (int xx = left, cx = cleft; xx <= right; ++xx, ++cx)\n"
            + "		{\n"
            + "			const int inputOffset = (yy * inputWidth + xx);\n"
            + "			const int convolutionOffset = (cy * convolutionWidth + cx) * inputChannels;\n"
            + "			for (int c = 0; c < inputChannels; ++c)\n"
            + "			{\n"
            + "				const float inputValue = input[inputPlaneSize * c + inputOffset];\n"
            + "				for (int o = 0; o < convolutionCount; ++o)\n"
            + "				{\n"
            + "					output[outputPlaneSize * o + gid] += inputValue * convolutions[o * convolutionSize + convolutionOffset + c];\n"
            + "				}\n"
            + "			}\n"
            + "		}\n"
            + "	}\n"
            + "}";
	
	/**
	 * @param commandLineArguments
	 * <br>must not be null
	 * @throws IOException 
	 */
	public static final void main(final String[] commandLineArguments) throws IOException {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File file = new File(arguments.get("file", ""));
		final File outputFile = new File(arguments.get("output", ""));
		
		Tools.debugPrint(file);
		
		final BufferedImage image = ImageIO.read(file);
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final int pixelCount = imageWidth * imageHeight;
		final int channelCount = 3;
		final float[] input = new float[pixelCount * channelCount];
		final int step = 2;
		final float[] convolutions = {
				1F, 0F, 0F, 1F, 0F, 0F,
				1F, 0F, 0F, 1F, 0F, 0F,
				
				0F, 1F, 0F, 0F, 1F, 0F,
				0F, 1F, 0F, 0F, 1F, 0F,
				
				0F, 0F, 1F, 0F, 0F, 1F,
				0F, 0F, 1F, 0F, 0F, 1F
		};
		final int convolutionSize = 2;
		final int convolutionCount = convolutions.length / (convolutionSize * convolutionSize * channelCount);
		Tools.debugPrint(pixelCount, convolutionSize, convolutionCount);
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			final int rgb = image.getRGB(pixel % imageWidth, pixel / imageWidth);
			input[pixelCount * 0 + pixel] = (rgb >> 16) & 0xFF;
			input[pixelCount * 1 + pixel] = (rgb >> 8) & 0xFF;
			input[pixelCount * 2 + pixel] = (rgb >> 0) & 0xFF;
		}
		
		CL.setExceptionsEnabled(true);
		
		final CLContext context = new CLContext(CL.CL_DEVICE_TYPE_GPU, 0);
		final CLKernel kernel = context.createAndBuildProgram(PROGRAM_SOURCE)
				.createKernel("convolution2D");
		final int outputWidth = imageWidth / step;
		final int outputHeight = imageHeight / step;
		final int outputPixelCount = outputWidth * outputHeight;
		final float[] output = new float[outputPixelCount * convolutionCount];
		
		kernel.setArg(0, context.createInputBuffer(input));
		kernel.setArg(1, context.createInputBuffer(channelCount, imageWidth, imageHeight));
		kernel.setArg(2, context.createInputBuffer(step));
		kernel.setArg(3, context.createInputBuffer(convolutions));
		kernel.setArg(4, context.createInputBuffer(convolutionCount, convolutionSize, convolutionSize));
		kernel.setArg(5, context.createOutputBuffer(Sizeof.cl_float, output.length));
		
		Tools.debugPrint();
		
		kernel.enqueueNDRange(output.length / convolutionCount);
		kernel.enqueueReadArg(5, output);
		
		context.release();
		Tools.debugPrint();
		
		final BufferedImage outputImage = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB);
		final ToInt<?> toRed = denormalizeTo(0, 255).using(output, outputPixelCount * 0, outputPixelCount * 1);
		final ToInt<?> toGreen = denormalizeTo(0, 255).using(output, outputPixelCount * 1, outputPixelCount * 2);
		final ToInt<?> toBlue = denormalizeTo(0, 255).using(output, outputPixelCount * 2, outputPixelCount * 3);
		
		for (int pixel = 0; pixel < outputPixelCount; ++pixel) {
			outputImage.setRGB(pixel % outputWidth, pixel / outputWidth,
					a8r8g8b8(0xFF,
							toRed.convert(output[pixel + outputPixelCount * 0]),
							toGreen.convert(output[pixel + outputPixelCount * 1]),
							toBlue.convert(output[pixel + outputPixelCount * 2])));
		}
		
		Tools.debugPrint("Writing", outputFile);
		ImageIO.write(outputImage, "png", outputFile);
	}
	
	public static final int a8r8g8b8(final int a8, final int r8, final int g8, final int b8) {
		return (a8 << 24) | (r8 << 16) | (g8 << 8) | (b8 << 0);
	}
	
	/**
	 * @author codistmonk (creation 2014-12-08)
	 */
	public static abstract class ToInt<S extends ToInt<?>> implements Serializable {
		
		private final int minimum;
		
		private final int maximum;
		
		protected ToInt(final int minimum, final int maximum) {
			this.minimum = minimum;
			this.maximum = maximum;
		}
		
		public final int getMinimum() {
			return this.minimum;
		}
		
		public final int getMaximum() {
			return this.maximum;
		}
		
		public abstract S using(float[] values, int begin, int end);
		
		public final S using(final float... values) {
			return this.using(values, 0, values.length);
		}
		
		public abstract int convert(float value);
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 1139958071040795451L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-12-08)
	 */	
	public static final class Clamp extends ToInt<Clamp> {
		
		public Clamp(final int minimum, final int maximum) {
			super(minimum, maximum);
		}
		
		@Override
		public final Clamp using(final float[] values, final int begin, final int end) {
			ignore(values);
			ignore(begin);
			ignore(end);
			
			return this;
		}
		
		@Override
		public final int convert(final float value) {
			return max(this.getMinimum(), min((int) value, this.getMaximum()));
		}
		
		public static final int clamp(final float value, final int minimum, final int maximum) {
			return max(minimum, min((int) value, maximum));
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 4832188313241788893L;
		
		public static final Clamp clampTo(final int minimum, final int maximum) {
			return new Clamp(minimum, maximum);
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2014-12-08)
	 */	
	public static final class ClampFrom01 extends ToInt<ClampFrom01> {
		
		private final float amplitude;
		
		public ClampFrom01(final int minimum, final int maximum) {
			super(minimum, maximum);
			this.amplitude = maximum - minimum;
		}
		
		@Override
		public final ClampFrom01 using(final float[] values, final int begin, final int end) {
			ignore(values);
			ignore(begin);
			ignore(end);
			
			return this;
		}
		
		@Override
		public final int convert(final float value) {
			return Clamp.clamp(this.getMinimum() + value * this.amplitude, this.getMinimum(), this.getMaximum());
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -4747349293163739327L;
		
		public static final ClampFrom01 clampFrom01To(final int minimum, final int maximum) {
			return new ClampFrom01(minimum, maximum);
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2014-12-08)
	 */	
	public static final class Denormalize extends ToInt<Denormalize> {
		
		private final Statistics statistics;
		
		private final int amplitude;
		
		public Denormalize(final int minimum, final int maximum) {
			super(minimum, maximum);
			this.statistics = new Statistics();
			this.amplitude = maximum - minimum;
		}
		
		@Override
		public final Denormalize using(final float[] values, final int begin, final int end) {
			for (int i = begin; i < end; ++i) {
				this.statistics.addValue(values[i]);
			}
			
			return this;
		}
		
		@Override
		public final int convert(final float value) {
			return (int) (this.getMinimum() + this.statistics.getNormalizedValue(value) * this.amplitude);
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -5809444469110819941L;
		
		public static final Denormalize denormalizeTo(final int minimum, final int maximum) {
			return new Denormalize(minimum, maximum);
		}
		
	}
	
}
