package imj.cl;

import static net.sourceforge.aprog.tools.Tools.array;
import static org.jocl.CL.CL_CONTEXT_PLATFORM;
import static org.jocl.CL.CL_MEM_COPY_HOST_PTR;
import static org.jocl.CL.CL_MEM_READ_ONLY;
import static org.jocl.CL.CL_MEM_READ_WRITE;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clCreateContext;
import static org.jocl.CL.clReleaseContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.aprog.tools.Tools;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_mem;

/**
 * @author codistmonk (creation 2014-12-02)
 */
public final class CLContext implements Serializable {
	
	private final CLDevice device;
	
	private final cl_context context;
	
	private final List<CLCommandQueue> commandQueues;
	
	private CLCommandQueue defaultCommandQueue;
	
	private final List<CLProgram> programs;
	
	private final List<cl_mem> buffers;
	
	public CLContext(final long preferredDeviceType, final int preferredDeviceIndex) {
		this(findDevice(preferredDeviceType, preferredDeviceIndex));
	}
	
	public CLContext() {
		this(new CLHardware().getPlatforms()[0].getDevices()[0]);
	}
	
	public CLContext(final CLDevice device) {
		this.device = device;
		
        final cl_context_properties contextProperties = new cl_context_properties();
        
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, device.getPlatform().getId());

        this.context = clCreateContext(
            contextProperties, 1, array(device.getId()), 
            null, null, null);
        this.commandQueues = new ArrayList<>();
        this.programs = new ArrayList<>();
        this.buffers = new ArrayList<>();
	}
	
	public final CLDevice getDevice() {
		return this.device;
	}
	
	public final cl_context getContext() {
		return this.context;
	}
	
	public final CLCommandQueue createCommandQueue() {
        return new CLCommandQueue(this);
	}
	
	public final CLCommandQueue getDefaultCommandQueue() {
		if (this.defaultCommandQueue == null) {
			this.defaultCommandQueue = this.createCommandQueue();
		}
		
		return this.defaultCommandQueue;
	}
	
	public final cl_mem createInputBuffer(final int... array) {
		return this.createInputBuffer(Sizeof.cl_float, array.length, Pointer.to(array));
	}
	
	public final cl_mem createInputBuffer(final float... array) {
		return this.createInputBuffer(Sizeof.cl_float, array.length, Pointer.to(array));
	}
	
	public final cl_mem createInputBuffer(final int elementSize, final long elementCount, final Pointer pointer) {
		return this.createBuffer(CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, elementSize, elementCount, pointer);
	}
	
	public final cl_mem createOutputBuffer(final int elementSize, final long elementCount) {
		return this.createBuffer(CL_MEM_READ_WRITE, elementSize, elementCount, null);
	}
	
	public final cl_mem createBuffer(final long flags, final int elementSize, final long elementCount, final Pointer pointer) {
		final cl_mem result = clCreateBuffer(this.getContext(), 
				flags, elementSize * elementCount, pointer, null);
		
		this.buffers.add(result);
		
		return result;
	}
	
	public final CLProgram createAndBuildProgram(final String source) {
		final CLProgram result = new CLProgram(this, source);
		
		this.programs.add(result);
		
		return result;
	}
	
	public final void releaseCommandQueues() {
		this.commandQueues.forEach(CLCommandQueue::release);
		this.commandQueues.clear();
		this.defaultCommandQueue = null;
	}
	
	public final void releasePrograms() {
		this.programs.forEach(CLProgram::release);
		this.programs.clear();
	}
	
	public final void releaseBuffers() {
		this.buffers.forEach(CL::clReleaseMemObject);
		this.buffers.clear();
	}
	
	public final void release() {
		this.releaseBuffers();
		this.releaseCommandQueues();
		clReleaseContext(this.getContext());
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
	private static final long serialVersionUID = 4356751746914058899L;
	
	public static final CLDevice findDevice(final long preferredDeviceType, final int preferredDeviceIndex) {
		CLDevice defaultResult = null;
		final CLHardware hardware = new CLHardware();
		int i = -1;
		
		for (final CLPlatform platform : hardware.getPlatforms()) {
			for (final CLDevice device : platform.getDevices()) {
				if (defaultResult == null) {
					defaultResult = device;
				}
				
				if (device.isType(preferredDeviceType)) {
					if (!defaultResult.isType(preferredDeviceType)) {
						defaultResult = device;
					}
					
					if (preferredDeviceIndex == ++i) {
						return device;
					}
				}
			}
		}
		
		Tools.debugError("Requested device not found");
		
		return defaultResult;
	}
	
}
