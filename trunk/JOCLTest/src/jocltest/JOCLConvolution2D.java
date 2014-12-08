package jocltest;

import static java.lang.Math.sqrt;
import imj.cl.CLContext;
import imj.cl.CLKernel;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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
            + "             __constant const int * inputDimensions,"
            + "             __constant const int * step,"
            + "             __constant const float * convolutionKernel,"
            + "             __constant const int * convolutionSize,"
            + "             __global float * output)"
            + "{\n"
            + "	const int gid = get_global_id(0);\n"
            + "	const int inputWidth = inputDimensions[0];\n"
            + "	const int inputHeight = inputDimensions[1];\n"
            + "	const int inputChannels = inputDimensions[2];\n"
            + "	const int s = *step;\n"
            + "	const int cs = *convolutionSize;\n"
            + "	const int x = s * (gid % (inputWidth / s));\n"
            + "	const int y = s * (gid / (inputWidth / s));\n"
            + "	const int half = cs / 2;\n"
            + "	const int left = max(0, x - half);\n"
            + "	const int right = min(x - half + cs, inputWidth) - 1;\n"
            + "	const int top = max(0, y - half);\n"
            + "	const int bottom = min(y - half + cs, inputHeight) - 1;\n"
            + "	const int cleft = left - (x - half);\n"
            + "	const int cright = (x - half + cs - 1) - right;\n"
            + "	const int ctop = top - (y - half);\n"
            + "	const int cbottom = (y - half + cs - 1) - bottom;\n"
            + "	output[gid] = 0;\n"
            + "	for (int yy = top, cy = ctop; yy <= bottom; ++yy, ++cy)\n"
            + "	{\n"
            + "		for (int xx = left, cx = cleft; xx <= right; ++xx, ++cx)\n"
            + "		{\n"
            + "			const int inputOffset = (yy * inputWidth + xx) * inputChannels;\n"
            + "			const int kernelOffset = (cy * cs + cx) * inputChannels;\n"
            + "			for (int c = 0; c < inputChannels; ++c)\n"
            + "			{\n"
            + "				output[gid] += input[inputOffset + c] * convolutionKernel[kernelOffset + c];\n"
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
		final float[] input = new float[pixelCount * 3];
		final int step = 2;
		final float[] convolutionKernel = {
				0F, 1F, 0F, 0F, 1F, 0F,
				0F, 1F, 0F, 0F, 1F, 0F
		};
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			final int rgb = image.getRGB(pixel % imageWidth, pixel / imageWidth);
			input[pixel * 3 + 0] = (rgb >> 16) & 0xFF;
			input[pixel * 3 + 1] = (rgb >> 8) & 0xFF;
			input[pixel * 3 + 2] = (rgb >> 0) & 0xFF;
		}
		
		CL.setExceptionsEnabled(true);
		
		final CLContext context = new CLContext(CL.CL_DEVICE_TYPE_GPU, 0);
		final CLKernel kernel = context.createAndBuildProgram(PROGRAM_SOURCE)
				.createKernel("convolution2D");
		final int outputWidth = imageWidth / step;
		final int outputHeight = imageHeight / step;
		final int outputPixelCount = outputWidth * outputHeight;
		final float[] output = new float[outputPixelCount];
		
		kernel.setArg(0, context.createInputBuffer(input));
		kernel.setArg(1, context.createInputBuffer(imageWidth, imageHeight, 3));
		kernel.setArg(2, context.createInputBuffer(step));
		kernel.setArg(3, context.createInputBuffer(convolutionKernel));
		kernel.setArg(4, context.createInputBuffer((int) sqrt(convolutionKernel.length / 3)));
		kernel.setArg(5, context.createOutputBuffer(Sizeof.cl_float, outputPixelCount));
		
		Tools.debugPrint();
		
		kernel.enqueueNDRange(output.length);
		kernel.enqueueReadArg(5, output);
		
		context.release();
		Tools.debugPrint();
		
		final BufferedImage outputImage = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB);
		final Statistics statistics = new Statistics();
		
		for (int pixel = 0; pixel < outputPixelCount; ++pixel) {
			statistics.addValue(output[pixel]);
		}
		
		for (int pixel = 0; pixel < outputPixelCount; ++pixel) {
			outputImage.setRGB(pixel % outputWidth, pixel / outputWidth, 0xFF000000 |
					(0x00010101 * (int) (255.0 * statistics.getNormalizedValue(output[pixel]))));
		}
		
		Tools.debugPrint("Writing", outputFile);
		ImageIO.write(outputImage, "png", outputFile);
	}

}
