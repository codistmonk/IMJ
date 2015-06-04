package imj.cl;

import static multij.tools.Tools.array;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clReleaseProgram;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jocl.cl_program;

/**
 * @author codistmonk (creation 2014-12-02)
 */
public final class CLProgram implements Serializable {
	
	private final CLContext context;
	
	private cl_program program;
	
	private final List<CLKernel> kernels;
	
	public CLProgram(final CLContext context, final String source) {
		this.context = context;
		this.program = clCreateProgramWithSource(context.getContext(),
				1, array(source), null, null);
		this.kernels = new ArrayList<>();
		
		clBuildProgram(this.program, 0, null, null, null, null);
	}
	
	public final CLContext getContext() {
		return this.context;
	}
	
	public final cl_program getProgram() {
		return this.program;
	}
	
	public final CLKernel createKernel(final String name) {
		final CLKernel result = new CLKernel(this, name);
		
		this.kernels.add(result);
		
		return result;
	}
	
	public final void releaseKernels() {
		this.kernels.forEach(CLKernel::release);
		this.kernels.clear();
	}
	
	public final void release() {
		clReleaseProgram(this.getProgram());
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
	private static final long serialVersionUID = -7819913693085761329L;
	
}
