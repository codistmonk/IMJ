package imj.cl;

import static org.jocl.CL.CL_DEVICE_TYPE_ALL;
import static org.jocl.CL.CL_PLATFORM_NAME;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetPlatformInfo;

import java.io.Serializable;
import java.util.Arrays;

import org.jocl.Pointer;
import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;

/**
 * @author codistmonk (creation 2014-12-01)
 */
public final class CLPlatform implements Serializable {
	
	private final cl_platform_id id;
	
	private final String name;
	
	private final CLDevice[] devices;
	
	public CLPlatform(final cl_platform_id id) {
		this.id = id;
		
		this.name = getString(id, CL_PLATFORM_NAME);
		
		final int[] numDevices = new int[1];
		clGetDeviceIDs(id, CL_DEVICE_TYPE_ALL, 0, null, numDevices);
		
		final cl_device_id[] deviceIds = new cl_device_id[numDevices[0]];
		clGetDeviceIDs(id, CL_DEVICE_TYPE_ALL, numDevices[0], deviceIds, null);
		
		this.devices = Arrays.stream(deviceIds).map(deviceId -> new CLDevice(this, deviceId)).toArray(CLDevice[]::new);
	}
	
	public final cl_platform_id getId() {
		return this.id;
	}
	
	public final String getName() {
		return this.name;
	}
	
	public final int getNumDevices() {
		return this.devices.length;
	}
	
	public final CLDevice[] getDevices() {
		return this.devices;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -5659790260474710120L;
	
	/**
	 * Returns the value of the platform info parameter with the given name
	 *
	 * @param platform
	 *            The platform
	 * @param paramName
	 *            The parameter name
	 * @return The value
	 */
	public static final String getString(final cl_platform_id platform, final int paramName) {
		// Obtain the length of the string that will be queried
		long size[] = new long[1];
		clGetPlatformInfo(platform, paramName, 0, null, size);

		// Create a buffer of the appropriate size and fill it with the info
		byte buffer[] = new byte[(int) size[0]];
		clGetPlatformInfo(platform, paramName, buffer.length,
				Pointer.to(buffer), null);

		// Create a string from the buffer (excluding the trailing \0 byte)
		return new String(buffer, 0, CLDevice.indexOfZero(buffer));
	}
	
}
