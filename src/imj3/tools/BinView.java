package imj3.tools;

import static java.lang.Math.min;
import static multij.swing.SwingTools.getFiles;
import static multij.swing.SwingTools.scrollable;
import static multij.tools.Tools.array;
import static multij.tools.Tools.debugPrint;
import static multij.tools.Tools.unchecked;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.io.UncheckedIOException;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import multij.swing.SwingTools;
import multij.tools.CommandLineArgumentsParser;

/**
 * @author codistmonk (creation 2015-04-04)
 */
public final class BinView extends JPanel {
	
	private static final long serialVersionUID = -5316982568644236740L;
	
	private final JTable table;
	
	public BinView(final ByteSource source, final int classBytes, final int itemWidth, final int itemHeight, final int itemChannelCount) {
		super(new BorderLayout());
		final int itemSize = classBytes + itemWidth * itemHeight * itemChannelCount;
		final int itemCount = (int) (source.getLength() / itemSize);
		
		debugPrint("itemCount:", itemCount);
		
		final int pageSize = min(itemCount, 600_000);
		final DefaultTableModel model = new DefaultTableModel(array("#", "class", "datum"), pageSize) {
			
			@Override
			public final Class<?> getColumnClass(final int columnIndex) {
				return 2 == columnIndex ? DUMMY.getClass() : super.getColumnClass(columnIndex);
			}
			
			private static final long serialVersionUID = 44752070418830499L;
			
		};
		this.table = new JTable(model);
		
		final byte[] item = new byte[itemSize];
		
		for (long i = 0L; i < pageSize * itemSize; i += itemSize) {
			final int itemId = (int) (i / itemSize);
			
			model.setValueAt(itemId, itemId, 0);
			
			source.get(i, item);
			
			StringBuilder labels = new StringBuilder();
			
			for (int j = 0; j < classBytes; ++j) {
				if (0 < j) {
					labels.append(' ');
				}
				
				labels.append(0xFF & item[j]);
			}
			
			model.setValueAt(labels, itemId, 1);
			model.setValueAt(DUMMY, itemId, 2);
		}
		
		SwingTools.setCheckAWT(false);
		
		try {
			// TODO add page selector
			this.add(scrollable(this.table), BorderLayout.CENTER);
		} finally {
			SwingTools.setCheckAWT(true);
		}
		
		this.table.setDefaultRenderer(DUMMY.getClass(), new TableCellRenderer() {
			
			private final BufferedImage image = new BufferedImage(itemWidth, itemHeight, BufferedImage.TYPE_3BYTE_BGR);
			
			private final JLabel label = new JLabel(new ImageIcon(this.image));
			
			@Override
			public final Component getTableCellRendererComponent(final JTable table, final Object value,
					final boolean isSelected, final boolean hasFocus, final int row, final int column) {
				final int itemIndex = (int) table.getValueAt(row, 0);
				
				source.get((long) itemSize * itemIndex, item);
				
				drawItem(item, classBytes, itemWidth, itemHeight, itemChannelCount, this.image);
				
				return this.label;
			}
			
		});
		
		this.table.setRowHeight(itemHeight + 2);
		this.table.setAutoCreateRowSorter(true);
		
		this.table.setDropTarget(new DropTarget() {
			
			@Override
			public final synchronized void drop(final DropTargetDropEvent event) {
				for (final File file : getFiles(event)) {
					main("bin", file.getPath());
				}
			}
			
			private static final long serialVersionUID = -8699426509985335278L;
			
		});
	}
	
	static final byte[] DUMMY = new byte[0];
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String... commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final int classBytes = arguments.get1("classBytes", 1);
		final int itemWidth = arguments.get1("itemWidth", 32);
		final int itemHeight = arguments.get1("itemWidth", itemWidth);
		final int itemChannels = arguments.get1("itemChannels", 3);
		final String binPath = arguments.get("bin", "");
		
		SwingTools.show(new BinView(new FileByteSource(binPath), classBytes, itemWidth, itemHeight, itemChannels), binPath, false);
	}
	
	public static final void drawItem(final byte[] data, final int offset, final int itemWidth, final int itemHeight,
			final int itemChannelCount, final BufferedImage image) {
		for (int y = 0; y < itemHeight; ++y) {
			for (int x = 0; x < itemWidth; ++x) {
				int rgb = ~0;
				
				for (int i = 0; i < itemChannelCount; ++i) {
					rgb = (rgb << 8) | (data[offset + itemWidth * itemHeight * i + y * itemWidth + x] & 0xFF);
				}
				
				image.setRGB(x, y, rgb);
			}
		}
	}
	
	/**
	 * @author codistmonk (creation 2016-06-20)
	 */
	public static abstract interface ByteSource extends Serializable {
		
		public abstract long getLength();
		
		public abstract byte[] get(long offset, byte[] result);
		
	}
	
	/**
	 * @author codistmonk (creation 2016-06-20)
	 */
	public static final class FileByteSource implements ByteSource, AutoCloseable {
		
		private final RandomAccessFile file;
		
		public FileByteSource(final String path) {
			this(newFile(path, "r"));
		}
		
		private FileByteSource(final RandomAccessFile file) {
			this.file = file;
		}
		
		@Override
		public final long getLength() {
			try {
				return this.file.length();
			} catch (final IOException exception) {
				throw new UncheckedIOException(exception);
			}
		}
		
		@Override
		public final byte[] get(final long offset, final byte[] result) {
			try {
				this.file.seek(offset);
				this.file.read(result);
			} catch (final IOException exception) {
				throw new UncheckedIOException(exception);
			}
			
			return result;
		}
		
		public final void destroy() {
			try {
				this.close();
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		}
		
		@Override
		public final void close() throws Exception {
			this.file.close();
		}
		
		private static final long serialVersionUID = 8721336663898473017L;
		
		public static final RandomAccessFile newFile(final String path, final String mode) {
			try {
				return new RandomAccessFile(path, mode);
			} catch (final FileNotFoundException exception) {
				throw new UncheckedIOException(exception);
			}
		}
		
		public static final FileInputStream newFile(final String path) {
			try {
				return new FileInputStream(path);
			} catch (final FileNotFoundException exception) {
				throw new UncheckedIOException(exception);
			}
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2016-06-20)
	 */
	public static final class ArrayByteSource implements ByteSource {
		
		private final byte[] data;
		
		public ArrayByteSource(final byte... data) {
			this.data = data;
		}
		
		@Override
		public final long getLength() {
			return this.data.length;
		}
		
		@Override
		public final byte[] get(final long offset, final byte[] result) {
			System.arraycopy(this.data, (int) offset, result, 0, result.length);
			
			return result;
		}
		
		private static final long serialVersionUID = -9211129354036079585L;
		
	}
	
}