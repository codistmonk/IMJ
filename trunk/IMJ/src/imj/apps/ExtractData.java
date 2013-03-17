package imj.apps;

import static imj.apps.ExtractRegions.maybeCacheImage;
import static imj.apps.modules.Annotations.parseBoolean;
import static imj.apps.modules.ViewFilter.parseChannels;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.Image;
import imj.ImageWrangler;
import imj.apps.modules.ViewFilter.Channel;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.aprog.tools.AbstractIterator;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2013-03-17)
 */
public final class ExtractData {
	
	private ExtractData() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws Exception If an error occurs 
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imageId = arguments.get("file", "");
		final int lod = arguments.get("lod", 0)[0];
		final Image image = maybeCacheImage(ImageWrangler.INSTANCE.load(imageId, lod));
		final String channelsAsString = arguments.get("channels", "red green blue");
		final String groupingAsString = arguments.get("grouping", "tile 1 1");
		final boolean interleaved = parseBoolean(arguments.get("interleaved", "1"));
		final Map<List<Integer>, Long> data = new HashMap<List<Integer>, Long>();
		final TicToc timer = new TicToc();
		final String simpleName = new File(imageId).getName();
		final PrintStream out = new PrintStream(arguments.get("out", simpleName +
				".lod" + lod +
				".channels_" + channelsAsString.trim().replaceAll("\\s+", "_") +
				".grouping_") + groupingAsString.trim().replaceAll("\\s+", "_") +
				".interleaved" + (interleaved ? "1" : "0") +
				".dat");
		
		System.out.println("Collecting data... (" + new Date(timer.tic()) + ")");
		
		for (final List<Integer> key : Grouping.newGrouping(image, groupingAsString, channelsAsString, interleaved)) {
			final Long count = data.get(key);
			
			if (count == null) {
				data.put(new ArrayList<Integer>(key), +1L);
			} else {
				data.put(key, count + 1L);
			}
		}
		
		System.out.println("Collecting data done (time: " + timer.toc() + " memory: " + usedMemory() + ")");
		
		System.out.println("Writing data... (" + new Date(timer.tic()) + ")");
		
		for (final Map.Entry<List<Integer>, Long> entry : data.entrySet()) {
			for (final Integer i : entry.getKey()) {
				out.print(i);
				out.print(" ");
			}
			
			out.println(entry.getValue());
		}
		
		System.out.println("Writing data done (time: " + timer.toc() + " memory: " + usedMemory() + ")");
		
		out.close();
	}
	
	/**
	 * @author codistmonk (creation 2013-03-17)
	 */
	public static abstract class Grouping implements Iterable<List<Integer>> {
		
		private final Image image;
		
		private final Channel[] channels;
		
		private final boolean interleaved;
		
		public Grouping(final Image image, final Channel[] channels, final boolean interleaved) {
			this.image = image;
			this.channels = channels;
			this.interleaved = interleaved;
		}
		
		public final Image getImage() {
			return this.image;
		}
		
		public final Channel[] getChannels() {
			return this.channels;
		}
		
		public final boolean isInterleaved() {
			return this.interleaved;
		}
		
		public static final Grouping newGrouping(final Image image,
				final String groupingAsString, final String channelsAsString, final boolean interleaved) {
			final String[] groupingAsStrings = groupingAsString.trim().split("\\s+");
			
			if (groupingAsStrings[0].equalsIgnoreCase("tile")) {
				final int tileRowCount = Integer.parseInt(groupingAsStrings[1]);
				final int tileColumnCount = Integer.parseInt(groupingAsStrings[2]);
				
				return new Tile(image, parseChannels(channelsAsString), interleaved, tileRowCount, tileColumnCount);
			}
			
			throw new IllegalArgumentException("Invalid grouping: " + groupingAsString);
		}
		
		/**
		 * @author codistmonk (creation 2013-03-17)
		 */
		public static final class Tile extends Grouping {
			
			private final int rowCount;
			
			private final int columnCount;
			
			public Tile(final Image image, final Channel[] channels, final boolean interleaved,
					final int rowCount, final int columnCount) {
				super(image, channels, interleaved);
				this.rowCount = rowCount;
				this.columnCount = columnCount;
			}
			
			public final int getRowCount() {
				return this.rowCount;
			}
			
			public final int getColumnCount() {
				return this.columnCount;
			}
			
			@Override
			public final Iterator<List<Integer>> iterator() {
				final Image image = this.getImage();
				final Channel[] channels = this.getChannels();
				final boolean interleaved = this.isInterleaved();
				final int imageRowCount = this.getImage().getRowCount();
				final int imageColumnCount = this.getImage().getColumnCount();
				final int tileRowCount = this.getRowCount();
				final int tileColumnCount = this.getColumnCount();
				final int channelCount = this.getChannels().length;
				final List<Integer> element = new ArrayList<Integer>(channelCount);
				
				return new AbstractIterator<List<Integer>>(null) {
					
					private int beginRowIndex = 0;
					
					private int beginColumnIndex = -tileColumnCount;
					
					{
						this.setNextElement(element);
					}
					
					@Override
					public final void remove() {
						throw new UnsupportedOperationException();
					}
					
					@Override
					protected final boolean updateNextElement() {
						this.beginColumnIndex += tileColumnCount;
						int endColumnIndex = this.beginColumnIndex + tileColumnCount;
						
						if (imageColumnCount <= endColumnIndex) {
							this.beginColumnIndex = 0;
							endColumnIndex = tileColumnCount;
							this.beginRowIndex += tileRowCount;
						}
						
						final int endRowIndex = this.beginRowIndex + tileRowCount;
						
						if (endRowIndex < imageRowCount && endColumnIndex < imageColumnCount) {
							element.clear();
							
							if (interleaved) {
								for (int rowIndex = this.beginRowIndex; rowIndex < endRowIndex; ++rowIndex) {
									for (int columnIndex = this.beginColumnIndex; columnIndex < endColumnIndex; ++columnIndex) {
										for (final Channel channel : channels) {
											element.add(channel.getValue(image.getValue(rowIndex, columnIndex)));
										}
									}
								}
							} else {
								for (final Channel channel : channels) {
									for (int rowIndex = this.beginRowIndex; rowIndex < endRowIndex; ++rowIndex) {
										for (int columnIndex = this.beginColumnIndex; columnIndex < endColumnIndex; ++columnIndex) {
											element.add(channel.getValue(image.getValue(rowIndex, columnIndex)));
										}
									}
								}
							}
							
							return true;
						}
						
						return false;
					}
					
				};
			}
			
		}
		
	}
	
}
