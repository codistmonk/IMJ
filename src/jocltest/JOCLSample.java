/*
 * JOCL - Java bindings for OpenCL
 * 
 * Copyright 2009 Marco Hutter - http://www.jocl.org/
 */

package jocltest;

import imj.cl.CLContext;
import imj.cl.CLKernel;

import multij.tools.TicToc;
import multij.tools.Tools;

import org.jocl.*;

/**
 * A small JOCL sample.
 */
public final class JOCLSample {
	
    /**
     * The source code of the OpenCL program to execute
     */
    private static final String PROGRAM_SOURCE =
        "__kernel void "+
        "sampleKernel(__global const float *a,"+
        "             __global const float *b,"+
        "             __global float *c)"+
        "{"+
        "    int gid = get_global_id(0);"+
        "    c[gid] = a[gid] * b[gid];"+
        "}";
    
	/**
     * The entry point of this sample
     * 
     * @param args Not used
     */
    public static final void main(final String[] args) {
    	final TicToc timer = new TicToc();
        // Create input and output data 
    	final int n = 100_000_000;
        
        Tools.debugPrint(n * 4L * 3L);
        
        final float srcA[] = new float[n];
        final float srcB[] = new float[n];
        final float dst[] = new float[n];
        
        Tools.debugPrint(timer.toctic());
        
        for (int i = 0; i < n; ++i) {
        	srcB[i] = srcA[i] = i;
        }
        
        Tools.debugPrint(timer.toctic());
        
        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);
        
        final CLContext context = new CLContext();
        
        Tools.debugPrint(timer.toctic());
        
        final CLKernel kernel = context.createAndBuildProgram(PROGRAM_SOURCE).createKernel("sampleKernel");
        
        // Set the arguments for the kernel
        kernel.setArg(0, context.createInputBuffer(srcA));
        kernel.setArg(1, context.createInputBuffer(srcB));
        kernel.setArg(2, context.createOutputBuffer(Sizeof.cl_float, n));
        
        Tools.debugPrint(timer.toctic());
        
        // Execute the kernel
        kernel.enqueueNDRange(n, 1);
        
        Tools.debugPrint(timer.toctic());
        
        kernel.enqueueReadArg(2, dst);
        
        Tools.debugPrint(timer.toctic());
        
        // Release kernel, program, and memory objects
        context.release();
        
        Tools.debugPrint(timer.toctic());
        
        // Verify the result
        boolean passed = true;
        final float epsilon = 1e-7f;
        
        for (int i = 0; i < n; ++i) {
            final float x = dst[i];
            final float y = srcA[i] * srcB[i];
            final boolean epsilonEqual = Math.abs(x - y) <= epsilon * Math.abs(x);
            
            if (!epsilonEqual) {
                passed = false;
                break;
            }
        }
        
        Tools.debugPrint(timer.toctic());
        
        System.out.println("Test "+(passed?"PASSED":"FAILED"));
        
        if (n <= 10) {
            System.out.println("Result: "+java.util.Arrays.toString(dst));
        }
    }
    
}
