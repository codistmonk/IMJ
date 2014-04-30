package imj2.tools;

import static java.lang.Integer.parseInt;
import static java.lang.Math.abs;
import static net.sourceforge.aprog.swing.SwingTools.horizontalSplit;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.swing.SwingTools.verticalSplit;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getOrCreate;

import imj2.tools.Image2DComponent.Painter;
import imj2.tools.PaletteBasedSegmentationTest.GenericTree;
import imj2.tools.PaletteBasedSegmentationTest.HistogramView;
import imj2.tools.PaletteBasedSegmentationTest.UpdaterTreeModelListener;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JSplitPane;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.DefaultTreeModel;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.Factory;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-04-23)
 */
public final class ColorSeparationTest {
	
	@Test
	public final void test() {
		SwingTools.useSystemLookAndFeel();
		SwingTools.setCheckAWT(false);
		
		final SimpleImageView imageView = new SimpleImageView();
		final HistogramView histogramView = new HistogramView();
		final GenericTree clustersEditor = new GenericTree("Clusters");
		final JSplitPane splitPane = horizontalSplit(imageView, verticalSplit(clustersEditor, histogramView));
		
		SwingTools.setCheckAWT(true);
		
		clustersEditor.getModel().addTreeModelListener(new UpdaterTreeModelListener() {
			
			@Override
			public final void update(final TreeModelEvent event) {
				imageView.refreshBuffer();
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -568519306296632304L;
			
		});
		
		imageView.getPainters().add(new Painter<SimpleImageView>() {
			
			@Override
			public final void paint(final Graphics2D g, final SimpleImageView component,
					final int width, final int height) {
				final Map<Integer, Collection<Integer>> clusters = getClusters(clustersEditor);
				
				debugPrint(clusters);
				
				final BufferedImage image = imageView.getImage();
				final BufferedImage buffer = imageView.getBufferImage();
				final int w = image.getWidth();
				final int h = image.getHeight();
				
				for (int y = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x) {
						final int rgb = image.getRGB(x, y);
						int bestCluster = rgb;
						int bestDistance = Integer.MAX_VALUE;
						
						for (final Map.Entry<Integer, Collection<Integer>> entry : clusters.entrySet()) {
							for (final Integer prototype : entry.getValue()) {
								final int distance = distance1(rgb, prototype);
								
								if (distance < bestDistance) {
									bestDistance = distance;
									bestCluster = entry.getKey();
								}
							}
						}
						
						buffer.setRGB(x, y, bestCluster);
					}
				}
				
				histogramView.refresh(image);
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 646058874106526093L;
			
		});
		
		show(splitPane, this.getClass().getSimpleName(), true);
	}
	
	public static final int distance1(final int rgb1, final int rgb2) {
		int result = 0;
		
		result += abs(((rgb1 >> 16) & 0xFF) - ((rgb2 >> 16) & 0xFF));
		result += abs(((rgb1 >> 8) & 0xFF) - ((rgb2 >> 8) & 0xFF));
		result += abs(((rgb1 >> 0) & 0xFF) - ((rgb2 >> 0) & 0xFF));
		
		return result;
	}
	
	public static final Map<Integer, Collection<Integer>> getClusters(final GenericTree clustersEditor) {
		final Map<Integer, Collection<Integer>> result = new TreeMap<Integer, Collection<Integer>>();
		final DefaultTreeModel model = clustersEditor.getModel();
		final Object root = model.getRoot();
		
		for (int i = 0; i < model.getChildCount(root); ++i) {
			final Object cluster = model.getChild(root, i);
			final Integer clusterRGB = stringsToRGB(cluster);
			
			for (int j = 0; j < model.getChildCount(cluster); ++j) {
				getOrCreate((Map) result, clusterRGB, Factory.DefaultFactory.TREE_SET_FACTORY)
				.add(stringsToRGB(model.getChild(cluster, j)));
			}
		}
		
		return result;
	}
	
	public static final int stringsToRGB(final Object object) {
		final String[] strings = object.toString().split("\\s+");
		
		return 0xFF000000 | ((parseInt(strings[0]) & 0xFF) << 16) | ((parseInt(strings[1]) & 0xFF) << 8) | ((parseInt(strings[2]) & 0xFF) << 0); 
	}
	
}
