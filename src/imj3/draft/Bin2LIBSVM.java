package imj3.draft;

import static multij.tools.Tools.baseName;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-09-02)
 */
public final class Bin2LIBSVM {
	
	private Bin2LIBSVM() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws IOException 
	 */
	public static final void main(final String[] commandLineArguments) throws IOException {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String binPath = arguments.get("bin", "");
		final int itemSize = arguments.get("itemSize", 2)[0];
		final String outPath = arguments.get("out", baseName(binPath) + ".txt");
		final byte[] item = new byte[itemSize];
		
		try (final InputStream input = new FileInputStream(binPath);
				final PrintStream output = new PrintStream(outPath)) {
			while (input.read(item) == itemSize) {
				final StringBuilder line = new StringBuilder();
				
				line.append(0xFF & item[0]);
				
				for (int i = 1; i < itemSize; ++i) {
					final int value = 0xFF & item[i];
					
					if (value != 0) {
						line.append(' ').append(i).append(':').append(value);
					}
				}
				
				output.println(line);
			}
		}
	}
	
}
