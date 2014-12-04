package imj2.draft;

import static imj2.draft.PaletteBasedHistograms.awtReadImage;
import static imj2.draft.PaletteBasedHistograms.forEachPixelIn;
import static java.lang.Math.max;
import static net.sourceforge.aprog.tools.Tools.baseName;

import imj2.core.Image2D;
import imj2.draft.PaletteBasedSegmentation.NearestNeighborRGBQuantizer;
import imj2.tools.AwtBackedImage;
import imj2.tools.IMJTools;
import imj2.tools.ColorSeparationTest.RGBTransformer;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.ComboBoxModel;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.Factory.DefaultFactory;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2014-12-04)
 */
public final class Labels2Cells {
	
	private Labels2Cells() {
		throw new IllegalInstantiationException();
	}
	
	static final Random RANDOM = new Random(0L);
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File root = new File(arguments.get("in", ""));
		final File paletteFile = new File(arguments.get("palette", "transformerSelectorModel.jo"));
		final ComboBoxModel<? extends RGBTransformer> palette = Tools.readObject(paletteFile.getPath());
		final TicToc timer = new TicToc();
		
		Tools.debugPrint(palette);
		final RGBTransformer quantizer = palette.getElementAt(1);
		Tools.debugPrint(quantizer);
		
		// temporary fix for a defective palette
		if (true) {
			final NearestNeighborRGBQuantizer nnq = (NearestNeighborRGBQuantizer) quantizer;
			
			nnq.getClusters().remove(0xFF008080);
			
			Tools.debugPrint(nnq.getClusters());
		}
		
		for (final File file : root.listFiles()) {
			final String fileName = file.getName();
			
			if (fileName.endsWith(".png")) {
				final String baseName = baseName(fileName);
				final String maskName = baseName + "_mask.png";
				final String labelsName = baseName + "_labels.png";
				final String cellsName = baseName + "_cells.png";
				final String segmentedName = baseName + "_segmented.png";
				final File maskFile = new File(file.getParentFile(), maskName);
				final File labelsFile = new File(file.getParentFile(), labelsName);
				final File cellsFile = new File(file.getParentFile(), cellsName);
				final File segmentedFile = new File(file.getParentFile(), segmentedName);
				
				if (maskFile.exists() && labelsFile.exists()) {
					final BufferedImage image = awtReadImage(file.getPath());
					final BufferedImage mask = awtReadImage(maskFile.getPath());
					final BufferedImage labels = awtReadImage(labelsFile.getPath());
					final BufferedImage cells = new BufferedImage(
							image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
					
					forEachPixelIn(image, (x, y) -> {
						if (0xFFFFFF00 == image.getRGB(x, y)) {
							mask.setRGB(x, y, 0);
						}
						
						return true;
					});
					
					IMJTools.forEachPixelInEachComponent4(new AwtBackedImage(labelsName, labels), new Image2D.Process() {
						
						private final List<Point> pixels = new ArrayList<>();
						
						private int cellId;
						
						@Override
						public final void pixel(final int x, final int y) {
							if ((mask.getRGB(x, y) & 1) != 0) {
								this.pixels.add(new Point(x, y));
							}
						}
						
						@Override
						public final void endOfPatch() {
							final int n = this.pixels.size();
							
							if (n < 10) {
								this.pixels.forEach(p -> cells.setRGB(p.x, p.y, 0));
							} else {
								final int k = max(1, this.pixels.size() / 30);
								
								if (1 < k && n <= 1000) {
									Tools.debugPrint(n, k, this.cellId);
									
									// TODO k-means on pixels
									final int[] clustering = new int[n];
									final Point[] kMeans = Tools.instances(k, DefaultFactory.forClass(Point.class));
									final int[] kCounts = new int[k];
									
									for (int i = 0; i < n; ++i) {
										clustering[i] = k * i / n;
									}
									
									for (int i = 0; i < 10; ++i) {
										computeMeans(this.pixels, clustering, kMeans, kCounts);
										recluster(this.pixels, clustering, kMeans);
									}
									
									for (int i = 0; i < n; ++i) {
										final Point p = this.pixels.get(i);
										cells.setRGB(p.x, p.y, this.cellId + 1 + clustering[i]);
									}
									
									this.cellId += k;
								} else {
									++this.cellId;
									this.pixels.forEach(p -> cells.setRGB(p.x, p.y, this.cellId));
								}
							}
							
							this.pixels.clear();
						}
						
					});
					
					final int[] cellId = { 0 };
					final List<Point> todo = new ArrayList<>();
					
					forEachPixelIn(cells, (x, y) -> {
						final int id = cells.getRGB(x, y);
						
						if (id != 0) {
							cellId[0] = id;
							
							todo.forEach(p -> cells.setRGB(p.x, p.y, id));
							todo.clear();
						} else if (cellId[0] != 0) {
							cells.setRGB(x, y, cellId[0]);
						} else {
							todo.add(new Point(x, y));
						}
						
						return true;
					});
					
					try {
						Tools.debugPrint("Writing", cellsFile);
//						ImageIO.write(cells, "png", cellsFile);
						PaletteBasedHistograms.outlineSegments(cells, mask, image, 0xFF0000FF);
						ImageIO.write(image, "png", cellsFile);
					} catch (final IOException exception) {
						exception.printStackTrace();
					}
					
//					return;
				}
			}
		}
	}
	
	public static final void computeMeans(final List<Point> points, final int[] clustering,
			final Point[] means, final int[] counts) {
		final int k = means.length;
		final int n = points.size();
		
		Arrays.fill(counts, 0);
		
		for (int i = 0; i < n; ++i) {
			final Point p = points.get(i);
			final int j = clustering[i];
			means[j].x += p.x;
			means[j].y += p.y;
			++counts[j];
		}
		
		for (int i = 0; i < k; ++i) {
			final int count = counts[i];
			
			if (count != 0) {
				means[i].x /= count;
				means[i].y /= count;
			}
		}
	}
	
	public static final void recluster(final List<Point> points,
			final int[] clustering, final Point[] centers) {
		final int n = points.size();
		final int k = centers.length;
		
		for (int i = 0; i < n; ++i) {
			final Point p = points.get(i);
			int nearest = clustering[i];
			double bestDistance = Double.POSITIVE_INFINITY;
			
			for (int j = 0; j < k; ++j) {
				final double distance = p.distance(centers[j]);
				
				if (distance < bestDistance) {
					nearest = j;
					bestDistance = distance;
				}
			}
			
			clustering[i] = nearest;
		}
	}
	
}
