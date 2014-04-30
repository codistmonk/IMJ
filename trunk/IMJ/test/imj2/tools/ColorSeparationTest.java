package imj2.tools;

import static java.lang.Math.round;
import static net.sourceforge.aprog.swing.SwingTools.horizontalSplit;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.swing.SwingTools.verticalSplit;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.invoke;
import static pixel3d.PolygonTools.X;
import static pixel3d.PolygonTools.Y;
import static pixel3d.PolygonTools.Z;
import imj2.tools.Image2DComponent.Painter;
import imj2.tools.PaletteBasedSegmentationTest.HistogramView;
import imj2.tools.PaletteBasedSegmentationTest.HistogramView.SegmentsUpdatedEvent;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JSplitPane;

import net.sourceforge.aprog.events.EventManager;
import net.sourceforge.aprog.events.EventManager.Event.Listener;
import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.MathTools.Statistics;
import net.sourceforge.aprog.tools.Tools;

import org.junit.Test;
import org.ojalgo.matrix.BasicMatrix;
import org.ojalgo.matrix.MatrixBuilder;
import org.ojalgo.matrix.MatrixUtils;
import org.ojalgo.matrix.PrimitiveMatrix;
import org.ojalgo.matrix.decomposition.QR;
import org.ojalgo.matrix.decomposition.SingularValue;

/**
 * @author codistmonk (creation 2014-04-30)
 */
public final class ColorSeparationTest {
	
	@Test
	public final void test() {
		SwingTools.useSystemLookAndFeel();
		SwingTools.setCheckAWT(false);
		
		final SimpleImageView imageView = new SimpleImageView();
		final HistogramView histogramView = new HistogramView();
		final JComboBox<? extends RGBTransformer> linearizerSelector = new JComboBox<>(Tools.array(RGBTransformer.Predefined.ID));
		final JSplitPane splitPane = horizontalSplit(imageView, verticalSplit(linearizerSelector, histogramView));
		
		SwingTools.setCheckAWT(true);
		
		linearizerSelector.addActionListener(new ActionListener() {
			
			@Override
			public final void actionPerformed(final ActionEvent event) {
				imageView.refreshBuffer();
			}
			
		});
		
		EventManager.getInstance().addListener(histogramView, SegmentsUpdatedEvent.class, new Object() {
			
			@Listener
			public final void segmentsUpdated(final SegmentsUpdatedEvent event) {
				final double[] points = histogramView.getUserPoints();
				final int[] segments = histogramView.getUserSegments();
				final int n = segments.length;
				final MatrixBuilder<Double> matrixBuilder = PrimitiveMatrix.getBuilder(4, n / 2 + 1);
				
				for (int i = 0; i < n; i += 2) {
					final int i1 = (segments[i + 0] - 1) * 3;
					final int i2 = (segments[i + 1] - 1) * 3;
					final double x1 = points[i1 + X];
					final double y1 = points[i1 + Y];
					final double z1 = points[i1 + Z];
					final double x2 = points[i2 + X];
					final double y2 = points[i2 + Y];
					final double z2 = points[i2 + Z];
					final double dx = x2 - x1;
					final double dy = y2 - y1;
					final double dz = z2 - z1;
					
					matrixBuilder.set(0, i / 2, dx);
					matrixBuilder.set(1, i / 2, dy);
					matrixBuilder.set(2, i / 2, dz);
					matrixBuilder.set(3, i / 2, -(x1 * dx + y1 * dy + z1 * dz));
				}
				
				matrixBuilder.set(3, n / 2, 1.0);
				
				final BasicMatrix m = matrixBuilder.build();
				
				try {
					final Method method = PrimitiveMatrix.class.getSuperclass().getDeclaredMethod("getComputedSingularValue");
					
					method.setAccessible(true);
					
					@SuppressWarnings("unchecked")
					final BasicMatrix mi = (BasicMatrix) ((SingularValue<Double>) method.invoke(m)).getInverse();
					@SuppressWarnings("unchecked")
					final DefaultComboBoxModel<RGBTransformer> linearizers = (DefaultComboBoxModel<RGBTransformer>) linearizerSelector.getModel();
					final int selectedIndex = linearizerSelector.getSelectedIndex();
					
					for (int i = 0; i < mi.getRowDim(); ++i) {
						linearizers.insertElementAt(new Linearizer("" + i, row(mi, i)), i + 1);
					}
					
					while (mi.getRowDim() + 1 < linearizers.getSize()) {
						linearizers.removeElementAt(mi.getRowDim() + 1);
					}
					
					linearizerSelector.setSelectedIndex(selectedIndex < linearizers.getSize() ? selectedIndex : 0);
					
					imageView.refreshBuffer();
				} catch (final Exception exception) {
					exception.printStackTrace();
				}
			}
			
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
	public static interface RGBTransformer extends Serializable {
		
		public abstract int transform(int rgb);
		
		/**
		 * @author codistmonk (creation 2014-04-30)
		 */
		public static enum Predefined implements RGBTransformer {
			
			ID {
				
				@Override
				public final int transform(final int rgb) {
					return rgb;
				}
				
			};
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2014-04-30)
	 */
	public static final class Linearizer implements RGBTransformer {
		
		private final String description;
		
		private final double[] hyperplane;
		
		private final Statistics statistics;
		
		public Linearizer(final String description, final double[] hyperplane) {
			this.description = description;
			this.hyperplane = hyperplane;
			this.statistics = new Statistics();
			
			this.statistics.addValue(transform(hyperplane, 0x00000000));
			this.statistics.addValue(transform(hyperplane, 0x000000FF));
			this.statistics.addValue(transform(hyperplane, 0x0000FFFF));
			this.statistics.addValue(transform(hyperplane, 0x0000FF00));
			this.statistics.addValue(transform(hyperplane, 0x00FF0000));
			this.statistics.addValue(transform(hyperplane, 0x00FF00FF));
			this.statistics.addValue(transform(hyperplane, 0x00FFFFFF));
			this.statistics.addValue(transform(hyperplane, 0x00FFFF00));
		}
		
		@Override
		public final int transform(final int rgb) {
			final int gray8 = uint8(round(
					this.statistics.getNormalizedValue(transform(this.hyperplane, rgb)) * 255.0));
			return 0xFF000000 | (gray8 * 0x00010101);
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
		
		public static final int red8(final int rgb) {
			return uint8(rgb >> 16);
		}
		
		public static final int green8(final int rgb) {
			return uint8(rgb >> 8);
		}
		
		public static final int blue8(final int rgb) {
			return uint8(rgb >> 0);
		}
		
		public static final int uint8(final int value) {
			return value & 0xFF;
		}
		
		public static final int uint8(final long value) {
			return (int) (value & 0xFF);
		}
		
	}
	
}
