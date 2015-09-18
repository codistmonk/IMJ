package imj3.draft;

import static java.lang.Double.parseDouble;
import static multij.swing.SwingTools.*;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.io.File;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.w3c.dom.Document;

import multij.tools.Canvas;
import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-09-18)
 */
public final class ShowSVG {
	
	private ShowSVG() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File svgFile = new File(arguments.get("svg", ""));
		final Document svg = SVGTools.readXML(svgFile);
		final double width0 = parseDouble(svg.getDocumentElement().getAttribute("width"));
		final double height0 = parseDouble(svg.getDocumentElement().getAttribute("height"));
		final double aspectRatio = width0 / height0;
		final int height = 1024;
		final int width = (int) (height * aspectRatio);
		final Canvas canvas = new Canvas().setFormat(width, height);
		final Map<String, List<Area>> regions = SVGTools.getRegions(svg);
		final AffineTransform transform = new AffineTransform();
		
		transform.scale(width / width0, height / height0);
		canvas.getGraphics().setColor(Color.GREEN);
		
		regions.values().forEach(list -> list.forEach(region -> {
			region.transform(transform);
			canvas.getGraphics().draw(region);
		}));
		
		SwingUtilities.invokeLater(() -> {
			show(canvas.getImage(), svgFile.getName(), false);
		});
	}
	
}
