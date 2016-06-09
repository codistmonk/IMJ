package imj3.draft;

import static multij.tools.Tools.*;

import imj3.core.Image2D;
import imj3.core.Image2D.Pixel2DProcessor;
import imj3.tools.BinView;
import imj3.tools.IMJTools;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2016-06-09)
 */
public final class Multifile2Bin {
	
	private Multifile2Bin() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String... commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imagePath = arguments.get("image", "");
		final String labelsPath = arguments.get("labels", baseName(imagePath) + "_labels.zip");
		final String outputPath = arguments.get("output", baseName(labelsPath) + ".bin");
		final int lod = arguments.get1("lod", 0);
		final int patchSide = arguments.get1("patchSide", 32);
		final int[] patchContext = arguments.get("patchContext", 0);
		final Image2D image = IMJTools.read(imagePath, lod);
		final Image2D labels = IMJTools.read(labelsPath, lod);
		final int perClassSampling = arguments.get1("perClassSampling", 100);
		final int[] xywh = arguments.get("bounds", 0, 0, image.getWidth(), image.getHeight());
		final int excludedLabel = arguments.get1("exclude", -1);
		final String seedString = arguments.get("seed", "");
		final boolean show = arguments.get("show", false);
		final Random random = new Random();
		
		if (!seedString.isEmpty()) {
			random.setSeed(Long.decode(seedString));
		}
		
		debugPrint("image:", imagePath);
		debugPrint("labels:", labelsPath);
		debugPrint("output:", outputPath);
		debugPrint("LOD:", lod, "width:", image.getWidth(), "height:", image.getHeight());
		debugPrint("bounds:", Arrays.toString(xywh), "perClassSampling:", perClassSampling);
		debugPrint("patchSide:", patchSide, "patchContext:", Arrays.toString(patchContext));
		debugPrint("seed:", "(" + seedString + ")");
		
		if (labels.getWidth() != image.getWidth() || labels.getHeight() != labels.getHeight()) {
			throw new IllegalArgumentException();
		}
		
		final Rectangle bounds = new Rectangle(xywh[0], xywh[1], xywh[2], xywh[3]);
		final Map<Integer, Map<Point, long[]>> available = new TreeMap<>();
		
		IMJTools.forEachTileIn(image, new Pixel2DProcessor() {
			
			private final Rectangle tileBounds = new Rectangle();
			
			private final Rectangle intersection = new Rectangle();
			
			@Override
			public final boolean pixel(final int tileX, final int tileY) {
				final Point tileXY = new Point(tileX, tileY);
				
				this.tileBounds.setBounds(tileX, tileY, image.getTileWidth(tileX), image.getTileHeight(tileY));
				
				Rectangle2D.intersect(bounds, this.tileBounds, this.intersection);
				
				if (!this.intersection.isEmpty()) {
					final int x0 = this.intersection.x;
					final int x1 = x0 + this.intersection.width;
					final int y0 = this.intersection.y;
					final int y1 = y0 + this.intersection.height;
					
					for (int y = y0; y < y1; ++y) {
						for (int x = y0; x < x1; ++x) {
							final int label = (int) labels.getPixelValue(x, y);
							
							if (label != excludedLabel) {
								++available.computeIfAbsent(label, __ -> new LinkedHashMap<>()).computeIfAbsent(tileXY, __ -> new long[1])[0];
							}
						}
					}
				}
				
				return true;
			}
			
			private static final long serialVersionUID = 2040728187903973625L;
			
		});
		
		for (final Map.Entry<Integer, Map<Point, long[]>> entry1 : available.entrySet()) {
			debugPrint("label:", Integer.toHexString(entry1.getKey()));
			
			for (final Map.Entry<Point, long[]> entry2 : entry1.getValue().entrySet()) {
				final Point tileXY = entry2.getKey();
				
				debugPrint("\t", "tile:", "(" + tileXY.x + " " + tileXY.y + ")", "count:", entry2.getValue()[0]);
			}
		}
		
		final Map<Point, Map<Integer, Collection<Long>>> requests = new HashMap<>();
		
		try (final FileOutputStream output = new FileOutputStream(outputPath)) {
			for (final Map.Entry<Integer, Map<Point, long[]>> entry1 : available.entrySet()) {
				final Point[] tiles = entry1.getValue().keySet().toArray(new Point[entry1.getValue().size()]);
				final long[] counts = entry1.getValue().values().stream().mapToLong(v -> v[0]).toArray();
				final long sum = Arrays.stream(counts).sum();
				
				final List<Long> indices = new ArrayList<>(perClassSampling);
				
				{
					int i = 0;
					
					final int m = (int) (perClassSampling % sum);
					
					for (; i < m; ++i) {
						Long candidate;
						
						do {
							candidate = random.nextLong();
							
							if (candidate < 0L) {
								candidate = -candidate;
							}
							
							candidate = candidate % sum;
						} while (indices.contains(candidate));
						
						indices.add(candidate);
					}
					
					{
						long j = 0L;
						
						for (; i < perClassSampling; ++i) {
							indices.add(j % sum);
						}
					}
				}
				
				for (final Long index : indices) {
					final int[] i = { 0 };
					
					for (long acc = 0L; i[0] < counts.length; ++i[0]) {
						acc += counts[i[0]];
						
						if (index < acc) {
							break;
						}
					}
					
					requests.computeIfAbsent(tiles[i[0]], __ -> new HashMap<>()).computeIfAbsent(entry1.getKey(), __ -> new ArrayList<>()).add(index);
				}
			}
			
			final Map<Integer, long[]> ids = new HashMap<>();
			
			IMJTools.forEachTileIn(image, new Pixel2DProcessor() {
				
				private final Rectangle tileBounds = new Rectangle();
				
				private final Rectangle intersection = new Rectangle();
				
				private final byte[] item = new byte[1 + patchSide * patchSide * image.getChannels().getChannelCount() * patchContext.length];
				
				@Override
				public final boolean pixel(final int tileX, final int tileY) {
					final Point tileXY = new Point(tileX, tileY);
					
					this.tileBounds.setBounds(tileX, tileY, image.getTileWidth(tileX), image.getTileHeight(tileY));
					
					Rectangle2D.intersect(bounds, this.tileBounds, this.intersection);
					
					if (!this.intersection.isEmpty()) {
						final int x0 = this.intersection.x;
						final int x1 = x0 + this.intersection.width;
						final int y0 = this.intersection.y;
						final int y1 = y0 + this.intersection.height;
						
						try {
							for (int y = y0; y < y1; ++y) {
								for (int x = y0; x < x1; ++x) {
									final int label = (int) labels.getPixelValue(x, y);
									
									if (label != excludedLabel) {
										final long[] id = ids.computeIfAbsent(label, __ -> new long[1]);
										final Map<Integer, Collection<Long>> tileRequests = requests.get(tileXY);
										
										if (tileRequests != null) {
											final Collection<Long> labelRequests = tileRequests.get(label);
											
											if (labelRequests != null) {
												while (labelRequests.remove(id[0])) {
													SVG2Bin.getItem(image, x, y, patchSide, patchContext, (byte) label, this.item);
													output.write(this.item);
												}
											}
										}
										
										++id[0];
									}
								}
							}
						} catch (final IOException exception) {
							exception.printStackTrace();
						}
					}
					
					return true;
				}
				
				private static final long serialVersionUID = 2040728187903973625L;
				
			});
			
			if (show) {
				BinView.main(outputPath);
			}
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

}
