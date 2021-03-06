package imj2.tools;

import static imj2.pixel3d.PolygonTools.X;
import static imj2.pixel3d.PolygonTools.Y;
import static imj2.pixel3d.PolygonTools.Z;
import static imj2.tools.IMJTools.blue8;
import static imj2.tools.IMJTools.green8;
import static imj2.tools.IMJTools.red8;
import static imj2.tools.IMJTools.uint8;
import static java.lang.Math.round;
import static multij.swing.SwingTools.horizontalSplit;
import static multij.swing.SwingTools.scrollable;
import static multij.swing.SwingTools.show;
import static multij.swing.SwingTools.verticalSplit;
import static multij.tools.Tools.array;

import imj2.draft.RGBTransformer;
import imj2.draft.PaletteBasedSegmentation.HistogramView;
import imj2.draft.PaletteBasedSegmentation.HistogramView.SegmentsUpdatedEvent;
import imj2.tools.Image2DComponent.Painter;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.lang.reflect.Method;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JSplitPane;

import multij.events.EventManager;
import multij.events.EventManager.Event.Listener;
import multij.swing.SwingTools;
import multij.tools.MathTools.Statistics;

import org.junit.Test;
import org.ojalgo.matrix.BasicMatrix;
import org.ojalgo.matrix.MatrixBuilder;
import org.ojalgo.matrix.PrimitiveMatrix;
import org.ojalgo.matrix.decomposition.SingularValue;

/**
 * @author codistmonk (creation 2014-04-30)
 */
public final class ColorSeparationTest {
	
	@Test
	public final void test() {
		SwingTools.useSystemLookAndFeel();
		
		final SimpleImageView imageView = new SimpleImageView();
		final HistogramView histogramView = new HistogramView();
		final JComboBox<? extends RGBTransformer> linearizerSelector = new JComboBox<>(array(RGBTransformer.Predefined.ID));
		
		SwingTools.setCheckAWT(false);
		final JSplitPane splitPane = horizontalSplit(imageView, verticalSplit(linearizerSelector, scrollable(histogramView)));
		SwingTools.setCheckAWT(true);
		
		linearizerSelector.addActionListener(new ActionListener() {
			
			@Override
			public final void actionPerformed(final ActionEvent event) {
				imageView.refreshBuffer();
			}
			
		});
		
		final double tx = +128.0;
		final double ty = +128.0;
		final double tz = -128.0;
		
		EventManager.getInstance().addListener(histogramView, SegmentsUpdatedEvent.class, new Serializable() {
			
			@Listener
			public final void segmentsUpdated(final SegmentsUpdatedEvent event) {
				final double[] points = histogramView.getUserPoints().toArray();
				final int[] segments = histogramView.getUserSegments().toArray();
				final int n = segments.length / 2;
				final MatrixBuilder<Double> matrixBuilder = PrimitiveMatrix.getBuilder(4, n + 1);
				
				for (int i = 0; i < 2 * n; i += 2) {
					final int i1 = (segments[i + 0] - 1) * 3;
					final int i2 = (segments[i + 1] - 1) * 3;
					final double x1 = points[i1 + X] - tx;
					final double y1 = points[i1 + Y] - ty;
					final double z1 = points[i1 + Z] - tz;
					final double x2 = points[i2 + X] - tx;
					final double y2 = points[i2 + Y] - ty;
					final double z2 = points[i2 + Z] - tz;
					final double dx = x2 - x1;
					final double dy = y2 - y1;
					final double dz = z2 - z1;
					
					matrixBuilder.set(0, i / 2, dx);
					matrixBuilder.set(1, i / 2, dy);
					matrixBuilder.set(2, i / 2, dz);
				}
				
				{
					final int i0 = (segments[0] - 1) * 3;
					
					matrixBuilder.set(0, n, -(points[i0 + X] - tx));
					matrixBuilder.set(1, n, -(points[i0 + Y] - ty));
					matrixBuilder.set(2, n, -(points[i0 + Z] - tz));
					matrixBuilder.set(3, n, 1.0);
				}
				
				final BasicMatrix m = matrixBuilder.build();
				
				try {
					final Method method = PrimitiveMatrix.class.getSuperclass().getDeclaredMethod("getComputedSingularValue");
					
					method.setAccessible(true);
					
					@SuppressWarnings("unchecked")
					final BasicMatrix mi = (BasicMatrix) ((SingularValue<Double>) method.invoke(m)).getInverse();
					@SuppressWarnings("unchecked")
					final DefaultComboBoxModel<RGBTransformer> linearizers = (DefaultComboBoxModel<RGBTransformer>) linearizerSelector.getModel();
					final int selectedIndex = linearizerSelector.getSelectedIndex();
					
					while (1 < linearizers.getSize()) {
						linearizers.removeElementAt(1);
					}
					
					for (int i = 0; i < n; ++i) {
						linearizers.addElement(new Linearizer2("" + i, row(mi, i), row(mi, n)));
					}
					
					linearizerSelector.setSelectedIndex(selectedIndex < linearizers.getSize() ? selectedIndex : 0);
					
					imageView.refreshBuffer();
				} catch (final Exception exception) {
					exception.printStackTrace();
				}
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -1050256525039613825L;
			
		});
		
		imageView.getPainters().add(new Painter<SimpleImageView>() {
			
			@Override
			public final void paint(final Graphics2D g, final SimpleImageView component,
					final int width, final int height) {
				final RGBTransformer transformer = (RGBTransformer) linearizerSelector.getSelectedItem();
				final BufferedImage image = imageView.getImage();
				final BufferedImage buffer = imageView.getBufferImage();
				final int w = image.getWidth();
				final int h = image.getHeight();
				
				for (int y = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x) {
						buffer.setRGB(x, y, transformer.transform(image.getRGB(x, y)));
					}
				}
				
				histogramView.refresh(image);
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 646058874106526093L;
			
		});
		
		histogramView.getUserPoints().addAll(128.0 + tx, 128.0 + ty, 128.0 + tz
				, 0.0 + tx, 0.0 + ty, 0.0 + tz
				, 255.0 + tx, 0.0 + ty, 0.0 + tz
				, 255.0 + tx, 255.0 + ty, 0.0 + tz
				, 0.0 + tx, 255.0 + ty, 0.0 + tz
				, 0.0 + tx, 0.0 + ty, 255.0 + tz
				, 255.0 + tx, 0.0 + ty, 255.0 + tz
				, 255.0 + tx, 255.0 + ty, 255.0 + tz
				, 0.0 + tx, 255.0 + ty, 255.0 + tz
				);
		histogramView.getUserSegments().addAll(1, 2, 1, 3, 1, 4, 1, 5, 1, 6, 1, 7, 1, 8, 1, 9);
		EventManager.getInstance().dispatch(histogramView.new SegmentsUpdatedEvent());
		
		show(splitPane, this.getClass().getSimpleName(), true);
	}
	
	public static final double[] row(final BasicMatrix matrix, final int rowIndex) {
		final int n = matrix.getColDim();
		final double[] result = new double[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = matrix.doubleValue(rowIndex, i);
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2014-04-30)
	 */
	public static final class RGBLinearizer implements RGBTransformer {
		
		private final String description;
		
		private final double[] hyperplane;
		
		private final Statistics statistics;
		
		public RGBLinearizer(final String description, final double[] hyperplane) {
			this.description = description;
			this.hyperplane = hyperplane;
			this.statistics = new Statistics();
			
			for (final int rgb : new int[] { 0x00000000, 0x000000FF, 0x0000FFFF, 0x0000FF00
					, 0x00FF0000, 0x00FF00FF, 0x00FFFFFF, 0x00FFFF00 }) {
				this.statistics.addValue(this.transformAndUnscale(rgb));
			}
		}
		
		@Override
		public final int transform(final int rgb) {
			final int gray8 = uint8(round(
					this.statistics.getNormalizedValue(this.transformAndUnscale(rgb)) * 255.0));
			
			return 0xFF000000 | (gray8 * 0x00010101);
		}
		
		private final double transformAndUnscale(final int rgb) {
			return transform(this.hyperplane, rgb);
		}
		
		@Override
		public final String toString() {
			return this.description;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 6933631179568013440L;
		
		public static final double transform(final double[] hyperplane, final int rgb) {
			return hyperplane[0] * red8(rgb) + hyperplane[1] * green8(rgb) + hyperplane[2] * blue8(rgb)
					+ hyperplane[3];
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2014-04-30)
	 */
	public static final class Linearizer2 implements RGBTransformer {
		
		private final String description;
		
		private final double[] hyperplane;
		
		private final double[] homogeneousHyperplane;
		
		private final Statistics statistics;
		
		public Linearizer2(final String description, final double[] hyperplane, final double[] homogeneousHyperplane) {
			this.description = description;
			this.hyperplane = hyperplane;
			this.homogeneousHyperplane = homogeneousHyperplane;
			this.statistics = new Statistics();
			
			for (final int rgb : new int[] { 0x00000000, 0x000000FF, 0x0000FFFF, 0x0000FF00
					, 0x00FF0000, 0x00FF00FF, 0x00FFFFFF, 0x00FFFF00 }) {
				this.statistics.addValue(this.transformAndUnscale(rgb));
			}
		}
		
		@Override
		public final int transform(final int rgb) {
			final int gray8 = uint8(round(
					this.statistics.getNormalizedValue(this.transformAndUnscale(rgb)) * 255.0));
			
			return 0xFF000000 | (gray8 * 0x00010101);
		}
		
		private final double transformAndUnscale(final int rgb) {
			return RGBLinearizer.transform(this.hyperplane, rgb) / RGBLinearizer.transform(this.homogeneousHyperplane, rgb);
		}
		
		@Override
		public final String toString() {
			return this.description;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 6933631179568013440L;
		
	}
	
}
