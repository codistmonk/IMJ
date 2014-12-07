/*
 * JOCL - Java bindings for OpenCL
 * 
 * Copyright 2009 Marco Hutter - http://www.jocl.org/
 */

package jocltest;

import java.util.Arrays;
import java.util.Random;

import imj.cl.CLContext;
import imj.cl.CLKernel;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

import org.jocl.*;

/**
 * A small JOCL sample.
 */
public final class JOCLConvolution1D {
	
	private JOCLConvolution1D() {
		throw new IllegalInstantiationException();
	}
	
    /**
     * The source code of the OpenCL program to execute
     */
    private static final String PROGRAM_SOURCE =
        "__kernel void "
        + "sampleKernel(__global const float * input,"
        + "             __constant const int * step,"
        + "             __constant const float * convolutionKernel,"
        + "             __constant const int * convolutionSize,"
        + "             __global float * output)"
        + "{\n"
        + "	const int gid = get_global_id(0);\n"
        + "	const int lid = get_local_id(0);\n"
        + "	__local float k[4096];\n"
        + "	if (lid == 0)\n"
        + "	{\n"
        + "		for (int i = 0; i < *convolutionSize; ++i)\n"
        + "		{\n"
        + "			k[i] = convolutionKernel[i];\n"
        + "		}\n"
        + "	}\n"
        + "	barrier(CLK_LOCAL_MEM_FENCE);\n"
        + "	output[gid] = 0;\n"
        + "	for (int i = 0; i < *convolutionSize; ++i)\n"
        + "	{\n"
        + "		output[gid] += input[gid * *step + i] * k[i];\n"
        + "	}\n"
        + "}";
    
    public static final void convolve1D(final float[] input, final int step,
    		final float[] convolutionKernel, final float[] output) {
    	final int convolutionSize = convolutionKernel.length;
		final int n = (input.length - convolutionSize + 1) / step;
    	
    	for (int i = 0; i < n; ++i) {
    		output[i] = 0F;
    		
    		for (int j = 0; j < convolutionSize; ++j) {
    			output[i] += input[i * step + j] * convolutionKernel[j];
    		}
    	}
    }
    
    public static final float[] newAverageKernel(final int n) {
    	final float[] result = new float[n];
    	
    	Arrays.fill(result, 1F / n);
    	
    	return result;
    }
    
	/**
     * The entry point of this sample
     * 
     * @param args Not used
     */
    public static final void main(final String[] args) {
    	final TicToc timer = new TicToc();
    	final Random random = new Random(0L);
    	final int inputSize = 1 << 10;
    	final int kernelSize = 1 << 2;
    	final int step = 1;
    	final int outputSize = (inputSize - kernelSize + 1) / step;
    	
    	Tools.debugPrint(inputSize, kernelSize, outputSize);
        
        final float[] input = new float[inputSize];
		final float[] convolutionKernel = newAverageKernel(kernelSize);
		final float[] output = new float[outputSize];
        
        Tools.debugPrint(timer.toctic());
        
        for (int i = 0; i < inputSize; ++i) {
        	input[i] = random.nextFloat();
        }
        
        Tools.debugPrint(timer.toctic());
        
        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);
        
        final CLContext context = new CLContext(CL.CL_DEVICE_TYPE_GPU, 0);
        final CLKernel kernel = context.createAndBuildProgram(PROGRAM_SOURCE).createKernel("sampleKernel");
        
        Tools.debugPrint(timer.toctic());
        
        // Set the arguments for the kernel
        kernel.setArg(0, context.createInputBuffer(input));
		kernel.setArg(1, context.createInputBuffer(new int[] { step }));
        kernel.setArg(2, context.createInputBuffer(convolutionKernel));
        kernel.setArg(3, context.createInputBuffer(new int[] { convolutionKernel.length }));
        kernel.setArg(4, context.createOutputBuffer(Sizeof.cl_float, inputSize));
        
//        Tools.debugPrint(timer.toctic());
        
        // Execute the kernel
        kernel.enqueueNDRange(output.length);
        
//        Tools.debugPrint(timer.toctic());
        
        kernel.enqueueReadArg(4, output);
        
        Tools.debugPrint(timer.toc());
        
        // Release kernel, program, and memory objects
        context.release();
        
        Tools.debugPrint(sum(output));
        
        timer.tic();
        convolve1D(input, step, convolutionKernel, output);
        Tools.debugPrint(timer.toc());
        
        Tools.debugPrint(sum(output));
    }
    
    public static final float sum(final float... values) {
    	float result = 0F;
    	
    	for (final float value : values) {
    		result += value;
    	}
    	
    	return result;
    }
    
}
