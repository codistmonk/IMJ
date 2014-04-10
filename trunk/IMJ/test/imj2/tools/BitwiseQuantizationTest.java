package imj2.tools;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import imj2.tools.Image2DComponent.Painter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.MathTools.Statistics;
import net.sourceforge.aprog.tools.Tools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-04-09)
 */
@SuppressWarnings("unchecked")
public final class BitwiseQuantizationTest {
	
//	@Test
	public final void test1() {
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
		Tools.debugPrint(quantizers);
		
		final SimpleImageView imageView = new SimpleImageView();
		final JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, quantizers.size() - 1, 1));
		
		imageView.getPainters().add(new Painter<SimpleImageView>() {
			
			@Override
			public final void paint(final Graphics2D g, final SimpleImageView component,
					final int width, final int height) {
				final ColorQuantizer quantizer = quantizers.get(((Number) spinner.getValue()).intValue());
				final BufferedImage image = imageView.getImage();
				final BufferedImage buffer = imageView.getBufferImage();
				final int w = buffer.getWidth();
				final int h = buffer.getHeight();
				
				for (int y = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x) {
						int north = 0;
						int west = 0;
						int east = 0;
						int south = 0;
						
						if (0 < y) {
							north = quantizer.quantize(image.getRGB(x, y - 1));
						}
						
						if (0 < x) {
							west = quantizer.quantize(image.getRGB(x - 1, y));
						}
						
						if (x + 1 < w) {
							east = quantizer.quantize(image.getRGB(x + 1, y));
						}
						
						if (y + 1 < h) {
							south = quantizer.quantize(image.getRGB(x, y + 1));
						}
						
						final int center = quantizer.quantize(image.getRGB(x, y));
						
						if (min(north, west, east, south) < center) {
							buffer.setRGB(x, y, Color.YELLOW.getRGB());
						}
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
		
		spinner.addChangeListener(new ChangeListener() {
			
			@Override
			public final void stateChanged(final ChangeEvent event) {
				imageView.refreshBuffer();
			}
			
		});
		
		panel.add(horizontalBox(spinner), BorderLayout.NORTH);
		panel.add(imageView, BorderLayout.CENTER);
		
		show(panel, this.getClass().getSimpleName(), true);
	}
	
	private static final List<Object[]> table = new ArrayList<Object[]>();
	
	static final List<ColorQuantizer> quantizers = new ArrayList<ColorQuantizer>();
	
	static {
		table.add(array("qRGB", 0, 0, 0, 0.0));
		table.add(array("qHSV", 0, 0, 0, 0.6955260904279362));
		table.add(array("qRGB", 1, 0, 0, 0.32641454294213));
		table.add(array("qRGB", 0, 0, 1, 0.6240309742241252));
		table.add(array("qRGB", 0, 1, 0, 0.7514458393503941));
		table.add(array("qHSV", 0, 0, 1, 0.8751917410004951));
		table.add(array("qHSV", 0, 1, 0, 1.126961289651885));
		table.add(array("qHSV", 1, 0, 0, 1.7813204349626734));
		table.add(array("qRGB", 1, 0, 1, 0.8583117538819247));
		table.add(array("qRGB", 1, 1, 0, 0.8692188281204635));
		table.add(array("qRGB", 2, 0, 0, 0.9808307435374101));
		table.add(array("qRGB", 0, 1, 1, 1.0380778379038296));
		table.add(array("qHSV", 0, 0, 2, 1.1659382941554577));
		table.add(array("qHSV", 0, 1, 1, 1.2559919477363735));
		table.add(array("qHSV", 1, 0, 1, 1.8951245566432926));
		table.add(array("qRGB", 0, 0, 2, 1.8980446031157745));
		table.add(array("qHSV", 1, 1, 0, 2.092400382244634));
		table.add(array("qHSV", 0, 2, 0, 2.115374741291907));
		table.add(array("qRGB", 0, 2, 0, 2.265005588909907));
		table.add(array("qHSV", 2, 0, 0, 4.528551120210534));
		table.add(array("qRGB", 1, 1, 1, 1.1180287803753424));
		table.add(array("qRGB", 2, 1, 0, 1.2748883526552783));
		table.add(array("qRGB", 2, 0, 1, 1.4067111100652796));
		table.add(array("qHSV", 0, 1, 2, 1.5203314800623147));
		table.add(array("qHSV", 0, 0, 3, 1.809747264609731));
		table.add(array("qRGB", 0, 1, 2, 1.9618195348396559));
		table.add(array("qRGB", 1, 0, 2, 2.064893103362241));
		table.add(array("qHSV", 1, 0, 2, 2.1016815466827374));
		table.add(array("qHSV", 1, 1, 1, 2.177229033053775));
		table.add(array("qHSV", 0, 2, 1, 2.2173938408647693));
		table.add(array("qRGB", 1, 2, 0, 2.2504013406529717));
		table.add(array("qRGB", 0, 2, 1, 2.2932977370369603));
		table.add(array("qRGB", 3, 0, 0, 2.2959598157696686));
		table.add(array("qHSV", 1, 2, 0, 2.8907950047008057));
		table.add(array("qHSV", 0, 3, 0, 4.270360710208371));
		table.add(array("qRGB", 0, 0, 3, 4.52553090293637));
		table.add(array("qHSV", 2, 0, 1, 4.592369919131593));
		table.add(array("qHSV", 2, 1, 0, 4.722213766837409));
		table.add(array("qRGB", 0, 3, 0, 5.333067162914784));
		table.add(array("qHSV", 3, 0, 0, 10.133061431545565));
		table.add(array("qRGB", 2, 1, 1, 1.4880175209433724));
		table.add(array("qRGB", 1, 1, 2, 2.0282165668025764));
		table.add(array("qHSV", 0, 1, 3, 2.152929318600153));
		table.add(array("qRGB", 1, 2, 1, 2.2559129235446513));
		table.add(array("qRGB", 3, 1, 0, 2.370351273948446));
		table.add(array("qHSV", 1, 1, 2, 2.3731733681910288));
		table.add(array("qRGB", 2, 2, 0, 2.389170595189107));
		table.add(array("qHSV", 0, 2, 2, 2.450676449335271));
		table.add(array("qRGB", 2, 0, 2, 2.4962070471616475));
		table.add(array("qHSV", 1, 0, 3, 2.614980863199656));
		table.add(array("qRGB", 3, 0, 1, 2.61968019557456));
		table.add(array("qRGB", 0, 2, 2, 2.775659263227969));
		table.add(array("qHSV", 1, 2, 1, 2.9645149300940132));
		table.add(array("qHSV", 0, 0, 4, 3.308055191838597));
		table.add(array("qRGB", 0, 1, 3, 4.3145002832409265));
		table.add(array("qHSV", 0, 3, 1, 4.356094355535418));
		table.add(array("qRGB", 1, 0, 3, 4.6480678384750584));
		table.add(array("qHSV", 2, 0, 2, 4.720369162242616));
		table.add(array("qHSV", 2, 1, 1, 4.771642243384687));
		table.add(array("qHSV", 1, 3, 0, 4.820816066888919));
		table.add(array("qRGB", 4, 0, 0, 4.951001286754124));
		table.add(array("qRGB", 0, 3, 1, 5.179776824191323));
		table.add(array("qRGB", 1, 3, 0, 5.245476477957572));
		table.add(array("qHSV", 2, 2, 0, 5.275040700873244));
		table.add(array("qHSV", 0, 4, 0, 8.608606861303441));
		table.add(array("qRGB", 0, 0, 4, 10.005984804008115));
		table.add(array("qHSV", 3, 0, 1, 10.16591826453798));
		table.add(array("qHSV", 3, 1, 0, 10.246741129698233));
		table.add(array("qRGB", 0, 4, 0, 11.621132862924227));
		table.add(array("qHSV", 4, 0, 0, 21.441441059860917));
		table.add(array("qRGB", 2, 1, 2, 2.345664153459157));
		table.add(array("qRGB", 2, 2, 1, 2.3803760800798126));
		table.add(array("qRGB", 3, 1, 1, 2.548324173801728));
		table.add(array("qRGB", 1, 2, 2, 2.7386225128738215));
		table.add(array("qHSV", 1, 1, 3, 2.890959844279944));
		table.add(array("qHSV", 0, 2, 3, 3.039789316612684));
		table.add(array("qRGB", 3, 2, 0, 3.0876627555063707));
		table.add(array("qHSV", 1, 2, 2, 3.149855857626206));
		table.add(array("qRGB", 3, 0, 2, 3.5456082043397177));
		table.add(array("qHSV", 0, 1, 4, 3.6408406868479344));
		table.add(array("qHSV", 1, 0, 4, 3.9301965018206424));
		table.add(array("qRGB", 1, 1, 3, 4.377510140614242));
		table.add(array("qRGB", 0, 2, 3, 4.55417176057843));
		table.add(array("qHSV", 0, 3, 2, 4.558363544642229));
		table.add(array("qRGB", 4, 1, 0, 4.866263779024119));
		table.add(array("qHSV", 1, 3, 1, 4.890021661874276));
		table.add(array("qHSV", 2, 1, 2, 4.896690972204208));
		table.add(array("qRGB", 2, 0, 3, 4.975510162691538));
		table.add(array("qHSV", 2, 0, 3, 5.073700328823798));
		table.add(array("qRGB", 1, 3, 1, 5.076416388035941));
		table.add(array("qRGB", 2, 3, 0, 5.186407432321236));
		table.add(array("qRGB", 4, 0, 1, 5.18922966136709));
		table.add(array("qRGB", 0, 3, 2, 5.253668905846729));
		table.add(array("qHSV", 2, 2, 1, 5.321301853278064));
		table.add(array("qHSV", 2, 3, 0, 6.78099035432321));
		table.add(array("qHSV", 0, 0, 5, 6.8001379002246205));
		table.add(array("qHSV", 0, 4, 1, 8.6804212735382));
		table.add(array("qHSV", 1, 4, 0, 8.96150522653321));
		table.add(array("qRGB", 0, 1, 4, 9.621269654573805));
		table.add(array("qRGB", 1, 0, 4, 10.10082618502018));
		table.add(array("qHSV", 3, 0, 2, 10.239604490826345));
		table.add(array("qHSV", 3, 1, 1, 10.273738901017087));
		table.add(array("qRGB", 5, 0, 0, 10.357214019008278));
		table.add(array("qHSV", 3, 2, 0, 10.595095232353103));
		table.add(array("qRGB", 0, 4, 1, 11.345079146799256));
		table.add(array("qRGB", 1, 4, 0, 11.499694035514397));
		table.add(array("qHSV", 0, 5, 0, 16.979860091230222));
		table.add(array("qHSV", 4, 0, 1, 21.447935998355156));
		table.add(array("qHSV", 4, 1, 0, 21.462964882808116));
		table.add(array("qRGB", 0, 0, 5, 21.56144319342579));
		table.add(array("qRGB", 0, 5, 0, 24.732918303318232));
		table.add(array("qHSV", 5, 0, 0, 43.77691374160295));
		table.add(array("qRGB", 2, 2, 2, 2.852922516764376));
		table.add(array("qRGB", 3, 2, 1, 3.0828291421052723));
		table.add(array("qRGB", 3, 1, 2, 3.3041165241821764));
		table.add(array("qHSV", 1, 2, 3, 3.6541400074569506));
		table.add(array("qHSV", 1, 1, 4, 4.215862457035969));
		table.add(array("qHSV", 0, 2, 4, 4.460470627495945));
		table.add(array("qRGB", 1, 2, 3, 4.539080994752359));
		table.add(array("qRGB", 2, 1, 3, 4.6363242231465));
		table.add(array("qRGB", 2, 3, 1, 5.004329263328033));
		table.add(array("qRGB", 4, 1, 1, 5.011027074098335));
		table.add(array("qHSV", 0, 3, 3, 5.062496531035296));
		table.add(array("qHSV", 1, 3, 2, 5.063458476675465));
		table.add(array("qRGB", 1, 3, 2, 5.147515511354933));
		table.add(array("qRGB", 4, 2, 0, 5.167587135912787));
		table.add(array("qHSV", 2, 1, 3, 5.260392593892533));
		table.add(array("qRGB", 3, 3, 0, 5.432007270000069));
		table.add(array("qHSV", 2, 2, 2, 5.446669044137193));
		table.add(array("qRGB", 3, 0, 3, 5.825331577595821));
		table.add(array("qRGB", 4, 0, 2, 5.931000598479003));
		table.add(array("qHSV", 2, 0, 4, 6.082311495314012));
		table.add(array("qRGB", 0, 3, 3, 6.254886430030773));
		table.add(array("qHSV", 2, 3, 1, 6.830073389040216));
		table.add(array("qHSV", 0, 1, 5, 7.122838491638112));
		table.add(array("qHSV", 1, 0, 5, 7.218793962972602));
		table.add(array("qHSV", 0, 4, 2, 8.848843990753025));
		table.add(array("qHSV", 1, 4, 1, 9.024514418743152));
		table.add(array("qRGB", 0, 2, 4, 9.322599398655775));
		table.add(array("qRGB", 1, 1, 4, 9.682368743150985));
		table.add(array("qRGB", 5, 1, 0, 10.172020644792356));
		table.add(array("qHSV", 3, 1, 2, 10.346634555923645));
		table.add(array("qRGB", 2, 0, 4, 10.349131860405612));
		table.add(array("qHSV", 2, 4, 0, 10.378143392591879));
		table.add(array("qHSV", 3, 0, 3, 10.46174533248007));
		table.add(array("qRGB", 5, 0, 1, 10.533497144893856));
		table.add(array("qHSV", 3, 2, 1, 10.62143108459401));
		table.add(array("qRGB", 0, 4, 2, 11.08684873356561));
		table.add(array("qRGB", 1, 4, 1, 11.212816854561149));
		table.add(array("qRGB", 2, 4, 0, 11.32752806549405));
		table.add(array("qHSV", 3, 3, 0, 11.635117350399002));
		table.add(array("qHSV", 0, 0, 6, 15.668032586962413));
		table.add(array("qHSV", 0, 5, 1, 17.04482075137555));
		table.add(array("qHSV", 1, 5, 0, 17.187108424535914));
		table.add(array("qRGB", 0, 1, 5, 21.08512918203094));
		table.add(array("qHSV", 4, 1, 1, 21.469367174690095));
		table.add(array("qHSV", 4, 0, 2, 21.473931969823585));
		table.add(array("qRGB", 6, 0, 0, 21.532049376223195));
		table.add(array("qHSV", 4, 2, 0, 21.59445165396762));
		table.add(array("qRGB", 1, 0, 5, 21.638632549717023));
		table.add(array("qRGB", 0, 5, 1, 24.37721172994415));
		table.add(array("qRGB", 1, 5, 0, 24.60734851909329));
		table.add(array("qHSV", 0, 6, 0, 32.73574351446413));
		table.add(array("qHSV", 5, 1, 0, 43.751204156283656));
		table.add(array("qHSV", 5, 0, 1, 43.76416717883281));
		table.add(array("qRGB", 0, 0, 6, 46.13239365025457));
		table.add(array("qRGB", 0, 6, 0, 52.70802175522528));
		table.add(array("qHSV", 6, 0, 0, 83.81508873246744));
		table.add(array("qRGB", 3, 2, 2, 3.527362691503131));
		table.add(array("qRGB", 2, 2, 3, 4.653057616366176));
		table.add(array("qHSV", 1, 2, 4, 4.950383818907108));
		table.add(array("qRGB", 2, 3, 2, 5.068365988414407));
		table.add(array("qRGB", 4, 2, 1, 5.178832026758057));
		table.add(array("qRGB", 3, 3, 1, 5.253549057030269));
		table.add(array("qRGB", 3, 1, 3, 5.433232494141032));
		table.add(array("qHSV", 1, 3, 3, 5.516563492291423));
		table.add(array("qRGB", 4, 1, 2, 5.635111396603524));
		table.add(array("qHSV", 2, 2, 3, 5.816758192084616));
		table.add(array("qRGB", 1, 3, 3, 6.166399561123543));
		table.add(array("qHSV", 2, 1, 4, 6.289879138851597));
		table.add(array("qHSV", 0, 3, 4, 6.345495263856095));
		table.add(array("qRGB", 4, 3, 0, 6.783776610747946));
		table.add(array("qHSV", 2, 3, 2, 6.958393497024418));
		table.add(array("qHSV", 1, 1, 5, 7.514279390344463));
		table.add(array("qHSV", 0, 2, 5, 7.850523125363171));
		table.add(array("qRGB", 4, 0, 3, 7.909364447767676));
		table.add(array("qHSV", 2, 0, 5, 8.893228803404542));
		table.add(array("qHSV", 1, 4, 2, 9.177754256712644));
		table.add(array("qHSV", 0, 4, 3, 9.270509807166043));
		table.add(array("qRGB", 1, 2, 4, 9.33366729062409));
		table.add(array("qRGB", 2, 1, 4, 9.890118915323102));
		table.add(array("qRGB", 0, 3, 4, 9.929929778266683));
		table.add(array("qRGB", 5, 2, 0, 10.138080062468157));
		table.add(array("qRGB", 5, 1, 1, 10.29114858366));
		table.add(array("qHSV", 2, 4, 1, 10.428669001108311));
		table.add(array("qHSV", 3, 1, 3, 10.577000076352872));
		table.add(array("qHSV", 3, 2, 2, 10.69795588995799));
		table.add(array("qRGB", 1, 4, 2, 10.949224161393213));
		table.add(array("qRGB", 3, 0, 4, 11.00602110019438));
		table.add(array("qRGB", 2, 4, 1, 11.02877492642026));
		table.add(array("qRGB", 5, 0, 2, 11.102983007881091));
		table.add(array("qHSV", 3, 0, 4, 11.158505372879214));
		table.add(array("qRGB", 3, 4, 0, 11.227760965671465));
		table.add(array("qRGB", 0, 4, 3, 11.309405765689151));
		table.add(array("qHSV", 3, 3, 1, 11.664870489106614));
		table.add(array("qHSV", 3, 4, 0, 14.395372498170593));
		table.add(array("qHSV", 1, 0, 6, 15.901936420764862));
		table.add(array("qHSV", 0, 1, 6, 15.991471787000199));
		table.add(array("qHSV", 0, 5, 2, 17.176936549609696));
		table.add(array("qHSV", 1, 5, 1, 17.247695065696494));
		table.add(array("qHSV", 2, 5, 0, 18.082575505826718));
		table.add(array("qRGB", 0, 2, 5, 20.405035493287702));
		table.add(array("qRGB", 1, 1, 5, 21.14347635507767));
		table.add(array("qRGB", 6, 1, 0, 21.293096999751032));
		table.add(array("qHSV", 4, 1, 2, 21.496241062179358));
		table.add(array("qHSV", 4, 0, 3, 21.580207287110984));
		table.add(array("qHSV", 4, 2, 1, 21.602329841806487));
		table.add(array("qRGB", 6, 0, 1, 21.67462497600947));
		table.add(array("qRGB", 2, 0, 5, 21.830723642418008));
		table.add(array("qHSV", 4, 3, 0, 22.133768474410587));
		table.add(array("qRGB", 0, 5, 2, 23.86359478382961));
		table.add(array("qRGB", 1, 5, 1, 24.244431205525952));
		table.add(array("qRGB", 2, 5, 0, 24.39691267076655));
		table.add(array("qHSV", 0, 6, 1, 32.80316538441022));
		table.add(array("qHSV", 1, 6, 0, 32.842233350233535));
		table.add(array("qHSV", 5, 2, 0, 43.74076975658067));
		table.add(array("qHSV", 5, 1, 1, 43.742010947423));
		table.add(array("qHSV", 5, 0, 2, 43.7562916943628));
		table.add(array("qHSV", 0, 0, 7, 44.92160259247629));
		table.add(array("qRGB", 7, 0, 0, 45.176087377745176));
		table.add(array("qRGB", 0, 1, 6, 45.62356017773505));
		table.add(array("qRGB", 1, 0, 6, 46.196411417482125));
		table.add(array("qRGB", 0, 6, 1, 52.301160399275815));
		table.add(array("qRGB", 1, 6, 0, 52.61193218654751));
		table.add(array("qHSV", 0, 7, 0, 61.80730533010471));
		table.add(array("qHSV", 6, 1, 0, 83.63587197808127));
		table.add(array("qHSV", 6, 0, 1, 83.76551042010584));
		table.add(array("qRGB", 0, 0, 7, 98.58169362835481));
		table.add(array("qRGB", 0, 7, 0, 113.80279045303948));
		table.add(array("qHSV", 7, 0, 0, 156.02265105719644));
		table.add(array("qRGB", 3, 2, 3, 5.253916642742753));
		table.add(array("qRGB", 3, 3, 2, 5.313590650823462));
		table.add(array("qRGB", 4, 2, 2, 5.575899042570833));
		table.add(array("qRGB", 2, 3, 3, 6.102583333976196));
		table.add(array("qRGB", 4, 3, 1, 6.637861657250586));
		table.add(array("qHSV", 1, 3, 4, 6.720167065205978));
		table.add(array("qHSV", 2, 2, 4, 6.853957182198859));
		table.add(array("qHSV", 2, 3, 3, 7.313034588867101));
		table.add(array("qRGB", 4, 1, 3, 7.4952032204868));
		table.add(array("qHSV", 1, 2, 5, 8.19464651896803));
		table.add(array("qHSV", 2, 1, 5, 9.128864210622176));
		table.add(array("qRGB", 2, 2, 4, 9.45054024064088));
		table.add(array("qHSV", 0, 3, 5, 9.50272830656282));
		table.add(array("qHSV", 1, 4, 3, 9.57225682172096));
		table.add(array("qRGB", 1, 3, 4, 9.878300576656232));
		table.add(array("qRGB", 5, 2, 1, 10.168317697289462));
		table.add(array("qHSV", 0, 4, 4, 10.325282646966802));
		table.add(array("qRGB", 3, 1, 4, 10.513446996566712));
		table.add(array("qHSV", 2, 4, 2, 10.553962637282169));
		table.add(array("qRGB", 2, 4, 2, 10.754583231629907));
		table.add(array("qRGB", 5, 1, 2, 10.78334660852396));
		table.add(array("qRGB", 3, 4, 1, 10.923400572688765));
		table.add(array("qHSV", 3, 2, 3, 10.940245342238159));
		table.add(array("qRGB", 5, 3, 0, 10.948747935514554));
		table.add(array("qRGB", 1, 4, 3, 11.177647318386894));
		table.add(array("qHSV", 3, 1, 4, 11.292017176980279));
		table.add(array("qHSV", 3, 3, 2, 11.749442609095881));
		table.add(array("qRGB", 4, 4, 0, 11.754305664224468));
		table.add(array("qRGB", 4, 0, 4, 12.707821916090875));
		table.add(array("qRGB", 5, 0, 3, 12.721094610289231));
		table.add(array("qHSV", 3, 0, 5, 13.321914762530062));
		table.add(array("qRGB", 0, 4, 4, 13.462907410406403));
		table.add(array("qHSV", 3, 4, 1, 14.428941545444602));
		table.add(array("qHSV", 1, 1, 6, 16.213530849741776));
		table.add(array("qHSV", 0, 2, 6, 16.595732776692728));
		table.add(array("qHSV", 2, 0, 6, 16.990247872051476));
		table.add(array("qHSV", 1, 5, 2, 17.37262548192689));
		table.add(array("qHSV", 0, 5, 3, 17.50097970500494));
		table.add(array("qHSV", 2, 5, 1, 18.135576856216254));
		table.add(array("qRGB", 0, 3, 5, 19.920991125299423));
		table.add(array("qRGB", 1, 2, 5, 20.43422559355158));
		table.add(array("qHSV", 3, 5, 0, 20.953539337715004));
		table.add(array("qRGB", 6, 2, 0, 21.039614618747564));
		table.add(array("qRGB", 2, 1, 5, 21.311634080572432));
		table.add(array("qRGB", 6, 1, 1, 21.402040306383505));
		table.add(array("qHSV", 4, 1, 3, 21.607639767259705));
		table.add(array("qHSV", 4, 2, 2, 21.635546199656854));
		table.add(array("qHSV", 4, 0, 4, 21.980255130529887));
		table.add(array("qRGB", 6, 0, 2, 22.113679071750695));
		table.add(array("qHSV", 4, 3, 1, 22.144316336683882));
		table.add(array("qRGB", 3, 0, 5, 22.328417765460777));
		table.add(array("qRGB", 0, 5, 3, 23.401054294240303));
		table.add(array("qRGB", 1, 5, 2, 23.725014384855967));
		table.add(array("qHSV", 4, 4, 0, 23.862897375364486));
		table.add(array("qRGB", 2, 5, 1, 24.024643580435026));
		table.add(array("qRGB", 3, 5, 0, 24.123412933265822));
		table.add(array("qHSV", 0, 6, 2, 32.89667758007501));
		table.add(array("qHSV", 1, 6, 1, 32.90781890164448));
		table.add(array("qHSV", 2, 6, 0, 33.32236993216267));
		table.add(array("qHSV", 5, 2, 1, 43.73216585611982));
		table.add(array("qHSV", 5, 1, 2, 43.73285247357186));
		table.add(array("qHSV", 5, 0, 3, 43.773924275773595));
		table.add(array("qHSV", 5, 3, 0, 43.87212842657174));
		table.add(array("qRGB", 0, 2, 6, 44.73957191563254));
		table.add(array("qRGB", 7, 1, 0, 44.91994210785489));
		table.add(array("qHSV", 1, 0, 7, 45.012075852208746));
		table.add(array("qRGB", 7, 0, 1, 45.321510970753366));
		table.add(array("qHSV", 0, 1, 7, 45.3975990469012));
		table.add(array("qRGB", 1, 1, 6, 45.67720247527094));
		table.add(array("qRGB", 2, 0, 6, 46.3468570278201));
		table.add(array("qRGB", 0, 6, 2, 51.60143636551707));
		table.add(array("qRGB", 1, 6, 1, 52.200354051560105));
		table.add(array("qRGB", 2, 6, 0, 52.442942082617805));
		table.add(array("qHSV", 1, 7, 0, 61.84624110147413));
		table.add(array("qHSV", 0, 7, 1, 61.98559575821476));
		table.add(array("qHSV", 6, 2, 0, 83.31021735981916));
		table.add(array("qHSV", 6, 1, 1, 83.59644325622138));
		table.add(array("qHSV", 6, 0, 2, 83.70003079189063));
		table.add(array("qRGB", 0, 1, 7, 98.09175375687065));
		table.add(array("qRGB", 1, 0, 7, 98.63222463427425));
		table.add(array("qRGB", 0, 7, 1, 113.35848060108387));
		table.add(array("qRGB", 1, 7, 0, 113.79100745068551));
		table.add(array("qHSV", 7, 1, 0, 155.76875083166007));
		table.add(array("qHSV", 7, 0, 1, 155.9388815899415));
		table.add(array("qRGB", 3, 3, 3, 6.340866483189372));
		table.add(array("qRGB", 4, 3, 2, 6.709653006265603));
		table.add(array("qRGB", 4, 2, 3, 7.129963963236756));
		table.add(array("qHSV", 2, 3, 4, 8.320823808478119));
		table.add(array("qHSV", 2, 2, 5, 9.692344955617365));
		table.add(array("qHSV", 1, 3, 5, 9.779610923631795));
		table.add(array("qRGB", 2, 3, 4, 9.857522392338288));
		table.add(array("qRGB", 3, 2, 4, 9.950763892439424));
		table.add(array("qRGB", 5, 2, 2, 10.507557830242826));
		table.add(array("qHSV", 1, 4, 4, 10.584318858953525));
		table.add(array("qRGB", 3, 4, 2, 10.638399022749269));
		table.add(array("qRGB", 5, 3, 1, 10.85649017076917));
		table.add(array("qHSV", 2, 4, 3, 10.886674213271974));
		table.add(array("qRGB", 2, 4, 3, 10.986209294528791));
		table.add(array("qRGB", 4, 4, 1, 11.46652530930045));
		table.add(array("qHSV", 3, 2, 4, 11.673906598335554));
		table.add(array("qHSV", 3, 3, 3, 11.994609999866599));
		table.add(array("qRGB", 4, 1, 4, 12.20218340832902));
		table.add(array("qRGB", 5, 1, 3, 12.312975745853725));
		table.add(array("qHSV", 0, 4, 5, 13.149357181796196));
		table.add(array("qRGB", 1, 4, 4, 13.35518595291978));
		table.add(array("qHSV", 3, 1, 5, 13.487052808959266));
		table.add(array("qHSV", 3, 4, 2, 14.518904965426142));
		table.add(array("qRGB", 5, 4, 0, 14.556491367623703));
		table.add(array("qHSV", 1, 2, 6, 16.799177364455836));
		table.add(array("qRGB", 5, 0, 4, 16.923951577513773));
		table.add(array("qHSV", 2, 1, 6, 17.266229614644402));
		table.add(array("qHSV", 1, 5, 3, 17.683854172496947));
		table.add(array("qHSV", 0, 3, 6, 18.027459361336454));
		table.add(array("qHSV", 2, 5, 2, 18.246274714415243));
		table.add(array("qHSV", 0, 5, 4, 18.349046936514938));
		table.add(array("qRGB", 1, 3, 5, 19.907650136450783));
		table.add(array("qHSV", 3, 0, 6, 20.31449096221656));
		table.add(array("qRGB", 2, 2, 5, 20.549489748044554));
		table.add(array("qHSV", 3, 5, 1, 20.994690069622262));
		table.add(array("qRGB", 6, 2, 1, 21.093244874228947));
		table.add(array("qRGB", 6, 3, 0, 21.19254912139334));
		table.add(array("qRGB", 0, 4, 5, 21.246600560116846));
		table.add(array("qHSV", 4, 2, 3, 21.75875632193585));
		table.add(array("qRGB", 3, 1, 5, 21.78644893297665));
		table.add(array("qRGB", 6, 1, 2, 21.79250600681563));
		table.add(array("qHSV", 4, 1, 4, 22.02443261543551));
		table.add(array("qHSV", 4, 3, 2, 22.186876867524546));
		table.add(array("qRGB", 1, 5, 3, 23.260883706223176));
		table.add(array("qRGB", 6, 0, 3, 23.386703338692957));
		table.add(array("qHSV", 4, 0, 5, 23.427093643965375));
		table.add(array("qRGB", 2, 5, 2, 23.493924375351092));
		table.add(array("qRGB", 4, 0, 5, 23.642266327011406));
		table.add(array("qRGB", 3, 5, 1, 23.741972884777905));
		table.add(array("qHSV", 4, 4, 1, 23.879017394229695));
		table.add(array("qRGB", 0, 5, 4, 23.943637893089022));
		table.add(array("qRGB", 4, 5, 0, 24.063262335361493));
		table.add(array("qHSV", 4, 5, 0, 28.615899360492723));
		table.add(array("qHSV", 1, 6, 2, 32.99827811369083));
		table.add(array("qHSV", 0, 6, 3, 33.1286253581266));
		table.add(array("qHSV", 2, 6, 1, 33.3839421900219));
		table.add(array("qHSV", 3, 6, 0, 35.009802069537756));
		table.add(array("qRGB", 0, 3, 6, 43.460896182311956));
		table.add(array("qHSV", 5, 2, 2, 43.73202855891318));
		table.add(array("qHSV", 5, 1, 3, 43.7535637956447));
		table.add(array("qHSV", 5, 3, 1, 43.86640260615535));
		table.add(array("qHSV", 5, 0, 4, 43.92909101484812));
		table.add(array("qHSV", 5, 4, 0, 44.55571124845371));
		table.add(array("qRGB", 7, 2, 0, 44.55833251204558));
		table.add(array("qRGB", 1, 2, 6, 44.776897391602574));
		table.add(array("qRGB", 7, 1, 1, 45.04637978612198));
		table.add(array("qHSV", 1, 1, 7, 45.482246920674726));
		table.add(array("qHSV", 2, 0, 7, 45.51365112224777));
		table.add(array("qRGB", 7, 0, 2, 45.70512868979869));
		table.add(array("qRGB", 2, 1, 6, 45.813611637632384));
		table.add(array("qHSV", 0, 2, 7, 45.89436224260916));
		table.add(array("qRGB", 3, 0, 6, 46.720243380629405));
		table.add(array("qRGB", 0, 6, 3, 50.56913459912434));
		table.add(array("qRGB", 1, 6, 2, 51.495507349823036));
		table.add(array("qRGB", 2, 6, 1, 52.024598427168634));
		table.add(array("qRGB", 3, 6, 0, 52.19106917882459));
		table.add(array("qHSV", 0, 7, 2, 62.01143653140464));
		table.add(array("qHSV", 1, 7, 1, 62.023247577307245));
		table.add(array("qHSV", 2, 7, 0, 62.02579926667945));
		table.add(array("qHSV", 6, 3, 0, 82.76435853485751));
		table.add(array("qHSV", 6, 2, 1, 83.27401911307462));
		table.add(array("qHSV", 6, 1, 2, 83.53199900326042));
		table.add(array("qHSV", 6, 0, 3, 83.58759338540732));
		table.add(array("qRGB", 0, 2, 7, 97.16239083873536));
		table.add(array("qRGB", 1, 1, 7, 98.13677798324206));
		table.add(array("qRGB", 2, 0, 7, 98.74559245319055));
		table.add(array("qRGB", 0, 7, 2, 112.52326535326874));
		table.add(array("qRGB", 1, 7, 1, 113.34355286396212));
		table.add(array("qRGB", 2, 7, 0, 113.78207678079858));
		table.add(array("qHSV", 7, 2, 0, 155.26271089738088));
		table.add(array("qHSV", 7, 1, 1, 155.69921989766797));
		table.add(array("qHSV", 7, 0, 2, 155.81640234578813));
		table.add(array("qRGB", 4, 3, 3, 7.688442546008208));
		table.add(array("qRGB", 3, 3, 4, 10.095141022638085));
		table.add(array("qRGB", 3, 4, 3, 10.864897114402869));
		table.add(array("qRGB", 5, 3, 2, 10.951615663240458));
		table.add(array("qHSV", 2, 3, 5, 11.070154272260991));
		table.add(array("qRGB", 4, 4, 2, 11.191632862199885));
		table.add(array("qRGB", 4, 2, 4, 11.52598396241005));
		table.add(array("qHSV", 2, 4, 4, 11.782463229695244));
		table.add(array("qRGB", 5, 2, 3, 11.819634155213707));
		table.add(array("qHSV", 3, 3, 4, 12.734447695338403));
		table.add(array("qRGB", 2, 4, 4, 13.198005564072307));
		table.add(array("qHSV", 1, 4, 5, 13.349700851194054));
		table.add(array("qHSV", 3, 2, 5, 13.89682265182176));
		table.add(array("qRGB", 5, 4, 1, 14.322611845014073));
		table.add(array("qHSV", 3, 4, 3, 14.765915006914788));
		table.add(array("qRGB", 5, 1, 4, 16.431998213672987));
		table.add(array("qHSV", 2, 2, 6, 17.794743002263));
		table.add(array("qHSV", 1, 3, 6, 18.19966125131184));
		table.add(array("qHSV", 1, 5, 4, 18.509866614595577));
		table.add(array("qHSV", 2, 5, 3, 18.52725317113241));
		table.add(array("qRGB", 2, 3, 5, 19.933533714232336));
		table.add(array("qHSV", 3, 1, 6, 20.533316034673383));
		table.add(array("qHSV", 0, 5, 5, 20.662238654155118));
		table.add(array("qRGB", 3, 2, 5, 20.95052655636009));
		table.add(array("qHSV", 3, 5, 2, 21.083018105884644));
		table.add(array("qHSV", 0, 4, 6, 21.110219649964062));
		table.add(array("qRGB", 6, 3, 1, 21.161371921806357));
		table.add(array("qRGB", 1, 4, 5, 21.18027776782719));
		table.add(array("qRGB", 6, 2, 2, 21.38663005776269));
		table.add(array("qHSV", 4, 2, 4, 22.196381149547953));
		table.add(array("qHSV", 4, 3, 3, 22.32389197746879));
		table.add(array("qRGB", 6, 1, 3, 23.0029729164763));
		table.add(array("qRGB", 2, 5, 3, 23.022684799085344));
		table.add(array("qRGB", 4, 1, 5, 23.086305798726343));
		table.add(array("qRGB", 3, 5, 2, 23.194828446980296));
		table.add(array("qRGB", 6, 4, 0, 23.288580709898017));
		table.add(array("qHSV", 4, 1, 5, 23.501867553611202));
		table.add(array("qRGB", 4, 5, 1, 23.68088624329254));
		table.add(array("qRGB", 1, 5, 4, 23.811048702797404));
		table.add(array("qHSV", 4, 4, 2, 23.930560280338014));
		table.add(array("qRGB", 5, 5, 0, 25.402895483675284));
		table.add(array("qRGB", 6, 0, 4, 26.864638036544115));
		table.add(array("qRGB", 5, 0, 5, 27.081377578441117));
		table.add(array("qRGB", 0, 5, 5, 28.54063915420294));
		table.add(array("qHSV", 4, 5, 1, 28.641730651838913));
		table.add(array("qHSV", 4, 0, 6, 28.854331775352687));
		table.add(array("qHSV", 1, 6, 3, 33.22460294147693));
		table.add(array("qHSV", 2, 6, 2, 33.46921764781381));
		table.add(array("qHSV", 0, 6, 4, 33.702579763034635));
		table.add(array("qHSV", 3, 6, 1, 35.06389034413055));
		table.add(array("qHSV", 4, 6, 0, 40.137731481784115));
		table.add(array("qRGB", 0, 4, 6, 42.525753512691516));
		table.add(array("qRGB", 1, 3, 6, 43.47286860289719));
		table.add(array("qHSV", 5, 2, 3, 43.75703255179147));
		table.add(array("qHSV", 5, 3, 2, 43.86973635535911));
		table.add(array("qHSV", 5, 1, 4, 43.9193503618413));
		table.add(array("qRGB", 7, 3, 0, 44.309567106007385));
		table.add(array("qHSV", 5, 4, 1, 44.55595283640167));
		table.add(array("qRGB", 7, 2, 1, 44.652283598962846));
		table.add(array("qHSV", 5, 0, 5, 44.70057087412785));
		table.add(array("qRGB", 2, 2, 6, 44.88342483911719));
		table.add(array("qRGB", 7, 1, 2, 45.40073048149536));
		table.add(array("qHSV", 2, 1, 7, 45.957826377522295));
		table.add(array("qHSV", 1, 2, 7, 45.97438884520198));
		table.add(array("qRGB", 3, 1, 6, 46.171046220192245));
		table.add(array("qRGB", 7, 0, 3, 46.74410786531797));
		table.add(array("qHSV", 0, 3, 7, 46.94047509487422));
		table.add(array("qHSV", 5, 5, 0, 46.98065616162865));
		table.add(array("qHSV", 3, 0, 7, 47.31084724651443));
		table.add(array("qRGB", 4, 0, 6, 47.69348627992129));
		table.add(array("qRGB", 0, 6, 4, 49.58886169746684));
		table.add(array("qRGB", 1, 6, 3, 50.45815908106512));
		table.add(array("qRGB", 2, 6, 2, 51.30975863355777));
		table.add(array("qRGB", 3, 6, 1, 51.76398032287591));
		table.add(array("qRGB", 4, 6, 0, 51.98456631116456));
		table.add(array("qHSV", 1, 7, 2, 62.0476400938254));
		table.add(array("qHSV", 0, 7, 3, 62.08910702114573));
		table.add(array("qHSV", 2, 7, 1, 62.19923587229031));
		table.add(array("qHSV", 3, 7, 0, 62.71396179748241));
		table.add(array("qHSV", 6, 4, 0, 82.01003193662558));
		table.add(array("qHSV", 6, 3, 1, 82.73396479269887));
		table.add(array("qHSV", 6, 2, 2, 83.2140183000991));
		table.add(array("qHSV", 6, 1, 3, 83.4241302922701));
		table.add(array("qHSV", 6, 0, 4, 83.43394427208376));
		table.add(array("qRGB", 0, 3, 7, 95.5024295094967));
		table.add(array("qRGB", 1, 2, 7, 97.19867684302076));
		table.add(array("qRGB", 2, 1, 7, 98.2421679833705));
		table.add(array("qRGB", 3, 0, 7, 99.01508349160125));
		table.add(array("qRGB", 0, 7, 3, 111.04199670996694));
		table.add(array("qRGB", 1, 7, 2, 112.50410102149714));
		table.add(array("qRGB", 2, 7, 1, 113.32974893931791));
		table.add(array("qRGB", 3, 7, 0, 113.8193934779908));
		table.add(array("qHSV", 7, 3, 0, 154.25179097350306));
		table.add(array("qHSV", 7, 2, 1, 155.19759088118153));
		table.add(array("qHSV", 7, 0, 3, 155.5731316317515));
		table.add(array("qHSV", 7, 1, 2, 155.57639462650135));
		table.add(array("qRGB", 4, 3, 4, 11.300385291883343));
		table.add(array("qRGB", 4, 4, 3, 11.413287765007333));
		table.add(array("qRGB", 5, 3, 3, 11.835590580277925));
		table.add(array("qRGB", 3, 4, 4, 13.105170302239044));
		table.add(array("qRGB", 5, 4, 2, 14.102725593278263));
		table.add(array("qHSV", 2, 4, 5, 14.342172542032769));
		table.add(array("qHSV", 3, 3, 5, 14.946261925820883));
		table.add(array("qHSV", 3, 4, 4, 15.46518525367638));
		table.add(array("qRGB", 5, 2, 4, 15.6937208294619));
		table.add(array("qHSV", 2, 3, 6, 19.079145485722293));
		table.add(array("qHSV", 2, 5, 4, 19.290435137212366));
		table.add(array("qRGB", 3, 3, 5, 20.16864052990391));
		table.add(array("qHSV", 1, 5, 5, 20.791984112466153));
		table.add(array("qHSV", 3, 2, 6, 20.96089276744169));
		table.add(array("qRGB", 2, 4, 5, 21.08938909810169));
		table.add(array("qHSV", 1, 4, 6, 21.243770468351563));
		table.add(array("qRGB", 6, 3, 2, 21.288110810768334));
		table.add(array("qHSV", 3, 5, 3, 21.312307819388156));
		table.add(array("qRGB", 4, 2, 5, 22.178404640544578));
		table.add(array("qRGB", 6, 2, 3, 22.45089367092038));
		table.add(array("qRGB", 3, 5, 3, 22.70561761512323));
		table.add(array("qHSV", 4, 3, 4, 22.783387113717136));
		table.add(array("qRGB", 4, 5, 2, 23.12311052644533));
		table.add(array("qRGB", 6, 4, 1, 23.13965229796283));
		table.add(array("qRGB", 2, 5, 4, 23.581710343484367));
		table.add(array("qHSV", 4, 2, 5, 23.712242409809047));
		table.add(array("qHSV", 4, 4, 3, 24.08132103092352));
		table.add(array("qRGB", 5, 5, 1, 25.045878832584336));
		table.add(array("qRGB", 6, 1, 4, 26.409192843237044));
		table.add(array("qRGB", 5, 1, 5, 26.529047924439634));
		table.add(array("qHSV", 0, 5, 6, 27.63065202910769));
		table.add(array("qRGB", 1, 5, 5, 28.431190265339374));
		table.add(array("qHSV", 4, 5, 2, 28.6992567436543));
		table.add(array("qHSV", 4, 1, 6, 28.982662338530055));
		table.add(array("qRGB", 6, 5, 0, 31.698950614604737));
		table.add(array("qHSV", 2, 6, 3, 33.68213058054486));
		table.add(array("qHSV", 1, 6, 4, 33.78916238071692));
		table.add(array("qHSV", 3, 6, 2, 35.137672403014726));
		table.add(array("qHSV", 0, 6, 5, 35.38285901058025));
		table.add(array("qRGB", 6, 0, 5, 35.798088797723715));
		table.add(array("qHSV", 4, 6, 1, 40.17942007592115));
		table.add(array("qRGB", 1, 4, 6, 42.499987755993985));
		table.add(array("qRGB", 2, 3, 6, 43.52716203099239));
		table.add(array("qHSV", 5, 3, 3, 43.906895816832254));
		table.add(array("qHSV", 5, 2, 4, 43.935627297756106));
		table.add(array("qRGB", 7, 3, 1, 44.349719267806194));
		table.add(array("qHSV", 5, 4, 2, 44.56590570376091));
		table.add(array("qHSV", 5, 1, 5, 44.70977685547477));
		table.add(array("qRGB", 7, 2, 2, 44.94814474219706));
		table.add(array("qRGB", 0, 5, 6, 45.098262849514896));
		table.add(array("qRGB", 3, 2, 6, 45.19680023082748));
		table.add(array("qRGB", 7, 4, 0, 45.24050897140638));
		table.add(array("qRGB", 7, 1, 3, 46.397617749593216));
		table.add(array("qHSV", 2, 2, 7, 46.42892542288154));
		table.add(array("qHSV", 5, 5, 1, 46.98756966975928));
		table.add(array("qHSV", 1, 3, 7, 47.01472807679523));
		table.add(array("qRGB", 4, 1, 6, 47.12882395174464));
		table.add(array("qHSV", 3, 1, 7, 47.69825145609503));
		table.add(array("qHSV", 5, 0, 6, 48.30892314902963));
		table.add(array("qHSV", 0, 4, 7, 49.13722160795946));
		table.add(array("qRGB", 1, 6, 4, 49.47449215886267));
		table.add(array("qRGB", 7, 0, 4, 49.56445112803157));
		table.add(array("qRGB", 2, 6, 3, 50.26044398911863));
		table.add(array("qRGB", 5, 0, 6, 50.31137045369451));
		table.add(array("qRGB", 0, 6, 5, 50.568076039984064));
		table.add(array("qRGB", 3, 6, 2, 51.032435381608266));
		table.add(array("qRGB", 4, 6, 1, 51.548862763853826));
		table.add(array("qRGB", 5, 6, 0, 52.552285583869214));
		table.add(array("qHSV", 4, 0, 7, 52.8129511776001));
		table.add(array("qHSV", 5, 6, 0, 54.13995592279949));
		table.add(array("qHSV", 1, 7, 3, 62.123968137878016));
		table.add(array("qHSV", 2, 7, 2, 62.221939996879684));
		table.add(array("qHSV", 0, 7, 4, 62.31871516714029));
		table.add(array("qHSV", 3, 7, 1, 62.87714478304283));
		table.add(array("qHSV", 4, 7, 0, 65.07473789181662));
		table.add(array("qHSV", 6, 5, 0, 81.4408397301875));
		table.add(array("qHSV", 6, 4, 1, 81.98400997685754));
		table.add(array("qHSV", 6, 3, 2, 82.67858543292775));
		table.add(array("qHSV", 6, 2, 3, 83.11171679692882));
		table.add(array("qHSV", 6, 1, 4, 83.27724778864844));
		table.add(array("qHSV", 6, 0, 5, 83.41585847422323));
		table.add(array("qRGB", 0, 4, 7, 92.95641588673492));
		table.add(array("qRGB", 1, 3, 7, 95.52435465074312));
		table.add(array("qRGB", 2, 2, 7, 97.28774943440655));
		table.add(array("qRGB", 3, 1, 7, 98.50079273033025));
		table.add(array("qRGB", 4, 0, 7, 99.70123907935444));
		table.add(array("qRGB", 0, 7, 4, 108.70942567609441));
		table.add(array("qRGB", 1, 7, 3, 111.01652069235655));
		table.add(array("qRGB", 2, 7, 2, 112.48198468055303));
		table.add(array("qRGB", 3, 7, 1, 113.35964114383401));
		table.add(array("qRGB", 4, 7, 0, 114.08455335800612));
		table.add(array("qHSV", 7, 4, 0, 152.26767295140053));
		table.add(array("qHSV", 7, 3, 1, 154.18872685701697));
		table.add(array("qHSV", 7, 2, 2, 155.07487080911943));
		table.add(array("qHSV", 7, 0, 4, 155.09750765195514));
		table.add(array("qHSV", 7, 1, 3, 155.33090566219985));
		table.add(array("qRGB", 4, 4, 4, 13.635492369684071));
		table.add(array("qRGB", 5, 4, 3, 14.339651153267729));
		table.add(array("qRGB", 5, 3, 4, 15.105009711688462));
		table.add(array("qHSV", 3, 4, 5, 17.613021066244166));
		table.add(array("qRGB", 3, 4, 5, 21.06899986017425));
		table.add(array("qRGB", 4, 3, 5, 21.163357953208294));
		table.add(array("qHSV", 2, 5, 5, 21.459022937564324));
		table.add(array("qHSV", 3, 5, 4, 21.953548327784002));
		table.add(array("qHSV", 2, 4, 6, 21.953794391617667));
		table.add(array("qHSV", 3, 3, 6, 22.015191420102827));
		table.add(array("qRGB", 6, 3, 3, 22.057818941088346));
		table.add(array("qRGB", 4, 5, 3, 22.611345132538872));
		table.add(array("qRGB", 6, 4, 2, 23.01722632330868));
		table.add(array("qRGB", 3, 5, 4, 23.26583544312819));
		table.add(array("qHSV", 4, 3, 5, 24.334741391416884));
		table.add(array("qRGB", 5, 5, 2, 24.513149100441726));
		table.add(array("qHSV", 4, 4, 4, 24.54206993455648));
		table.add(array("qRGB", 5, 2, 5, 25.583221839197073));
		table.add(array("qRGB", 6, 2, 4, 25.672319962794653));
		table.add(array("qHSV", 1, 5, 6, 27.723584486538304));
		table.add(array("qRGB", 2, 5, 5, 28.24066686271142));
		table.add(array("qHSV", 4, 5, 3, 28.858335408264463));
		table.add(array("qHSV", 4, 2, 6, 29.245750524384043));
		table.add(array("qRGB", 6, 5, 1, 31.41318872510949));
		table.add(array("qHSV", 2, 6, 4, 34.22040714241764));
		table.add(array("qRGB", 6, 1, 5, 35.27323810665261));
		table.add(array("qHSV", 3, 6, 3, 35.32334270831286));
		table.add(array("qHSV", 1, 6, 5, 35.45535791369207));
		table.add(array("qHSV", 4, 6, 2, 40.23476684721952));
		table.add(array("qHSV", 0, 6, 6, 40.75582097662498));
		table.add(array("qRGB", 2, 4, 6, 42.47457324801316));
		table.add(array("qRGB", 3, 3, 6, 43.743810942045144));
		table.add(array("qHSV", 5, 3, 4, 44.10013792348927));
		table.add(array("qRGB", 7, 3, 2, 44.5399115043061));
		table.add(array("qHSV", 5, 4, 3, 44.61240036097907));
		table.add(array("qHSV", 5, 2, 5, 44.75074825138069));
		table.add(array("qRGB", 1, 5, 6, 45.02628653090765));
		table.add(array("qRGB", 7, 4, 1, 45.19548827530423));
		table.add(array("qRGB", 7, 2, 3, 45.85128412568048));
		table.add(array("qRGB", 4, 2, 6, 46.104706678281275));
		table.add(array("qHSV", 5, 5, 2, 47.00216719845255));
		table.add(array("qHSV", 2, 3, 7, 47.434430403929944));
		table.add(array("qHSV", 3, 2, 7, 48.113864245075504));
		table.add(array("qHSV", 5, 1, 6, 48.35414705431072));
		table.add(array("qRGB", 7, 1, 4, 49.16085177151545));
		table.add(array("qHSV", 1, 4, 7, 49.200744348108195));
		table.add(array("qRGB", 2, 6, 4, 49.26688031984588));
		table.add(array("qRGB", 5, 1, 6, 49.736293373767865));
		table.add(array("qRGB", 3, 6, 3, 49.95796953990217));
		table.add(array("qRGB", 1, 6, 5, 50.455210971385455));
		table.add(array("qRGB", 4, 6, 2, 50.79571129684579));
		table.add(array("qRGB", 7, 5, 0, 51.06486999908676));
		table.add(array("qRGB", 5, 6, 1, 52.116365829212135));
		table.add(array("qHSV", 4, 1, 7, 53.10463249735915));
		table.add(array("qHSV", 0, 5, 7, 53.78015545839082));
		table.add(array("qHSV", 5, 6, 1, 54.159747593732384));
		table.add(array("qRGB", 6, 6, 0, 56.65347676334426));
		table.add(array("qRGB", 7, 0, 5, 57.05088417395533));
		table.add(array("qRGB", 6, 0, 6, 57.387399296171345));
		table.add(array("qRGB", 0, 6, 6, 59.83107919981765));
		table.add(array("qHSV", 2, 7, 3, 62.295216190063));
		table.add(array("qHSV", 1, 7, 4, 62.35076210418596));
		table.add(array("qHSV", 3, 7, 2, 62.89298902640965));
		table.add(array("qHSV", 0, 7, 5, 63.112997894458175));
		table.add(array("qHSV", 4, 7, 1, 65.22501380465377));
		table.add(array("qHSV", 5, 0, 7, 67.35803299473069));
		table.add(array("qHSV", 5, 7, 0, 72.5529842358517));
		table.add(array("qHSV", 6, 5, 1, 81.41961158006531));
		table.add(array("qHSV", 6, 4, 2, 81.93752641201267));
		table.add(array("qHSV", 6, 3, 3, 82.59066390393983));
		table.add(array("qHSV", 6, 6, 0, 82.68591896276428));
		table.add(array("qHSV", 6, 2, 4, 82.97878839973524));
		table.add(array("qHSV", 6, 1, 5, 83.27251053606977));
		table.add(array("qHSV", 6, 0, 6, 84.8281185595414));
		table.add(array("qRGB", 0, 5, 7, 90.63206534182964));
		table.add(array("qRGB", 1, 4, 7, 92.95451501587132));
		table.add(array("qRGB", 2, 3, 7, 95.58421617689663));
		table.add(array("qRGB", 3, 2, 7, 97.52004006544156));
		table.add(array("qRGB", 4, 1, 7, 99.1720497524499));
		table.add(array("qRGB", 5, 0, 7, 101.56712234851004));
		table.add(array("qRGB", 0, 7, 5, 105.99908554244503));
		table.add(array("qRGB", 1, 7, 4, 108.67401118003978));
		table.add(array("qRGB", 2, 7, 3, 110.9808282044467));
		table.add(array("qRGB", 3, 7, 2, 112.49699129233186));
		table.add(array("qRGB", 4, 7, 1, 113.61400042659453));
		table.add(array("qRGB", 5, 7, 0, 115.24588503540951));
		table.add(array("qHSV", 7, 5, 0, 148.4866525222472));
		table.add(array("qHSV", 7, 4, 1, 152.20702004897882));
		table.add(array("qHSV", 7, 3, 2, 154.06650658928004));
		table.add(array("qHSV", 7, 0, 5, 154.22820386668872));
		table.add(array("qHSV", 7, 2, 3, 154.8302458267778));
		table.add(array("qHSV", 7, 1, 4, 154.85496721006646));
		table.add(array("qRGB", 5, 4, 4, 16.452327018216238));
		table.add(array("qRGB", 4, 4, 5, 21.576691496711987));
		table.add(array("qRGB", 4, 5, 4, 23.151504858179628));
		table.add(array("qRGB", 6, 4, 3, 23.29238320073896));
		table.add(array("qHSV", 3, 5, 5, 23.855937179825872));
		table.add(array("qRGB", 5, 5, 3, 24.010895759787857));
		table.add(array("qRGB", 5, 3, 5, 24.345372151173603));
		table.add(array("qHSV", 3, 4, 6, 24.483664759259558));
		table.add(array("qRGB", 6, 3, 4, 24.84121606806918));
		table.add(array("qHSV", 4, 4, 5, 26.121861030618177));
		table.add(array("qRGB", 3, 5, 5, 27.975681776202745));
		table.add(array("qHSV", 2, 5, 6, 28.22285519819319));
		table.add(array("qHSV", 4, 5, 4, 29.326939071136668));
		table.add(array("qHSV", 4, 3, 6, 29.941881324210264));
		table.add(array("qRGB", 6, 5, 2, 30.97884589500067));
		table.add(array("qRGB", 6, 2, 5, 34.346490652277055));
		table.add(array("qHSV", 3, 6, 4, 35.80586465661823));
		table.add(array("qHSV", 2, 6, 5, 35.831301740769405));
		table.add(array("qHSV", 4, 6, 3, 40.37922211577201));
		table.add(array("qHSV", 1, 6, 6, 40.81321272585543));
		table.add(array("qRGB", 3, 4, 6, 42.525019656531576));
		table.add(array("qRGB", 4, 3, 6, 44.51060848937224));
		table.add(array("qHSV", 5, 4, 4, 44.81908847853915));
		table.add(array("qRGB", 2, 5, 6, 44.902350932273606));
		table.add(array("qHSV", 5, 3, 5, 44.954338178029914));
		table.add(array("qRGB", 7, 4, 2, 45.209654110379404));
		table.add(array("qRGB", 7, 3, 3, 45.25226318990082));
		table.add(array("qHSV", 5, 5, 3, 47.067622456344374));
		table.add(array("qHSV", 5, 2, 6, 48.43284417039696));
		table.add(array("qRGB", 7, 2, 4, 48.48082801682692));
		table.add(array("qRGB", 5, 2, 6, 48.67260126855884));
		table.add(array("qRGB", 3, 6, 4, 48.93754909302421));
		table.add(array("qHSV", 3, 3, 7, 49.02681684469041));
		table.add(array("qHSV", 2, 4, 7, 49.56029119812139));
		table.add(array("qRGB", 4, 6, 3, 49.67978878365301));
		table.add(array("qRGB", 2, 6, 5, 50.2467901034742));
		table.add(array("qRGB", 7, 5, 1, 50.89632581346104));
		table.add(array("qRGB", 5, 6, 2, 51.349978241781436));
		table.add(array("qHSV", 4, 2, 7, 53.40986543747504));
		table.add(array("qHSV", 1, 5, 7, 53.832715409307504));
		table.add(array("qHSV", 5, 6, 2, 54.17452931216259));
		table.add(array("qRGB", 6, 6, 1, 56.24604742164139));
		table.add(array("qRGB", 7, 1, 5, 56.576264479355444));
		table.add(array("qRGB", 6, 1, 6, 56.814909591996674));
		table.add(array("qRGB", 1, 6, 6, 59.72839648703533));
		table.add(array("qHSV", 2, 7, 4, 62.51176621430742));
		table.add(array("qHSV", 3, 7, 3, 62.96174689694058));
		table.add(array("qHSV", 1, 7, 5, 63.14358049455555));
		table.add(array("qHSV", 0, 6, 7, 63.465936946588535));
		table.add(array("qHSV", 4, 7, 2, 65.23155454626794));
		table.add(array("qHSV", 0, 7, 6, 66.36369767041624));
		table.add(array("qHSV", 5, 1, 7, 67.49295507997199));
		table.add(array("qRGB", 7, 6, 0, 72.32032989402973));
		table.add(array("qHSV", 5, 7, 1, 72.6504972897907));
		table.add(array("qRGB", 7, 0, 6, 76.11764832983383));
		table.add(array("qHSV", 6, 5, 2, 81.38302809602966));
		table.add(array("qHSV", 6, 4, 3, 81.86192037913459));
		table.add(array("qHSV", 6, 3, 4, 82.47611149206824));
		table.add(array("qHSV", 6, 6, 1, 82.67225695734406));
		table.add(array("qHSV", 6, 2, 5, 83.00311321520888));
		table.add(array("qHSV", 6, 1, 6, 84.70426204696611));
		table.add(array("qHSV", 6, 7, 0, 90.32392117480995));
		table.add(array("qRGB", 1, 5, 7, 90.59284659085877));
		table.add(array("qRGB", 2, 4, 7, 92.96533250892645));
		table.add(array("qRGB", 0, 6, 7, 94.23816148736213));
		table.add(array("qRGB", 3, 3, 7, 95.76126172444539));
		table.add(array("qHSV", 6, 0, 7, 96.77929387101405));
		table.add(array("qRGB", 4, 2, 7, 98.15417423649613));
		table.add(array("qRGB", 5, 1, 7, 101.01783865813661));
		table.add(array("qRGB", 1, 7, 5, 105.9478151508339));
		table.add(array("qRGB", 0, 7, 6, 106.2108223866618));
		table.add(array("qRGB", 6, 0, 7, 106.88184436821574));
		table.add(array("qRGB", 2, 7, 4, 108.61661495825513));
		table.add(array("qRGB", 3, 7, 3, 110.9684674837774));
		table.add(array("qRGB", 4, 7, 2, 112.72714747788302));
		table.add(array("qRGB", 5, 7, 1, 114.76166638761399));
		table.add(array("qRGB", 6, 7, 0, 119.55559324301412));
		table.add(array("qHSV", 7, 6, 0, 141.64444052630054));
		table.add(array("qHSV", 7, 5, 1, 148.4251492515059));
		table.add(array("qHSV", 7, 4, 2, 152.0899192589707));
		table.add(array("qHSV", 7, 0, 6, 153.00264635518943));
		table.add(array("qHSV", 7, 3, 3, 153.83156011108096));
		table.add(array("qHSV", 7, 1, 5, 153.98670951089673));
		table.add(array("qHSV", 7, 2, 4, 154.36075363667933));
		table.add(array("qRGB", 5, 4, 5, 24.071811935682792));
		table.add(array("qRGB", 5, 5, 4, 24.519064488681344));
		table.add(array("qRGB", 6, 4, 4, 25.197573119249448));
		table.add(array("qRGB", 4, 5, 5, 27.88679667800512));
		table.add(array("qHSV", 3, 5, 6, 30.125155623703037));
		table.add(array("qRGB", 6, 5, 3, 30.57235916304928));
		table.add(array("qHSV", 4, 5, 5, 30.80517648683717));
		table.add(array("qHSV", 4, 4, 6, 31.699842078117207));
		table.add(array("qRGB", 6, 3, 5, 32.99240577170763));
		table.add(array("qHSV", 3, 6, 5, 37.271137072988665));
		table.add(array("qHSV", 4, 6, 4, 40.75410626562716));
		table.add(array("qHSV", 2, 6, 6, 41.107826122575894));
		table.add(array("qRGB", 4, 4, 6, 42.983901487038864));
		table.add(array("qRGB", 3, 5, 6, 44.73696740093618));
		table.add(array("qRGB", 7, 4, 3, 45.575935181665926));
		table.add(array("qHSV", 5, 4, 5, 45.73706665815075));
		table.add(array("qRGB", 5, 3, 6, 46.93140066470426));
		table.add(array("qHSV", 5, 5, 4, 47.31457765402981));
		table.add(array("qRGB", 7, 3, 4, 47.57904720235397));
		table.add(array("qRGB", 4, 6, 4, 48.59959709126427));
		table.add(array("qHSV", 5, 3, 6, 48.70967709693754));
		table.add(array("qRGB", 3, 6, 5, 49.90415093680079));
		table.add(array("qRGB", 5, 6, 3, 50.19162493994238));
		table.add(array("qRGB", 7, 5, 2, 50.64474510643907));
		table.add(array("qHSV", 3, 4, 7, 50.97542315094293));
		table.add(array("qHSV", 2, 5, 7, 54.10765028158217));
		table.add(array("qHSV", 4, 3, 7, 54.119314448377594));
		table.add(array("qHSV", 5, 6, 3, 54.240483810016585));
		table.add(array("qRGB", 6, 6, 2, 55.51362394958891));
		table.add(array("qRGB", 7, 2, 5, 55.72738548239443));
		table.add(array("qRGB", 6, 2, 6, 55.74309141050442));
		table.add(array("qRGB", 2, 6, 6, 59.53589671516055));
		table.add(array("qHSV", 3, 7, 4, 63.157139098842364));
		table.add(array("qHSV", 2, 7, 5, 63.28833907338459));
		table.add(array("qHSV", 1, 6, 7, 63.50457128922028));
		table.add(array("qHSV", 4, 7, 3, 65.29115018166584));
		table.add(array("qHSV", 1, 7, 6, 66.39194736243954));
		table.add(array("qHSV", 5, 2, 7, 67.61437384583752));
		table.add(array("qRGB", 7, 6, 1, 71.99761869066867));
		table.add(array("qHSV", 5, 7, 2, 72.62668860926118));
		table.add(array("qRGB", 7, 1, 6, 75.56911192801118));
		table.add(array("qHSV", 6, 5, 3, 81.33113264372975));
		table.add(array("qHSV", 6, 4, 4, 81.78259962534494));
		table.add(array("qHSV", 6, 3, 5, 82.54804950625551));
		table.add(array("qHSV", 6, 6, 2, 82.64653580433713));
		table.add(array("qHSV", 0, 7, 7, 82.98009795855553));
		table.add(array("qHSV", 6, 2, 6, 84.49086288438515));
		table.add(array("qHSV", 6, 7, 1, 90.3539481096517));
		table.add(array("qRGB", 2, 5, 7, 90.52673250324939));
		table.add(array("qRGB", 3, 4, 7, 93.04225296123272));
		table.add(array("qRGB", 1, 6, 7, 94.15118331333835));
		table.add(array("qRGB", 4, 3, 7, 96.30652031331188));
		table.add(array("qHSV", 6, 1, 7, 96.66311028783815));
		table.add(array("qRGB", 5, 2, 7, 99.9525311596843));
		table.add(array("qRGB", 2, 7, 5, 105.85586615081006));
		table.add(array("qRGB", 1, 7, 6, 106.13379451983877));
		table.add(array("qRGB", 6, 1, 7, 106.30844111204077));
		table.add(array("qRGB", 3, 7, 4, 108.55788479941238));
		table.add(array("qRGB", 4, 7, 3, 111.1485560507927));
		table.add(array("qRGB", 5, 7, 2, 113.84057827200076));
		table.add(array("qRGB", 6, 7, 1, 119.0611377850553));
		table.add(array("qRGB", 0, 7, 7, 121.61122326525965));
		table.add(array("qRGB", 7, 0, 7, 122.22938888033444));
		table.add(array("qHSV", 7, 7, 0, 130.39792724870412));
		table.add(array("qRGB", 7, 7, 0, 133.93140854155877));
		table.add(array("qHSV", 7, 6, 1, 141.57678709847553));
		table.add(array("qHSV", 7, 5, 2, 148.3157517124646));
		table.add(array("qHSV", 7, 4, 3, 151.85812919582494));
		table.add(array("qHSV", 7, 1, 6, 152.75698339389902));
		table.add(array("qHSV", 7, 3, 4, 153.36844533228413));
		table.add(array("qHSV", 7, 2, 5, 153.50798443432024));
		table.add(array("qHSV", 7, 0, 7, 154.42826003900322));
		table.add(array("qRGB", 5, 5, 5, 29.16035761415699));
		table.add(array("qRGB", 6, 5, 4, 31.076711561370868));
		table.add(array("qRGB", 6, 4, 5, 32.067109009705945));
		table.add(array("qHSV", 4, 5, 6, 36.083595936249296));
		table.add(array("qHSV", 4, 6, 5, 41.964190211364595));
		table.add(array("qHSV", 3, 6, 6, 42.29930100726572));
		table.add(array("qRGB", 4, 5, 6, 44.727040748103285));
		table.add(array("qRGB", 5, 4, 6, 44.968623478214795));
		table.add(array("qRGB", 7, 4, 4, 47.278835169696876));
		table.add(array("qHSV", 5, 5, 5, 48.217951512529));
		table.add(array("qRGB", 5, 6, 4, 49.01993078520322));
		table.add(array("qRGB", 4, 6, 5, 49.51021618599725));
		table.add(array("qHSV", 5, 4, 6, 49.547304173959944));
		table.add(array("qRGB", 7, 5, 3, 50.45381424547857));
		table.add(array("qRGB", 6, 3, 6, 53.916454183431405));
		table.add(array("qRGB", 6, 6, 3, 54.37236041600201));
		table.add(array("qRGB", 7, 3, 5, 54.41183262515647));
		table.add(array("qHSV", 5, 6, 4, 54.45350302726497));
		table.add(array("qHSV", 3, 5, 7, 55.244904587366044));
		table.add(array("qHSV", 4, 4, 7, 55.64610512093585));
		table.add(array("qRGB", 3, 6, 6, 59.209737174708884));
		table.add(array("qHSV", 2, 6, 7, 63.6751728390727));
		table.add(array("qHSV", 3, 7, 5, 63.88987040305082));
		table.add(array("qHSV", 4, 7, 4, 65.43916877735103));
		table.add(array("qHSV", 2, 7, 6, 66.50671872962488));
		table.add(array("qHSV", 5, 3, 7, 67.9918424704696));
		table.add(array("qRGB", 7, 6, 2, 71.40548869524387));
		table.add(array("qHSV", 5, 7, 3, 72.65552942805532));
		table.add(array("qRGB", 7, 2, 6, 74.539198911411));
		table.add(array("qHSV", 6, 5, 4, 81.28294078298102));
		table.add(array("qHSV", 6, 4, 5, 81.90954861718659));
		table.add(array("qHSV", 6, 6, 3, 82.60810662817708));
		table.add(array("qHSV", 1, 7, 7, 83.01474461331952));
		table.add(array("qHSV", 6, 3, 6, 84.12128748832396));
		table.add(array("qHSV", 6, 7, 2, 90.30191795302524));
		table.add(array("qRGB", 3, 5, 7, 90.44343880501384));
		table.add(array("qRGB", 4, 4, 7, 93.39992710387831));
		table.add(array("qRGB", 2, 6, 7, 93.98542612049617));
		table.add(array("qHSV", 6, 2, 7, 96.531808332587));
		table.add(array("qRGB", 5, 3, 7, 97.98623863661277));
		table.add(array("qRGB", 6, 2, 7, 105.18986761294468));
		table.add(array("qRGB", 3, 7, 5, 105.72241391531153));
		table.add(array("qRGB", 2, 7, 6, 105.9861999725296));
		table.add(array("qRGB", 4, 7, 4, 108.64397196690527));
		table.add(array("qRGB", 5, 7, 3, 112.1823057298339));
		table.add(array("qRGB", 6, 7, 2, 118.1068250687499));
		table.add(array("qRGB", 1, 7, 7, 121.49344484417644));
		table.add(array("qRGB", 7, 1, 7, 121.63454416537526));
		table.add(array("qHSV", 7, 7, 1, 130.27815752171145));
		table.add(array("qRGB", 7, 7, 1, 133.45192537538827));
		table.add(array("qHSV", 7, 6, 2, 141.4759110619432));
		table.add(array("qHSV", 7, 5, 3, 148.09813296839445));
		table.add(array("qHSV", 7, 4, 4, 151.41705304769513));
		table.add(array("qHSV", 7, 2, 6, 152.30122027302545));
		table.add(array("qHSV", 7, 3, 5, 152.53646872834864));
		table.add(array("qHSV", 7, 1, 7, 154.0768707620358));
		table.add(array("qRGB", 6, 5, 5, 35.400137435029016));
		table.add(array("qRGB", 5, 5, 6, 45.8233771590495));
		table.add(array("qHSV", 4, 6, 6, 46.40245579086771));
		table.add(array("qRGB", 5, 6, 5, 49.782515993143186));
		table.add(array("qRGB", 7, 5, 4, 51.041409413692236));
		table.add(array("qRGB", 6, 4, 6, 51.54216148099061));
		table.add(array("qHSV", 5, 5, 6, 51.940531483076605));
		table.add(array("qRGB", 7, 4, 5, 53.137756535881834));
		table.add(array("qRGB", 6, 6, 4, 53.14396294559098));
		table.add(array("qHSV", 5, 6, 5, 55.228715876666485));
		table.add(array("qRGB", 4, 6, 6, 58.79585620026274));
		table.add(array("qHSV", 4, 5, 7, 59.203972102699964));
		table.add(array("qHSV", 3, 6, 7, 64.44018880057266));
		table.add(array("qHSV", 4, 7, 5, 66.07804953551427));
		table.add(array("qHSV", 3, 7, 6, 67.02198891573136));
		table.add(array("qHSV", 5, 4, 7, 68.82377873985625));
		table.add(array("qRGB", 7, 6, 3, 70.45097650807729));
		table.add(array("qHSV", 5, 7, 4, 72.72379724972508));
		table.add(array("qRGB", 7, 3, 6, 72.7496334594355));
		table.add(array("qHSV", 6, 5, 5, 81.45839848659669));
		table.add(array("qHSV", 6, 6, 4, 82.57432023168315));
		table.add(array("qHSV", 2, 7, 7, 83.09689252187526));
		table.add(array("qHSV", 6, 4, 6, 83.60367097668954));
		table.add(array("qHSV", 6, 7, 3, 90.24254703335666));
		table.add(array("qRGB", 4, 5, 7, 90.47157622213018));
		table.add(array("qRGB", 3, 6, 7, 93.69327279840577));
		table.add(array("qRGB", 5, 4, 7, 94.78848826316275));
		table.add(array("qHSV", 6, 3, 7, 96.34074236006498));
		table.add(array("qRGB", 6, 3, 7, 103.09409877318586));
		table.add(array("qRGB", 4, 7, 5, 105.64751778991774));
		table.add(array("qRGB", 3, 7, 6, 105.73289902046317));
		table.add(array("qRGB", 5, 7, 4, 109.50856313511423));
		table.add(array("qRGB", 6, 7, 3, 116.35263661572063));
		table.add(array("qRGB", 7, 2, 7, 120.47091510995996));
		table.add(array("qRGB", 2, 7, 7, 121.25909325650642));
		table.add(array("qHSV", 7, 7, 2, 130.189412070187));
		table.add(array("qRGB", 7, 7, 2, 132.51054904265393));
		table.add(array("qHSV", 7, 6, 3, 141.2750518038755));
		table.add(array("qHSV", 7, 5, 4, 147.68593530869273));
		table.add(array("qHSV", 7, 4, 5, 150.63169725118138));
		table.add(array("qHSV", 7, 3, 6, 151.39341368885468));
		table.add(array("qHSV", 7, 2, 7, 153.7261405181208));
		table.add(array("qRGB", 6, 5, 6, 51.16588678216485));
		table.add(array("qRGB", 6, 6, 5, 53.65792546587413));
		table.add(array("qRGB", 7, 5, 5, 54.86314628308258));
		table.add(array("qHSV", 5, 6, 6, 58.5541643813996));
		table.add(array("qRGB", 5, 6, 6, 58.88231069937044));
		table.add(array("qHSV", 4, 6, 7, 67.29232843725568));
		table.add(array("qHSV", 4, 7, 6, 68.98398547192));
		table.add(array("qRGB", 7, 6, 4, 69.35909510351173));
		table.add(array("qRGB", 7, 4, 6, 70.20612999951547));
		table.add(array("qHSV", 5, 5, 7, 70.9522982391356));
		table.add(array("qHSV", 5, 7, 5, 73.15155535858463));
		table.add(array("qHSV", 6, 6, 5, 82.78120697495775));
		table.add(array("qHSV", 6, 5, 6, 83.28443665149418));
		table.add(array("qHSV", 3, 7, 7, 83.45320638949539));
		table.add(array("qHSV", 6, 7, 4, 90.172638730084));
		table.add(array("qRGB", 5, 5, 7, 91.24646560431594));
		table.add(array("qRGB", 4, 6, 7, 93.2732797260707));
		table.add(array("qHSV", 6, 4, 7, 96.09723782084295));
		table.add(array("qRGB", 6, 4, 7, 99.54726990857749));
		table.add(array("qRGB", 4, 7, 6, 105.39366045848752));
		table.add(array("qRGB", 5, 7, 5, 106.18657681840735));
		table.add(array("qRGB", 6, 7, 4, 113.434492479518));
		table.add(array("qRGB", 7, 3, 7, 118.27215956840143));
		table.add(array("qRGB", 3, 7, 7, 120.8193361510788));
		table.add(array("qHSV", 7, 7, 3, 130.01513071046674));
		table.add(array("qRGB", 7, 7, 3, 130.72885419357505));
		table.add(array("qHSV", 7, 6, 4, 140.9012578035098));
		table.add(array("qHSV", 7, 5, 5, 146.95565866567614));
		table.add(array("qHSV", 7, 4, 6, 149.59413316853056));
		table.add(array("qHSV", 7, 3, 7, 153.00106824868672));
		table.add(array("qRGB", 6, 6, 6, 62.13342555613777));
		table.add(array("qRGB", 7, 5, 6, 68.76461465385874));
		table.add(array("qRGB", 7, 6, 5, 69.64939924785347));
		table.add(array("qHSV", 5, 7, 6, 75.49661507284307));
		table.add(array("qHSV", 5, 6, 7, 76.37570829713836));
		table.add(array("qHSV", 6, 6, 6, 84.60279131029957));
		table.add(array("qHSV", 4, 7, 7, 84.87036525141197));
		table.add(array("qHSV", 6, 7, 5, 90.25307437485817));
		table.add(array("qRGB", 5, 6, 7, 93.09297348388682));
		table.add(array("qRGB", 6, 5, 7, 95.09765215140195));
		table.add(array("qHSV", 6, 5, 7, 96.05791272541956));
		table.add(array("qRGB", 5, 7, 6, 105.35293816829977));
		table.add(array("qRGB", 6, 7, 5, 109.55213823708338));
		table.add(array("qRGB", 7, 4, 7, 114.43406884005788));
		table.add(array("qRGB", 4, 7, 7, 120.06463886098378));
		table.add(array("qRGB", 7, 7, 4, 127.61800680953633));
		table.add(array("qHSV", 7, 7, 4, 129.69888897137199));
		table.add(array("qHSV", 7, 6, 5, 140.26914343765014));
		table.add(array("qHSV", 7, 5, 6, 146.10742102345884));
		table.add(array("qHSV", 7, 4, 7, 151.5596422793711));
		table.add(array("qRGB", 7, 6, 6, 76.82768387008761));
		table.add(array("qHSV", 5, 7, 7, 89.91254983728324));
		table.add(array("qHSV", 6, 7, 6, 91.6934546217888));
		table.add(array("qRGB", 6, 6, 7, 95.13524389792414));
		table.add(array("qHSV", 6, 6, 7, 97.3143392560389));
		table.add(array("qRGB", 6, 7, 6, 107.54519533200947));
		table.add(array("qRGB", 7, 5, 7, 109.04192747205848));
		table.add(array("qRGB", 5, 7, 7, 119.07036843710114));
		table.add(array("qRGB", 7, 7, 5, 123.04945636715787));
		table.add(array("qHSV", 7, 7, 5, 129.19031913082105));
		table.add(array("qHSV", 7, 6, 6, 139.72383239068853));
		table.add(array("qHSV", 7, 5, 7, 148.72214401655344));
		table.add(array("qHSV", 6, 7, 7, 103.02970911320593));
		table.add(array("qRGB", 7, 6, 7, 106.51658523554815));
		table.add(array("qRGB", 6, 7, 7, 119.09765442358427));
		table.add(array("qRGB", 7, 7, 6, 119.10670119292193));
		table.add(array("qHSV", 7, 7, 6, 129.00909805599323));
		table.add(array("qHSV", 7, 6, 7, 143.4394848729931));
		table.add(array("qRGB", 7, 7, 7, 126.23189633081189));
		table.add(array("qHSV", 7, 7, 7, 134.22861938161395));
		
		for (final Object[] row : table) {
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
			
			final int a = (abc[0] & 0xFF) >> this.qA;
			final int b = (abc[1] & 0xFF) >> this.qB;
			final int c = (abc[2] & 0xFF) >> this.qC;
			
			return (a << (16 - this.qB - this.qC)) | (b << (8 - this.qC)) | (c << 0);
		}
		
		public final String getType() {
			if (this instanceof RGBQuantizer) {
				return "qRGB";
			}
			
			if (this instanceof HSVQuantizer) {
				return "qHSV";
			}
			
			return this.getClass().getSimpleName();
		}
		
		@Override
		public final String toString() {
			return this.getType() + this.qA + "" + this.qB + "" + this.qC;
		}
		
		protected abstract void rgbToABC(final int rgb, final int[] abc);
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -5601399591176973099L;
		
		public static final ColorQuantizer newQuantizer(final String type, final int qA, final int qB, final int qC) {
			if ("qRGB".equals(type)) {
				return new RGBQuantizer(qA, qC, qB);
			}
			
			if ("qHSV".equals(type)) {
				return new HSVQuantizer(qA, qC, qB);
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
		protected final void rgbToABC(final int rgb, final int[] abc) {
			rgbToRGB(rgb, abc);
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -739890245396913487L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-04-10)
	 */
	public static final class HSVQuantizer extends ColorQuantizer {
		
		public HSVQuantizer(final int qH, final int qS, final int qV) {
			super(qH, qS, qV);
		}
		
		@Override
		protected final void rgbToABC(final int rgb, final int[] abc) {
			rgbToRGB(rgb, abc);
			rgbToHSV(abc, abc);
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -739890245396913487L;
		
	}
	
	public static final void shutdownAndWait(final ExecutorService executor, final long milliseconds) {
		executor.shutdown();
		
		try {
			executor.awaitTermination(milliseconds, TimeUnit.MILLISECONDS);
		} catch (final InterruptedException exception) {
			exception.printStackTrace();
		}
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
	
	/**
	 * @author codistmonk (creation 2014-04-10)
	 */
	public static final class DoubleArrayComparator implements Serializable, Comparator<double[]> {
		
		@Override
		public final int compare(final double[] array1, final double[] array2) {
			final int n1 = array1.length;
			final int n2 = array2.length;
			final int n = Math.min(n1, n2);
			
			for (int i = 0; i < n; ++i) {
				final int comparison = Double.compare(array1[i], array2[i]);
				
				if (comparison != 0) {
					return comparison;
				}
			}
			
			return n1 - n2;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -88586465954519984L;
		
		public static final DoubleArrayComparator INSTANCE = new DoubleArrayComparator();
		
	}
	
}
