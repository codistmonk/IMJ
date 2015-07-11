package imj3.draft;

import static multij.tools.Tools.*;
import de.schlichtherle.truezip.file.TFile;
import imj3.tools.MultifileImage2D;
import imj3.tools.MultifileSource;
import imj3.tools.SVS2Multifile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.zip.ZipInputStream;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;
import multij.tools.Tools;
import multij.xml.XMLTools;

import org.w3c.dom.Document;

/**
 * @author codistmonk (creation 2015-05-02)
 */
public final class UpdateMultifile {
	
	private UpdateMultifile() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String path = arguments.get("file", "");
		final String tilesPath = path + "/tiles/";
		final boolean includeHTMLViewer = arguments.get("includeHTMLViewer", 1)[0] != 0;
		final MultifileImage2D image = new MultifileImage2D(new MultifileSource(path), 0);
		
		new TFile(tilesPath).mkdir();
		
		for (final TFile file : new TFile(path).listFiles()) {
			final String oldName = file.getName();
			
			final String newName = tilesPath + oldName;
			
			if (oldName.startsWith("tile_")) {
				debugPrint(oldName + " -> " + newName);
				file.mv(new TFile(newName));
			}
		}
		
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final int optimalTileWidth = image.getOptimalTileWidth();
		final int optimalTileHeight = image.getOptimalTileHeight();
		final String tileFormat = image.getTileFormat();
		final int[] level = { 0 };
		
		{
			final Document newXML = SVS2Multifile.newMetadata(imageWidth, imageHeight, optimalTileWidth, optimalTileHeight,
					tileFormat, image.getMetadata().get("micronsPerPixel").toString(), level);
			
			try (final ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
				XMLTools.write(newXML, buffer, 0);
				
				debugPrint("Writing", path + "/metadata.xml");
				
				new TFile(path + "/metadata.xml").input(new ByteArrayInputStream(buffer.toByteArray()));
			}
		}
		
		if (includeHTMLViewer) {
			debugPrint("Including HTML viewer");
			includeHTMLViewer(path, imageWidth, imageHeight, optimalTileWidth, level[0] - 1, tileFormat);
		}
	}
	
	public static final void includeHTMLViewer(final String path,
			final int imageWidth, final int imageHeight, final int tileSize,
			final int lastLOD, final String tileFormat) throws IOException {
		try (final ZipInputStream template = new ZipInputStream(getResourceAsStream("lib/openseadragon/template.zip"))) {
			for (java.util.zip.ZipEntry entry = template.getNextEntry(); entry != null; entry = template.getNextEntry()) {
				if (!entry.isDirectory()) {
					final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					
					Tools.write(template, buffer);
					
					final TFile file = new TFile(path + "/" + entry.getName());
					
					file.getParentFile().mkdirs();
					file.createNewFile();
					file.input(new ByteArrayInputStream(buffer.toByteArray()));
				}
			}
		}
		
		{
			final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			final PrintStream out = new PrintStream(buffer);
			
			out.println("var tilePrefix = \"" + "tiles/tile_" +"\";");
			out.println("var imageWidth = " + imageWidth +";");
			out.println("var imageHeight = " + imageHeight +";");
			out.println("var tileSize = " + tileSize +";");
			out.println("var lastLOD = " + lastLOD +";");
			out.println("var tileFormat = \"" + tileFormat +"\";");
			
			final TFile file = new TFile(path + "/index_files/imj_metadata.js");
			file.createNewFile();
			file.input(new ByteArrayInputStream(buffer.toByteArray()));
		}
	}
	
}
