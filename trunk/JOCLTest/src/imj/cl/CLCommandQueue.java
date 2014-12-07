package imj.cl;

import static org.jocl.CL.CL_TRUE;
import static org.jocl.CL.clCreateCommandQueue;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadBuffer;
import static org.jocl.CL.clReleaseCommandQueue;

import java.io.Serializable;

import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_mem;

/**
 * @author codistmonk (creation 2014-12-02)
 */
public final class CLCommandQueue implements Serializable {
	
	private final cl_command_queue commandQueue;
	
	public CLCommandQueue(final CLContext context) {
		this.commandQueue = clCreateCommandQueue(context.getContext(), context.getDevice().getId(), 0, null);
	}
	
	public final cl_command_queue getCommandQueue() {
		return this.commandQueue;
	}
	
	public final void enqueueNDRangeKernel(final CLKernel kernel,
			final long[] globalWorkSize, final long[] localWorkSize) {
		clEnqueueNDRangeKernel(this.getCommandQueue(), kernel.getKernel(), 1, null,
				globalWorkSize, localWorkSize, 0, null, null);
	}
	
	public final void enqueueReadBuffer(final cl_mem buffer, final float[] result) {
		clEnqueueReadBuffer(this.getCommandQueue(), buffer, CL_TRUE, 0,
				Sizeof.cl_float * result.length, Pointer.to(result), 0, null, null);
	}
	
	public final void release() {
		clReleaseCommandQueue(this.getCommandQueue());
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -8782273179290708884L;
	
}
