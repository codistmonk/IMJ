package imj2.tools;

import static net.sourceforge.aprog.tools.Tools.debugPrint;

import imj2.core.Image.Monochannel;
import imj2.core.LinearPackedGrayImage;
import imj2.core.TiledImage2D;
import imj2.tools.IMJTools.TileProcessor;

import java.util.Date;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2014-05-03)
 */
public final class InteractiveClassifier {
	
	private InteractiveClassifier() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		{
			final TicToc timer = new TicToc();
			
			debugPrint("Loading image started...", new Date(timer.tic()));
			
//			final TiledImage2D image = new LociBackedImage("../Libraries/images/svs/SYS08_A10_7414-005.svs");
			final TiledImage2D image = (TiledImage2D) new MultifileImage(
					"../Libraries/images/jpg/SYS08_A10_7414-005", "SYS08_A10_7414-005_lod0", null).getLODImage(11);
			
			debugPrint("Loading image done in", timer.toc(), "ms");
			debugPrint(image.getWidth(), image.getHeight(), image.getPixelCount());
			
			debugPrint("Analyzing image started...", new Date(timer.tic()));
			
			final int w = image.getWidth();
			final int h = image.getHeight();
			final int s = 40;
			
			/*
			 * <-------------------w------------------->
			 * <---s/2--><-----s-----><-----s----->
			 * *---------0---*--------0------*-----0---*
			 * 
			 * <-----------------------w----------------------->
			 * <---s/2--><-----s-----><-----s-----><-----s----->
			 * *---------0---*--------0------*-----0-----------*
			 * 
			 * numberOf("*") = 1+ceiling((w-s/2)/s)
			 */
			
			final LinearPackedGrayImage segmentation = new LinearPackedGrayImage(
					"", (1L + ceiling(w - s / 2, s)) * (2L + ceiling(h - s / 2, s))
					+ (2L + ceiling(w - s / 2, s)) * (1L + ceiling(h - s / 2, s))
					, new Monochannel(Integer.highestOneBit(s - 1)));
			
			debugPrint(segmentation.getPixelCount());
			
			IMJTools.forEachTileIn(image, new TileProcessor() {
				
				@Override
				public final void pixel(final Info info) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public final void endOfTile() {
					// NOP
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 8075249791734409997L;
				
			});
			
			debugPrint("Analyzing image done in", timer.toc(), "ms");
		}
	}
	
	public static final int ceiling(final int a, final int b) {
		return (a + b - 1) / b;
	}
	
}
