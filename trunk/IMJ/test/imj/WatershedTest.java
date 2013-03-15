package imj;

import static imj.IMJTools.image;
import static imj.Labeling.NeighborhoodShape.CONNECTIVITY_4;
import static imj.MorphologicalOperations.edges8;
import static imj.RegionalExtremaTest.assertImageEquals;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.IMJTools.StatisticsSelector;
import imj.ImageOfBufferedImage.Feature;
import imj.apps.modules.ImageComponent;

import java.util.Date;

import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-01-27)
 */
public final class WatershedTest {
	
	@Test
	public final void test1() {
		final Image image = image(new int[][] {
				{ 0 }
		});
		final Image expected = image(new int[][] {
				{ 1 }
		});
		final Image initialLabels = new RegionalMinima(image, CONNECTIVITY_4).getResult();
		final Image watershed = new Watershed(image, initialLabels, CONNECTIVITY_4).getResult();
		
		assertImageEquals(expected, watershed);
	}
	
	@Test
	public final void test2() {
		final Image image = image(new int[][] {
				{ 0, 1, 1 },
				{ 1, 1, 0 },
		});
		final Image expected = image(new int[][] {
				{ 1, 1, 2 },
				{ 1, 2, 2 },
		});
		final Image initialLabels = new RegionalMinima(image, CONNECTIVITY_4).getResult();
		final Image watershed = new Watershed(image, initialLabels, CONNECTIVITY_4).getResult();
		
		assertImageEquals(expected, watershed);
	}
	
	@Test
	public final void test3() {
		final Image image = image(new int[][] {
				{ 1, 1, 1, 1 },
				{ 0, 1, 1, 0 },
				{ 1, 1, 1, 1 },
		});
		final Image expected = image(new int[][] {
				{ 1, 1, 2, 2 },
				{ 1, 1, 2, 2 },
				{ 1, 1, 2, 2 },
		});
		final Image initialLabels = new RegionalMinima(image, CONNECTIVITY_4).getResult();
		final Image watershed = new Watershed(image, initialLabels, CONNECTIVITY_4).getResult();
		
		assertImageEquals(expected, watershed);
	}
	
	@Test
	public final void test4() {
		final Image image = image(new int[][] {
				{ 0, 2, 2, 1, 1, 0 },
		});
		final Image expected = image(new int[][] {
				{ 1, 1, 2, 2, 2, 2 },
		});
		final Image initialLabels = new RegionalMinima(image, CONNECTIVITY_4).getResult();
		final Image watershed = new Watershed(image, initialLabels, CONNECTIVITY_4).getResult();
		
		assertImageEquals(expected, watershed);
	}
	
	@Test
	public final void test5() {
		final Image image = image(new int[][] {
				{ 3, 2, 2, 3, 3, 2, 1, 0, 3, 3, 1 },
		});
		final Image expected = image(new int[][] {
				{ 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3 },
		});
		final Image initialLabels = new RegionalMinima(image, CONNECTIVITY_4).getResult();
		final Image watershed = new Watershed(image, initialLabels, CONNECTIVITY_4).getResult();
		
		assertImageEquals(expected, watershed);
	}
	
//	@Test
	public final void test7() {
		final TicToc timer = new TicToc();
//		final String imageId = "test/imj/12003.jpg";
		final String imageId = "../Libraries/images/16088-4.png";
		
		debugPrint("Loading image:", new Date(timer.tic()));
		final Image image = image(imageId, Feature.MAX_RGB);
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
//		debugPrint("Extracting edges:", new Date(timer.tic()));
//		final Image edges = edges8(image);
//		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
//		
//		ImageComponent.showAdjusted(imageId, image, edges);
		ImageComponent.showAdjusted(imageId, new WatershedStack(image, 2, StatisticsSelector.MEAN).getAllImages());
	}
	
	/**
	 * @author codistmonk (creation 2013-01-27)
	 */
	public static final class WatershedStack extends LabelingStack {
		
		public WatershedStack(final Image image, final int n,
				final StatisticsSelector reconstructionFeature) {
			super(image, n, reconstructionFeature);
			// NOP
		}
		
		@Override
		protected final Labeling newLabeling(final Image image) {
			final TicToc timer = new TicToc();
			
			debugPrint("Extracting edges:", new Date(timer.tic()));
			final Image edges = edges8(image);
			debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
			
			debugPrint("Computing markers:", new Date(timer.tic()));
			final Image initialLabels = new RegionalMinima(edges, CONNECTIVITY_4).getResult();
			debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
			
			debugPrint("Computing watershed:", new Date(timer.tic()));
			final Watershed result = new Watershed(edges, initialLabels, CONNECTIVITY_4);
			debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
			
			return result;
		}
		
	}
	
}
