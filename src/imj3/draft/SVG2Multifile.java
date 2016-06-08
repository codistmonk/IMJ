package imj3.draft;

import static imj3.draft.SVGTools.readXML;
import static imj3.tools.IMJTools.createZipImage;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static multij.tools.Tools.baseName;
import static multij.tools.Tools.debugPrint;
import static multij.tools.Tools.unchecked;
import static multij.xml.XMLTools.getNumber;

import imj3.tools.IMJTools.TileGenerator;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import multij.tools.Canvas;
import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author codistmonk (creation 2016-06-07)
 */
public final class SVG2Multifile {
	
	private SVG2Multifile() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String... commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String svgPath = arguments.get("svg", "");
		final String outputPath = arguments.get("output", baseName(svgPath) + "_rasterized.zip");
		final Document xml = readXML(new File(svgPath));
		final Element svg = xml.getDocumentElement();
		final int width = arguments.get1("width", getNumber(svg, "@width").intValue());
		final int height = arguments.get1("height", getNumber(svg, "@height").intValue());
		final int optimalTileWidth = arguments.get1("tileWidth", 512);
		final int optimalTileHeight = arguments.get1("tileHeight", optimalTileWidth);
		final double micronsPerPixel = parseDouble(arguments.get("micronsPerPixel", "0.25"));
		final Color clearColor = new Color(arguments.get1("clearColor", 0), true);
		final String[] classIds = arguments.get("classIds", "0,1").split(",");
		final String tileFormat = arguments.get("tileFormat", "png");
		
		debugPrint("svg:", svgPath);
		debugPrint("output:", outputPath);
		debugPrint("width:", width, "height:", height, "tileWidth:", optimalTileWidth, "tileHeight:", optimalTileHeight, "tileFormat:", tileFormat);
		debugPrint("clearColor:", clearColor.getRGB(), "classIds:", Arrays.toString(classIds));
		
		createZipImage(outputPath, width, height, optimalTileWidth, optimalTileHeight, tileFormat, micronsPerPixel, new TileGenerator() {
			
			private final Canvas canvas = new Canvas();
			
			private ZipOutputStream output;
			
			private String type;
			
			private int w, h;
			
			private double scale;
			
			@Override
			public final void setOutput(final ZipOutputStream output) {
				this.output = output;
			}
			
			@Override
			public final void setElement(final Element imageElement) {
				this.type = imageElement.getAttribute("type");
				this.w = getNumber(imageElement, "@width").intValue();
				this.h = getNumber(imageElement, "@height").intValue();
				
				if (this.type.startsWith("lod")) {
					this.scale = pow(2.0, -parseInt(this.type.substring("lod".length())));
				} else {
					this.scale = 1.0;
				}
			}
			
			@Override
			public final boolean pixel(final int tileX, final int tileY) {
				if (tileX == 0) {
					debugPrint("type:", this.type, "tileX:", tileX, "tileY:", tileY);
				}
				
				try {
					final int tileWidth = min(optimalTileWidth, this.w - tileX);
					final int tileHeight = min(optimalTileHeight, this.h - tileY);
					
					this.canvas.setFormat(tileWidth, tileHeight, BufferedImage.TYPE_3BYTE_BGR).clear(clearColor);
					
					this.canvas.getGraphics().translate(-tileX, -tileY);
					SVG2PNG.draw(xml, this.scale, classIds, this.canvas.getGraphics());
					this.canvas.getGraphics().translate(tileX, tileY);
					
					this.output.putNextEntry(new ZipEntry("tiles/tile_" + this.type + "_y" + tileY + "_x" + tileX + "." + tileFormat));
					ImageIO.write(this.canvas.getImage(), tileFormat, this.output);
					this.output.closeEntry();
				} catch (final Exception exception) {
					throw unchecked(exception);
				}
				
				return true;
			}
			
			private static final long serialVersionUID = -5394797515523626659L;
			
		});
	}

}
