package pixel3d;

/**
 * @author codistmonk (creation 2014-04-28)
 */
public final class ARGBShader implements PolygonTools.Processor {
	
	private final Renderer renderer;
	
	private final int argb;
	
	public ARGBShader(final Renderer renderer, final int argb) {
		this.argb = argb;
		this.renderer = renderer;
	}
	
	@Override
	public final void pixel(final double x, final double y, final double z) {
		this.renderer.addPixel(x, y, z, this.argb);
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 2591204837732124053L;
	
}
