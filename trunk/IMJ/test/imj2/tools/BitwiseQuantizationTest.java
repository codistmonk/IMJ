package imj2.tools;

import static imj2.tools.BitwiseQuantizationTest.ColorTools1.distance1;
import static imj2.tools.BitwiseQuantizationTest.ColorTools1.distance2;
import static imj2.tools.BitwiseQuantizationTest.ColorTools1.hsvToRGB;
import static imj2.tools.BitwiseQuantizationTest.ColorTools1.min;
import static imj2.tools.BitwiseQuantizationTest.ColorTools1.quantize;
import static imj2.tools.BitwiseQuantizationTest.ColorTools1.rgbToHSV;
import static imj2.tools.BitwiseQuantizationTest.ColorTools1.rgbToRGB;
import static imj2.tools.BitwiseQuantizationTest.ColorTools2.rgbToXYZ;
import static imj2.tools.BitwiseQuantizationTest.ColorTools2.xyzToCIELAB;
import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.debug;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.instances;
import imj2.tools.Image2DComponent.Painter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jgencode.primitivelists.IntList;
import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.Canvas;
import net.sourceforge.aprog.tools.Factory.DefaultFactory;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.MathTools.Statistics;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-04-09)
 */
@SuppressWarnings("unchecked")
public final class BitwiseQuantizationTest {
	
//	@Test
	public final void test1() {
		debugPrint();
		
		final TreeMap<double[], String> lines = new TreeMap<double[], String>(DoubleArrayComparator.INSTANCE);
		
		for (int qR0 = 0; qR0 <= 7; ++qR0) {
			final int qR = qR0;
			
			for (int qG0 = 0; qG0 <= 7; ++qG0) {
				final int qG = qG0;
				
				for (int qB0 = 0; qB0 <= 7; ++qB0) {
					final int qB = qB0;
					
					MultiThreadTools.getExecutor().submit(new Runnable() {
						
						@Override
						public final void run() {
							final int[] rgb = new int[3];
							final int[] qRGB = rgb.clone();
							final float[] xyz = new float[3];
							final float[] cielab = new float[3];
							final float[] qCIELAB = cielab.clone();
							final Statistics error = new Statistics();
							
							for (int color = 0; color <= 0x00FFFFFF; ++color) {
								rgbToRGB(color, rgb);
								
								rgbToXYZ(rgb, xyz);
								xyzToCIELAB(xyz, cielab);
								
								quantize(rgb, qR, qG, qB, qRGB);
								rgbToXYZ(qRGB, xyz);
								xyzToCIELAB(xyz, qCIELAB);
								
								error.addValue(distance2(cielab, qCIELAB));
							}
							
							final double[] key = { qR + qG + qB, error.getMean() };
							final String line = "qRGB: " + qR + " " + qG + " " + qB + " " + ((qR + qG + qB)) +
									" error: " + error.getMinimum() + " <= " + error.getMean() +
									" ( " + sqrt(error.getVariance()) + " ) <= " + error.getMaximum();
							
							synchronized (lines) {
								lines.put(key, line);
								System.out.println(line);
							}
						}
						
					});
				}
			}
		}
		
		for (int qH0 = 0; qH0 <= 7; ++qH0) {
			final int qH = qH0;
			
			for (int qS0 = 0; qS0 <= 7; ++qS0) {
				final int qS = qS0;
				
				for (int qV0 = 0; qV0 <= 7; ++qV0) {
					final int qV = qV0;
					
					MultiThreadTools.getExecutor().submit(new Runnable() {
						
						@Override
						public final void run() {
							final int[] rgb = new int[3];
							final int[] qRGB = rgb.clone();
							final float[] xyz = new float[3];
							final float[] cielab = new float[3];
							final float[] qCIELAB = cielab.clone();
							final int[] hsv = new int[3];
							final int[] qHSV = hsv.clone();
							final Statistics error = new Statistics();
							
							for (int color = 0; color <= 0x00FFFFFF; ++color) {
								rgbToRGB(color, rgb);
								
								rgbToXYZ(rgb, xyz);
								xyzToCIELAB(xyz, cielab);
								
								rgbToHSV(rgb, hsv);
								quantize(hsv, qH, qS, qV, qHSV);
								hsvToRGB(qHSV, qRGB);
								rgbToXYZ(qRGB, xyz);
								xyzToCIELAB(xyz, qCIELAB);
								
								error.addValue(distance2(cielab, qCIELAB));
							}
							
							final double[] key = { qH + qS + qV, error.getMean() };
							final String line = "qHSV: " + qH + " " + qS + " " + qV + " " + ((qH + qS + qV)) +
									" error: " + error.getMinimum() + " <= " + error.getMean() +
									" ( " + sqrt(error.getVariance()) + " ) <= " + error.getMaximum();
							
							synchronized (lines) {
								lines.put(key, line);
								System.out.println(line);
							}
						}
						
					});
				}
			}
		}
		
		shutdownAndWait(MultiThreadTools.getExecutor(), Long.MAX_VALUE);
		
		System.out.println();
		
		for (final String line : lines.values()) {
			System.out.println(line);
		}
	}
	
	@Test
	public final void test2() {
		final Color contourColor = Color.GREEN;
		
		debugPrint(quantizers);
		
		final SimpleImageView imageView = new SimpleImageView();
		final JSpinner mergeSpinner = new JSpinner(new SpinnerNumberModel(80, 0, 1000, 10));
		final JComboBox<String> qTypeSelector = new JComboBox<String>(array(RGBQuantizer.TYPE, HSVQuantizer.TYPE, XYZQuantizer.TYPE, CIELABQuantizer.TYPE));
		final JSpinner qASpinner = new JSpinner(new SpinnerNumberModel(0, 0, 7, 1));
		final JSpinner qBSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 7, 1));
		final JSpinner qCSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 7, 1));
		final JCheckBox showQuantizationCheckBox = new JCheckBox("Show quantization", false);
//		final JSpinner spinner = new JSpinner(new SpinnerNumberModel(12, 0, quantizers.size() - 1, 1));
		
		imageView.getPainters().add(new Painter<SimpleImageView>() {
			
			private Canvas labels = new Canvas();
			
			@Override
			public final void paint(final Graphics2D g, final SimpleImageView component,
					final int width, final int height) {
				final TicToc timer = new TicToc();
				
				debugPrint("Quantization started...", new Date(timer.tic()));
				
//				final ColorQuantizer quantizer = quantizers.get(((Number) spinner.getValue()).intValue());
				final ColorQuantizer quantizer = ColorQuantizer.newQuantizer(qTypeSelector.getSelectedItem().toString(),
						((Number) qASpinner.getValue()).intValue(), ((Number) qBSpinner.getValue()).intValue(), ((Number) qCSpinner.getValue()).intValue());
				final BufferedImage image = imageView.getImage();
				final BufferedImage buffer = imageView.getBufferImage();
				final int w = buffer.getWidth();
				final int h = buffer.getHeight();
				final int minimumComponentSize = ((Number) mergeSpinner.getValue()).intValue();
				final boolean showQuantization = showQuantizationCheckBox.isSelected();
				
				this.labels.setFormat(w, h, BufferedImage.TYPE_3BYTE_BGR);
				
				for (int y = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x) {
						final int rgb = image.getRGB(x, y);
						
						this.labels.getImage().setRGB(x, y, quantizer.pack(rgb));
						
						if (showQuantization) {
							imageView.getBufferImage().setRGB(x, y, quantizer.quantize(rgb));
						}
					}
				}
				
				debugPrint("Quantization done in", timer.toc(), "ms");
				debugPrint("Labeling started...", new Date(timer.tic()));
				
				final SchedulingData schedulingData = new SchedulingData();
				int totalPixelCount = 0;
				final IntList small = new IntList();
				
				for (int y = 0, pixel = 0, labelId = 0xFF000000; y < h; ++y) {
					for (int x = 0; x < w; ++x, ++pixel) {
						if (!schedulingData.getDone().get(pixel)) {
							schedulingData.getTodo().add(pixel);
							
							final int rgb = this.labels.getImage().getRGB(x, y);
							
							for (int i = 0; i < schedulingData.getTodo().size(); ++i) {
								final int p = schedulingData.getTodo().get(i);
								
								this.labels.getImage().setRGB(p % w, p / w, labelId);
								this.process(w, h, schedulingData, p % w, p / w, p, rgb);
							}
							
							final int componentPixelCount = schedulingData.getTodo().size();
							
							++labelId;
							totalPixelCount += componentPixelCount;
							
							if (componentPixelCount < minimumComponentSize) {
								small.add(pixel);
							}
							
							schedulingData.getTodo().clear();
						}
					}
				}
				
				schedulingData.getDone().clear();
				
				if (w * h != totalPixelCount) {
					System.err.println(debug(Tools.DEBUG_STACK_OFFSET, "Error:", "expected:", w * h, "actual:", totalPixelCount));
				}
				
				debugPrint("Labeling done in", timer.toc(), "ms");
				debugPrint("Merging started...", new Date(timer.tic()));
				
				final TicToc progressTimer = new TicToc();
				
				progressTimer.tic();
				
				while (!small.isEmpty()) {
					if (5000L <= progressTimer.toc()) {
						debugPrint(small.size());
						progressTimer.tic();
					}
					
					final int pixel = small.remove(0);
					final int x = pixel % w;
					final int y = pixel / w;
					final int labelId = this.labels.getImage().getRGB(x, y);
					
					if (!schedulingData.getDone().get(pixel)) {
						schedulingData.getTodo().add(pixel);
						
						for (int i = 0; i < schedulingData.getTodo().size(); ++i) {
							final int p = schedulingData.getTodo().get(i);
							
							this.process(w, h, schedulingData, p % w, p / w, p, labelId);
						}
						
						if (schedulingData.getTodo().size() < minimumComponentSize) {
							final IntList neighborLabels = new IntList();
							final IntList neighborRGBs = new IntList();
							
							for (int i = 0; i < schedulingData.getTodo().size(); ++i) {
								final int p = schedulingData.getTodo().get(i);
								final int xx = p % w;
								final int yy = p / w;
								
								if (0 < yy) {
									final int neighborLabel = this.labels.getImage().getRGB(xx, yy - 1);
									
									if (neighborLabel != labelId) {
										neighborLabels.add(neighborLabel);
										neighborRGBs.add(image.getRGB(xx, yy - 1));
									}
								}
								
								if (0 < xx) {
									final int neighborLabel = this.labels.getImage().getRGB(xx - 1, yy);
									
									if (neighborLabel != labelId) {
										neighborLabels.add(neighborLabel);
										neighborRGBs.add(image.getRGB(xx - 1, yy));
									}
								}
								
								if (xx + 1 < w) {
									final int neighborLabel = this.labels.getImage().getRGB(xx + 1, yy);
									
									if (neighborLabel != labelId) {
										neighborLabels.add(neighborLabel);
										neighborRGBs.add(image.getRGB(xx + 1, yy));
									}
								}
								
								if (yy + 1 < h) {
									final int neighborLabel = this.labels.getImage().getRGB(xx, yy + 1);
									
									if (neighborLabel != labelId) {
										neighborLabels.add(neighborLabel);
										neighborRGBs.add(image.getRGB(xx, yy + 1));
									}
								}
							}
							
							int neighborLabel = -1;
							int closestNeighborColorDistance = Integer.MAX_VALUE;
							final int[] rgb = rgbToRGB(image.getRGB(x, y), new int[3]);
							final int[] neighborRGB = new int[3];
							
							for (int i = 0; i < neighborLabels.size(); ++i) {
								rgbToRGB(neighborRGBs.get(i), neighborRGB);
								
								final int d = distance1(rgb, neighborRGB);
								
								if (d < closestNeighborColorDistance) {
									closestNeighborColorDistance = d;
									neighborLabel = neighborLabels.get(i);
								}
							}
							
							for (int i = 0; i < schedulingData.getTodo().size(); ++i) {
								final int p = schedulingData.getTodo().get(i);
								final int xx = p % w;
								final int yy = p / w;
								
								this.labels.getImage().setRGB(xx, yy, neighborLabel);
							}
						}
						
						schedulingData.getDone().clear();
						schedulingData.getTodo().clear();
					}
				}
				
				debugPrint("Merging done in", timer.toc(), "ms");
				debugPrint("Rendering started...", new Date(timer.tic()));
				
				for (int y = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x) {
						int north = 0;
						int west = 0;
						int east = 0;
						int south = 0;
						
						if (0 < y) {
							north = this.labels.getImage().getRGB(x, y - 1);
						}
						
						if (0 < x) {
							west = this.labels.getImage().getRGB(x - 1, y);
						}
						
						if (x + 1 < w) {
							east = this.labels.getImage().getRGB(x + 1, y);
						}
						
						if (y + 1 < h) {
							south = this.labels.getImage().getRGB(x, y + 1);
						}
						
						final int center = this.labels.getImage().getRGB(x, y);
						
//						buffer.setRGB(x, y, center << 2);
						
						if (min(north, west, east, south) < center) {
							buffer.setRGB(x, y, contourColor.getRGB());
						}
					}
				}
				
				debugPrint("Rendering done in", timer.toc(), "ms");
				debugPrint("All done in", timer.getTotalTime(), "ms");
			}
			
			private final void process(final int w, final int h,
					final SchedulingData schedulingData, final int x, final int y,
					final int pixel, final int labelRGB) {
				schedulingData.getDone().set(pixel);
				
				if (0 < y && this.labels.getImage().getRGB(x, y - 1) == labelRGB) {
					final int neighbor = pixel - w;
					
					if (!schedulingData.getDone().get(neighbor)) {
						schedulingData.getDone().set(neighbor);
						schedulingData.getTodo().add(neighbor);
					}
				}
				
				if (0 < x && this.labels.getImage().getRGB(x - 1, y) == labelRGB) {
					final int neighbor = pixel - 1;
					
					if (!schedulingData.getDone().get(neighbor)) {
						schedulingData.getDone().set(neighbor);
						schedulingData.getTodo().add(neighbor);
					}
				}
				
				if (x + 1 < w && this.labels.getImage().getRGB(x + 1, y) == labelRGB) {
					final int neighbor = pixel + 1;
					
					if (!schedulingData.getDone().get(neighbor)) {
						schedulingData.getDone().set(neighbor);
						schedulingData.getTodo().add(neighbor);
					}
				}
				
				if (y + 1 < h && this.labels.getImage().getRGB(x, y + 1) == labelRGB) {
					final int neighbor = pixel + w;
					
					if (!schedulingData.getDone().get(neighbor)) {
						schedulingData.getDone().set(neighbor);
						schedulingData.getTodo().add(neighbor);
					}
				}
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 8306989611117085093L;
			
		});
		
		final JPanel panel = new JPanel(new BorderLayout());
		
		SwingTools.setCheckAWT(false);
		
		final Updater updater = new Updater(imageView);
		
		mergeSpinner.addChangeListener(updater);
		qTypeSelector.addActionListener(updater);
		qASpinner.addChangeListener(updater);
		qBSpinner.addChangeListener(updater);
		qCSpinner.addChangeListener(updater);
//		spinner.addChangeListener(updater);
		
		showQuantizationCheckBox.addActionListener(updater);
		
		panel.add(horizontalBox(
				new JLabel("mergeIf<"), mergeSpinner,
				new JLabel("qType:"), qTypeSelector, new JLabel("qA:"), qASpinner,
				new JLabel("qB:"), qBSpinner, new JLabel("qC:"), qCSpinner/*, new JLabel("q:"), spinner*/,
				showQuantizationCheckBox), BorderLayout.NORTH);
		panel.add(imageView, BorderLayout.CENTER);
		
		show(panel, this.getClass().getSimpleName(), true);
	}
	
	/**
	 * @author codistmonk (creation 2014-04-25)
	 */
	public static final class Updater implements ChangeListener, ActionListener, Serializable {
		
		private final SimpleImageView imageView;
		
		public Updater(final SimpleImageView imageView) {
			this.imageView = imageView;
		}
		
		@Override
		public final void actionPerformed(final ActionEvent event) {
			this.imageView.refreshBuffer();
		}
		
		@Override
		public final void stateChanged(final ChangeEvent event) {
			this.imageView.refreshBuffer();
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 3336259394282831156L;
		
	}
	
	private static final List<Object[]> qTable = new ArrayList<Object[]>();
	
	static final List<ColorQuantizer> quantizers = new ArrayList<ColorQuantizer>();
	
	static final Statistics[] cielabStatistics = instances(3, DefaultFactory.forClass(Statistics.class));
	
	static final Statistics[] xyzStatistics = instances(3, DefaultFactory.forClass(Statistics.class));
	
	static {
		{
			debugPrint("Computing RGB -> CIE-L*a*b* (D65, 2°) bounds...");
			
			if (false) {
				final int[] rgb = new int[3];
				final float[] cielab = new float[3];
				
				for (int color = 0; color <= 0x00FFFFFF; ++color) {
					if ((color % 200000) == 0) {
						debugPrint(Integer.toHexString(color));
					}
					
					ColorTools2.rgbToCIELAB(rgbToRGB(color, rgb), cielab);
					
					cielabStatistics[0].addValue(cielab[0]);
					cielabStatistics[1].addValue(cielab[1]);
					cielabStatistics[2].addValue(cielab[2]);
				}
			} else {
				cielabStatistics[0].addValue(0.0);
				cielabStatistics[0].addValue(100.0);
				cielabStatistics[1].addValue(-86.18462371826172);
				cielabStatistics[1].addValue(98.25423431396484);
				cielabStatistics[2].addValue(-107.86367797851562);
				cielabStatistics[2].addValue(94.48248291015625);
			}
			
			debugPrint("L*:", cielabStatistics[0].getMinimum(), cielabStatistics[0].getMaximum());
			debugPrint("a*:", cielabStatistics[1].getMinimum(), cielabStatistics[1].getMaximum());
			debugPrint("b*:", cielabStatistics[2].getMinimum(), cielabStatistics[2].getMaximum());
		}
		
		{
			debugPrint("Computing RGB -> XYZ (D65, 2°) bounds...");
			
			if (false) {
				final int[] rgb = new int[3];
				final float[] xyz = new float[3];
				
				for (int color = 0; color <= 0x00FFFFFF; ++color) {
					if ((color % 200000) == 0) {
						debugPrint(Integer.toHexString(color));
					}
					
					ColorTools2.rgbToXYZ(rgbToRGB(color, rgb), xyz);
					
					xyzStatistics[0].addValue(xyz[0]);
					xyzStatistics[1].addValue(xyz[1]);
					xyzStatistics[2].addValue(xyz[2]);
				}
			} else {
				xyzStatistics[0].addValue(0.0);
				xyzStatistics[0].addValue(95.05000305175781);
				xyzStatistics[1].addValue(0.0);
				xyzStatistics[1].addValue(100.0);
				xyzStatistics[2].addValue(0.0);
				xyzStatistics[2].addValue(108.9000015258789);
			}
			
			debugPrint("X:", xyzStatistics[0].getMinimum(), xyzStatistics[0].getMaximum());
			debugPrint("Y:", xyzStatistics[1].getMinimum(), xyzStatistics[1].getMaximum());
			debugPrint("Z:", xyzStatistics[2].getMinimum(), xyzStatistics[2].getMaximum());
		}
		
		debugPrint("Initializing quantizers...");
		
		qTable.add(array("qRGB", 0, 0, 0, 0.0));
		qTable.add(array("qHSV", 0, 0, 0, 0.6955260904279362));
		qTable.add(array("qRGB", 1, 0, 0, 0.32641454294213));
		qTable.add(array("qRGB", 0, 0, 1, 0.6240309742241252));
		qTable.add(array("qRGB", 0, 1, 0, 0.7514458393503941));
		qTable.add(array("qHSV", 0, 0, 1, 0.8751917410004951));
		qTable.add(array("qHSV", 0, 1, 0, 1.126961289651885));
		qTable.add(array("qHSV", 1, 0, 0, 1.7813204349626734));
		qTable.add(array("qRGB", 1, 0, 1, 0.8583117538819247));
		qTable.add(array("qRGB", 1, 1, 0, 0.8692188281204635));
		qTable.add(array("qRGB", 2, 0, 0, 0.9808307435374101));
		qTable.add(array("qRGB", 0, 1, 1, 1.0380778379038296));
		qTable.add(array("qHSV", 0, 0, 2, 1.1659382941554577));
		qTable.add(array("qHSV", 0, 1, 1, 1.2559919477363735));
		qTable.add(array("qHSV", 1, 0, 1, 1.8951245566432926));
		qTable.add(array("qRGB", 0, 0, 2, 1.8980446031157745));
		qTable.add(array("qHSV", 1, 1, 0, 2.092400382244634));
		qTable.add(array("qHSV", 0, 2, 0, 2.115374741291907));
		qTable.add(array("qRGB", 0, 2, 0, 2.265005588909907));
		qTable.add(array("qHSV", 2, 0, 0, 4.528551120210534));
		qTable.add(array("qRGB", 1, 1, 1, 1.1180287803753424));
		qTable.add(array("qRGB", 2, 1, 0, 1.2748883526552783));
		qTable.add(array("qRGB", 2, 0, 1, 1.4067111100652796));
		qTable.add(array("qHSV", 0, 1, 2, 1.5203314800623147));
		qTable.add(array("qHSV", 0, 0, 3, 1.809747264609731));
		qTable.add(array("qRGB", 0, 1, 2, 1.9618195348396559));
		qTable.add(array("qRGB", 1, 0, 2, 2.064893103362241));
		qTable.add(array("qHSV", 1, 0, 2, 2.1016815466827374));
		qTable.add(array("qHSV", 1, 1, 1, 2.177229033053775));
		qTable.add(array("qHSV", 0, 2, 1, 2.2173938408647693));
		qTable.add(array("qRGB", 1, 2, 0, 2.2504013406529717));
		qTable.add(array("qRGB", 0, 2, 1, 2.2932977370369603));
		qTable.add(array("qRGB", 3, 0, 0, 2.2959598157696686));
		qTable.add(array("qHSV", 1, 2, 0, 2.8907950047008057));
		qTable.add(array("qHSV", 0, 3, 0, 4.270360710208371));
		qTable.add(array("qRGB", 0, 0, 3, 4.52553090293637));
		qTable.add(array("qHSV", 2, 0, 1, 4.592369919131593));
		qTable.add(array("qHSV", 2, 1, 0, 4.722213766837409));
		qTable.add(array("qRGB", 0, 3, 0, 5.333067162914784));
		qTable.add(array("qHSV", 3, 0, 0, 10.133061431545565));
		qTable.add(array("qRGB", 2, 1, 1, 1.4880175209433724));
		qTable.add(array("qRGB", 1, 1, 2, 2.0282165668025764));
		qTable.add(array("qHSV", 0, 1, 3, 2.152929318600153));
		qTable.add(array("qRGB", 1, 2, 1, 2.2559129235446513));
		qTable.add(array("qRGB", 3, 1, 0, 2.370351273948446));
		qTable.add(array("qHSV", 1, 1, 2, 2.3731733681910288));
		qTable.add(array("qRGB", 2, 2, 0, 2.389170595189107));
		qTable.add(array("qHSV", 0, 2, 2, 2.450676449335271));
		qTable.add(array("qRGB", 2, 0, 2, 2.4962070471616475));
		qTable.add(array("qHSV", 1, 0, 3, 2.614980863199656));
		qTable.add(array("qRGB", 3, 0, 1, 2.61968019557456));
		qTable.add(array("qRGB", 0, 2, 2, 2.775659263227969));
		qTable.add(array("qHSV", 1, 2, 1, 2.9645149300940132));
		qTable.add(array("qHSV", 0, 0, 4, 3.308055191838597));
		qTable.add(array("qRGB", 0, 1, 3, 4.3145002832409265));
		qTable.add(array("qHSV", 0, 3, 1, 4.356094355535418));
		qTable.add(array("qRGB", 1, 0, 3, 4.6480678384750584));
		qTable.add(array("qHSV", 2, 0, 2, 4.720369162242616));
		qTable.add(array("qHSV", 2, 1, 1, 4.771642243384687));
		qTable.add(array("qHSV", 1, 3, 0, 4.820816066888919));
		qTable.add(array("qRGB", 4, 0, 0, 4.951001286754124));
		qTable.add(array("qRGB", 0, 3, 1, 5.179776824191323));
		qTable.add(array("qRGB", 1, 3, 0, 5.245476477957572));
		qTable.add(array("qHSV", 2, 2, 0, 5.275040700873244));
		qTable.add(array("qHSV", 0, 4, 0, 8.608606861303441));
		qTable.add(array("qRGB", 0, 0, 4, 10.005984804008115));
		qTable.add(array("qHSV", 3, 0, 1, 10.16591826453798));
		qTable.add(array("qHSV", 3, 1, 0, 10.246741129698233));
		qTable.add(array("qRGB", 0, 4, 0, 11.621132862924227));
		qTable.add(array("qHSV", 4, 0, 0, 21.441441059860917));
		qTable.add(array("qRGB", 2, 1, 2, 2.345664153459157));
		qTable.add(array("qRGB", 2, 2, 1, 2.3803760800798126));
		qTable.add(array("qRGB", 3, 1, 1, 2.548324173801728));
		qTable.add(array("qRGB", 1, 2, 2, 2.7386225128738215));
		qTable.add(array("qHSV", 1, 1, 3, 2.890959844279944));
		qTable.add(array("qHSV", 0, 2, 3, 3.039789316612684));
		qTable.add(array("qRGB", 3, 2, 0, 3.0876627555063707));
		qTable.add(array("qHSV", 1, 2, 2, 3.149855857626206));
		qTable.add(array("qRGB", 3, 0, 2, 3.5456082043397177));
		qTable.add(array("qHSV", 0, 1, 4, 3.6408406868479344));
		qTable.add(array("qHSV", 1, 0, 4, 3.9301965018206424));
		qTable.add(array("qRGB", 1, 1, 3, 4.377510140614242));
		qTable.add(array("qRGB", 0, 2, 3, 4.55417176057843));
		qTable.add(array("qHSV", 0, 3, 2, 4.558363544642229));
		qTable.add(array("qRGB", 4, 1, 0, 4.866263779024119));
		qTable.add(array("qHSV", 1, 3, 1, 4.890021661874276));
		qTable.add(array("qHSV", 2, 1, 2, 4.896690972204208));
		qTable.add(array("qRGB", 2, 0, 3, 4.975510162691538));
		qTable.add(array("qHSV", 2, 0, 3, 5.073700328823798));
		qTable.add(array("qRGB", 1, 3, 1, 5.076416388035941));
		qTable.add(array("qRGB", 2, 3, 0, 5.186407432321236));
		qTable.add(array("qRGB", 4, 0, 1, 5.18922966136709));
		qTable.add(array("qRGB", 0, 3, 2, 5.253668905846729));
		qTable.add(array("qHSV", 2, 2, 1, 5.321301853278064));
		qTable.add(array("qHSV", 2, 3, 0, 6.78099035432321));
		qTable.add(array("qHSV", 0, 0, 5, 6.8001379002246205));
		qTable.add(array("qHSV", 0, 4, 1, 8.6804212735382));
		qTable.add(array("qHSV", 1, 4, 0, 8.96150522653321));
		qTable.add(array("qRGB", 0, 1, 4, 9.621269654573805));
		qTable.add(array("qRGB", 1, 0, 4, 10.10082618502018));
		qTable.add(array("qHSV", 3, 0, 2, 10.239604490826345));
		qTable.add(array("qHSV", 3, 1, 1, 10.273738901017087));
		qTable.add(array("qRGB", 5, 0, 0, 10.357214019008278));
		qTable.add(array("qHSV", 3, 2, 0, 10.595095232353103));
		qTable.add(array("qRGB", 0, 4, 1, 11.345079146799256));
		qTable.add(array("qRGB", 1, 4, 0, 11.499694035514397));
		qTable.add(array("qHSV", 0, 5, 0, 16.979860091230222));
		qTable.add(array("qHSV", 4, 0, 1, 21.447935998355156));
		qTable.add(array("qHSV", 4, 1, 0, 21.462964882808116));
		qTable.add(array("qRGB", 0, 0, 5, 21.56144319342579));
		qTable.add(array("qRGB", 0, 5, 0, 24.732918303318232));
		qTable.add(array("qHSV", 5, 0, 0, 43.77691374160295));
		qTable.add(array("qRGB", 2, 2, 2, 2.852922516764376));
		qTable.add(array("qRGB", 3, 2, 1, 3.0828291421052723));
		qTable.add(array("qRGB", 3, 1, 2, 3.3041165241821764));
		qTable.add(array("qHSV", 1, 2, 3, 3.6541400074569506));
		qTable.add(array("qHSV", 1, 1, 4, 4.215862457035969));
		qTable.add(array("qHSV", 0, 2, 4, 4.460470627495945));
		qTable.add(array("qRGB", 1, 2, 3, 4.539080994752359));
		qTable.add(array("qRGB", 2, 1, 3, 4.6363242231465));
		qTable.add(array("qRGB", 2, 3, 1, 5.004329263328033));
		qTable.add(array("qRGB", 4, 1, 1, 5.011027074098335));
		qTable.add(array("qHSV", 0, 3, 3, 5.062496531035296));
		qTable.add(array("qHSV", 1, 3, 2, 5.063458476675465));
		qTable.add(array("qRGB", 1, 3, 2, 5.147515511354933));
		qTable.add(array("qRGB", 4, 2, 0, 5.167587135912787));
		qTable.add(array("qHSV", 2, 1, 3, 5.260392593892533));
		qTable.add(array("qRGB", 3, 3, 0, 5.432007270000069));
		qTable.add(array("qHSV", 2, 2, 2, 5.446669044137193));
		qTable.add(array("qRGB", 3, 0, 3, 5.825331577595821));
		qTable.add(array("qRGB", 4, 0, 2, 5.931000598479003));
		qTable.add(array("qHSV", 2, 0, 4, 6.082311495314012));
		qTable.add(array("qRGB", 0, 3, 3, 6.254886430030773));
		qTable.add(array("qHSV", 2, 3, 1, 6.830073389040216));
		qTable.add(array("qHSV", 0, 1, 5, 7.122838491638112));
		qTable.add(array("qHSV", 1, 0, 5, 7.218793962972602));
		qTable.add(array("qHSV", 0, 4, 2, 8.848843990753025));
		qTable.add(array("qHSV", 1, 4, 1, 9.024514418743152));
		qTable.add(array("qRGB", 0, 2, 4, 9.322599398655775));
		qTable.add(array("qRGB", 1, 1, 4, 9.682368743150985));
		qTable.add(array("qRGB", 5, 1, 0, 10.172020644792356));
		qTable.add(array("qHSV", 3, 1, 2, 10.346634555923645));
		qTable.add(array("qRGB", 2, 0, 4, 10.349131860405612));
		qTable.add(array("qHSV", 2, 4, 0, 10.378143392591879));
		qTable.add(array("qHSV", 3, 0, 3, 10.46174533248007));
		qTable.add(array("qRGB", 5, 0, 1, 10.533497144893856));
		qTable.add(array("qHSV", 3, 2, 1, 10.62143108459401));
		qTable.add(array("qRGB", 0, 4, 2, 11.08684873356561));
		qTable.add(array("qRGB", 1, 4, 1, 11.212816854561149));
		qTable.add(array("qRGB", 2, 4, 0, 11.32752806549405));
		qTable.add(array("qHSV", 3, 3, 0, 11.635117350399002));
		qTable.add(array("qHSV", 0, 0, 6, 15.668032586962413));
		qTable.add(array("qHSV", 0, 5, 1, 17.04482075137555));
		qTable.add(array("qHSV", 1, 5, 0, 17.187108424535914));
		qTable.add(array("qRGB", 0, 1, 5, 21.08512918203094));
		qTable.add(array("qHSV", 4, 1, 1, 21.469367174690095));
		qTable.add(array("qHSV", 4, 0, 2, 21.473931969823585));
		qTable.add(array("qRGB", 6, 0, 0, 21.532049376223195));
		qTable.add(array("qHSV", 4, 2, 0, 21.59445165396762));
		qTable.add(array("qRGB", 1, 0, 5, 21.638632549717023));
		qTable.add(array("qRGB", 0, 5, 1, 24.37721172994415));
		qTable.add(array("qRGB", 1, 5, 0, 24.60734851909329));
		qTable.add(array("qHSV", 0, 6, 0, 32.73574351446413));
		qTable.add(array("qHSV", 5, 1, 0, 43.751204156283656));
		qTable.add(array("qHSV", 5, 0, 1, 43.76416717883281));
		qTable.add(array("qRGB", 0, 0, 6, 46.13239365025457));
		qTable.add(array("qRGB", 0, 6, 0, 52.70802175522528));
		qTable.add(array("qHSV", 6, 0, 0, 83.81508873246744));
		qTable.add(array("qRGB", 3, 2, 2, 3.527362691503131));
		qTable.add(array("qRGB", 2, 2, 3, 4.653057616366176));
		qTable.add(array("qHSV", 1, 2, 4, 4.950383818907108));
		qTable.add(array("qRGB", 2, 3, 2, 5.068365988414407));
		qTable.add(array("qRGB", 4, 2, 1, 5.178832026758057));
		qTable.add(array("qRGB", 3, 3, 1, 5.253549057030269));
		qTable.add(array("qRGB", 3, 1, 3, 5.433232494141032));
		qTable.add(array("qHSV", 1, 3, 3, 5.516563492291423));
		qTable.add(array("qRGB", 4, 1, 2, 5.635111396603524));
		qTable.add(array("qHSV", 2, 2, 3, 5.816758192084616));
		qTable.add(array("qRGB", 1, 3, 3, 6.166399561123543));
		qTable.add(array("qHSV", 2, 1, 4, 6.289879138851597));
		qTable.add(array("qHSV", 0, 3, 4, 6.345495263856095));
		qTable.add(array("qRGB", 4, 3, 0, 6.783776610747946));
		qTable.add(array("qHSV", 2, 3, 2, 6.958393497024418));
		qTable.add(array("qHSV", 1, 1, 5, 7.514279390344463));
		qTable.add(array("qHSV", 0, 2, 5, 7.850523125363171));
		qTable.add(array("qRGB", 4, 0, 3, 7.909364447767676));
		qTable.add(array("qHSV", 2, 0, 5, 8.893228803404542));
		qTable.add(array("qHSV", 1, 4, 2, 9.177754256712644));
		qTable.add(array("qHSV", 0, 4, 3, 9.270509807166043));
		qTable.add(array("qRGB", 1, 2, 4, 9.33366729062409));
		qTable.add(array("qRGB", 2, 1, 4, 9.890118915323102));
		qTable.add(array("qRGB", 0, 3, 4, 9.929929778266683));
		qTable.add(array("qRGB", 5, 2, 0, 10.138080062468157));
		qTable.add(array("qRGB", 5, 1, 1, 10.29114858366));
		qTable.add(array("qHSV", 2, 4, 1, 10.428669001108311));
		qTable.add(array("qHSV", 3, 1, 3, 10.577000076352872));
		qTable.add(array("qHSV", 3, 2, 2, 10.69795588995799));
		qTable.add(array("qRGB", 1, 4, 2, 10.949224161393213));
		qTable.add(array("qRGB", 3, 0, 4, 11.00602110019438));
		qTable.add(array("qRGB", 2, 4, 1, 11.02877492642026));
		qTable.add(array("qRGB", 5, 0, 2, 11.102983007881091));
		qTable.add(array("qHSV", 3, 0, 4, 11.158505372879214));
		qTable.add(array("qRGB", 3, 4, 0, 11.227760965671465));
		qTable.add(array("qRGB", 0, 4, 3, 11.309405765689151));
		qTable.add(array("qHSV", 3, 3, 1, 11.664870489106614));
		qTable.add(array("qHSV", 3, 4, 0, 14.395372498170593));
		qTable.add(array("qHSV", 1, 0, 6, 15.901936420764862));
		qTable.add(array("qHSV", 0, 1, 6, 15.991471787000199));
		qTable.add(array("qHSV", 0, 5, 2, 17.176936549609696));
		qTable.add(array("qHSV", 1, 5, 1, 17.247695065696494));
		qTable.add(array("qHSV", 2, 5, 0, 18.082575505826718));
		qTable.add(array("qRGB", 0, 2, 5, 20.405035493287702));
		qTable.add(array("qRGB", 1, 1, 5, 21.14347635507767));
		qTable.add(array("qRGB", 6, 1, 0, 21.293096999751032));
		qTable.add(array("qHSV", 4, 1, 2, 21.496241062179358));
		qTable.add(array("qHSV", 4, 0, 3, 21.580207287110984));
		qTable.add(array("qHSV", 4, 2, 1, 21.602329841806487));
		qTable.add(array("qRGB", 6, 0, 1, 21.67462497600947));
		qTable.add(array("qRGB", 2, 0, 5, 21.830723642418008));
		qTable.add(array("qHSV", 4, 3, 0, 22.133768474410587));
		qTable.add(array("qRGB", 0, 5, 2, 23.86359478382961));
		qTable.add(array("qRGB", 1, 5, 1, 24.244431205525952));
		qTable.add(array("qRGB", 2, 5, 0, 24.39691267076655));
		qTable.add(array("qHSV", 0, 6, 1, 32.80316538441022));
		qTable.add(array("qHSV", 1, 6, 0, 32.842233350233535));
		qTable.add(array("qHSV", 5, 2, 0, 43.74076975658067));
		qTable.add(array("qHSV", 5, 1, 1, 43.742010947423));
		qTable.add(array("qHSV", 5, 0, 2, 43.7562916943628));
		qTable.add(array("qHSV", 0, 0, 7, 44.92160259247629));
		qTable.add(array("qRGB", 7, 0, 0, 45.176087377745176));
		qTable.add(array("qRGB", 0, 1, 6, 45.62356017773505));
		qTable.add(array("qRGB", 1, 0, 6, 46.196411417482125));
		qTable.add(array("qRGB", 0, 6, 1, 52.301160399275815));
		qTable.add(array("qRGB", 1, 6, 0, 52.61193218654751));
		qTable.add(array("qHSV", 0, 7, 0, 61.80730533010471));
		qTable.add(array("qHSV", 6, 1, 0, 83.63587197808127));
		qTable.add(array("qHSV", 6, 0, 1, 83.76551042010584));
		qTable.add(array("qRGB", 0, 0, 7, 98.58169362835481));
		qTable.add(array("qRGB", 0, 7, 0, 113.80279045303948));
		qTable.add(array("qHSV", 7, 0, 0, 156.02265105719644));
		qTable.add(array("qRGB", 3, 2, 3, 5.253916642742753));
		qTable.add(array("qRGB", 3, 3, 2, 5.313590650823462));
		qTable.add(array("qRGB", 4, 2, 2, 5.575899042570833));
		qTable.add(array("qRGB", 2, 3, 3, 6.102583333976196));
		qTable.add(array("qRGB", 4, 3, 1, 6.637861657250586));
		qTable.add(array("qHSV", 1, 3, 4, 6.720167065205978));
		qTable.add(array("qHSV", 2, 2, 4, 6.853957182198859));
		qTable.add(array("qHSV", 2, 3, 3, 7.313034588867101));
		qTable.add(array("qRGB", 4, 1, 3, 7.4952032204868));
		qTable.add(array("qHSV", 1, 2, 5, 8.19464651896803));
		qTable.add(array("qHSV", 2, 1, 5, 9.128864210622176));
		qTable.add(array("qRGB", 2, 2, 4, 9.45054024064088));
		qTable.add(array("qHSV", 0, 3, 5, 9.50272830656282));
		qTable.add(array("qHSV", 1, 4, 3, 9.57225682172096));
		qTable.add(array("qRGB", 1, 3, 4, 9.878300576656232));
		qTable.add(array("qRGB", 5, 2, 1, 10.168317697289462));
		qTable.add(array("qHSV", 0, 4, 4, 10.325282646966802));
		qTable.add(array("qRGB", 3, 1, 4, 10.513446996566712));
		qTable.add(array("qHSV", 2, 4, 2, 10.553962637282169));
		qTable.add(array("qRGB", 2, 4, 2, 10.754583231629907));
		qTable.add(array("qRGB", 5, 1, 2, 10.78334660852396));
		qTable.add(array("qRGB", 3, 4, 1, 10.923400572688765));
		qTable.add(array("qHSV", 3, 2, 3, 10.940245342238159));
		qTable.add(array("qRGB", 5, 3, 0, 10.948747935514554));
		qTable.add(array("qRGB", 1, 4, 3, 11.177647318386894));
		qTable.add(array("qHSV", 3, 1, 4, 11.292017176980279));
		qTable.add(array("qHSV", 3, 3, 2, 11.749442609095881));
		qTable.add(array("qRGB", 4, 4, 0, 11.754305664224468));
		qTable.add(array("qRGB", 4, 0, 4, 12.707821916090875));
		qTable.add(array("qRGB", 5, 0, 3, 12.721094610289231));
		qTable.add(array("qHSV", 3, 0, 5, 13.321914762530062));
		qTable.add(array("qRGB", 0, 4, 4, 13.462907410406403));
		qTable.add(array("qHSV", 3, 4, 1, 14.428941545444602));
		qTable.add(array("qHSV", 1, 1, 6, 16.213530849741776));
		qTable.add(array("qHSV", 0, 2, 6, 16.595732776692728));
		qTable.add(array("qHSV", 2, 0, 6, 16.990247872051476));
		qTable.add(array("qHSV", 1, 5, 2, 17.37262548192689));
		qTable.add(array("qHSV", 0, 5, 3, 17.50097970500494));
		qTable.add(array("qHSV", 2, 5, 1, 18.135576856216254));
		qTable.add(array("qRGB", 0, 3, 5, 19.920991125299423));
		qTable.add(array("qRGB", 1, 2, 5, 20.43422559355158));
		qTable.add(array("qHSV", 3, 5, 0, 20.953539337715004));
		qTable.add(array("qRGB", 6, 2, 0, 21.039614618747564));
		qTable.add(array("qRGB", 2, 1, 5, 21.311634080572432));
		qTable.add(array("qRGB", 6, 1, 1, 21.402040306383505));
		qTable.add(array("qHSV", 4, 1, 3, 21.607639767259705));
		qTable.add(array("qHSV", 4, 2, 2, 21.635546199656854));
		qTable.add(array("qHSV", 4, 0, 4, 21.980255130529887));
		qTable.add(array("qRGB", 6, 0, 2, 22.113679071750695));
		qTable.add(array("qHSV", 4, 3, 1, 22.144316336683882));
		qTable.add(array("qRGB", 3, 0, 5, 22.328417765460777));
		qTable.add(array("qRGB", 0, 5, 3, 23.401054294240303));
		qTable.add(array("qRGB", 1, 5, 2, 23.725014384855967));
		qTable.add(array("qHSV", 4, 4, 0, 23.862897375364486));
		qTable.add(array("qRGB", 2, 5, 1, 24.024643580435026));
		qTable.add(array("qRGB", 3, 5, 0, 24.123412933265822));
		qTable.add(array("qHSV", 0, 6, 2, 32.89667758007501));
		qTable.add(array("qHSV", 1, 6, 1, 32.90781890164448));
		qTable.add(array("qHSV", 2, 6, 0, 33.32236993216267));
		qTable.add(array("qHSV", 5, 2, 1, 43.73216585611982));
		qTable.add(array("qHSV", 5, 1, 2, 43.73285247357186));
		qTable.add(array("qHSV", 5, 0, 3, 43.773924275773595));
		qTable.add(array("qHSV", 5, 3, 0, 43.87212842657174));
		qTable.add(array("qRGB", 0, 2, 6, 44.73957191563254));
		qTable.add(array("qRGB", 7, 1, 0, 44.91994210785489));
		qTable.add(array("qHSV", 1, 0, 7, 45.012075852208746));
		qTable.add(array("qRGB", 7, 0, 1, 45.321510970753366));
		qTable.add(array("qHSV", 0, 1, 7, 45.3975990469012));
		qTable.add(array("qRGB", 1, 1, 6, 45.67720247527094));
		qTable.add(array("qRGB", 2, 0, 6, 46.3468570278201));
		qTable.add(array("qRGB", 0, 6, 2, 51.60143636551707));
		qTable.add(array("qRGB", 1, 6, 1, 52.200354051560105));
		qTable.add(array("qRGB", 2, 6, 0, 52.442942082617805));
		qTable.add(array("qHSV", 1, 7, 0, 61.84624110147413));
		qTable.add(array("qHSV", 0, 7, 1, 61.98559575821476));
		qTable.add(array("qHSV", 6, 2, 0, 83.31021735981916));
		qTable.add(array("qHSV", 6, 1, 1, 83.59644325622138));
		qTable.add(array("qHSV", 6, 0, 2, 83.70003079189063));
		qTable.add(array("qRGB", 0, 1, 7, 98.09175375687065));
		qTable.add(array("qRGB", 1, 0, 7, 98.63222463427425));
		qTable.add(array("qRGB", 0, 7, 1, 113.35848060108387));
		qTable.add(array("qRGB", 1, 7, 0, 113.79100745068551));
		qTable.add(array("qHSV", 7, 1, 0, 155.76875083166007));
		qTable.add(array("qHSV", 7, 0, 1, 155.9388815899415));
		qTable.add(array("qRGB", 3, 3, 3, 6.340866483189372));
		qTable.add(array("qRGB", 4, 3, 2, 6.709653006265603));
		qTable.add(array("qRGB", 4, 2, 3, 7.129963963236756));
		qTable.add(array("qHSV", 2, 3, 4, 8.320823808478119));
		qTable.add(array("qHSV", 2, 2, 5, 9.692344955617365));
		qTable.add(array("qHSV", 1, 3, 5, 9.779610923631795));
		qTable.add(array("qRGB", 2, 3, 4, 9.857522392338288));
		qTable.add(array("qRGB", 3, 2, 4, 9.950763892439424));
		qTable.add(array("qRGB", 5, 2, 2, 10.507557830242826));
		qTable.add(array("qHSV", 1, 4, 4, 10.584318858953525));
		qTable.add(array("qRGB", 3, 4, 2, 10.638399022749269));
		qTable.add(array("qRGB", 5, 3, 1, 10.85649017076917));
		qTable.add(array("qHSV", 2, 4, 3, 10.886674213271974));
		qTable.add(array("qRGB", 2, 4, 3, 10.986209294528791));
		qTable.add(array("qRGB", 4, 4, 1, 11.46652530930045));
		qTable.add(array("qHSV", 3, 2, 4, 11.673906598335554));
		qTable.add(array("qHSV", 3, 3, 3, 11.994609999866599));
		qTable.add(array("qRGB", 4, 1, 4, 12.20218340832902));
		qTable.add(array("qRGB", 5, 1, 3, 12.312975745853725));
		qTable.add(array("qHSV", 0, 4, 5, 13.149357181796196));
		qTable.add(array("qRGB", 1, 4, 4, 13.35518595291978));
		qTable.add(array("qHSV", 3, 1, 5, 13.487052808959266));
		qTable.add(array("qHSV", 3, 4, 2, 14.518904965426142));
		qTable.add(array("qRGB", 5, 4, 0, 14.556491367623703));
		qTable.add(array("qHSV", 1, 2, 6, 16.799177364455836));
		qTable.add(array("qRGB", 5, 0, 4, 16.923951577513773));
		qTable.add(array("qHSV", 2, 1, 6, 17.266229614644402));
		qTable.add(array("qHSV", 1, 5, 3, 17.683854172496947));
		qTable.add(array("qHSV", 0, 3, 6, 18.027459361336454));
		qTable.add(array("qHSV", 2, 5, 2, 18.246274714415243));
		qTable.add(array("qHSV", 0, 5, 4, 18.349046936514938));
		qTable.add(array("qRGB", 1, 3, 5, 19.907650136450783));
		qTable.add(array("qHSV", 3, 0, 6, 20.31449096221656));
		qTable.add(array("qRGB", 2, 2, 5, 20.549489748044554));
		qTable.add(array("qHSV", 3, 5, 1, 20.994690069622262));
		qTable.add(array("qRGB", 6, 2, 1, 21.093244874228947));
		qTable.add(array("qRGB", 6, 3, 0, 21.19254912139334));
		qTable.add(array("qRGB", 0, 4, 5, 21.246600560116846));
		qTable.add(array("qHSV", 4, 2, 3, 21.75875632193585));
		qTable.add(array("qRGB", 3, 1, 5, 21.78644893297665));
		qTable.add(array("qRGB", 6, 1, 2, 21.79250600681563));
		qTable.add(array("qHSV", 4, 1, 4, 22.02443261543551));
		qTable.add(array("qHSV", 4, 3, 2, 22.186876867524546));
		qTable.add(array("qRGB", 1, 5, 3, 23.260883706223176));
		qTable.add(array("qRGB", 6, 0, 3, 23.386703338692957));
		qTable.add(array("qHSV", 4, 0, 5, 23.427093643965375));
		qTable.add(array("qRGB", 2, 5, 2, 23.493924375351092));
		qTable.add(array("qRGB", 4, 0, 5, 23.642266327011406));
		qTable.add(array("qRGB", 3, 5, 1, 23.741972884777905));
		qTable.add(array("qHSV", 4, 4, 1, 23.879017394229695));
		qTable.add(array("qRGB", 0, 5, 4, 23.943637893089022));
		qTable.add(array("qRGB", 4, 5, 0, 24.063262335361493));
		qTable.add(array("qHSV", 4, 5, 0, 28.615899360492723));
		qTable.add(array("qHSV", 1, 6, 2, 32.99827811369083));
		qTable.add(array("qHSV", 0, 6, 3, 33.1286253581266));
		qTable.add(array("qHSV", 2, 6, 1, 33.3839421900219));
		qTable.add(array("qHSV", 3, 6, 0, 35.009802069537756));
		qTable.add(array("qRGB", 0, 3, 6, 43.460896182311956));
		qTable.add(array("qHSV", 5, 2, 2, 43.73202855891318));
		qTable.add(array("qHSV", 5, 1, 3, 43.7535637956447));
		qTable.add(array("qHSV", 5, 3, 1, 43.86640260615535));
		qTable.add(array("qHSV", 5, 0, 4, 43.92909101484812));
		qTable.add(array("qHSV", 5, 4, 0, 44.55571124845371));
		qTable.add(array("qRGB", 7, 2, 0, 44.55833251204558));
		qTable.add(array("qRGB", 1, 2, 6, 44.776897391602574));
		qTable.add(array("qRGB", 7, 1, 1, 45.04637978612198));
		qTable.add(array("qHSV", 1, 1, 7, 45.482246920674726));
		qTable.add(array("qHSV", 2, 0, 7, 45.51365112224777));
		qTable.add(array("qRGB", 7, 0, 2, 45.70512868979869));
		qTable.add(array("qRGB", 2, 1, 6, 45.813611637632384));
		qTable.add(array("qHSV", 0, 2, 7, 45.89436224260916));
		qTable.add(array("qRGB", 3, 0, 6, 46.720243380629405));
		qTable.add(array("qRGB", 0, 6, 3, 50.56913459912434));
		qTable.add(array("qRGB", 1, 6, 2, 51.495507349823036));
		qTable.add(array("qRGB", 2, 6, 1, 52.024598427168634));
		qTable.add(array("qRGB", 3, 6, 0, 52.19106917882459));
		qTable.add(array("qHSV", 0, 7, 2, 62.01143653140464));
		qTable.add(array("qHSV", 1, 7, 1, 62.023247577307245));
		qTable.add(array("qHSV", 2, 7, 0, 62.02579926667945));
		qTable.add(array("qHSV", 6, 3, 0, 82.76435853485751));
		qTable.add(array("qHSV", 6, 2, 1, 83.27401911307462));
		qTable.add(array("qHSV", 6, 1, 2, 83.53199900326042));
		qTable.add(array("qHSV", 6, 0, 3, 83.58759338540732));
		qTable.add(array("qRGB", 0, 2, 7, 97.16239083873536));
		qTable.add(array("qRGB", 1, 1, 7, 98.13677798324206));
		qTable.add(array("qRGB", 2, 0, 7, 98.74559245319055));
		qTable.add(array("qRGB", 0, 7, 2, 112.52326535326874));
		qTable.add(array("qRGB", 1, 7, 1, 113.34355286396212));
		qTable.add(array("qRGB", 2, 7, 0, 113.78207678079858));
		qTable.add(array("qHSV", 7, 2, 0, 155.26271089738088));
		qTable.add(array("qHSV", 7, 1, 1, 155.69921989766797));
		qTable.add(array("qHSV", 7, 0, 2, 155.81640234578813));
		qTable.add(array("qRGB", 4, 3, 3, 7.688442546008208));
		qTable.add(array("qRGB", 3, 3, 4, 10.095141022638085));
		qTable.add(array("qRGB", 3, 4, 3, 10.864897114402869));
		qTable.add(array("qRGB", 5, 3, 2, 10.951615663240458));
		qTable.add(array("qHSV", 2, 3, 5, 11.070154272260991));
		qTable.add(array("qRGB", 4, 4, 2, 11.191632862199885));
		qTable.add(array("qRGB", 4, 2, 4, 11.52598396241005));
		qTable.add(array("qHSV", 2, 4, 4, 11.782463229695244));
		qTable.add(array("qRGB", 5, 2, 3, 11.819634155213707));
		qTable.add(array("qHSV", 3, 3, 4, 12.734447695338403));
		qTable.add(array("qRGB", 2, 4, 4, 13.198005564072307));
		qTable.add(array("qHSV", 1, 4, 5, 13.349700851194054));
		qTable.add(array("qHSV", 3, 2, 5, 13.89682265182176));
		qTable.add(array("qRGB", 5, 4, 1, 14.322611845014073));
		qTable.add(array("qHSV", 3, 4, 3, 14.765915006914788));
		qTable.add(array("qRGB", 5, 1, 4, 16.431998213672987));
		qTable.add(array("qHSV", 2, 2, 6, 17.794743002263));
		qTable.add(array("qHSV", 1, 3, 6, 18.19966125131184));
		qTable.add(array("qHSV", 1, 5, 4, 18.509866614595577));
		qTable.add(array("qHSV", 2, 5, 3, 18.52725317113241));
		qTable.add(array("qRGB", 2, 3, 5, 19.933533714232336));
		qTable.add(array("qHSV", 3, 1, 6, 20.533316034673383));
		qTable.add(array("qHSV", 0, 5, 5, 20.662238654155118));
		qTable.add(array("qRGB", 3, 2, 5, 20.95052655636009));
		qTable.add(array("qHSV", 3, 5, 2, 21.083018105884644));
		qTable.add(array("qHSV", 0, 4, 6, 21.110219649964062));
		qTable.add(array("qRGB", 6, 3, 1, 21.161371921806357));
		qTable.add(array("qRGB", 1, 4, 5, 21.18027776782719));
		qTable.add(array("qRGB", 6, 2, 2, 21.38663005776269));
		qTable.add(array("qHSV", 4, 2, 4, 22.196381149547953));
		qTable.add(array("qHSV", 4, 3, 3, 22.32389197746879));
		qTable.add(array("qRGB", 6, 1, 3, 23.0029729164763));
		qTable.add(array("qRGB", 2, 5, 3, 23.022684799085344));
		qTable.add(array("qRGB", 4, 1, 5, 23.086305798726343));
		qTable.add(array("qRGB", 3, 5, 2, 23.194828446980296));
		qTable.add(array("qRGB", 6, 4, 0, 23.288580709898017));
		qTable.add(array("qHSV", 4, 1, 5, 23.501867553611202));
		qTable.add(array("qRGB", 4, 5, 1, 23.68088624329254));
		qTable.add(array("qRGB", 1, 5, 4, 23.811048702797404));
		qTable.add(array("qHSV", 4, 4, 2, 23.930560280338014));
		qTable.add(array("qRGB", 5, 5, 0, 25.402895483675284));
		qTable.add(array("qRGB", 6, 0, 4, 26.864638036544115));
		qTable.add(array("qRGB", 5, 0, 5, 27.081377578441117));
		qTable.add(array("qRGB", 0, 5, 5, 28.54063915420294));
		qTable.add(array("qHSV", 4, 5, 1, 28.641730651838913));
		qTable.add(array("qHSV", 4, 0, 6, 28.854331775352687));
		qTable.add(array("qHSV", 1, 6, 3, 33.22460294147693));
		qTable.add(array("qHSV", 2, 6, 2, 33.46921764781381));
		qTable.add(array("qHSV", 0, 6, 4, 33.702579763034635));
		qTable.add(array("qHSV", 3, 6, 1, 35.06389034413055));
		qTable.add(array("qHSV", 4, 6, 0, 40.137731481784115));
		qTable.add(array("qRGB", 0, 4, 6, 42.525753512691516));
		qTable.add(array("qRGB", 1, 3, 6, 43.47286860289719));
		qTable.add(array("qHSV", 5, 2, 3, 43.75703255179147));
		qTable.add(array("qHSV", 5, 3, 2, 43.86973635535911));
		qTable.add(array("qHSV", 5, 1, 4, 43.9193503618413));
		qTable.add(array("qRGB", 7, 3, 0, 44.309567106007385));
		qTable.add(array("qHSV", 5, 4, 1, 44.55595283640167));
		qTable.add(array("qRGB", 7, 2, 1, 44.652283598962846));
		qTable.add(array("qHSV", 5, 0, 5, 44.70057087412785));
		qTable.add(array("qRGB", 2, 2, 6, 44.88342483911719));
		qTable.add(array("qRGB", 7, 1, 2, 45.40073048149536));
		qTable.add(array("qHSV", 2, 1, 7, 45.957826377522295));
		qTable.add(array("qHSV", 1, 2, 7, 45.97438884520198));
		qTable.add(array("qRGB", 3, 1, 6, 46.171046220192245));
		qTable.add(array("qRGB", 7, 0, 3, 46.74410786531797));
		qTable.add(array("qHSV", 0, 3, 7, 46.94047509487422));
		qTable.add(array("qHSV", 5, 5, 0, 46.98065616162865));
		qTable.add(array("qHSV", 3, 0, 7, 47.31084724651443));
		qTable.add(array("qRGB", 4, 0, 6, 47.69348627992129));
		qTable.add(array("qRGB", 0, 6, 4, 49.58886169746684));
		qTable.add(array("qRGB", 1, 6, 3, 50.45815908106512));
		qTable.add(array("qRGB", 2, 6, 2, 51.30975863355777));
		qTable.add(array("qRGB", 3, 6, 1, 51.76398032287591));
		qTable.add(array("qRGB", 4, 6, 0, 51.98456631116456));
		qTable.add(array("qHSV", 1, 7, 2, 62.0476400938254));
		qTable.add(array("qHSV", 0, 7, 3, 62.08910702114573));
		qTable.add(array("qHSV", 2, 7, 1, 62.19923587229031));
		qTable.add(array("qHSV", 3, 7, 0, 62.71396179748241));
		qTable.add(array("qHSV", 6, 4, 0, 82.01003193662558));
		qTable.add(array("qHSV", 6, 3, 1, 82.73396479269887));
		qTable.add(array("qHSV", 6, 2, 2, 83.2140183000991));
		qTable.add(array("qHSV", 6, 1, 3, 83.4241302922701));
		qTable.add(array("qHSV", 6, 0, 4, 83.43394427208376));
		qTable.add(array("qRGB", 0, 3, 7, 95.5024295094967));
		qTable.add(array("qRGB", 1, 2, 7, 97.19867684302076));
		qTable.add(array("qRGB", 2, 1, 7, 98.2421679833705));
		qTable.add(array("qRGB", 3, 0, 7, 99.01508349160125));
		qTable.add(array("qRGB", 0, 7, 3, 111.04199670996694));
		qTable.add(array("qRGB", 1, 7, 2, 112.50410102149714));
		qTable.add(array("qRGB", 2, 7, 1, 113.32974893931791));
		qTable.add(array("qRGB", 3, 7, 0, 113.8193934779908));
		qTable.add(array("qHSV", 7, 3, 0, 154.25179097350306));
		qTable.add(array("qHSV", 7, 2, 1, 155.19759088118153));
		qTable.add(array("qHSV", 7, 0, 3, 155.5731316317515));
		qTable.add(array("qHSV", 7, 1, 2, 155.57639462650135));
		qTable.add(array("qRGB", 4, 3, 4, 11.300385291883343));
		qTable.add(array("qRGB", 4, 4, 3, 11.413287765007333));
		qTable.add(array("qRGB", 5, 3, 3, 11.835590580277925));
		qTable.add(array("qRGB", 3, 4, 4, 13.105170302239044));
		qTable.add(array("qRGB", 5, 4, 2, 14.102725593278263));
		qTable.add(array("qHSV", 2, 4, 5, 14.342172542032769));
		qTable.add(array("qHSV", 3, 3, 5, 14.946261925820883));
		qTable.add(array("qHSV", 3, 4, 4, 15.46518525367638));
		qTable.add(array("qRGB", 5, 2, 4, 15.6937208294619));
		qTable.add(array("qHSV", 2, 3, 6, 19.079145485722293));
		qTable.add(array("qHSV", 2, 5, 4, 19.290435137212366));
		qTable.add(array("qRGB", 3, 3, 5, 20.16864052990391));
		qTable.add(array("qHSV", 1, 5, 5, 20.791984112466153));
		qTable.add(array("qHSV", 3, 2, 6, 20.96089276744169));
		qTable.add(array("qRGB", 2, 4, 5, 21.08938909810169));
		qTable.add(array("qHSV", 1, 4, 6, 21.243770468351563));
		qTable.add(array("qRGB", 6, 3, 2, 21.288110810768334));
		qTable.add(array("qHSV", 3, 5, 3, 21.312307819388156));
		qTable.add(array("qRGB", 4, 2, 5, 22.178404640544578));
		qTable.add(array("qRGB", 6, 2, 3, 22.45089367092038));
		qTable.add(array("qRGB", 3, 5, 3, 22.70561761512323));
		qTable.add(array("qHSV", 4, 3, 4, 22.783387113717136));
		qTable.add(array("qRGB", 4, 5, 2, 23.12311052644533));
		qTable.add(array("qRGB", 6, 4, 1, 23.13965229796283));
		qTable.add(array("qRGB", 2, 5, 4, 23.581710343484367));
		qTable.add(array("qHSV", 4, 2, 5, 23.712242409809047));
		qTable.add(array("qHSV", 4, 4, 3, 24.08132103092352));
		qTable.add(array("qRGB", 5, 5, 1, 25.045878832584336));
		qTable.add(array("qRGB", 6, 1, 4, 26.409192843237044));
		qTable.add(array("qRGB", 5, 1, 5, 26.529047924439634));
		qTable.add(array("qHSV", 0, 5, 6, 27.63065202910769));
		qTable.add(array("qRGB", 1, 5, 5, 28.431190265339374));
		qTable.add(array("qHSV", 4, 5, 2, 28.6992567436543));
		qTable.add(array("qHSV", 4, 1, 6, 28.982662338530055));
		qTable.add(array("qRGB", 6, 5, 0, 31.698950614604737));
		qTable.add(array("qHSV", 2, 6, 3, 33.68213058054486));
		qTable.add(array("qHSV", 1, 6, 4, 33.78916238071692));
		qTable.add(array("qHSV", 3, 6, 2, 35.137672403014726));
		qTable.add(array("qHSV", 0, 6, 5, 35.38285901058025));
		qTable.add(array("qRGB", 6, 0, 5, 35.798088797723715));
		qTable.add(array("qHSV", 4, 6, 1, 40.17942007592115));
		qTable.add(array("qRGB", 1, 4, 6, 42.499987755993985));
		qTable.add(array("qRGB", 2, 3, 6, 43.52716203099239));
		qTable.add(array("qHSV", 5, 3, 3, 43.906895816832254));
		qTable.add(array("qHSV", 5, 2, 4, 43.935627297756106));
		qTable.add(array("qRGB", 7, 3, 1, 44.349719267806194));
		qTable.add(array("qHSV", 5, 4, 2, 44.56590570376091));
		qTable.add(array("qHSV", 5, 1, 5, 44.70977685547477));
		qTable.add(array("qRGB", 7, 2, 2, 44.94814474219706));
		qTable.add(array("qRGB", 0, 5, 6, 45.098262849514896));
		qTable.add(array("qRGB", 3, 2, 6, 45.19680023082748));
		qTable.add(array("qRGB", 7, 4, 0, 45.24050897140638));
		qTable.add(array("qRGB", 7, 1, 3, 46.397617749593216));
		qTable.add(array("qHSV", 2, 2, 7, 46.42892542288154));
		qTable.add(array("qHSV", 5, 5, 1, 46.98756966975928));
		qTable.add(array("qHSV", 1, 3, 7, 47.01472807679523));
		qTable.add(array("qRGB", 4, 1, 6, 47.12882395174464));
		qTable.add(array("qHSV", 3, 1, 7, 47.69825145609503));
		qTable.add(array("qHSV", 5, 0, 6, 48.30892314902963));
		qTable.add(array("qHSV", 0, 4, 7, 49.13722160795946));
		qTable.add(array("qRGB", 1, 6, 4, 49.47449215886267));
		qTable.add(array("qRGB", 7, 0, 4, 49.56445112803157));
		qTable.add(array("qRGB", 2, 6, 3, 50.26044398911863));
		qTable.add(array("qRGB", 5, 0, 6, 50.31137045369451));
		qTable.add(array("qRGB", 0, 6, 5, 50.568076039984064));
		qTable.add(array("qRGB", 3, 6, 2, 51.032435381608266));
		qTable.add(array("qRGB", 4, 6, 1, 51.548862763853826));
		qTable.add(array("qRGB", 5, 6, 0, 52.552285583869214));
		qTable.add(array("qHSV", 4, 0, 7, 52.8129511776001));
		qTable.add(array("qHSV", 5, 6, 0, 54.13995592279949));
		qTable.add(array("qHSV", 1, 7, 3, 62.123968137878016));
		qTable.add(array("qHSV", 2, 7, 2, 62.221939996879684));
		qTable.add(array("qHSV", 0, 7, 4, 62.31871516714029));
		qTable.add(array("qHSV", 3, 7, 1, 62.87714478304283));
		qTable.add(array("qHSV", 4, 7, 0, 65.07473789181662));
		qTable.add(array("qHSV", 6, 5, 0, 81.4408397301875));
		qTable.add(array("qHSV", 6, 4, 1, 81.98400997685754));
		qTable.add(array("qHSV", 6, 3, 2, 82.67858543292775));
		qTable.add(array("qHSV", 6, 2, 3, 83.11171679692882));
		qTable.add(array("qHSV", 6, 1, 4, 83.27724778864844));
		qTable.add(array("qHSV", 6, 0, 5, 83.41585847422323));
		qTable.add(array("qRGB", 0, 4, 7, 92.95641588673492));
		qTable.add(array("qRGB", 1, 3, 7, 95.52435465074312));
		qTable.add(array("qRGB", 2, 2, 7, 97.28774943440655));
		qTable.add(array("qRGB", 3, 1, 7, 98.50079273033025));
		qTable.add(array("qRGB", 4, 0, 7, 99.70123907935444));
		qTable.add(array("qRGB", 0, 7, 4, 108.70942567609441));
		qTable.add(array("qRGB", 1, 7, 3, 111.01652069235655));
		qTable.add(array("qRGB", 2, 7, 2, 112.48198468055303));
		qTable.add(array("qRGB", 3, 7, 1, 113.35964114383401));
		qTable.add(array("qRGB", 4, 7, 0, 114.08455335800612));
		qTable.add(array("qHSV", 7, 4, 0, 152.26767295140053));
		qTable.add(array("qHSV", 7, 3, 1, 154.18872685701697));
		qTable.add(array("qHSV", 7, 2, 2, 155.07487080911943));
		qTable.add(array("qHSV", 7, 0, 4, 155.09750765195514));
		qTable.add(array("qHSV", 7, 1, 3, 155.33090566219985));
		qTable.add(array("qRGB", 4, 4, 4, 13.635492369684071));
		qTable.add(array("qRGB", 5, 4, 3, 14.339651153267729));
		qTable.add(array("qRGB", 5, 3, 4, 15.105009711688462));
		qTable.add(array("qHSV", 3, 4, 5, 17.613021066244166));
		qTable.add(array("qRGB", 3, 4, 5, 21.06899986017425));
		qTable.add(array("qRGB", 4, 3, 5, 21.163357953208294));
		qTable.add(array("qHSV", 2, 5, 5, 21.459022937564324));
		qTable.add(array("qHSV", 3, 5, 4, 21.953548327784002));
		qTable.add(array("qHSV", 2, 4, 6, 21.953794391617667));
		qTable.add(array("qHSV", 3, 3, 6, 22.015191420102827));
		qTable.add(array("qRGB", 6, 3, 3, 22.057818941088346));
		qTable.add(array("qRGB", 4, 5, 3, 22.611345132538872));
		qTable.add(array("qRGB", 6, 4, 2, 23.01722632330868));
		qTable.add(array("qRGB", 3, 5, 4, 23.26583544312819));
		qTable.add(array("qHSV", 4, 3, 5, 24.334741391416884));
		qTable.add(array("qRGB", 5, 5, 2, 24.513149100441726));
		qTable.add(array("qHSV", 4, 4, 4, 24.54206993455648));
		qTable.add(array("qRGB", 5, 2, 5, 25.583221839197073));
		qTable.add(array("qRGB", 6, 2, 4, 25.672319962794653));
		qTable.add(array("qHSV", 1, 5, 6, 27.723584486538304));
		qTable.add(array("qRGB", 2, 5, 5, 28.24066686271142));
		qTable.add(array("qHSV", 4, 5, 3, 28.858335408264463));
		qTable.add(array("qHSV", 4, 2, 6, 29.245750524384043));
		qTable.add(array("qRGB", 6, 5, 1, 31.41318872510949));
		qTable.add(array("qHSV", 2, 6, 4, 34.22040714241764));
		qTable.add(array("qRGB", 6, 1, 5, 35.27323810665261));
		qTable.add(array("qHSV", 3, 6, 3, 35.32334270831286));
		qTable.add(array("qHSV", 1, 6, 5, 35.45535791369207));
		qTable.add(array("qHSV", 4, 6, 2, 40.23476684721952));
		qTable.add(array("qHSV", 0, 6, 6, 40.75582097662498));
		qTable.add(array("qRGB", 2, 4, 6, 42.47457324801316));
		qTable.add(array("qRGB", 3, 3, 6, 43.743810942045144));
		qTable.add(array("qHSV", 5, 3, 4, 44.10013792348927));
		qTable.add(array("qRGB", 7, 3, 2, 44.5399115043061));
		qTable.add(array("qHSV", 5, 4, 3, 44.61240036097907));
		qTable.add(array("qHSV", 5, 2, 5, 44.75074825138069));
		qTable.add(array("qRGB", 1, 5, 6, 45.02628653090765));
		qTable.add(array("qRGB", 7, 4, 1, 45.19548827530423));
		qTable.add(array("qRGB", 7, 2, 3, 45.85128412568048));
		qTable.add(array("qRGB", 4, 2, 6, 46.104706678281275));
		qTable.add(array("qHSV", 5, 5, 2, 47.00216719845255));
		qTable.add(array("qHSV", 2, 3, 7, 47.434430403929944));
		qTable.add(array("qHSV", 3, 2, 7, 48.113864245075504));
		qTable.add(array("qHSV", 5, 1, 6, 48.35414705431072));
		qTable.add(array("qRGB", 7, 1, 4, 49.16085177151545));
		qTable.add(array("qHSV", 1, 4, 7, 49.200744348108195));
		qTable.add(array("qRGB", 2, 6, 4, 49.26688031984588));
		qTable.add(array("qRGB", 5, 1, 6, 49.736293373767865));
		qTable.add(array("qRGB", 3, 6, 3, 49.95796953990217));
		qTable.add(array("qRGB", 1, 6, 5, 50.455210971385455));
		qTable.add(array("qRGB", 4, 6, 2, 50.79571129684579));
		qTable.add(array("qRGB", 7, 5, 0, 51.06486999908676));
		qTable.add(array("qRGB", 5, 6, 1, 52.116365829212135));
		qTable.add(array("qHSV", 4, 1, 7, 53.10463249735915));
		qTable.add(array("qHSV", 0, 5, 7, 53.78015545839082));
		qTable.add(array("qHSV", 5, 6, 1, 54.159747593732384));
		qTable.add(array("qRGB", 6, 6, 0, 56.65347676334426));
		qTable.add(array("qRGB", 7, 0, 5, 57.05088417395533));
		qTable.add(array("qRGB", 6, 0, 6, 57.387399296171345));
		qTable.add(array("qRGB", 0, 6, 6, 59.83107919981765));
		qTable.add(array("qHSV", 2, 7, 3, 62.295216190063));
		qTable.add(array("qHSV", 1, 7, 4, 62.35076210418596));
		qTable.add(array("qHSV", 3, 7, 2, 62.89298902640965));
		qTable.add(array("qHSV", 0, 7, 5, 63.112997894458175));
		qTable.add(array("qHSV", 4, 7, 1, 65.22501380465377));
		qTable.add(array("qHSV", 5, 0, 7, 67.35803299473069));
		qTable.add(array("qHSV", 5, 7, 0, 72.5529842358517));
		qTable.add(array("qHSV", 6, 5, 1, 81.41961158006531));
		qTable.add(array("qHSV", 6, 4, 2, 81.93752641201267));
		qTable.add(array("qHSV", 6, 3, 3, 82.59066390393983));
		qTable.add(array("qHSV", 6, 6, 0, 82.68591896276428));
		qTable.add(array("qHSV", 6, 2, 4, 82.97878839973524));
		qTable.add(array("qHSV", 6, 1, 5, 83.27251053606977));
		qTable.add(array("qHSV", 6, 0, 6, 84.8281185595414));
		qTable.add(array("qRGB", 0, 5, 7, 90.63206534182964));
		qTable.add(array("qRGB", 1, 4, 7, 92.95451501587132));
		qTable.add(array("qRGB", 2, 3, 7, 95.58421617689663));
		qTable.add(array("qRGB", 3, 2, 7, 97.52004006544156));
		qTable.add(array("qRGB", 4, 1, 7, 99.1720497524499));
		qTable.add(array("qRGB", 5, 0, 7, 101.56712234851004));
		qTable.add(array("qRGB", 0, 7, 5, 105.99908554244503));
		qTable.add(array("qRGB", 1, 7, 4, 108.67401118003978));
		qTable.add(array("qRGB", 2, 7, 3, 110.9808282044467));
		qTable.add(array("qRGB", 3, 7, 2, 112.49699129233186));
		qTable.add(array("qRGB", 4, 7, 1, 113.61400042659453));
		qTable.add(array("qRGB", 5, 7, 0, 115.24588503540951));
		qTable.add(array("qHSV", 7, 5, 0, 148.4866525222472));
		qTable.add(array("qHSV", 7, 4, 1, 152.20702004897882));
		qTable.add(array("qHSV", 7, 3, 2, 154.06650658928004));
		qTable.add(array("qHSV", 7, 0, 5, 154.22820386668872));
		qTable.add(array("qHSV", 7, 2, 3, 154.8302458267778));
		qTable.add(array("qHSV", 7, 1, 4, 154.85496721006646));
		qTable.add(array("qRGB", 5, 4, 4, 16.452327018216238));
		qTable.add(array("qRGB", 4, 4, 5, 21.576691496711987));
		qTable.add(array("qRGB", 4, 5, 4, 23.151504858179628));
		qTable.add(array("qRGB", 6, 4, 3, 23.29238320073896));
		qTable.add(array("qHSV", 3, 5, 5, 23.855937179825872));
		qTable.add(array("qRGB", 5, 5, 3, 24.010895759787857));
		qTable.add(array("qRGB", 5, 3, 5, 24.345372151173603));
		qTable.add(array("qHSV", 3, 4, 6, 24.483664759259558));
		qTable.add(array("qRGB", 6, 3, 4, 24.84121606806918));
		qTable.add(array("qHSV", 4, 4, 5, 26.121861030618177));
		qTable.add(array("qRGB", 3, 5, 5, 27.975681776202745));
		qTable.add(array("qHSV", 2, 5, 6, 28.22285519819319));
		qTable.add(array("qHSV", 4, 5, 4, 29.326939071136668));
		qTable.add(array("qHSV", 4, 3, 6, 29.941881324210264));
		qTable.add(array("qRGB", 6, 5, 2, 30.97884589500067));
		qTable.add(array("qRGB", 6, 2, 5, 34.346490652277055));
		qTable.add(array("qHSV", 3, 6, 4, 35.80586465661823));
		qTable.add(array("qHSV", 2, 6, 5, 35.831301740769405));
		qTable.add(array("qHSV", 4, 6, 3, 40.37922211577201));
		qTable.add(array("qHSV", 1, 6, 6, 40.81321272585543));
		qTable.add(array("qRGB", 3, 4, 6, 42.525019656531576));
		qTable.add(array("qRGB", 4, 3, 6, 44.51060848937224));
		qTable.add(array("qHSV", 5, 4, 4, 44.81908847853915));
		qTable.add(array("qRGB", 2, 5, 6, 44.902350932273606));
		qTable.add(array("qHSV", 5, 3, 5, 44.954338178029914));
		qTable.add(array("qRGB", 7, 4, 2, 45.209654110379404));
		qTable.add(array("qRGB", 7, 3, 3, 45.25226318990082));
		qTable.add(array("qHSV", 5, 5, 3, 47.067622456344374));
		qTable.add(array("qHSV", 5, 2, 6, 48.43284417039696));
		qTable.add(array("qRGB", 7, 2, 4, 48.48082801682692));
		qTable.add(array("qRGB", 5, 2, 6, 48.67260126855884));
		qTable.add(array("qRGB", 3, 6, 4, 48.93754909302421));
		qTable.add(array("qHSV", 3, 3, 7, 49.02681684469041));
		qTable.add(array("qHSV", 2, 4, 7, 49.56029119812139));
		qTable.add(array("qRGB", 4, 6, 3, 49.67978878365301));
		qTable.add(array("qRGB", 2, 6, 5, 50.2467901034742));
		qTable.add(array("qRGB", 7, 5, 1, 50.89632581346104));
		qTable.add(array("qRGB", 5, 6, 2, 51.349978241781436));
		qTable.add(array("qHSV", 4, 2, 7, 53.40986543747504));
		qTable.add(array("qHSV", 1, 5, 7, 53.832715409307504));
		qTable.add(array("qHSV", 5, 6, 2, 54.17452931216259));
		qTable.add(array("qRGB", 6, 6, 1, 56.24604742164139));
		qTable.add(array("qRGB", 7, 1, 5, 56.576264479355444));
		qTable.add(array("qRGB", 6, 1, 6, 56.814909591996674));
		qTable.add(array("qRGB", 1, 6, 6, 59.72839648703533));
		qTable.add(array("qHSV", 2, 7, 4, 62.51176621430742));
		qTable.add(array("qHSV", 3, 7, 3, 62.96174689694058));
		qTable.add(array("qHSV", 1, 7, 5, 63.14358049455555));
		qTable.add(array("qHSV", 0, 6, 7, 63.465936946588535));
		qTable.add(array("qHSV", 4, 7, 2, 65.23155454626794));
		qTable.add(array("qHSV", 0, 7, 6, 66.36369767041624));
		qTable.add(array("qHSV", 5, 1, 7, 67.49295507997199));
		qTable.add(array("qRGB", 7, 6, 0, 72.32032989402973));
		qTable.add(array("qHSV", 5, 7, 1, 72.6504972897907));
		qTable.add(array("qRGB", 7, 0, 6, 76.11764832983383));
		qTable.add(array("qHSV", 6, 5, 2, 81.38302809602966));
		qTable.add(array("qHSV", 6, 4, 3, 81.86192037913459));
		qTable.add(array("qHSV", 6, 3, 4, 82.47611149206824));
		qTable.add(array("qHSV", 6, 6, 1, 82.67225695734406));
		qTable.add(array("qHSV", 6, 2, 5, 83.00311321520888));
		qTable.add(array("qHSV", 6, 1, 6, 84.70426204696611));
		qTable.add(array("qHSV", 6, 7, 0, 90.32392117480995));
		qTable.add(array("qRGB", 1, 5, 7, 90.59284659085877));
		qTable.add(array("qRGB", 2, 4, 7, 92.96533250892645));
		qTable.add(array("qRGB", 0, 6, 7, 94.23816148736213));
		qTable.add(array("qRGB", 3, 3, 7, 95.76126172444539));
		qTable.add(array("qHSV", 6, 0, 7, 96.77929387101405));
		qTable.add(array("qRGB", 4, 2, 7, 98.15417423649613));
		qTable.add(array("qRGB", 5, 1, 7, 101.01783865813661));
		qTable.add(array("qRGB", 1, 7, 5, 105.9478151508339));
		qTable.add(array("qRGB", 0, 7, 6, 106.2108223866618));
		qTable.add(array("qRGB", 6, 0, 7, 106.88184436821574));
		qTable.add(array("qRGB", 2, 7, 4, 108.61661495825513));
		qTable.add(array("qRGB", 3, 7, 3, 110.9684674837774));
		qTable.add(array("qRGB", 4, 7, 2, 112.72714747788302));
		qTable.add(array("qRGB", 5, 7, 1, 114.76166638761399));
		qTable.add(array("qRGB", 6, 7, 0, 119.55559324301412));
		qTable.add(array("qHSV", 7, 6, 0, 141.64444052630054));
		qTable.add(array("qHSV", 7, 5, 1, 148.4251492515059));
		qTable.add(array("qHSV", 7, 4, 2, 152.0899192589707));
		qTable.add(array("qHSV", 7, 0, 6, 153.00264635518943));
		qTable.add(array("qHSV", 7, 3, 3, 153.83156011108096));
		qTable.add(array("qHSV", 7, 1, 5, 153.98670951089673));
		qTable.add(array("qHSV", 7, 2, 4, 154.36075363667933));
		qTable.add(array("qRGB", 5, 4, 5, 24.071811935682792));
		qTable.add(array("qRGB", 5, 5, 4, 24.519064488681344));
		qTable.add(array("qRGB", 6, 4, 4, 25.197573119249448));
		qTable.add(array("qRGB", 4, 5, 5, 27.88679667800512));
		qTable.add(array("qHSV", 3, 5, 6, 30.125155623703037));
		qTable.add(array("qRGB", 6, 5, 3, 30.57235916304928));
		qTable.add(array("qHSV", 4, 5, 5, 30.80517648683717));
		qTable.add(array("qHSV", 4, 4, 6, 31.699842078117207));
		qTable.add(array("qRGB", 6, 3, 5, 32.99240577170763));
		qTable.add(array("qHSV", 3, 6, 5, 37.271137072988665));
		qTable.add(array("qHSV", 4, 6, 4, 40.75410626562716));
		qTable.add(array("qHSV", 2, 6, 6, 41.107826122575894));
		qTable.add(array("qRGB", 4, 4, 6, 42.983901487038864));
		qTable.add(array("qRGB", 3, 5, 6, 44.73696740093618));
		qTable.add(array("qRGB", 7, 4, 3, 45.575935181665926));
		qTable.add(array("qHSV", 5, 4, 5, 45.73706665815075));
		qTable.add(array("qRGB", 5, 3, 6, 46.93140066470426));
		qTable.add(array("qHSV", 5, 5, 4, 47.31457765402981));
		qTable.add(array("qRGB", 7, 3, 4, 47.57904720235397));
		qTable.add(array("qRGB", 4, 6, 4, 48.59959709126427));
		qTable.add(array("qHSV", 5, 3, 6, 48.70967709693754));
		qTable.add(array("qRGB", 3, 6, 5, 49.90415093680079));
		qTable.add(array("qRGB", 5, 6, 3, 50.19162493994238));
		qTable.add(array("qRGB", 7, 5, 2, 50.64474510643907));
		qTable.add(array("qHSV", 3, 4, 7, 50.97542315094293));
		qTable.add(array("qHSV", 2, 5, 7, 54.10765028158217));
		qTable.add(array("qHSV", 4, 3, 7, 54.119314448377594));
		qTable.add(array("qHSV", 5, 6, 3, 54.240483810016585));
		qTable.add(array("qRGB", 6, 6, 2, 55.51362394958891));
		qTable.add(array("qRGB", 7, 2, 5, 55.72738548239443));
		qTable.add(array("qRGB", 6, 2, 6, 55.74309141050442));
		qTable.add(array("qRGB", 2, 6, 6, 59.53589671516055));
		qTable.add(array("qHSV", 3, 7, 4, 63.157139098842364));
		qTable.add(array("qHSV", 2, 7, 5, 63.28833907338459));
		qTable.add(array("qHSV", 1, 6, 7, 63.50457128922028));
		qTable.add(array("qHSV", 4, 7, 3, 65.29115018166584));
		qTable.add(array("qHSV", 1, 7, 6, 66.39194736243954));
		qTable.add(array("qHSV", 5, 2, 7, 67.61437384583752));
		qTable.add(array("qRGB", 7, 6, 1, 71.99761869066867));
		qTable.add(array("qHSV", 5, 7, 2, 72.62668860926118));
		qTable.add(array("qRGB", 7, 1, 6, 75.56911192801118));
		qTable.add(array("qHSV", 6, 5, 3, 81.33113264372975));
		qTable.add(array("qHSV", 6, 4, 4, 81.78259962534494));
		qTable.add(array("qHSV", 6, 3, 5, 82.54804950625551));
		qTable.add(array("qHSV", 6, 6, 2, 82.64653580433713));
		qTable.add(array("qHSV", 0, 7, 7, 82.98009795855553));
		qTable.add(array("qHSV", 6, 2, 6, 84.49086288438515));
		qTable.add(array("qHSV", 6, 7, 1, 90.3539481096517));
		qTable.add(array("qRGB", 2, 5, 7, 90.52673250324939));
		qTable.add(array("qRGB", 3, 4, 7, 93.04225296123272));
		qTable.add(array("qRGB", 1, 6, 7, 94.15118331333835));
		qTable.add(array("qRGB", 4, 3, 7, 96.30652031331188));
		qTable.add(array("qHSV", 6, 1, 7, 96.66311028783815));
		qTable.add(array("qRGB", 5, 2, 7, 99.9525311596843));
		qTable.add(array("qRGB", 2, 7, 5, 105.85586615081006));
		qTable.add(array("qRGB", 1, 7, 6, 106.13379451983877));
		qTable.add(array("qRGB", 6, 1, 7, 106.30844111204077));
		qTable.add(array("qRGB", 3, 7, 4, 108.55788479941238));
		qTable.add(array("qRGB", 4, 7, 3, 111.1485560507927));
		qTable.add(array("qRGB", 5, 7, 2, 113.84057827200076));
		qTable.add(array("qRGB", 6, 7, 1, 119.0611377850553));
		qTable.add(array("qRGB", 0, 7, 7, 121.61122326525965));
		qTable.add(array("qRGB", 7, 0, 7, 122.22938888033444));
		qTable.add(array("qHSV", 7, 7, 0, 130.39792724870412));
		qTable.add(array("qRGB", 7, 7, 0, 133.93140854155877));
		qTable.add(array("qHSV", 7, 6, 1, 141.57678709847553));
		qTable.add(array("qHSV", 7, 5, 2, 148.3157517124646));
		qTable.add(array("qHSV", 7, 4, 3, 151.85812919582494));
		qTable.add(array("qHSV", 7, 1, 6, 152.75698339389902));
		qTable.add(array("qHSV", 7, 3, 4, 153.36844533228413));
		qTable.add(array("qHSV", 7, 2, 5, 153.50798443432024));
		qTable.add(array("qHSV", 7, 0, 7, 154.42826003900322));
		qTable.add(array("qRGB", 5, 5, 5, 29.16035761415699));
		qTable.add(array("qRGB", 6, 5, 4, 31.076711561370868));
		qTable.add(array("qRGB", 6, 4, 5, 32.067109009705945));
		qTable.add(array("qHSV", 4, 5, 6, 36.083595936249296));
		qTable.add(array("qHSV", 4, 6, 5, 41.964190211364595));
		qTable.add(array("qHSV", 3, 6, 6, 42.29930100726572));
		qTable.add(array("qRGB", 4, 5, 6, 44.727040748103285));
		qTable.add(array("qRGB", 5, 4, 6, 44.968623478214795));
		qTable.add(array("qRGB", 7, 4, 4, 47.278835169696876));
		qTable.add(array("qHSV", 5, 5, 5, 48.217951512529));
		qTable.add(array("qRGB", 5, 6, 4, 49.01993078520322));
		qTable.add(array("qRGB", 4, 6, 5, 49.51021618599725));
		qTable.add(array("qHSV", 5, 4, 6, 49.547304173959944));
		qTable.add(array("qRGB", 7, 5, 3, 50.45381424547857));
		qTable.add(array("qRGB", 6, 3, 6, 53.916454183431405));
		qTable.add(array("qRGB", 6, 6, 3, 54.37236041600201));
		qTable.add(array("qRGB", 7, 3, 5, 54.41183262515647));
		qTable.add(array("qHSV", 5, 6, 4, 54.45350302726497));
		qTable.add(array("qHSV", 3, 5, 7, 55.244904587366044));
		qTable.add(array("qHSV", 4, 4, 7, 55.64610512093585));
		qTable.add(array("qRGB", 3, 6, 6, 59.209737174708884));
		qTable.add(array("qHSV", 2, 6, 7, 63.6751728390727));
		qTable.add(array("qHSV", 3, 7, 5, 63.88987040305082));
		qTable.add(array("qHSV", 4, 7, 4, 65.43916877735103));
		qTable.add(array("qHSV", 2, 7, 6, 66.50671872962488));
		qTable.add(array("qHSV", 5, 3, 7, 67.9918424704696));
		qTable.add(array("qRGB", 7, 6, 2, 71.40548869524387));
		qTable.add(array("qHSV", 5, 7, 3, 72.65552942805532));
		qTable.add(array("qRGB", 7, 2, 6, 74.539198911411));
		qTable.add(array("qHSV", 6, 5, 4, 81.28294078298102));
		qTable.add(array("qHSV", 6, 4, 5, 81.90954861718659));
		qTable.add(array("qHSV", 6, 6, 3, 82.60810662817708));
		qTable.add(array("qHSV", 1, 7, 7, 83.01474461331952));
		qTable.add(array("qHSV", 6, 3, 6, 84.12128748832396));
		qTable.add(array("qHSV", 6, 7, 2, 90.30191795302524));
		qTable.add(array("qRGB", 3, 5, 7, 90.44343880501384));
		qTable.add(array("qRGB", 4, 4, 7, 93.39992710387831));
		qTable.add(array("qRGB", 2, 6, 7, 93.98542612049617));
		qTable.add(array("qHSV", 6, 2, 7, 96.531808332587));
		qTable.add(array("qRGB", 5, 3, 7, 97.98623863661277));
		qTable.add(array("qRGB", 6, 2, 7, 105.18986761294468));
		qTable.add(array("qRGB", 3, 7, 5, 105.72241391531153));
		qTable.add(array("qRGB", 2, 7, 6, 105.9861999725296));
		qTable.add(array("qRGB", 4, 7, 4, 108.64397196690527));
		qTable.add(array("qRGB", 5, 7, 3, 112.1823057298339));
		qTable.add(array("qRGB", 6, 7, 2, 118.1068250687499));
		qTable.add(array("qRGB", 1, 7, 7, 121.49344484417644));
		qTable.add(array("qRGB", 7, 1, 7, 121.63454416537526));
		qTable.add(array("qHSV", 7, 7, 1, 130.27815752171145));
		qTable.add(array("qRGB", 7, 7, 1, 133.45192537538827));
		qTable.add(array("qHSV", 7, 6, 2, 141.4759110619432));
		qTable.add(array("qHSV", 7, 5, 3, 148.09813296839445));
		qTable.add(array("qHSV", 7, 4, 4, 151.41705304769513));
		qTable.add(array("qHSV", 7, 2, 6, 152.30122027302545));
		qTable.add(array("qHSV", 7, 3, 5, 152.53646872834864));
		qTable.add(array("qHSV", 7, 1, 7, 154.0768707620358));
		qTable.add(array("qRGB", 6, 5, 5, 35.400137435029016));
		qTable.add(array("qRGB", 5, 5, 6, 45.8233771590495));
		qTable.add(array("qHSV", 4, 6, 6, 46.40245579086771));
		qTable.add(array("qRGB", 5, 6, 5, 49.782515993143186));
		qTable.add(array("qRGB", 7, 5, 4, 51.041409413692236));
		qTable.add(array("qRGB", 6, 4, 6, 51.54216148099061));
		qTable.add(array("qHSV", 5, 5, 6, 51.940531483076605));
		qTable.add(array("qRGB", 7, 4, 5, 53.137756535881834));
		qTable.add(array("qRGB", 6, 6, 4, 53.14396294559098));
		qTable.add(array("qHSV", 5, 6, 5, 55.228715876666485));
		qTable.add(array("qRGB", 4, 6, 6, 58.79585620026274));
		qTable.add(array("qHSV", 4, 5, 7, 59.203972102699964));
		qTable.add(array("qHSV", 3, 6, 7, 64.44018880057266));
		qTable.add(array("qHSV", 4, 7, 5, 66.07804953551427));
		qTable.add(array("qHSV", 3, 7, 6, 67.02198891573136));
		qTable.add(array("qHSV", 5, 4, 7, 68.82377873985625));
		qTable.add(array("qRGB", 7, 6, 3, 70.45097650807729));
		qTable.add(array("qHSV", 5, 7, 4, 72.72379724972508));
		qTable.add(array("qRGB", 7, 3, 6, 72.7496334594355));
		qTable.add(array("qHSV", 6, 5, 5, 81.45839848659669));
		qTable.add(array("qHSV", 6, 6, 4, 82.57432023168315));
		qTable.add(array("qHSV", 2, 7, 7, 83.09689252187526));
		qTable.add(array("qHSV", 6, 4, 6, 83.60367097668954));
		qTable.add(array("qHSV", 6, 7, 3, 90.24254703335666));
		qTable.add(array("qRGB", 4, 5, 7, 90.47157622213018));
		qTable.add(array("qRGB", 3, 6, 7, 93.69327279840577));
		qTable.add(array("qRGB", 5, 4, 7, 94.78848826316275));
		qTable.add(array("qHSV", 6, 3, 7, 96.34074236006498));
		qTable.add(array("qRGB", 6, 3, 7, 103.09409877318586));
		qTable.add(array("qRGB", 4, 7, 5, 105.64751778991774));
		qTable.add(array("qRGB", 3, 7, 6, 105.73289902046317));
		qTable.add(array("qRGB", 5, 7, 4, 109.50856313511423));
		qTable.add(array("qRGB", 6, 7, 3, 116.35263661572063));
		qTable.add(array("qRGB", 7, 2, 7, 120.47091510995996));
		qTable.add(array("qRGB", 2, 7, 7, 121.25909325650642));
		qTable.add(array("qHSV", 7, 7, 2, 130.189412070187));
		qTable.add(array("qRGB", 7, 7, 2, 132.51054904265393));
		qTable.add(array("qHSV", 7, 6, 3, 141.2750518038755));
		qTable.add(array("qHSV", 7, 5, 4, 147.68593530869273));
		qTable.add(array("qHSV", 7, 4, 5, 150.63169725118138));
		qTable.add(array("qHSV", 7, 3, 6, 151.39341368885468));
		qTable.add(array("qHSV", 7, 2, 7, 153.7261405181208));
		qTable.add(array("qRGB", 6, 5, 6, 51.16588678216485));
		qTable.add(array("qRGB", 6, 6, 5, 53.65792546587413));
		qTable.add(array("qRGB", 7, 5, 5, 54.86314628308258));
		qTable.add(array("qHSV", 5, 6, 6, 58.5541643813996));
		qTable.add(array("qRGB", 5, 6, 6, 58.88231069937044));
		qTable.add(array("qHSV", 4, 6, 7, 67.29232843725568));
		qTable.add(array("qHSV", 4, 7, 6, 68.98398547192));
		qTable.add(array("qRGB", 7, 6, 4, 69.35909510351173));
		qTable.add(array("qRGB", 7, 4, 6, 70.20612999951547));
		qTable.add(array("qHSV", 5, 5, 7, 70.9522982391356));
		qTable.add(array("qHSV", 5, 7, 5, 73.15155535858463));
		qTable.add(array("qHSV", 6, 6, 5, 82.78120697495775));
		qTable.add(array("qHSV", 6, 5, 6, 83.28443665149418));
		qTable.add(array("qHSV", 3, 7, 7, 83.45320638949539));
		qTable.add(array("qHSV", 6, 7, 4, 90.172638730084));
		qTable.add(array("qRGB", 5, 5, 7, 91.24646560431594));
		qTable.add(array("qRGB", 4, 6, 7, 93.2732797260707));
		qTable.add(array("qHSV", 6, 4, 7, 96.09723782084295));
		qTable.add(array("qRGB", 6, 4, 7, 99.54726990857749));
		qTable.add(array("qRGB", 4, 7, 6, 105.39366045848752));
		qTable.add(array("qRGB", 5, 7, 5, 106.18657681840735));
		qTable.add(array("qRGB", 6, 7, 4, 113.434492479518));
		qTable.add(array("qRGB", 7, 3, 7, 118.27215956840143));
		qTable.add(array("qRGB", 3, 7, 7, 120.8193361510788));
		qTable.add(array("qHSV", 7, 7, 3, 130.01513071046674));
		qTable.add(array("qRGB", 7, 7, 3, 130.72885419357505));
		qTable.add(array("qHSV", 7, 6, 4, 140.9012578035098));
		qTable.add(array("qHSV", 7, 5, 5, 146.95565866567614));
		qTable.add(array("qHSV", 7, 4, 6, 149.59413316853056));
		qTable.add(array("qHSV", 7, 3, 7, 153.00106824868672));
		qTable.add(array("qRGB", 6, 6, 6, 62.13342555613777));
		qTable.add(array("qRGB", 7, 5, 6, 68.76461465385874));
		qTable.add(array("qRGB", 7, 6, 5, 69.64939924785347));
		qTable.add(array("qHSV", 5, 7, 6, 75.49661507284307));
		qTable.add(array("qHSV", 5, 6, 7, 76.37570829713836));
		qTable.add(array("qHSV", 6, 6, 6, 84.60279131029957));
		qTable.add(array("qHSV", 4, 7, 7, 84.87036525141197));
		qTable.add(array("qHSV", 6, 7, 5, 90.25307437485817));
		qTable.add(array("qRGB", 5, 6, 7, 93.09297348388682));
		qTable.add(array("qRGB", 6, 5, 7, 95.09765215140195));
		qTable.add(array("qHSV", 6, 5, 7, 96.05791272541956));
		qTable.add(array("qRGB", 5, 7, 6, 105.35293816829977));
		qTable.add(array("qRGB", 6, 7, 5, 109.55213823708338));
		qTable.add(array("qRGB", 7, 4, 7, 114.43406884005788));
		qTable.add(array("qRGB", 4, 7, 7, 120.06463886098378));
		qTable.add(array("qRGB", 7, 7, 4, 127.61800680953633));
		qTable.add(array("qHSV", 7, 7, 4, 129.69888897137199));
		qTable.add(array("qHSV", 7, 6, 5, 140.26914343765014));
		qTable.add(array("qHSV", 7, 5, 6, 146.10742102345884));
		qTable.add(array("qHSV", 7, 4, 7, 151.5596422793711));
		qTable.add(array("qRGB", 7, 6, 6, 76.82768387008761));
		qTable.add(array("qHSV", 5, 7, 7, 89.91254983728324));
		qTable.add(array("qHSV", 6, 7, 6, 91.6934546217888));
		qTable.add(array("qRGB", 6, 6, 7, 95.13524389792414));
		qTable.add(array("qHSV", 6, 6, 7, 97.3143392560389));
		qTable.add(array("qRGB", 6, 7, 6, 107.54519533200947));
		qTable.add(array("qRGB", 7, 5, 7, 109.04192747205848));
		qTable.add(array("qRGB", 5, 7, 7, 119.07036843710114));
		qTable.add(array("qRGB", 7, 7, 5, 123.04945636715787));
		qTable.add(array("qHSV", 7, 7, 5, 129.19031913082105));
		qTable.add(array("qHSV", 7, 6, 6, 139.72383239068853));
		qTable.add(array("qHSV", 7, 5, 7, 148.72214401655344));
		qTable.add(array("qHSV", 6, 7, 7, 103.02970911320593));
		qTable.add(array("qRGB", 7, 6, 7, 106.51658523554815));
		qTable.add(array("qRGB", 6, 7, 7, 119.09765442358427));
		qTable.add(array("qRGB", 7, 7, 6, 119.10670119292193));
		qTable.add(array("qHSV", 7, 7, 6, 129.00909805599323));
		qTable.add(array("qHSV", 7, 6, 7, 143.4394848729931));
		qTable.add(array("qRGB", 7, 7, 7, 126.23189633081189));
		qTable.add(array("qHSV", 7, 7, 7, 134.22861938161395));
		
		for (final Object[] row : qTable) {
			final String type = (String) row[0];
			
			/*if ("qHSV".equals(type))*/ {
				final int qA = ((Number) row[1]).intValue();
				final int qB = ((Number) row[2]).intValue();
				final int qC = ((Number) row[3]).intValue();
				final int q = qA + qB + qC;
				
				if (quantizers.size() <= q) {
					quantizers.add(ColorQuantizer.newQuantizer(type, qA, qB, qC));
				}
			}
		}
	}
	
	public static final int iround(final double value) {
		return (int) round(value);
	}
	
	public static final void shutdownAndWait(final ExecutorService executor, final long milliseconds) {
		executor.shutdown();
		
		try {
			executor.awaitTermination(milliseconds, TimeUnit.MILLISECONDS);
		} catch (final InterruptedException exception) {
			exception.printStackTrace();
		}
	}
	
	/**
	 * @author codistmonk (creation 2014-04-10)
	 */
	public static abstract class ColorQuantizer implements Serializable {
		
		private final int qA;
		
		private final int qB;
		
		private final int qC;
		
		protected ColorQuantizer(final int qA, final int qB, final int qC) {
			this.qA = qA;
			this.qB = qB;
			this.qC = qC;
		}
		
		public final int quantize(final int rgb) {
			final int[] abc = new int[3];
			
			this.rgbToABC(rgb, abc);
			
			abc[0] = ((abc[0] & 0xFF) >> this.qA) << this.qA;
			abc[1] = ((abc[1] & 0xFF) >> this.qB) << this.qB;
			abc[2] = ((abc[2] & 0xFF) >> this.qC) << this.qC;
			
			this.abcToRGB(abc, abc);
			
			return (abc[0] << 16) | (abc[1] << 8) | (abc[2] << 0);
		}
		
		public final int getBinCount() {
			return this.pack(0x00FFFFFF) + 1;
		}
		
		public final int pack(final int rgb) {
			final int[] abc = new int[3];
			
			this.rgbToABC(rgb, abc);
			
			final int a = (abc[0] & 0xFF) >> this.qA;
			final int b = (abc[1] & 0xFF) >> this.qB;
			final int c = (abc[2] & 0xFF) >> this.qC;
			
			return (a << (16 - this.qB - this.qC)) | (b << (8 - this.qC)) | (c << 0);
		}
		
		public abstract String getType();
		
		@Override
		public final String toString() {
			return this.getType() + this.qA + "" + this.qB + "" + this.qC;
		}
		
		protected abstract void rgbToABC(final int rgb, final int[] abc);
		
		protected abstract void abcToRGB(final int[] abc, final int[] rgb);
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -5601399591176973099L;
		
		public static final ColorQuantizer newQuantizer(final String type, final int qA, final int qB, final int qC) {
			if (RGBQuantizer.TYPE.equals(type)) {
				return new RGBQuantizer(qA, qC, qB);
			}
			
			if (HSVQuantizer.TYPE.equals(type)) {
				return new HSVQuantizer(qA, qC, qB);
			}
			
			if (XYZQuantizer.TYPE.equals(type)) {
				return new XYZQuantizer(qA, qC, qB);
			}
			
			if (CIELABQuantizer.TYPE.equals(type)) {
				return new CIELABQuantizer(qA, qC, qB);
			}
			
			throw new IllegalArgumentException();
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2014-04-10)
	 */
	public static final class RGBQuantizer extends ColorQuantizer {
		
		public RGBQuantizer(final int qR, final int qG, final int qB) {
			super(qR, qG, qB);
		}
		
		@Override
		public final String getType() {
			return TYPE;
		}
		
		@Override
		protected final void rgbToABC(final int rgb, final int[] abc) {
			rgbToRGB(rgb, abc);
		}
		
		@Override
		protected final void abcToRGB(final int[] abc, final int[] rgb) {
			System.arraycopy(abc, 0, rgb, 0, abc.length);
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -739890245396913487L;
		
		/**
		 * {@value}.
		 */
		public static final String TYPE = "qRGB";
		
	}
	
	/**
	 * @author codistmonk (creation 2014-04-10)
	 */
	public static final class HSVQuantizer extends ColorQuantizer {
		
		public HSVQuantizer(final int qH, final int qS, final int qV) {
			super(qH, qS, qV);
		}
		
		@Override
		public final String getType() {
			return TYPE;
		}
		
		@Override
		protected final void rgbToABC(final int rgb, final int[] abc) {
			rgbToRGB(rgb, abc);
			rgbToHSV(abc, abc);
		}
		
		@Override
		protected final void abcToRGB(final int[] abc, final int[] rgb) {
			hsvToRGB(abc, rgb);
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -739890245396913487L;
		
		/**
		 * {@value}.
		 */
		public static final String TYPE = "qHSV";
		
	}
	
	/**
	 * @author codistmonk (creation 2014-04-10)
	 */
	public static final class XYZQuantizer extends ColorQuantizer {
		
		public XYZQuantizer(final int qX, final int qY, final int qZ) {
			super(qX, qY, qZ);
		}
		
		@Override
		public final String getType() {
			return TYPE;
		}
		
		@Override
		protected final void rgbToABC(final int rgb, final int[] abc) {
			final float[] xyz = ColorTools2.rgbToXYZ(rgbToRGB(rgb, abc));
			
			for (int i = 0; i < abc.length; ++i) {
				abc[i] = iround(xyzStatistics[i].getNormalizedValue(xyz[i]) * 255.0);
			}
		}
		
		@Override
		protected final void abcToRGB(final int[] abc, final int[] rgb) {
			final float[] xyz = new float[abc.length];
			
			for (int i = 0; i < abc.length; ++i) {
				xyz[i] = (float) xyzStatistics[i].getDenormalizedValue(abc[i] / 255.0);
			}
			
			ColorTools2.xyzToRGB(xyz, rgb);
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -5420720995966390554L;
		
		/**
		 * {@value}.
		 */
		public static final String TYPE = "qXYZ";
		
	}
	
	/**
	 * @author codistmonk (creation 2014-04-10)
	 */
	public static final class CIELABQuantizer extends ColorQuantizer {
		
		public CIELABQuantizer(final int qL, final int qA, final int qB) {
			super(qL, qA, qB);
		}
		
		@Override
		public final String getType() {
			return TYPE;
		}
		
		@Override
		protected final void rgbToABC(final int rgb, final int[] abc) {
			final float[] cielab = ColorTools2.rgbToCIELAB(rgbToRGB(rgb, abc));
			
			for (int i = 0; i < abc.length; ++i) {
				abc[i] = iround(cielabStatistics[i].getNormalizedValue(cielab[i]) * 255.0);
			}
		}
		
		@Override
		protected final void abcToRGB(final int[] abc, final int[] rgb) {
			final float[] cielab = new float[abc.length];
			
			for (int i = 0; i < abc.length; ++i) {
				cielab[i] = (float) cielabStatistics[i].getDenormalizedValue(abc[i] / 255.0);
			}
			
			ColorTools2.cielabToRGB(cielab, rgb);
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -5420720995966390554L;
		
		/**
		 * {@value}.
		 */
		public static final String TYPE = "qCIE-L*a*b*";
		
	}
	
	/**
	 * @author codistmonk (creation 2014-04-24)
	 */
	public static final class ColorTools1 {
		
		private ColorTools1() {
			throw new IllegalInstantiationException();
		}
		
		public static final int[] ints(final int... result) {
			return result;
		}
		
		public static final int packRGB(final int[] rgb) {
			return 0xFF000000 | ((rgb[0] & 0xFF) << 16) | ((rgb[1] & 0xFF) << 8) |((rgb[2] & 0xFF) << 0);
		}
		
		public static final int packCIELAB(final float[] cielab) {
			return 0xFF000000 | (round((cielab[0] % 100F) * 2.55F) << 16) | (round((cielab[1] % 100F) * 2.55F) << 8) | (round((cielab[2] % 100F) * 2.55F) << 0);
		}
		
		public static final int[] rgbToRGB(final int rgb, final int[] result) {
			result[0] = (rgb >> 16) & 0xFF;
			result[1] = (rgb >> 8) & 0xFF;
			result[2] = (rgb >> 0) & 0xFF;
			
			return result;
		}
		
		public static final int[] rgbToHSV(final int[] rgb, final int[] result) {
			final float[] hsv = new float[3];
			
			Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], hsv);
			
			result[0] = round(hsv[0] * 255F);
			result[1] = round(hsv[1] * 255F);
			result[2] = round(hsv[2] * 255F);
			
			return result;
		}
		
		public static final int[] hsvToRGB(final int[] hsv, final int[] result) {
			return rgbToRGB(Color.HSBtoRGB(hsv[0] / 255F, hsv[1] / 255F, hsv[2] / 255F), result);
		}
		
		public static final float[] rgbToCIELAB(final int[] rgb) {
			return rgbToCIELAB(rgb, new float[3]);
		}
		
		public static final float[] rgbToCIELAB(final int[] rgb, final float[] result) {
			return xyzToCIELAB(rgbToXYZ(rgb, result));
		}
		
		public static final int[] cielabToRGB(final float[] cielab) {
			return cielabToRGB(cielab, new int[3]);
		}
		
		public static final int[] cielabToRGB(final float[] cielab, final int[] result) {
			return xyzToRGB(cielabToXYZ(cielab, new float[3]), result);
		}
		
		public static final float[] rgbToXYZ(final int[] rgb) {
			return rgbToXYZ(rgb, new float[3]);
		}
		
		public static final float[] rgbToXYZ(final int[] rgb, final float[] result) {
			// http://en.wikipedia.org/wiki/CIE_1931_color_space
			
			final float r = rgb[0] / 255F;
			final float g = rgb[1] / 255F;
			final float b = rgb[2] / 255F;
			final float b21 = 0.17697F;
			
			result[0] = (0.49F * r + 0.31F * g + 0.20F * b) / b21;
			result[1] = (b21 * r + 0.81240F * g + 0.01063F * b) / b21;
			result[2] = (0.00F * r + 0.01F * g + 0.99F * b) / b21;
			
			return result;
		}
		
		public static final int[] xyzToRGB(final float[] xyz) {
			return xyzToRGB(xyz, new int[3]);
		}
		
		public static final int[] xyzToRGB(final float[] xyz, final int[] result) {
			// http://en.wikipedia.org/wiki/CIE_1931_color_space
			
			final float x = xyz[0];
			final float y = xyz[1];
			final float z = xyz[2];
			
			result[0] = round(255F * (0.41847F * x - 0.15866F * y - 0.082835F * z));
			result[1] = round(255F * (-0.091169F * x + 0.25243F * y + 0.015708F * z));
			result[2] = round(255F * (0.00092090F * x - 0.0025498F * y + 0.17860F * z));
			
			return result;
		}
		
		public static final float[] xyzToCIELAB(final float[] abc) {
			return xyzToCIELAB(abc, abc);
		}
		
		public static final float[] xyzToCIELAB(final float[] xyz, final float[] result) {
			// http://en.wikipedia.org/wiki/Illuminant_D65
			
			final float d65X = 0.95047F;
			final float d65Y = 1.0000F;
			final float d65Z = 1.08883F;
			
			// http://en.wikipedia.org/wiki/Lab_color_space
			
			final float fX = f(xyz[0] / d65X);
			final float fY = f(xyz[1] / d65Y);
			final float fZ = f(xyz[2] / d65Z);
			
			result[0] = 116F * fY - 16F;
			result[1] = 500F * (fX - fY);
			result[2] = 200F * (fY - fZ);
			
			return result;
		}
		
		public static final float[] cielabToXYZ(final float[] abc) {
			return cielabToXYZ(abc, abc);
		}
		
		public static final float[] cielabToXYZ(final float[] cielab, final float[] result) {
			// http://en.wikipedia.org/wiki/Illuminant_D65
			
			final float d65X = 0.95047F;
			final float d65Y = 1.0000F;
			final float d65Z = 1.08883F;
			
			// http://en.wikipedia.org/wiki/Lab_color_space
			
			final float lStar = cielab[0];
			final float aStar = cielab[1];
			final float bStar = cielab[2];
			final float c = (lStar + 16F) / 116F;
			
			result[0] = d65X * fInv(c + aStar / 500F);
			result[1] = d65Y * fInv(c);
			result[2] = d65Z * fInv(c - bStar / 200F);
			
			return result;
		}
		
		public static final float f(final float t) {
			return cube(6F / 29F) < t ? (float) pow(t, 1.0 / 3.0) : square(29F / 6F) * t / 3F + 4F / 29F;
		}
		
		public static final float fInv(final float t) {
			return 6F / 29F < t ? cube(t) : 3F * square(6F / 29F) * (t - 4F / 29F);
		}
		
		public static final float square(final float value) {
			return value * value;
		}
		
		public static final float cube(final float value) {
			return value * value * value;
		}
		
		public static final int[] quantize(final int[] abc, final int q, final int[] result) {
			return quantize(abc, q, q, q, result);
		}
		
		public static final int[] quantize(final int[] abc, final int qA, final int qB, final int qC, final int[] result) {
			result[0] = abc[0] & ((~0) << qA);
			result[1] = abc[1] & ((~0) << qB);
			result[2] = abc[2] & ((~0) << qC);
			
			return result;
		}
		
		public static final int distance1(final int[] abc1, final int[] abc2) {
			final int n = abc1.length;
			int result = 0;
			
			for (int i = 0; i < n; ++i) {
				result += abs(abc1[i] - abc2[i]);
			}
			
			return result;
		}
		
		public static final double distance2(final float[] abc1, final float[] abc2) {
			final int n = abc1.length;
			double sum = 0.0;
			
			for (int i = 0; i < n; ++i) {
				sum += square(abc1[i] - abc2[i]);
			}
			
			return sqrt(sum);
		}
		
		public static final int min(final int... values) {
			int result = Integer.MAX_VALUE;
			
			for (final int value : values) {
				if (value < result) {
					result = value;
				}
			}
			
			return result;
		}
		
		public static final int max(final int... values) {
			int result = Integer.MIN_VALUE;
			
			for (final int value : values) {
				if (result < value) {
					result = value;
				}
			}
			
			return result;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2014-04-24)
	 */
	public static final class ColorTools2 {
		
		private ColorTools2() {
			throw new IllegalInstantiationException();
		}
		
		public static final float[] floats(final float... result) {
			return result;
		}
		
		public static final int[] xyzToRGB(final float[] xyz) {
			return xyzToRGB(xyz, new int[3]);
		}
		
		public static final int[] xyzToRGB(final float[] xyz, final int[] rgb) {
			// http://www.easyrgb.com/index.php?X=MATH&H=01#text1
			
			final double x = xyz[0] / 100.0;        //xyz[0] from 0 to  95.047      (Observer = 2°, Illuminant = D65)
			final double y = xyz[1] / 100.0;        //xyz[1] from 0 to 100.000
			final double z = xyz[2] / 100.0;        //xyz[2] from 0 to 108.883

			double r = x *  3.2406 + y * -1.5372F + z * -0.4986;
			double g = x * -0.9689 + y *  1.8758F + z *  0.0415;
			double b = x *  0.0557 + y * -0.2040F + z *  1.0570;

			if (r > 0.0031308) {
				r = 1.055 * pow(r, 1.0 / 2.4) - 0.055;
			} else {
				r = 12.92 * r;
			}
			
			if ( g > 0.0031308 ) {
				g = 1.055 * pow(g, 1.0 / 2.4) - 0.055;
			} else {
				g = 12.92 * g;
			}
			
			if (b > 0.0031308) {
				b = 1.055 * pow(b, 1.0 / 2.4) - 0.055;
			} else {
				b = 12.92 * b;
			}

			rgb[0] = (int) round(r * 255.0);
			rgb[1] = (int) round(g * 255.0);
			rgb[2] = (int) round(b * 255.0);
					
			return rgb;
		}
		
		public static final float[] rgbToXYZ(final int[] rgb) {
			return rgbToXYZ(rgb, new float[3]);
		}
		
		public static final float[] rgbToXYZ(final int[] rgb, final float[] xyz) {
			// http://www.easyrgb.com/index.php?X=MATH&H=02#text2
			
			double r = rgb[0] / 255.0;        //R from 0 to 255
			double g = rgb[1] / 255.0;        //G from 0 to 255
			double b = rgb[2] / 255.0;        //B from 0 to 255

			if (r > 0.04045) {
				r = pow((r + 0.055) / 1.055, 2.4);
			} else {
				r = r / 12.92;
			}
			
			if (g > 0.04045) {
				g = pow((g + 0.055) / 1.055, 2.4);
			} else {
				g = g / 12.92;
			}
			
			if (b > 0.04045) {
				b = pow((b + 0.055) / 1.055, 2.4);
			} else {
				b = b / 12.92;
			}
			
			r = r * 100.0;
			g = g * 100.0;
			b = b * 100.0;
			
			//Observer = 2°, Illuminant = D65
			xyz[0] = (float) (r * 0.4124 + g * 0.3576 + b * 0.1805);
			xyz[1] = (float) (r * 0.2126 + g * 0.7152 + b * 0.0722);
			xyz[2] = (float) (r * 0.0193 + g * 0.1192 + b * 0.9505);
			
			return xyz;
		}
		
		public static final float[] xyzToCIELAB(final float[] abc) {
			return xyzToCIELAB(abc, abc);
		}
		
		public static final float[] xyzToCIELAB(final float[] xyz, final float[] cielab) {
			return xyzToCIELAB(xyz, Illuminant.D65.getX2Y2Z2(), cielab);
		}
		
		public static final float[] xyzToCIELAB(final float[] xyz, final float[] referenceXYZ, final float[] cielab) {
			// http://www.easyrgb.com/index.php?X=MATH&H=07#text7
			
			double x = xyz[0] / referenceXYZ[0];
			double y = xyz[1] / referenceXYZ[1];
			double z = xyz[2] / referenceXYZ[2];
			
			if (x > 0.008856) {
				x = pow(x, 1.0 / 3.0);
			} else {
				x = 7.787 * x + 16.0 / 116.0;
			}
			
			if ( y > 0.008856 ) {
				y = pow(y, 1.0 / 3.0);
			} else {
				y = 7.787 * y + 16.0 / 116.0;
			}
			
			if (z > 0.008856) {
				z = pow(z, 1.0 / 3.0);
			} else {
				z = 7.787 * z + 16.0 / 116.0;
			}
			
			cielab[0] = (float) (116.0 * y - 16.0); // L*
			cielab[1] = (float) (500.0 * (x - y)); // a*
			cielab[2] = (float) (200.0 * (y - z)); // b*
			
			return cielab;
		}
		
		public static final float[] cielabToXYZ(final float[] abc) {
			return cielabToXYZ(abc, abc);
		}
		
		public static final float[] cielabToXYZ(final float[] cielab, final float[] xyz) {
			return cielabToXYZ(cielab, Illuminant.D65.getX2Y2Z2(), xyz);
		}
		
		public static final float[] cielabToXYZ(final float[] cielab, final float[] referenceXYZ, final float[] xyz) {
			// http://www.easyrgb.com/index.php?X=MATH&H=08#text8
			
			double y = (cielab[0] + 16.0) / 116.0;
			double x = cielab[1] / 500.0 + y;
			double z = y - cielab[2] / 200.0;
			
			if (cube(y) > 0.008856) {
				y = cube(y);
			} else {
				y = (y - 16.0 / 116.0) / 7.787;
			}
			
			if (cube(x) > 0.008856) {
				x = cube(x);
			} else {
				x = (x - 16.0 / 116.0) / 7.787;
			}
			
			if (cube(z) > 0.008856) {
				z = cube(z);
			} else {
				z = (z - 16.0 / 116.0) / 7.787;
			}
			
			xyz[0] = (float) (referenceXYZ[0] * x);
			xyz[1] = (float) (referenceXYZ[1] * y);
			xyz[2] = (float) (referenceXYZ[2] * z);
			
			return xyz;
		}
		
		public static final float[] rgbToCIELAB(final int[] rgb) {
			return rgbToCIELAB(rgb, new float[3]);
		}
		
		public static final float[] rgbToCIELAB(final int[] rgb, final float[] cielab) {
			return rgbToCIELAB(rgb, Illuminant.D65.getX2Y2Z2(), cielab);
		}
		
		public static final float[] rgbToCIELAB(final int[] rgb, final float[] referenceXYZ, final float[] cielab) {
			return xyzToCIELAB(rgbToXYZ(rgb, cielab), referenceXYZ, cielab);
		}
		
		public static final int[] cielabToRGB(final float[] cielab) {
			return cielabToRGB(cielab, new int[3]);
		}
		
		public static final int[] cielabToRGB(final float[] cielab, final int[] rgb) {
			return cielabToRGB(cielab, Illuminant.D65.getX2Y2Z2(), rgb);
		}
		
		public static final int[] cielabToRGB(final float[] cielab, final float[] referenceXYZ, final int[] rgb) {
			return xyzToRGB(cielabToXYZ(cielab, referenceXYZ, new float[3]), rgb);
		}
		
		public static final double cube(final double value) {
			return value * value * value;
		}
		
		/**
		 * @author codistmonk (creation 2014-04-24)
		 */
		public static enum Illuminant {
			
			A {
				
				@Override
				public final float[] getX2Y2Z2() {
					return floats(109.850F, 100F, 35.585F);
				}
				
				@Override
				public final float[] getX10Y10Z10() {
					return floats(111.144F, 100F, 35.200F);
				}
				
			}, C {
				
				@Override
				public final float[] getX2Y2Z2() {
					return floats(98.074F, 100F, 118.232F);
				}
				
				@Override
				public final float[] getX10Y10Z10() {
					return floats(97.285F, 100F, 116.145F);
				}
				
			}, D50 {
				
				@Override
				public final float[] getX2Y2Z2() {
					return floats(96.422F, 100F, 82.521F);
				}
				
				@Override
				public final float[] getX10Y10Z10() {
					return floats(96.720F, 100F, 81.427F);
				}
				
			}, D55 {
				
				@Override
				public final float[] getX2Y2Z2() {
					return floats(95.682F, 100F, 92.149F);
				}
				
				@Override
				public final float[] getX10Y10Z10() {
					return floats(95.799F, 100F, 90.926F);
				}
				
			}, D65 {
				
				@Override
				public final float[] getX2Y2Z2() {
					return floats(95.047F, 100F, 108.883F);
				}
				
				@Override
				public final float[] getX10Y10Z10() {
					return floats(94.811F, 100F, 107.304F);
				}
				
			}, D75 {
				
				@Override
				public final float[] getX2Y2Z2() {
					return floats(94.972F, 100F, 122.638F);
				}
				
				@Override
				public final float[] getX10Y10Z10() {
					return floats(94.416F, 100F, 120.641F);
				}
				
			}, F2 {
				
				@Override
				public final float[] getX2Y2Z2() {
					return floats(99.187F, 100F, 67.395F);
				}
				
				@Override
				public final float[] getX10Y10Z10() {
					return floats(103.280F, 100F, 69.026F);
				}
				
			}, F7 {
				
				@Override
				public final float[] getX2Y2Z2() {
					return floats(95.044F, 100F, 108.755F);
				}
				
				@Override
				public final float[] getX10Y10Z10() {
					return floats(95.792F, 100F, 107.687F);
				}
				
			}, F11 {
				
				@Override
				public final float[] getX2Y2Z2() {
					return floats(100.966F, 100F, 64.370F);
				}
				
				@Override
				public final float[] getX10Y10Z10() {
					return floats(103.866F, 100F, 65.627F);
				}
				
			};
			
			public abstract float[] getX2Y2Z2();
			
			public abstract float[] getX10Y10Z10();
			
		}
		
	}
	
}
