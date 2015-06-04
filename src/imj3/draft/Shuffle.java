package imj3.draft;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.Random;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-05-05)
 */
public final class Shuffle {
	
	private Shuffle() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws IOException 
	 */
	public static final void main(final String[] commandLineArguments) throws IOException {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File inputFile = new File(arguments.get("file", ""));
		final int chunkSize = arguments.get("chunkSize", 1)[0];
		final File outputFile = new File(arguments.get("out", inputFile.getPath()));
		final String seedAsString = arguments.get("seed", "");
		final Random random;
		
		if (seedAsString.isEmpty()) {
			random = new Random();
		} else {
			random = new Random(Long.decode(seedAsString));
		}
		
		if (!inputFile.equals(outputFile)) {
			Files.copy(inputFile.toPath(), outputFile.toPath());
		}
		
		try (final RandomAccessFile file = new RandomAccessFile(outputFile, "rw")) {
			final long n = file.length() / chunkSize;
			final byte[] a = new byte[chunkSize], b = a.clone();
			
			for (long i = 0; i < n; ++i) {
				final long j = random.nextLong() % n;
				final long offsetI = i * chunkSize;
				final long offsetJ = j * chunkSize;
				
				read(file, offsetI, a);
				read(file, offsetJ, b);
				write(file, offsetJ, a);
				write(file, offsetI, b);
			}
		}
	}
	
	public static final void read(final RandomAccessFile file, final long offset, final byte[] result) throws IOException {
		file.seek(offset);
		file.readFully(result);
	}
	
	public static final void write(final RandomAccessFile file, final long offset, final byte[] bytes) throws IOException {
		file.seek(offset);
		file.write(bytes);
	}
	
}
