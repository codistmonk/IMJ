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
import static net.sourceforge.aprog.swing.SwingTools.scrollable;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.aprog.af.AbstractAFAction;
import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.swing.SwingTools;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class PipelineViewFilter extends ViewFilter {
	
	private final JList filters;
	
	private final JPanel cards;
	
	public PipelineViewFilter(final Context context) {
		super(context);
		this.filters = new JList(new DefaultListModel());
		this.cards = new JPanel(new CardLayout());
		
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
		
		this.filters.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public final void valueChanged(final ListSelectionEvent event) {
				PipelineViewFilter.this.filterSelected();
			}
			
		});
	}
	
	@Override
	public final void doInitialize() {
		this.sourceImageChanged();
		
		final ListModel model = this.filters.getModel();
		final int n = model.getSize();
		
		for (int i = 0; i < n; ++i) {
			((ViewFilter) model.getElementAt(i)).initialize();
		}
	}
	
	@Override
	protected final void invalidateCache() {
		super.invalidateCache();
		
		final ListModel model = this.filters.getModel();
		final int n = model.getSize();
		
		for (int i = 0; i < n; ++i) {
			((ViewFilter) model.getElementAt(i)).invalidateCache();
		}
	}
	
	@Override
	protected final void sourceImageChanged() {
		final ListModel model = this.filters.getModel();
		final int n = model.getSize();
		
		if (0 < n) {
			if (this.getSource() != null) {
				((ViewFilter) model.getElementAt(0)).setSource(this.getSource());
			} else {
				((ViewFilter) model.getElementAt(0)).setSourceImage(this.getImage().getSource());
			}
			
			for (int i = 1; i < n; ++i) {
				((ViewFilter) model.getElementAt(i)).setSource(((ViewFilter) model.getElementAt(i - 1)));
			}
		}
	}
	
	@Override
	protected final Component newInputPanel(final ActionListener previewAction, final Map<String, JTextField> textFields) {
		final ViewFilter[] viewFilters = this.getContext().get("viewFilters");
		final JTabbedPane tabs = new JTabbedPane();
		final JSplitPane split = horizontalSplit(scrollable(this.filters), tabs);
		final JPanel result = new JPanel(new BorderLayout());
		final JList filterSelector = new JList(viewFilters);
		
		tabs.add("Filters", scrollable(filterSelector));
		tabs.add("Parameters", scrollable(this.cards));
		
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
						PipelineViewFilter.this.addFilter(filter, previewAction);
					}
				}
			}
			
		});
		
		return result;
	}
	
	@Override
	protected final ComplexFilter newComplexFilter() {
		final ListModel listModel = this.filters.getModel();
		
		return new ComplexFilter(false, true) {
			
			@Override
			public final int getNewValue(final int index, final int oldValue, final Channel channel) {
				final int n = listModel.getSize();
				
				return 0 < n ? ((ViewFilter) listModel.getElementAt(n - 1)).getImage().getValue(index) : oldValue;
			}
			
		};
	}
	
	final void addFilter(final ViewFilter prototype, final ActionListener previewAction) {
		try {
			final ViewFilter filter = prototype.getClass().getConstructor(Context.class).newInstance(this.getContext());
			final Map<String, JTextField> textFields = new HashMap<String, JTextField>();
			
			((DefaultListModel) this.filters.getModel()).addElement(filter);
			
			this.cards.add(filter.newInputPanel(new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					filter.retrieveParameters(textFields);
					previewAction.actionPerformed(event);
				}
				
			}, textFields), "" + filter.getId());
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
	
	final void filterSelected() {
		final ViewFilter selectedFilter = this.getSelectedFilter();
		
		if (selectedFilter != null) {
			final CardLayout layout = (CardLayout) this.cards.getLayout();
			
			layout.show(this.cards, "" + selectedFilter.getId());
		}
	}
	
}
