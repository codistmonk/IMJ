package imj;

import static imj.IMJTools.writePGM;
import static imj.IMJTools.writePPM;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.usedMemory;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Date;

import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-01-31)
 */
public final class ImageWranglerTest {
	
	@Test
	public final void test1() throws FileNotFoundException {
		final TicToc timer = new TicToc();
//		final String imageId = "test/imj/12003.jpg";
		final String images = "../Libraries/images/";
		final String imageName = "40267.svs";
		final String imageId = images + imageName;
		
		for (int lod = 0; lod <= 10; ++lod) {
			debugPrint("LOD:", lod);
			debugPrint("Loading image:", new Date(timer.tic()));
			final Image image = ImageWrangler.INSTANCE.load(imageId, lod);
			debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
			
			debugPrint("Writing PGM:", new Date(timer.tic()));
			writePGM(image, new FileOutputStream(images + "pgm/" + imageName + ".lod" + lod + ".pgm"));
			debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
			
			debugPrint("Writing PPM:", new Date(timer.tic()));
			writePPM(image, new FileOutputStream(images + "ppm/" + imageName + ".lod" + lod + ".ppm"));
			debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
			
			if (image.getRowCount() <= 1 || image.getColumnCount() <= 1) {
				break;
			}
		}
		
//		ImageComponent.show(imageId, image);
	}
	
}
