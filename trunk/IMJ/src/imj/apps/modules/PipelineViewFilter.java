package imj.apps.modules;

import static imj.apps.modules.ShowActions.ACTIONS_DELETE_LIST_ITEM;
import static imj.apps.modules.ShowActions.ACTIONS_MOVE_LIST_ITEM_DOWN;
import static imj.apps.modules.ShowActions.ACTIONS_MOVE_LIST_ITEM_UP;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_BACK_SPACE;
import static java.awt.event.KeyEvent.VK_DELETE;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_KP_DOWN;
import static java.awt.event.KeyEvent.VK_KP_UP;
import static java.awt.event.KeyEvent.VK_UP;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static javax.swing.SwingUtilities.isLeftMouseButton;
import static javax.swing.SwingUtilities.isRightMouseButton;
import static net.sourceforge.aprog.af.AFTools.item;
import static net.sourceforge.aprog.i18n.Messages.translate;
import static net.sourceforge.aprog.swing.SwingTools.horizontalSplit;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import net.sourceforge.aprog.af.AbstractAFAction;
import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class PipelineViewFilter extends ViewFilter {
	
	private final JList filters;
	
	public PipelineViewFilter(final Context context) {
		super(context);
		this.filters = new JList(new DefaultListModel());
		
		this.getParameters().clear();
		
		this.filters.setPreferredSize(new Dimension(128, 128));
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
		
		this.filters.addKeyListener(new KeyAdapter() {
			
			@Override
			public final void keyPressed(final KeyEvent event) {
				
				final JList list = ShowActions.getList(event.getSource());
				final int index = list.getSelectedIndex();
				
				if (0 <= index) {
					final boolean altDown = (event.getModifiersEx() & ALT_DOWN_MASK) == ALT_DOWN_MASK;
					final AbstractAFAction action;
					
					switch (event.getKeyCode()) {
					case VK_UP:
					case VK_KP_UP:
						if (altDown) {
							action = context.get(ACTIONS_MOVE_LIST_ITEM_UP);
						} else {
							action = null;
						}
						break;
					case VK_DOWN:
					case VK_KP_DOWN:
						if (altDown) {
							action = context.get(ACTIONS_MOVE_LIST_ITEM_DOWN);
						} else {
							action = null;
						}
						break;
					case VK_BACK_SPACE:
					case VK_DELETE:
						action = context.get(ACTIONS_DELETE_LIST_ITEM);
						break;
					default:
						action = null;
						break;
					}
					
					if (action != null) {
						action.perform(event);
					}
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
