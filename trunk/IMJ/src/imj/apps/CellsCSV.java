package imj.apps;

import static java.awt.Color.WHITE;
import static java.lang.Double.parseDouble;
import static java.lang.Math.PI;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.set;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import imj.apps.modules.Annotations;
import imj.apps.modules.ShowActions;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import javax.imageio.ImageIO;

import loci.formats.IFormatReader;
import loci.formats.ImageReader;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-06-29)
 */
public final class CellsCSV {
	
	private CellsCSV() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String filePath = arguments.get("file", "");
		final String outPath = arguments.get("out", "cells_mask.png");
		final String imagePath = arguments.get("image", "");
		final String xmlPath = arguments.get("xml", ShowActions.baseName(imagePath) + ".xml");
		final int lod = arguments.get("lod", 0)[0];
		final IFormatReader reader = new ImageReader();
		final Annotations annotations = Annotations.fromXML(xmlPath);
		final Collection<String> cellTypes = set(arguments.get("cellTypes", "Cell").split(","));
		final List<String> fieldNames = new ArrayList<String>();
		final boolean reduceCells = arguments.get("reduceCells", 1)[0] != 0;
		int cellTypeFieldIndex = -1;
		int cellCenterXFieldIndex = -1;
		int cellCenterYFieldIndex = -1;
		int cellAreaFieldIndex = -1;
		final int reduction = 1 << lod;
		final int cellReduction = reduceCells ? reduction : 1;
		
		try {
			final Scanner scanner = new Scanner(new File(filePath));
			
			scanner.useLocale(Locale.ENGLISH);
			
			reader.setId(imagePath);
			
			final int width = reader.getSizeX();
			final int height = reader.getSizeY();
			final int w = width / reduction;
			final int h = height / reduction;
			final double micronsPerPixel = annotations.getMicronsPerPixel();
			final double metersPerPixel = micronsPerPixel * 1.0E-6;
			
			debugPrint("image:", imagePath);
			debugPrint("width:", width, "height:", height);
			debugPrint("micronsPerPixel:", micronsPerPixel);
			
			final BufferedImage mask = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
			final Graphics2D g = mask.createGraphics();
			
			g.setColor(WHITE);
			
			while (scanner.hasNext()) {
				final String line = scanner.nextLine();
				final String[] fields = line.split(",");
				
				if (fieldNames.isEmpty()) {
					fieldNames.addAll(Arrays.asList(fields));
					cellTypeFieldIndex = fieldNames.indexOf("class_name");
					cellCenterXFieldIndex = fieldNames.indexOf("X Center (coor.)");
					cellCenterYFieldIndex = fieldNames.indexOf("Y Center (coor.)");
					cellAreaFieldIndex = fieldNames.indexOf("Area_[sq.um]");
				} else {
					if (cellTypes.contains(fields[cellTypeFieldIndex])) {
						final int x = (int) (parseDouble(fields[cellCenterXFieldIndex]) / metersPerPixel / reduction);
						final int y = (int) (parseDouble(fields[cellCenterYFieldIndex]) / metersPerPixel / reduction);
						final int r = (int) (sqrt(parseDouble(fields[cellAreaFieldIndex]) / PI) / micronsPerPixel / cellReduction);
						final int d = 2 * r;
						
						g.fillOval(x - r, y - r, d, d);
					}
				}
			}
			
			g.dispose();
			scanner.close();
			
			ImageIO.write(mask, "png", new File(outPath));
		} finally {
			try {
				reader.close();
			} catch (final IOException exception) {
				throw unchecked(exception);
			}
		}
	}
	
}
