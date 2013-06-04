package imj.apps;

import static imj.database.IMJDatabaseTools.checkDatabase;
import imj.database.PatchDatabase;
import imj.database.Sample;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Map;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-06-04)
 */
public final class CheckSampleDatabase {
	
	private CheckSampleDatabase() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws Exception 
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String fileName = arguments.get("file", "");
		final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName));
		
		try {
			final Map<Object, Object> database = (Map<Object, Object>) ois.readObject();
			final PatchDatabase<Sample> sampleDatabase = (PatchDatabase<Sample>) database.get("samples");
			
			checkDatabase(sampleDatabase);
		} finally {
			ois.close();
		}
	}
	
}
