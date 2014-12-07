package imj.cl;

import static org.jocl.CL.CL_DEVICE_ADDRESS_BITS;
import static org.jocl.CL.CL_DEVICE_ERROR_CORRECTION_SUPPORT;
import static org.jocl.CL.CL_DEVICE_GLOBAL_MEM_SIZE;
import static org.jocl.CL.CL_DEVICE_IMAGE2D_MAX_HEIGHT;
import static org.jocl.CL.CL_DEVICE_IMAGE2D_MAX_WIDTH;
import static org.jocl.CL.CL_DEVICE_IMAGE3D_MAX_DEPTH;
import static org.jocl.CL.CL_DEVICE_IMAGE3D_MAX_HEIGHT;
import static org.jocl.CL.CL_DEVICE_IMAGE3D_MAX_WIDTH;
import static org.jocl.CL.CL_DEVICE_IMAGE_SUPPORT;
import static org.jocl.CL.CL_DEVICE_LOCAL_MEM_SIZE;
import static org.jocl.CL.CL_DEVICE_LOCAL_MEM_TYPE;
import static org.jocl.CL.CL_DEVICE_MAX_CLOCK_FREQUENCY;
import static org.jocl.CL.CL_DEVICE_MAX_COMPUTE_UNITS;
import static org.jocl.CL.CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE;
import static org.jocl.CL.CL_DEVICE_MAX_MEM_ALLOC_SIZE;
import static org.jocl.CL.CL_DEVICE_MAX_READ_IMAGE_ARGS;
import static org.jocl.CL.CL_DEVICE_MAX_WORK_GROUP_SIZE;
import static org.jocl.CL.CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS;
import static org.jocl.CL.CL_DEVICE_MAX_WORK_ITEM_SIZES;
import static org.jocl.CL.CL_DEVICE_MAX_WRITE_IMAGE_ARGS;
import static org.jocl.CL.CL_DEVICE_NAME;
import static org.jocl.CL.CL_DEVICE_PREFERRED_VECTOR_WIDTH_CHAR;
import static org.jocl.CL.CL_DEVICE_PREFERRED_VECTOR_WIDTH_DOUBLE;
import static org.jocl.CL.CL_DEVICE_PREFERRED_VECTOR_WIDTH_FLOAT;
import static org.jocl.CL.CL_DEVICE_PREFERRED_VECTOR_WIDTH_INT;
import static org.jocl.CL.CL_DEVICE_PREFERRED_VECTOR_WIDTH_LONG;
import static org.jocl.CL.CL_DEVICE_PREFERRED_VECTOR_WIDTH_SHORT;
import static org.jocl.CL.CL_DEVICE_QUEUE_PROPERTIES;
import static org.jocl.CL.CL_DEVICE_SINGLE_FP_CONFIG;
import static org.jocl.CL.CL_DEVICE_TYPE;
import static org.jocl.CL.CL_DEVICE_TYPE_ACCELERATOR;
import static org.jocl.CL.CL_DEVICE_TYPE_CPU;
import static org.jocl.CL.CL_DEVICE_TYPE_DEFAULT;
import static org.jocl.CL.CL_DEVICE_TYPE_GPU;
import static org.jocl.CL.CL_DEVICE_VENDOR;
import static org.jocl.CL.CL_DEVICE_VERSION;
import static org.jocl.CL.CL_DRIVER_VERSION;
import static org.jocl.CL.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE;
import static org.jocl.CL.CL_QUEUE_PROFILING_ENABLE;
import static org.jocl.CL.clGetDeviceInfo;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_device_id;

/**
 * @author codistmonk (creation 2014-12-01)
 */
public final class CLDevice implements Serializable {
	
	private final CLPlatform platform;
	
	private final cl_device_id id;
	
	private final String name;
	
	private final String vendor;
	
	private final String version;
	
	private final String driverVersion;
	
	private final long type;
	
	private final int maxComputeUnits;
	
	private final long[] maxWorkItemSizes;
	
	private final long maxWorkGroupSize;
	
	private final long maxClockFrequency;
	
	private final int addressBits;
	
	private final long maxMemAllocSize;
	
	private final long globalMemSize;
	
	private final int errorCorrectionSupport;
	
	private final int localMemType;
	
	private final long localMemSize;
	
	private final long maxConstantBufferSize;
	
	private final long queueProperties;
	
	private final int imageSupport;
	
	private final int maxReadImageArgs;
	
	private final int maxWriteImageArgs;
	
	private final long singleFpConfig;
	
	private final long image2dMaxWidth;
	
	private final long image2dMaxHeight;
	
	private final long image3dMaxWidth;
	
	private final long image3dMaxHeight;
	
	private final long image3dMaxDepth;
	
	private final int preferredVectorWidthChar;
	
	private final int preferredVectorWidthShort;
	
	private final int preferredVectorWidthInt;
	
	private final int preferredVectorWidthLong;
	
	private final int preferredVectorWidthFloat;
	
	private final int preferredVectorWidthDouble;
	
	public CLDevice(final CLPlatform platform, final cl_device_id id) {
		this.platform = platform;
		this.id = id;
		this.name = getString(id, CL_DEVICE_NAME);
		this.vendor = getString(id, CL_DEVICE_VENDOR);
		this.version = getString(id, CL_DEVICE_VERSION);
		this.driverVersion = getString(id, CL_DRIVER_VERSION);
		this.type = getLong(id, CL_DEVICE_TYPE);
		this.maxComputeUnits = getInt(id, CL_DEVICE_MAX_COMPUTE_UNITS);
		final long maxWorkItemDimensions = getLong(id, CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS);
		this.maxWorkItemSizes = getSizes(id, CL_DEVICE_MAX_WORK_ITEM_SIZES, (int) maxWorkItemDimensions);
		this.maxWorkGroupSize = getSize(id, CL_DEVICE_MAX_WORK_GROUP_SIZE);
		this.maxClockFrequency = getLong(id, CL_DEVICE_MAX_CLOCK_FREQUENCY);
		this.addressBits = getInt(id, CL_DEVICE_ADDRESS_BITS);
		this.maxMemAllocSize = getLong(id, CL_DEVICE_MAX_MEM_ALLOC_SIZE);
		this.globalMemSize = getLong(id, CL_DEVICE_GLOBAL_MEM_SIZE);
		this.errorCorrectionSupport = getInt(id, CL_DEVICE_ERROR_CORRECTION_SUPPORT);
		this.localMemType = getInt(id, CL_DEVICE_LOCAL_MEM_TYPE);
		this.localMemSize = getLong(id, CL_DEVICE_LOCAL_MEM_SIZE);
		this.maxConstantBufferSize = getLong(id, CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE);
		this.queueProperties = getLong(id, CL_DEVICE_QUEUE_PROPERTIES);
		this.imageSupport = getInt(id, CL_DEVICE_IMAGE_SUPPORT);
		this.maxReadImageArgs = getInt(id, CL_DEVICE_MAX_READ_IMAGE_ARGS);
		this.maxWriteImageArgs = getInt(id, CL_DEVICE_MAX_WRITE_IMAGE_ARGS);
		this.singleFpConfig = getLong(id, CL_DEVICE_SINGLE_FP_CONFIG);
		this.image2dMaxWidth = getSize(id, CL_DEVICE_IMAGE2D_MAX_WIDTH);
		this.image2dMaxHeight = getSize(id, CL_DEVICE_IMAGE2D_MAX_HEIGHT);
		this.image3dMaxWidth = getSize(id, CL_DEVICE_IMAGE3D_MAX_WIDTH);
		this.image3dMaxHeight = getSize(id, CL_DEVICE_IMAGE3D_MAX_HEIGHT);
		this.image3dMaxDepth = getSize(id, CL_DEVICE_IMAGE3D_MAX_DEPTH);
		this.preferredVectorWidthChar = getInt(id, CL_DEVICE_PREFERRED_VECTOR_WIDTH_CHAR);
		this.preferredVectorWidthShort = getInt(id, CL_DEVICE_PREFERRED_VECTOR_WIDTH_SHORT);
		this.preferredVectorWidthInt = getInt(id, CL_DEVICE_PREFERRED_VECTOR_WIDTH_INT);
		this.preferredVectorWidthLong = getInt(id, CL_DEVICE_PREFERRED_VECTOR_WIDTH_LONG);
		this.preferredVectorWidthFloat = getInt(id, CL_DEVICE_PREFERRED_VECTOR_WIDTH_FLOAT);
		this.preferredVectorWidthDouble = getInt(id, CL_DEVICE_PREFERRED_VECTOR_WIDTH_DOUBLE);
	}
	
	public final CLPlatform getPlatform() {
		return this.platform;
	}
	
	public final cl_device_id getId() {
		return this.id;
	}
	
	public final String getName() {
		return this.name;
	}
	
	public final String getVendor() {
		return this.vendor;
	}
	
	public final String getVersion() {
		return this.version;
	}
	
	public final String getDriverVersion() {
		return this.driverVersion;
	}
	
	public final long getType() {
		return this.type;
	}
	
	public final int getMaxComputeUnits() {
		return this.maxComputeUnits;
	}
	
	public final long getMaxWorkItemDimensions() {
		return this.getMaxWorkItemSizes().length;
	}
	
	public final long[] getMaxWorkItemSizes() {
		return this.maxWorkItemSizes;
	}
	
	public final long getMaxWorkGroupSize() {
		return this.maxWorkGroupSize;
	}
	
	public final long getMaxClockFrequency() {
		return this.maxClockFrequency;
	}
	
	public final int getAddressBits() {
		return this.addressBits;
	}
	
	public final long getMaxMemAllocSize() {
		return this.maxMemAllocSize;
	}
	
	public final long getGlobalMemSize() {
		return this.globalMemSize;
	}
	
	public final int getErrorCorrectionSupport() {
		return this.errorCorrectionSupport;
	}
	
	public final int getLocalMemType() {
		return this.localMemType;
	}
	
	public final long getLocalMemSize() {
		return this.localMemSize;
	}
	
	public final long getMaxConstantBufferSize() {
		return this.maxConstantBufferSize;
	}
	
	public final long getQueueProperties() {
		return this.queueProperties;
	}
	
	public final int getImageSupport() {
		return this.imageSupport;
	}
	
	public final int getMaxReadImageArgs() {
		return this.maxReadImageArgs;
	}
	
	public final int getMaxWriteImageArgs() {
		return this.maxWriteImageArgs;
	}
	
	public final long getSingleFpConfig() {
		return this.singleFpConfig;
	}
	
	public final long getImage2dMaxWidth() {
		return this.image2dMaxWidth;
	}
	
	public final long getImage2dMaxHeight() {
		return this.image2dMaxHeight;
	}
	
	public final long getImage3dMaxWidth() {
		return this.image3dMaxWidth;
	}
	
	public final long getImage3dMaxHeight() {
		return this.image3dMaxHeight;
	}
	
	public final long getImage3dMaxDepth() {
		return this.image3dMaxDepth;
	}
	
	public final int getPreferredVectorWidthChar() {
		return this.preferredVectorWidthChar;
	}
	
	public final int getPreferredVectorWidthShort() {
		return this.preferredVectorWidthShort;
	}
	
	public final int getPreferredVectorWidthInt() {
		return this.preferredVectorWidthInt;
	}
	
	public final int getPreferredVectorWidthLong() {
		return this.preferredVectorWidthLong;
	}
	
	public final int getPreferredVectorWidthFloat() {
		return this.preferredVectorWidthFloat;
	}
	
	public final int getPreferredVectorWidthDouble() {
		return this.preferredVectorWidthDouble;
	}
	
	public final boolean isType(final long mask) {
		return test(this.getType(), mask);
	}
	
	public final boolean isTypeCPU() {
		return this.isType(CL_DEVICE_TYPE_CPU);
	}
	
	public final boolean isTypeGPU() {
		return this.isType(CL_DEVICE_TYPE_GPU);
	}
	
	public final boolean isTypeAccelerator() {
		return this.isType(CL_DEVICE_TYPE_ACCELERATOR);
	}
	
	public final boolean isTypeDefault() {
		return this.isType(CL_DEVICE_TYPE_DEFAULT);
	}
	
	public final boolean hasQueueProperty(final long mask) {
		return test(this.getQueueProperties(), mask);
	}
	
	public final boolean hasQueueOutOfOrderExecModeEnable() {
		return this.hasQueueProperty(CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE);
	}
	
	public final boolean hasQueueProfilingEnable() {
		return this.hasQueueProperty(CL_QUEUE_PROFILING_ENABLE);
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -7752639727927613161L;
	
	public static final boolean test(final long flags, final long mask) {
		return (flags & mask) != 0L;
	}
	
	/**
	 * Returns the value of the device info parameter with the given name
	 *
	 * @param device
	 *            The device
	 * @param paramName
	 *            The parameter name
	 * @return The value
	 */
	public static final int getInt(final cl_device_id device, final int paramName) {
		return getInts(device, paramName, 1)[0];
	}
	
	/**
	 * Returns the values of the device info parameter with the given name
	 *
	 * @param device
	 *            The device
	 * @param paramName
	 *            The parameter name
	 * @param numValues
	 *            The number of values
	 * @return The value
	 */
	public static final int[] getInts(final cl_device_id device, final int paramName,
			final int numValues) {
		int values[] = new int[numValues];
		clGetDeviceInfo(device, paramName, Sizeof.cl_int * numValues,
				Pointer.to(values), null);
		return values;
	}
	
	/**
	 * Returns the value of the device info parameter with the given name
	 *
	 * @param device
	 *            The device
	 * @param paramName
	 *            The parameter name
	 * @return The value
	 */
	public static final long getLong(final cl_device_id device, final int paramName) {
		return getLongs(device, paramName, 1)[0];
	}
	
	/**
	 * Returns the values of the device info parameter with the given name
	 *
	 * @param device
	 *            The device
	 * @param paramName
	 *            The parameter name
	 * @param numValues
	 *            The number of values
	 * @return The value
	 */
	public static final long[] getLongs(final cl_device_id device, final int paramName,
			final int numValues) {
		long values[] = new long[numValues];
		clGetDeviceInfo(device, paramName, Sizeof.cl_long * numValues,
				Pointer.to(values), null);
		
		return values;
	}
	
	/**
	 * Returns the value of the device info parameter with the given name
	 *
	 * @param device
	 *            The device
	 * @param paramName
	 *            The parameter name
	 * @return The value
	 */
	public static final String getString(final cl_device_id device, final int paramName) {
		// Obtain the length of the string that will be queried
		long size[] = new long[1];
		clGetDeviceInfo(device, paramName, 0, null, size);

		// Create a buffer of the appropriate size and fill it with the info
		byte buffer[] = new byte[(int) size[0]];
		clGetDeviceInfo(device, paramName, buffer.length, Pointer.to(buffer),
				null);

		// Create a string from the buffer (excluding the trailing \0 byte)
		return new String(buffer, 0, indexOfZero(buffer));
	}
	
	public static final int indexOfZero(final byte[] bytes) {
		final int n = bytes.length;
		
		for (int i = 0; i < n; ++i) {
			if (0 == bytes[i]) {
				return i;
			}
		}
		
		return n;
	}
	
	/**
	 * Returns the value of the device info parameter with the given name
	 *
	 * @param device
	 *            The device
	 * @param paramName
	 *            The parameter name
	 * @return The value
	 */
	public static final long getSize(final cl_device_id device, final int paramName) {
		return getSizes(device, paramName, 1)[0];
	}
	
	/**
	 * Returns the values of the device info parameter with the given name
	 *
	 * @param device
	 *            The device
	 * @param paramName
	 *            The parameter name
	 * @param numValues
	 *            The number of values
	 * @return The value
	 */
	public static long[] getSizes(final cl_device_id device, final int paramName, final int numValues) {
		// The size of the returned data has to depend on
		// the size of a size_t, which is handled here
		ByteBuffer buffer = ByteBuffer.allocate(numValues * Sizeof.size_t)
				.order(ByteOrder.nativeOrder());
		clGetDeviceInfo(device, paramName, Sizeof.size_t * numValues,
				Pointer.to(buffer), null);
		long values[] = new long[numValues];
		if (Sizeof.size_t == 4) {
			for (int i = 0; i < numValues; i++) {
				values[i] = buffer.getInt(i * Sizeof.size_t);
			}
		} else {
			for (int i = 0; i < numValues; i++) {
				values[i] = buffer.getLong(i * Sizeof.size_t);
			}
		}
		
		return values;
	}
	
}
