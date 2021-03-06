package imj.apps;

import static imj.IMJTools.loadAndTryToCache;
import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static multij.tools.Tools.baseName;
import static multij.tools.Tools.usedMemory;

import imj.Image;
import imj.apps.modules.Annotations;
import imj.apps.modules.Annotations.Annotation;
import imj.apps.modules.Annotations.Annotation.Region;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.ShowActions.UseAnnotationAsROI;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;
import multij.tools.TicToc;

/**
 * @author codistmonk (creation 2013-03-12)
 */
public final class ExtractRegions {
	
	private ExtractRegions() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imageId = arguments.get("file", "");
		final String annotationsId = arguments.get("annotations", baseName(imageId) + ".xml");
		final Annotations annotations = Annotations.fromXML(annotationsId);
		final int[] forceLods = arguments.get("lods");
		int regionCount = 0;
		
		for (final Annotation annotation : annotations.getAnnotations()) {
			regionCount += annotation.getRegions().size();
		}
		
		if (regionCount == 0) {
			System.out.println("No region found");
			
			return;
		}
		
		final String baseName = baseName(new File(imageId).getName());
		final List<Image> lods = loadLods(imageId);
		final int lodCount = lods.size();
		
		for (int lod = 0; lod < lodCount; ++lod) {
			if (forceLods.length != 0 && Arrays.binarySearch(forceLods, lod) < 0) {
				continue;
			}
			
			final TicToc timer = new TicToc();
			
			System.out.println("Processing lod " + lod + "... (" + new Date(timer.tic()) + ")");
			
			final Image image = lods.get(lod);
			final RegionOfInterest roi = RegionOfInterest.newInstance(image.getRowCount(), image.getColumnCount());
			int regionId = 1;
			final Collection<Region> allRegions = UseAnnotationAsROI.collectAllRegions(annotations);
			
			for (final Region region : allRegions) {
				System.out.println("Processing region " + regionId + "... (" + new Date(timer.tic()) + ")");
				
				final Annotation annotation = (Annotation) region.getParent();
				final File categoryDirectory = new File("" + annotation.getUserObject());
				
				categoryDirectory.mkdir();
				
				UseAnnotationAsROI.set(roi, lod, asList(region), allRegions);
				
				final Rectangle bounds = getBounds(roi);
				final BufferedImage out = new BufferedImage(bounds.width, bounds.height, TYPE_3BYTE_BGR);
				final int endY = bounds.y + bounds.height;
				final int endX = bounds.x + bounds.width;
				
				for (int y = bounds.y; y < endY; ++y) {
					for (int x = bounds.x; x < endX; ++x) {
						if (roi.get(y, x)) {
							out.setRGB(x - bounds.x, y - bounds.y, image.getValue(y, x));
						}
					}
				}
				
				try {
					ImageIO.write(out, "png", new File(categoryDirectory,
							baseName + ".lod" + lod + ".region" + regionId + ".png"));
				} catch (final IOException exception) {
					exception.printStackTrace();
				}
				
				System.out.println("Processing region " + regionId + " done (time:" + timer.toc() + " memory:" + usedMemory() + ")");
				
				++regionId;
			}
			
			System.out.println("Processing lod " + lod + " done (time:" + timer.getTotalTime() + " memory:" + usedMemory() + ")");
		}
	}
	
	public static final Rectangle getBounds(final RegionOfInterest roi) {
		final int rowCount = roi.getRowCount();
		final int columnCount = roi.getColumnCount();
		final Rectangle result = new Rectangle(columnCount - 1, rowCount - 1, 1, 1);
		int maxX = 0;
		int maxY = 0;
		
		for (int rowIndex = 0; rowIndex < rowCount; ++rowIndex) {
			for (int columnIndex = 0; columnIndex < columnCount; ++columnIndex) {
				if (roi.get(rowIndex, columnIndex)) {
					result.x = min(result.x, columnIndex);
					result.y = min(result.y, rowIndex);
					maxX = max(maxX, columnIndex);
					maxY = max(maxY, rowIndex);
				}
			}
		}
		
		result.width = max(1, 1 + maxX - result.x);
		result.height = max(1, 1 + maxY - result.y);
		
		return result;
	}
	
	public static final Iterable<Region> collectRegions(final Annotations annotations, final String excludedNamePattern) {
		final Collection<Region> result = new ArrayList<Region>();
		
		for (final Annotation annotation : annotations.getAnnotations()) {
			if (!("" + annotation.getUserObject()).matches(excludedNamePattern)) {
				result.addAll(annotation.getRegions());
			}
		}
		
		return result;
	}
	
	public static final List<Image> loadLods(final String imageId) {
		final List<Image> result = new ArrayList<Image>();
		int lod = 0;
		Image image = loadAndTryToCache(imageId, lod);
		result.add(image);
		
		while (1 < image.getRowCount() && 1 < image.getColumnCount()) {
			image = loadAndTryToCache(imageId, ++lod);
			
			result.add(image);
		}
		
		return result;
	}
	
}
