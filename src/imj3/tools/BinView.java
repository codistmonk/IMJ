package imj3.tools;

import static multij.swing.SwingTools.getFiles;
import static multij.swing.SwingTools.scrollable;
import static multij.tools.Tools.array;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import multij.swing.SwingTools;

/**
 * @author codistmonk (creation 2015-04-04)
 */
public final class BinView extends JPanel {
	
	private static final long serialVersionUID = -5316982568644236740L;
	
	private final JTable table;
	
	public BinView(final byte[] data, final int itemWidth, final int itemHeight, final int itemChannelCount) {
		super(new BorderLayout());
		final int rowSize = 1 + itemWidth * itemHeight * itemChannelCount;
		final DefaultTableModel model = new DefaultTableModel(array("#", "class", "datum"), data.length / rowSize) {
			
			@Override
			public final Class<?> getColumnClass(final int columnIndex) {
				return 2 == columnIndex ? DUMMY.getClass() : super.getColumnClass(columnIndex);
			}
			
			private static final long serialVersionUID = 44752070418830499L;
			
		};
		this.table = new JTable(model);
		
		for (int i = 0; i < data.length; i += rowSize) {
			model.setValueAt(i / rowSize, i / rowSize, 0);
			model.setValueAt(data[i] & 0xFF, i / rowSize, 1);
			model.setValueAt(DUMMY, i / rowSize, 2);
		}
		
		SwingTools.setCheckAWT(false);
		
		try {
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
				
				for (int y = 0; y < itemHeight; ++y) {
					for (int x = 0; x < itemWidth; ++x) {
						int rgb = ~0;
						
						for (int i = 0; i < itemChannelCount; ++i) {
							rgb = (rgb << 8) | (data[itemIndex * rowSize + 1 + itemWidth * itemHeight * i + y * itemWidth + x] & 0xFF);
						}
						
						this.image.setRGB(x, y, rgb);
					}
				}
				
				return this.label;
			}
			
		});
		
		this.table.setRowHeight(itemHeight + 2);
		this.table.setAutoCreateRowSorter(true);
		
		this.table.setDropTarget(new DropTarget() {
			
			@Override
			public final synchronized void drop(final DropTargetDropEvent event) {
				for (final File file : getFiles(event)) {
					main(file.getPath());
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
		final String path = commandLineArguments[0];
		
		SwingTools.show(new BinView(GroundTruth2Bin.read(path), 32, 32, 3), path, false);
	}
	
}