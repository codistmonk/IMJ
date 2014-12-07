package imj.cl;

import static org.jocl.CL.clGetPlatformIDs;

import java.io.Serializable;
import java.util.Arrays;

import org.jocl.cl_platform_id;

/**
 * @author codistmonk (creation 2014-12-01)
 */
public final class CLHardware implements Serializable {
	
	private final CLPlatform[] platforms;
	
	{
		final int[] numPlatforms = new int[1];
		clGetPlatformIDs(0, null, numPlatforms);
		
		final cl_platform_id[] platformIds = new cl_platform_id[numPlatforms[0]];
		clGetPlatformIDs(platformIds.length, platformIds, null);
		
		this.platforms = Arrays.stream(platformIds).map(CLPlatform::new).toArray(CLPlatform[]::new);
	}
	
	public final int getNumPlatforms() {
		return this.platforms.length;
	}
	
	public final CLPlatform[] getPlatforms() {
		return this.platforms;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -275687338778462065L;
	
}
