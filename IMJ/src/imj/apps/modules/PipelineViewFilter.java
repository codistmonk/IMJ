package imj.apps.modules;

import static imj.apps.modules.ShowActions.ACTIONS_DELETE_LIST_ITEM;
import static imj.apps.modules.ShowActions.ACTIONS_MOVE_LIST_ITEM_DOWN;
import static imj.apps.modules.ShowActions.ACTIONS_MOVE_LIST_ITEM_UP;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static javax.swing.SwingUtilities.getAncestorOfClass;
import static javax.swing.SwingUtilities.isLeftMouseButton;
import static javax.swing.SwingUtilities.isRightMouseButton;
import static net.sourceforge.aprog.af.AFTools.item;
import static net.sourceforge.aprog.i18n.Messages.translate;
import static net.sourceforge.aprog.swing.SwingTools.horizontalSplit;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import net.sourceforge.aprog.af.AFTools;
import net.sourceforge.aprog.af.AbstractAFAction;
import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.i18n.Messages;
import net.sourceforge.aprog.swing.AbstractCustomizableAction;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class PipelineViewFilter extends ViewFilter {
	
	private final JList filters;
	
	public PipelineViewFilter(final Context context) {
		super(context);
		this.filters = new JList(new DefaultListModel());
		
		this.getParameters().clear();
		
		this.filters.setSelectionMode(SINGLE_SELECTION);
		
		this.filters.addMouseListener(new MouseAdapter() {
			
			private final JPopupMenu popup;
			
			{
				this.popup = new JPopupMenu();
				
				this.popup.add(item("Move up", context, ACTIONS_MOVE_LIST_ITEM_UP));
				this.popup.add(item("Move down", context, ACTIONS_MOVE_LIST_ITEM_DOWN));
				this.popup.addSeparator();
				this.popup.add(item("Delete", context, ACTIONS_DELETE_LIST_ITEM));
			}
			
			@Override
			public final void mouseClicked(final MouseEvent event) {
				final ViewFilter selectedFilter = PipelineViewFilter.this.getSelectedFilter();
				
				if (isRightMouseButton(event) && selectedFilter != null) {
					this.popup.show(event.getComponent(), event.getX(), event.getY());
				}
			}
			
		});
	}
	
	@Override
	public final void initialize() {
		// TODO
	}
	
	@Override
	public final int getNewValue(final int index, final int oldValue, final Channel channel) {
		// TODO
		return oldValue;
	}
	
	@Override
	protected final boolean splitInputChannels() {
		return false;
	}
	
	@Override
	protected final Component newInputPanel(final ActionListener previewAction, final Map<String, JTextField> textFields) {
		final ViewFilter[] viewFilters = this.getContext().get("viewFilters");
		final JTabbedPane tabs = new JTabbedPane();
		final JSplitPane split = horizontalSplit(this.filters, tabs);
		final JPanel result = new JPanel(new BorderLayout());
		final JList filterSelector = new JList(viewFilters);
		final JPanel configurationsPanel = new JPanel(new CardLayout());
		
		tabs.add("Filters", filterSelector);
		tabs.add("Parameters", configurationsPanel);
		
		result.add(split, BorderLayout.CENTER);
		
		filterSelector.setSelectionMode(SINGLE_SELECTION);
		filterSelector.setToolTipText("Double-click to use filter");
		
		translate(filterSelector);
		
		filterSelector.addMouseListener(new MouseAdapter() {
			
			@Override
			public final void mouseClicked(final MouseEvent event) {
				if (isLeftMouseButton(event) && event.getClickCount() == 2) {
					final ViewFilter filter = (ViewFilter) filterSelector.getSelectedValue();
					
					if (filter != null) {
						PipelineViewFilter.this.addFilter(filter);
					}
				}
			}
			
		});
		
		//TODO setup configurationsPanel
		
		return result;
	}
	
	final void addFilter(final ViewFilter prototype) {
		try {
			((DefaultListModel) this.filters.getModel()).addElement(
					prototype.getClass().getConstructor(Context.class).newInstance(this.getContext()));
			
			this.filters.invalidate();
			this.filters.repaint();
		} catch (final Exception exception) {
			exception.printStackTrace();
		}
	}
	
	final int getSelectedFilterIndex() {
		return this.filters.getSelectedIndex();
	}
	
	final ViewFilter getSelectedFilter() {
		return (ViewFilter) this.filters.getSelectedValue();
	}
	
}
