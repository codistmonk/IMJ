package imj.cl;

import static org.jocl.CL.clCreateKernel;
import static org.jocl.CL.clReleaseKernel;
import static org.jocl.CL.clSetKernelArg;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;

/**
 * @author codistmonk (creation 2014-12-02)
 */
public final class CLKernel implements Serializable {
	
	private final CLProgram program;
	
	private final String name;
	
	private final cl_kernel kernel;
	
	private final Map<Integer, cl_mem> args;
	
	public CLKernel(final CLProgram program, final String name) {
		this.program = program;
		this.name = name;
        this.kernel = clCreateKernel(program.getProgram(), name, null);
        this.args = new HashMap<>();
	}
	
	public final CLProgram getProgram() {
		return this.program;
	}
	
	public final String getName() {
		return this.name;
	}
	
	public final cl_kernel getKernel() {
		return this.kernel;
	}
	
	public final void setArg(final int argIndex, final cl_mem buffer) {
		this.args.put(argIndex, buffer);
        clSetKernelArg(this.getKernel(), argIndex, Sizeof.cl_mem, Pointer.to(buffer));
	}
	
	public final cl_mem getArg(final int index) {
		return this.args.get(index);
	}
	
	public final void enqueueNDRange(final long globalWorkSize, final long localWorkSize) {
		this.enqueueNDRange(new long[] { globalWorkSize }, new long[] { localWorkSize });
	}
	
	public final void enqueueNDRange(final long globalWorkSize) {
		this.enqueueNDRange(new long[] { globalWorkSize }, null);
	}
	
	public final void enqueueNDRange(final long[] globalWorkSize, final long[] localWorkSize) {
		this.getDefaultCommandQueue().enqueueNDRangeKernel(
				this, globalWorkSize, localWorkSize);
	}
	
	public final void enqueueReadArg(final int argIndex, final float[] result) {
		this.getDefaultCommandQueue().enqueueReadBuffer(
				this.getArg(argIndex), result);
	}
	
	public final CLCommandQueue getDefaultCommandQueue() {
		return this.getProgram().getContext().getDefaultCommandQueue();
	}
	
	public final void release() {
		clReleaseKernel(this.getKernel());
	}
	
	@Override
	protected final void finalize() throws Throwable {
		try {
			this.release();
		} finally {
			super.finalize();
		}
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -7567834302825720852L;
	
}
