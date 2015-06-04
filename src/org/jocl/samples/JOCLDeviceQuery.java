/*
 * JOCL - Java bindings for OpenCL
 *
 * Copyright 2010 Marco Hutter - http://www.jocl.org/
 */

package org.jocl.samples;

import static org.jocl.CL.*;

import imj.cl.*;

import java.util.*;

import multij.tools.Tools;

/**
 * A JOCL program that queries and prints information about all available
 * devices.
 */
public class JOCLDeviceQuery {

	/**
	 * The entry point of this program
	 *
	 * @param args
	 *            Not used
	 */
	public static void main(final String args[]) {
		final CLHardware clHardware = new CLHardware();

		System.out.println("Number of platforms: "
				+ clHardware.getNumPlatforms());

		final CLPlatform[] platforms = clHardware.getPlatforms();

		// Collect all devices of all platforms
		final List<CLDevice> devices = new ArrayList<>();
		for (final CLPlatform platform : platforms) {
			System.out.println("Number of devices in platform " + platform.getName()
					+ ": " + platform.getNumDevices());
			
			devices.addAll(Arrays.asList(platform.getDevices()));
		}
		
		// Print the infos about all devices
		for (final CLDevice device : devices) {
			// CL_DEVICE_NAME
			final String deviceName = device.getName();
			System.out.println("--- Info for device " + deviceName + ": ---");
			System.out.printf("CL_DEVICE_NAME: \t\t\t%s\n", deviceName);

			// CL_DEVICE_VENDOR
			System.out.printf("CL_DEVICE_VENDOR: \t\t\t%s\n", device.getVendor());

			// CL_DEVICE_VERSION
			System.out.printf("CL_DEVICE_VERSION: \t\t\t%s\n", device.getVersion());

			// CL_DRIVER_VERSION
			System.out.printf("CL_DRIVER_VERSION: \t\t\t%s\n", device.getDriverVersion());

			// CL_DEVICE_TYPE
			if (device.isTypeCPU())
				System.out.printf("CL_DEVICE_TYPE:\t\t\t\t%s\n",
						"CL_DEVICE_TYPE_CPU");
			if (device.isTypeGPU())
				System.out.printf("CL_DEVICE_TYPE:\t\t\t\t%s\n",
						"CL_DEVICE_TYPE_GPU");
			if (device.isTypeAccelerator())
				System.out.printf("CL_DEVICE_TYPE:\t\t\t\t%s\n",
						"CL_DEVICE_TYPE_ACCELERATOR");
			if (device.isTypeDefault())
				System.out.printf("CL_DEVICE_TYPE:\t\t\t\t%s\n",
						"CL_DEVICE_TYPE_DEFAULT");

			// CL_DEVICE_MAX_COMPUTE_UNITS
			System.out.printf("CL_DEVICE_MAX_COMPUTE_UNITS:\t\t%d\n", device.getMaxComputeUnits());

			// CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS
			System.out.printf("CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS:\t%d\n",
					device.getMaxWorkItemDimensions());

			// CL_DEVICE_MAX_WORK_ITEM_SIZES
			System.out.printf(
					"CL_DEVICE_MAX_WORK_ITEM_SIZES:\t\t%s \n",
					Tools.join(" / ", Arrays.stream(device.getMaxWorkItemSizes()).mapToObj(Long::new).toArray()));

			// CL_DEVICE_MAX_WORK_GROUP_SIZE
			System.out.printf("CL_DEVICE_MAX_WORK_GROUP_SIZE:\t\t%d\n",
					device.getMaxWorkGroupSize());

			// CL_DEVICE_MAX_CLOCK_FREQUENCY
			System.out.printf("CL_DEVICE_MAX_CLOCK_FREQUENCY:\t\t%d MHz\n",
					device.getMaxClockFrequency());

			// CL_DEVICE_ADDRESS_BITS
			System.out.printf("CL_DEVICE_ADDRESS_BITS:\t\t\t%d\n", device.getAddressBits());

			// CL_DEVICE_MAX_MEM_ALLOC_SIZE
			System.out.printf("CL_DEVICE_MAX_MEM_ALLOC_SIZE:\t\t%d MByte\n",
					(int) (device.getMaxMemAllocSize() / (1024 * 1024)));

			// CL_DEVICE_GLOBAL_MEM_SIZE
			System.out.printf("CL_DEVICE_GLOBAL_MEM_SIZE:\t\t%d MByte\n",
					(int) (device.getGlobalMemSize() / (1024 * 1024)));

			// CL_DEVICE_ERROR_CORRECTION_SUPPORT
			System.out.printf("CL_DEVICE_ERROR_CORRECTION_SUPPORT:\t%s\n",
					device.getErrorCorrectionSupport() != 0 ? "yes" : "no");

			// CL_DEVICE_LOCAL_MEM_TYPE
			System.out.printf("CL_DEVICE_LOCAL_MEM_TYPE:\t\t%s\n",
					device.getLocalMemType() == 1 ? "local" : "global");

			// CL_DEVICE_LOCAL_MEM_SIZE
			System.out.printf("CL_DEVICE_LOCAL_MEM_SIZE:\t\t%d KByte\n",
					(int) (device.getLocalMemSize() / 1024));

			// CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE
			System.out.printf(
					"CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE:\t%d KByte\n",
					(int) (device.getMaxConstantBufferSize() / 1024));

			// CL_DEVICE_QUEUE_PROPERTIES
			if (device.hasQueueOutOfOrderExecModeEnable())
				System.out.printf("CL_DEVICE_QUEUE_PROPERTIES:\t\t%s\n",
						"CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE");
			if (device.hasQueueProfilingEnable())
				System.out.printf("CL_DEVICE_QUEUE_PROPERTIES:\t\t%s\n",
						"CL_QUEUE_PROFILING_ENABLE");

			// CL_DEVICE_IMAGE_SUPPORT
			System.out.printf("CL_DEVICE_IMAGE_SUPPORT:\t\t%d\n", device.getImageSupport());

			// CL_DEVICE_MAX_READ_IMAGE_ARGS
			System.out.printf("CL_DEVICE_MAX_READ_IMAGE_ARGS:\t\t%d\n", device.getMaxReadImageArgs());

			// CL_DEVICE_MAX_WRITE_IMAGE_ARGS
			System.out.printf("CL_DEVICE_MAX_WRITE_IMAGE_ARGS:\t\t%d\n", device.getMaxWriteImageArgs());

			// CL_DEVICE_SINGLE_FP_CONFIG
			System.out.printf("CL_DEVICE_SINGLE_FP_CONFIG:\t\t%s\n",
					stringFor_cl_device_fp_config(device.getSingleFpConfig()));

			// CL_DEVICE_IMAGE2D_MAX_WIDTH
			System.out.printf("CL_DEVICE_2D_MAX_WIDTH\t\t\t%d\n", device.getImage2dMaxWidth());

			// CL_DEVICE_IMAGE2D_MAX_HEIGHT
			System.out.printf("CL_DEVICE_2D_MAX_HEIGHT\t\t\t%d\n", device.getImage2dMaxHeight());

			// CL_DEVICE_IMAGE3D_MAX_WIDTH
			System.out.printf("CL_DEVICE_3D_MAX_WIDTH\t\t\t%d\n", device.getImage3dMaxWidth());

			// CL_DEVICE_IMAGE3D_MAX_HEIGHT
			System.out.printf("CL_DEVICE_3D_MAX_HEIGHT\t\t\t%d\n", device.getImage3dMaxHeight());

			// CL_DEVICE_IMAGE3D_MAX_DEPTH
			System.out.printf("CL_DEVICE_3D_MAX_DEPTH\t\t\t%d\n", device.getImage3dMaxDepth());

			// CL_DEVICE_PREFERRED_VECTOR_WIDTH_<type>
			System.out.printf("CL_DEVICE_PREFERRED_VECTOR_WIDTH_<t>\t");
			System.out
					.printf("CHAR %d, SHORT %d, INT %d, LONG %d, FLOAT %d, DOUBLE %d\n\n\n",
							device.getPreferredVectorWidthChar(),
							device.getPreferredVectorWidthShort(),
							device.getPreferredVectorWidthInt(),
							device.getPreferredVectorWidthLong(),
							device.getPreferredVectorWidthFloat(),
							device.getPreferredVectorWidthDouble());
		}
	}
	
}
