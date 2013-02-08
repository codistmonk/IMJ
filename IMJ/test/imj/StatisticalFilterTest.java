package imj;

import static imj.IMJTools.image;
import static imj.IMJTools.StatisticsSelector.MEAN;
import static imj.MorphologicalOperations.StructuringElement.SIMPLE_CONNECTIVITY_8;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.IMJTools.StatisticsSelector;
import imj.ImageOfBufferedImage.Feature;
import imj.MorphologicalOperations.StructuringElement;
import imj.VariationStatisticsFilter.Variation;
import imj.VariationStatisticsFilter.Variation.Predefined;

import java.util.Date;

import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-02-08)
 */
public final class StatisticalFilterTest {
	
	@Test
	public final void test01() {
		final TicToc timer = new TicToc();
		final String root = "test/imj/";
		final String imageId = "12003.jpg";
		
		debugPrint("Loading image:", new Date(timer.tic()));
		final Image image = image(root + imageId, Feature.MAX_RGB);
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		debugPrint("Applying filter:", new Date(timer.tic()));
		final StatisticsSelector selector = StatisticsSelector.AMPLITUDE;
		final Image result = new ValueStatisticsFilter(image, selector, SIMPLE_CONNECTIVITY_8).getResult();
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		ImageComponent.show("Value Statistics Filter Result", result);
	}
	
	@Test
	public final void test02() {
		final TicToc timer = new TicToc();
		final String root = "test/imj/";
		final String imageId = "12003.jpg";
		
		debugPrint("Loading image:", new Date(timer.tic()));
		final Image image = image(root + imageId, Feature.MAX_RGB);
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		debugPrint("Applying filter:", new Date(timer.tic()));
		final StatisticsSelector selector = StatisticsSelector.MAXIMUM;
		final Predefined variation = Variation.Predefined.ABSOLUTE_DIFFERENCE;
		final Image result = new VariationStatisticsFilter(image, selector, variation, SIMPLE_CONNECTIVITY_8).getResult();
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		ImageComponent.show("Value Statistics Filter Result", result);
	}
	
}
