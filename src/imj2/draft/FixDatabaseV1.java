package imj2.draft;

import static imj2.tools.MultifileImage.setIdAttributes;
import static multij.tools.Tools.debugPrint;
import static multij.xml.XMLTools.parse;
import imj2.tools.MultifileImage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;
import multij.xml.XMLTools;

/**
 * @author codistmonk (creation 2013-11-11)
 */
public final class FixDatabaseV1 {
	
	private FixDatabaseV1() {
		throw new IllegalInstantiationException();
	}
	
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String rootPath = arguments.get("path", "");
		final Document database = getDatabaseV1(rootPath);
		final List<Element> imageNodes = (List<Element>) (Object) XMLTools.getNodes(database, "//image");
		final List<String> databaseLOD0Ids = new ArrayList<String>();
		
		{
			final String suffix = "_lod0";
			
			for (final Element element : imageNodes) {
				final String id = element.getAttribute("id");
				
				if (id.endsWith(suffix)) {
					final int i = id.lastIndexOf(suffix);
					databaseLOD0Ids.add(id.substring(0, i));
				}
			}
		}
		
		final File root = new File(rootPath);
		final List<String> multifileIds = new ArrayList<String>();
		
		{
			final String suffix = "_lod0_0_0.jpg";
			
			for (final File f : root.listFiles()) {
				final String name = f.getName();
				if (f.getName().endsWith(suffix)) {
					final int i = name.lastIndexOf(suffix);
					multifileIds.add(name.substring(0, i));
				}
			}
		}
		
		final Collection<String> missingFromDatabase = new LinkedHashSet<String>(multifileIds);
		final Collection<String> missingFromFilesystem = new LinkedHashSet<String>(databaseLOD0Ids);
		
		missingFromDatabase.removeAll(databaseLOD0Ids);
		missingFromFilesystem.removeAll(multifileIds);
		
		debugPrint("missingFromDatabase:", missingFromDatabase);
		debugPrint("missingFromFilesystem:", missingFromFilesystem);
	}
	
	public static final Document getDatabaseV1(final String databaseRoot) {
		return setIdAttributes(parse(MultifileImage.open(databaseRoot + "/" + "imj_database.xml")));
	}
	
}
