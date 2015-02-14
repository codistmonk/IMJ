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
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;
import net.sourceforge.aprog.tools.MathTools.Statistics;

import org.jocl.CL;
import org.jocl.Sizeof;

/**
 * @author codistmonk (creation 2014-12-07)
 */
public final class JOCLConvolution2D {
	
	private JOCLConvolution2D() {
		throw new IllegalInstantiationException();
	}
    
    public static final TemporaryObject<CLContext> temporaryContext = new TemporaryObject<CLContext>(2_000L) {
    	
		@Override
		protected final CLContext newObject() {
			return new CLContext(CL.CL_DEVICE_TYPE_GPU, 0);
		}
		
		@Override
		protected final void deleteObject(final CLContext context) {
			context.release();
		}
		
		private static final long serialVersionUID = -956670618965318967L;
		
	};
	
	public static final Convolutions2D setInput(final Convolutions2D convolutions, final BufferedImage image) {
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final int pixelCount = imageWidth * imageHeight;
		
		Tools.debugPrint(imageWidth, imageHeight, pixelCount);
		
		final int channelCount = 3;
		final float[] input = new float[pixelCount * channelCount];
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			final int rgb = image.getRGB(pixel % imageWidth, pixel / imageWidth);
			input[pixelCount * 0 + pixel] = (rgb >> 16) & 0xFF;
			input[pixelCount * 1 + pixel] = (rgb >> 8) & 0xFF;
			input[pixelCount * 2 + pixel] = (rgb >> 0) & 0xFF;
		}
		
		convolutions.setInput(input).setInputDimensions(channelCount, imageWidth, imageHeight);
		
		return convolutions;
	}
    
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
		
		CL.setExceptionsEnabled(true);
		
		final Convolutions2D c = new Convolutions2D(temporaryContext.get());
		
		setInput(c, ImageIO.read(file)).setStepping(1, 2);
		
		final float[] convolutions = {
				1F, 0F, 0F, 1F, 0F, 0F,
				1F, 0F, 0F, 1F, 0F, 0F,
				
				0F, 1F, 0F, 0F, 1F, 0F,
				0F, 1F, 0F, 0F, 1F, 0F,
				
				0F, 0F, 1F, 0F, 0F, 1F,
				0F, 0F, 1F, 0F, 0F, 1F
		};
		final int convolutionSize = 2;
		final int convolutionCount = convolutions.length / (convolutionSize * convolutionSize * c.getInputChannelCount());
		
		c.setConvolutions(convolutions).setConvolutionsDimensions(convolutionCount, convolutionSize, convolutionSize);
		c.setOutput();
		
		Tools.debugPrint();
		
		final float[] output = c.compute();
		final int outputWidth = c.getOutputWidth();
		final int outputHeight = c.getOutputHeight();
		final int outputPixelCount = outputWidth * outputHeight;
		
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
	
	/**
	 * @author codistmonk (creation 2015-02-14)
	 * @param <T>
	 */
	public static abstract class TemporaryObject<T> implements Serializable {
		
		private final long milliseconds;
		
		private long lastAccess;
		
		private Timer terminator;
		
		private T object;
		
		protected TemporaryObject(final long milliseconds) {
			this.milliseconds = milliseconds;
		}
		
		public final long getMilliseconds() {
			return this.milliseconds;
		}
		
		public final synchronized T get() {
			this.lastAccess = System.currentTimeMillis();
			
			if (this.object == null) {
				this.object = this.newObject();
				this.terminator = new Timer();
				this.terminator.schedule(new TimerTask() {
					
					@Override
					public final void run() {
						TemporaryObject.this.update();
					}
					
				}, this.getMilliseconds(), this.getMilliseconds());
			}
			
			return this.object;
		}
		
		final synchronized void update() {
			if (this.getMilliseconds() <= System.currentTimeMillis() - this.lastAccess) {
				this.deleteObject(this.object);
				this.object = null;
				
				this.terminator.cancel();
				this.terminator = null;
			}
		}
		
		protected abstract T newObject();
		
		protected void deleteObject(final T object) {
			ignore(object);
		}
		
		private static final long serialVersionUID = -2821058824513633614L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-14)
	 */
	public static final class Convolutions2D implements Serializable {
		
		private final CLContext context;
		
		private final CLKernel kernel;
		
		private int inputChannelCount, inputWidth, inputHeight, stride, outputChannelCount;
		
		private long outputLength;
		
		public Convolutions2D(final CLContext context) {
			this.context = context;
			this.kernel = context.createAndBuildProgram(PROGRAM_SOURCE)
					.createKernel("convolution2D");
		}
		
		public final CLContext getContext() {
			return this.context;
		}
		
		public final CLKernel getKernel() {
			return this.kernel;
		}
		
		public final int getInputChannelCount() {
			return this.inputChannelCount;
		}
		
		public final int getInputWidth() {
			return this.inputWidth;
		}
		
		public final int getInputHeight() {
			return this.inputHeight;
		}
		
		public final int getStride() {
			return this.stride;
		}
		
		public final int getOutputChannelCount() {
			return this.outputChannelCount;
		}
		
		public final long getOutputLength() {
			return this.outputLength;
		}
		
		public final Convolutions2D setInput(final float... input) {
			this.getKernel().setArg(0, this.getContext().createInputBuffer(input));
			
			return this;
		}
		
		public final Convolutions2D setInputDimensions(final int inputChannelCount, final int inputWidth, final int inputHeight) {
			this.getKernel().setArg(1, this.getContext().createInputBuffer(inputChannelCount, inputWidth, inputHeight));
			
			this.inputChannelCount = inputChannelCount;
			this.inputWidth = inputWidth;
			this.inputHeight = inputHeight;
			
			return this;
		}
		
		public final Convolutions2D setStepping(final int sparsity, final int stride) {
			this.getKernel().setArg(2, this.getContext().createInputBuffer(sparsity, stride));
			
			this.stride = stride;
			
			return this;
		}
		
		public final Convolutions2D setConvolutions(final float... convolutions) {
			this.getKernel().setArg(3, this.getContext().createInputBuffer(convolutions));
			
			return this;
		}
		
		public final Convolutions2D setConvolutionsDimensions(final int convolutionCount, final int convolutionWidth, final int convolutionHeight) {
			this.getKernel().setArg(4, this.getContext().createInputBuffer(convolutionCount, convolutionWidth, convolutionHeight));
			
			this.outputChannelCount = convolutionCount;
			
			return this;
		}
		
		public final Convolutions2D setOutput() {
			this.outputLength = (long) this.getOutputWidth() * this.getOutputHeight() * this.getOutputChannelCount();
			
			this.getKernel().setArg(5, this.getContext().createOutputBuffer(Sizeof.cl_float, this.getOutputLength()));
			
			return this;
		}
		
		public final int getOutputWidth() {
			return this.getInputWidth() / this.getStride();
		}
		
		public final int getOutputHeight() {
			return this.getInputHeight() / this.getStride();
		}
		
		public final float[] compute() {
			final float[] result = new float[(int) this.getOutputLength()];
			
			this.compute(result);
			
			return result;
		}
		
		public final Convolutions2D compute(final float[] output) {
			getKernel().enqueueNDRange(this.getOutputLength() / this.getOutputChannelCount());
			
			if (output != null) {
				this.getKernel().enqueueReadArg(5, output);
			}
			
			return this;
		}
		
		private static final long serialVersionUID = -8143736361978173371L;
		
	    public static final String PROGRAM_SOURCE =
	            "__kernel void "
	            + "convolution2D(__global const float * input,"
	            + "             __global const int * inputDimensions,"
	            + "             __global const int * stepping,"
	            + "             __global const float * convolutions,"
	            + "             __global const int * convolutionsDimensions,"
	            + "             __global float * output)"
	            + "{\n"
	            + "	const int gid = get_global_id(0);\n"
	            + "	const int inputChannels = inputDimensions[0];\n"
	            + "	const int inputWidth = inputDimensions[1];\n"
	            + "	const int inputHeight = inputDimensions[2];\n"
	            + "	const int inputPlaneSize = inputWidth * inputHeight;\n"
	            + "	const int sparsity = stepping[0];\n"
	            + "	const int stride = stepping[1];\n"
	            + "	const int convolutionCount = convolutionsDimensions[0];\n"
	            + "	const int convolutionWidth = convolutionsDimensions[1];\n"
	            + "	const int convolutionHeight = convolutionsDimensions[2];\n"
	            + "	const int convolutionSize = convolutionWidth * convolutionHeight * inputChannels;\n"
	            + "	const int x = stride * (gid % (inputWidth / stride));\n"
	            + "	const int y = stride * (gid / (inputWidth / stride));\n"
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
	            + "	const int outputPlaneSize = (inputWidth / stride) * (inputHeight / stride);\n"
	            + "	for (int o = 0; o < convolutionCount; ++o)\n"
	            + "	{\n"
	            + "		output[outputPlaneSize * o + gid] = 0;\n"
	            + "	}\n"
	            + "	for (int yy = top, cy = ctop; yy <= bottom; yy += sparsity, ++cy)\n"
	            + "	{\n"
	            + "		for (int xx = left, cx = cleft; xx <= right; xx += sparsity, ++cx)\n"
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
		
	}
	
}
