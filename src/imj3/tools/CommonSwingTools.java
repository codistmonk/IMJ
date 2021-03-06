package imj3.tools;

import static multij.swing.SwingTools.horizontalBox;
import static multij.tools.Tools.cast;
import static multij.tools.Tools.unchecked;
import imj3.tools.CommonTools.Property;

import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import multij.swing.MouseHandler;
import multij.swing.SwingTools;
import multij.tools.IllegalInstantiationException;
import multij.tools.Tools;

/**
 * @author codistmonk (creation 2015-01-20)
 */
public final class CommonSwingTools {
	
	private CommonSwingTools() {
		throw new IllegalInstantiationException();
	}
	
	public static final Color newRandomColor() {
		return new Color(0x60000000 | (int) (Math.random() * (1 << 24)), true);
	}
	
	public static final TreePath getPath(final DefaultTreeModel model, final TreeNode node) {
		return new TreePath(model.getPathToRoot(node));
	}
	
	public static final void setModel(final JTree tree, final Object object, final String rootEditTitle, final Instantiator instantiator) {
		final DefaultTreeModel model = new DefaultTreeModel(buildNode(tree, new UIScaffold(object), rootEditTitle));
		
		new MouseHandler() {
			
			@Override
			public final void mouseClicked(final MouseEvent event) {
				this.mouseUsed(event);
			}
			
			@Override
			public final void mousePressed(final MouseEvent event) {
				this.mouseUsed(event);
			}
			
			@Override
			public final void mouseReleased(final MouseEvent event) {
				this.mouseUsed(event);
			}
			
			private final void mouseUsed(final MouseEvent event) {
				if (event.isPopupTrigger()) {
					final TreePath path = tree.getPathForLocation(event.getX(), event.getY());
					final DefaultMutableTreeNode node = path == null ? null : (DefaultMutableTreeNode) path.getLastPathComponent();
					final UserObject userObject = node == null ? null : cast(UserObject.class, node.getUserObject());
					
					if (userObject != null) {
						final UIScaffold scaffold = userObject.getUIScaffold();
						final Object object = scaffold.getObject();
						final JPopupMenu popup = new JPopupMenu();
						
						popup.add(item("Edit...", e -> userObject.edit()));
						
						if (userObject.getContainer() != null) {
							popup.add(item("Remove", e -> {
								userObject.getContainer().remove(object);
								model.removeNodeFromParent((MutableTreeNode) path.getLastPathComponent());
							}));
						}
						
						for (final Method inlineList : scaffold.getInlineLists()) {
							final InlineList annotation = inlineList.getAnnotation(InlineList.class);
							
							try {
								@SuppressWarnings("unchecked")
								final List<Object> list = (List<Object>) inlineList.invoke(object);
								final String element = annotation.element();
								
								this.addListItem(popup, node, element, annotation.elementClass(), list);
							} catch (final Exception exception) {
								exception.printStackTrace();
							}
						}
						
						{
							int i = -1;
							
							for (final Map.Entry<String, Method> entry : scaffold.getNestedLists().entrySet()) {
								final MutableTreeNode nestingNode = (MutableTreeNode) model.getChild(node, ++i);
								final NestedList annotation = entry.getValue().getAnnotation(NestedList.class);
								
								try {
									@SuppressWarnings("unchecked")
									final List<Object> list = (List<Object>) entry.getValue().invoke(object);
									final String element = annotation.element();
									
									this.addListItem(popup, nestingNode, element, annotation.elementClass(), list);
								} catch (final Exception exception) {
									Tools.debugPrint(object);
									Tools.debugPrint(entry.getValue());
									exception.printStackTrace();
								}
							}
						}
						
						popup.show(tree, event.getX(), event.getY());
					}
				}
			}
			
			private final void addListItem(final JPopupMenu popup, final MutableTreeNode nestingNode, final String element, final Class<?> elementClass, final List<Object> list) {
				popup.add(item("Add " + element + "...", e -> {
					final Object newElement = instantiator.newInstanceOf(elementClass);
					
					if (newElement != null) {
						final UIScaffold newElementScaffold = new UIScaffold(newElement);
						
						newElementScaffold.edit("New " + element, new Runnable() {
							
							@Override
							public final void run() {
								final DefaultMutableTreeNode newNode = addNode(tree, newElementScaffold, nestingNode, element, list);
								list.add(newElementScaffold.getObject());
								tree.setSelectionPath(getPath(model, newNode));
							}
							
						});
					}
				}));
			}
			
			private static final long serialVersionUID = -475200304537897055L;
			
		}.addTo(tree);
		
		tree.setModel(model);
	}
	
	public static final DefaultMutableTreeNode buildNode(final JTree tree, final UIScaffold scaffold, final String editTitle) {
		final Object object = scaffold.getObject();
		final DefaultMutableTreeNode result = new DefaultMutableTreeNode(scaffold);
		final DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		
		{
			model.valueForPathChanged(new TreePath(model.getPathToRoot(result)),
					new UserObject(scaffold, editTitle, tree, result, null));
			
			for (final Method method : scaffold.getInlineLists()) {
				try {
					@SuppressWarnings("unchecked")
					final List<Object> list = (List<Object>) method.invoke(object);
					
					for (final Object o : list) {
						addNode(tree, new UIScaffold(o), result, method.getAnnotation(InlineList.class).element(), list);
					}
				} catch (final Exception exception) {
					exception.printStackTrace();
				}
			}
			
			for (final Map.Entry<String, Method> entry : scaffold.getNestedLists().entrySet()) {
				final DefaultMutableTreeNode nestingNode = new DefaultMutableTreeNode(entry.getKey());
				model.insertNodeInto(nestingNode, result, model.getChildCount(result));
				
				try {
					final Method method = entry.getValue();
					@SuppressWarnings("unchecked")
					final List<Object> list = (List<Object>) method.invoke(object);
					
					for (final Object o : list) {
						addNode(tree, new UIScaffold(o), nestingNode, method.getAnnotation(NestedList.class).element(), list);
					}
				} catch (final Exception exception) {
					exception.printStackTrace();
				}
			}
		}
		
		return result;
	}
	
	static final DefaultMutableTreeNode addNode(final JTree tree, final UIScaffold scaffold, final MutableTreeNode parent, final String element, final Collection<?> container) {
		final DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		final DefaultMutableTreeNode result = buildNode(tree, scaffold, "Edit " + element);
		
		result.setUserObject(new UserObject(scaffold, element, tree, result, container));
		
		model.insertNodeInto(result, parent, model.getChildCount(parent));
		
		return result;
	}
	
	public static final void showEditDialog(final String title, final Runnable ifOkClicked, final Property... properties) {
		final JDialog dialog = new JDialog((Window) null, title);
		final Box box = Box.createVerticalBox();
		final Map<Property, Supplier<String>> newValues = new IdentityHashMap<>();
		
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		for (final Property property : properties) {
			final JTextField field = new JTextField("" + property.getGetter().get());
			
			box.add(horizontalBox(new JLabel(property.getName()), field));
			
			newValues.put(property, field::getText);
		}
		
		box.add(horizontalBox(
				Box.createHorizontalGlue(),
				new JButton(new AbstractAction("Ok") {
					
					@Override
					public final void actionPerformed(final ActionEvent event) {
						for (final Property property : properties) {
							property.getParser().apply(newValues.get(property).get());
						}
						
						dialog.dispose();
						
						ifOkClicked.run();
					}
					
					private static final long serialVersionUID = -1250254465599248142L;
				
				}),
				new JButton(new AbstractAction("Cancel") {
					
					@Override
					public final void actionPerformed(final ActionEvent event) {
						dialog.dispose();
					}
					
					private static final long serialVersionUID = -1250254465599248142L;
					
				})
		));
		
		dialog.add(box);
		
		SwingTools.packAndCenter(dialog).setVisible(true);
	}
	
	public static final JPanel center(final Component component) {
		final JPanel result = new JPanel(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		
		c.anchor = GridBagConstraints.CENTER;
		
		result.add(component, c);
		
		return result;
	}
	
	public static final <C extends Component> C limitHeight(final C component) {
		component.setMaximumSize(new Dimension(component.getMaximumSize().width, component.getPreferredSize().height));
		
		return component;
	}
	
	public static final JMenuItem item(final String text, final ActionListener action) {
		final JMenuItem result = new JMenuItem(text);
		
		result.addActionListener(action);
		
		return result;
	}
	
	public static final void repeat(final Window window, final int delay, final ActionListener action) {
		final Timer timer = new Timer(delay, action) {
			
			@Override
			public final void stop() {
				try {
					action.actionPerformed(null);
				} finally {
					super.stop();
				}
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 4028342893634389147L;
			
		};
		
		timer.setRepeats(true);
		timer.start();
		
		window.addWindowListener(new WindowAdapter() {
			
			@Override
			public final void windowClosing(final WindowEvent event) {
				timer.stop();
			}
			
		});
	}
	
	/**
	 * @author codistmonk (creation 2015-01-19)
	 */
	public static final class HighlightComposite implements Composite {
		
		@Override
		public final CompositeContext createContext(final ColorModel srcColorModel,
				final ColorModel dstColorModel, final RenderingHints hints) {
			return new CompositeContext() {
				
				@Override
				public final void dispose() {
					// NOP
				}
				
				@Override
				public final void compose(final Raster src, final Raster dstIn, final WritableRaster dstOut) {
					final Rectangle inBounds = dstIn.getBounds();
					final Rectangle outBounds = dstOut.getBounds();
					final int[] buffer = dstIn.getPixels(inBounds.x, inBounds.y, inBounds.width, inBounds.height, (int[]) null);
					int n = buffer.length;
					
					for (int i = 0; i < n; ++i) {
						if (((i + 1) % 4) != 0) {
							buffer[i] ^= 0x808080;
						}
					}
					
					dstOut.setPixels(outBounds.x, outBounds.y, outBounds.width, outBounds.height, buffer);
				}
				
			};
		}
		
		public static final HighlightComposite INSTANCE = new HighlightComposite();
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-16)
	 */
	public static final class UIScaffold implements Serializable {
		
		private final Object object;
		
		private final Method[] stringGetter;
		
		private final Map<String, Method> propertyGetters;
		
		private final Map<String, Method> propertySetters;
		
		private final List<Method> inlineLists;
		
		private final Map<String, Method> nestedLists;
		
		private final List<String> orderedProperties;
		
		public UIScaffold(final Object object) {
			this.object = object;
			this.stringGetter = new Method[1];
			final PropertyOrderComparator propertyOrderComparator = this.new PropertyOrderComparator();
			this.propertyGetters = new TreeMap<>(propertyOrderComparator);
			this.propertySetters = new TreeMap<>(propertyOrderComparator);
			this.inlineLists = new ArrayList<>();
			this.nestedLists = new TreeMap<>(propertyOrderComparator);
			this.orderedProperties = new ArrayList<>();
			
			{
				final Collection<String> tmp = new LinkedHashSet<>();
				
				collectOrderedProperties(object.getClass(), tmp);
				
				this.orderedProperties.addAll(tmp);
			}
			
			for (final Method method : object.getClass().getMethods()) {
				for (final Annotation annotation : method.getAnnotations()) {
					final StringGetter stringGetter0 = cast(StringGetter.class, annotation);
					final PropertyGetter propertyGetter = cast(PropertyGetter.class, annotation);
					final PropertySetter propertySetter = cast(PropertySetter.class, annotation);
					final InlineList inlineList = cast(InlineList.class, annotation);
					final NestedList nestedList = cast(NestedList.class, annotation);
					
					if (stringGetter0 != null) {
						this.stringGetter[0] = method;
					}
					
					if (propertyGetter != null) {
						this.propertyGetters.put(propertyGetter.value(), method);
					}
					
					if (propertySetter != null) {
						this.propertySetters.put(propertySetter.value(), method);
					}
					
					if (inlineList != null) {
						this.inlineLists.add(method);
					}
					
					if (nestedList != null) {
						this.nestedLists.put(nestedList.name(), method);
					}
				}
			}
		}
		
		public final Object getObject() {
			return this.object;
		}
		
		public final Method getStringGetter() {
			return this.stringGetter[0];
		}
		
		public final Map<String, Method> getPropertyGetters() {
			return this.propertyGetters;
		}
		
		public final Map<String, Method> getPropertySetters() {
			return this.propertySetters;
		}
		
		public final List<Method> getInlineLists() {
			return this.inlineLists;
		}
		
		public final Map<String, Method> getNestedLists() {
			return this.nestedLists;
		}
		
		public final void edit(final String title, final Runnable actionIfOk) {
			final List<Property> properties = new ArrayList<>();
			
			for (final Map.Entry<String, Method> entry : this.getPropertyGetters().entrySet()) {
				final Method setter = this.getPropertySetters().get(entry.getKey());
				
				if (setter != null) {
					properties.add(new Property(entry.getKey(), () -> {
						try {
							return entry.getValue().invoke(this.getObject());
						} catch (final Exception exception) {
							throw unchecked(exception);
						}
					}, string -> {
						try {
							return setter.invoke(this.getObject(), string);
						} catch (final Exception exception) {
							throw unchecked(exception);
						}
					}));
				}
			}
			
			showEditDialog(title, actionIfOk, properties.toArray(new Property[properties.size()]));
		}
		
		final List<String> getOrderedProperties() {
			return this.orderedProperties;
		}
		
		/**
		 * @author codistmonk (creation 2015-02-27)
		 */
		final class PropertyOrderComparator implements Comparator<String> {
			
			@Override
			public final int compare(final String property1, final String property2) {
				final List<String> orderedProperties = UIScaffold.this.getOrderedProperties();
				
				return Integer.compare(orderedProperties.indexOf(property1), orderedProperties.indexOf(property2));
			}
			
		}
		
		private static final long serialVersionUID = -5160722477511458349L;
		
		public static final void collectOrderedProperties(final Class<?> cls, final Collection<String> properties) {
			if (cls.getSuperclass() != null) {
				collectOrderedProperties(cls.getSuperclass(), properties);
			}
			
			for (final Class<?> superInterface : cls.getInterfaces()) {
				collectOrderedProperties(superInterface, properties);
			}
			
			{
				final PropertyOrdering ordering = cls.getDeclaredAnnotation(PropertyOrdering.class);
				
				if (ordering != null) {
					properties.addAll(Arrays.asList(ordering.value()));
				}
			}
			
			for (final Method method : cls.getDeclaredMethods()) {
				final PropertyGetter getter = method.getDeclaredAnnotation(PropertyGetter.class);
				
				if (getter != null) {
					properties.add(getter.value());
				}
				
				final NestedList list = method.getDeclaredAnnotation(NestedList.class);
				
				if (list != null) {
					properties.add(list.name());
				}
			}
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-17)
	 */
	public static final class UserObject implements Editable {
		
		private final UIScaffold uiScaffold;
		
		private final String editTitle;
		
		private final Runnable afterEdit;
		
		private final Collection<?> container;
		
		public UserObject(final UIScaffold uiScaffold, final String editTitle, final JTree tree, final TreeNode node, final Collection<?> container) {
			this.uiScaffold = uiScaffold;
			this.editTitle = editTitle;
			final DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
			final TreePath path = new TreePath(model.getPathToRoot(node));
			this.afterEdit = new Runnable() {
				
				@Override
				public final void run() {
					model.valueForPathChanged(path, UserObject.this);
					tree.getRootPane().validate();
				}
				
			};
			this.container = container;
		}
		
		public final UIScaffold getUIScaffold() {
			return this.uiScaffold;
		}
		
		public final String getEditTitle() {
			return this.editTitle;
		}
		
		public final Runnable getAfterEdit() {
			return this.afterEdit;
		}
		
		@Override
		public final Collection<?> getContainer() {
			return this.container;
		}
		
		@Override
		public final void edit() {
			this.getUIScaffold().edit(this.getEditTitle(), this.getAfterEdit());
		}
		
		@Override
		public final String toString() {
			try {
				final UIScaffold scaffold = this.getUIScaffold();
				final Method stringGetter = scaffold.getStringGetter();
				final Object object = scaffold.getObject();
				
				return stringGetter != null ? (String) stringGetter.invoke(object) : object.toString();
			} catch (final Exception exception) {
				throw Tools.unchecked(exception);
			}
		}
		
		private static final long serialVersionUID = 7406734693107943367L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-16)
	 */
	public static abstract interface Editable extends Serializable {
		
		public abstract void edit();
		
		public abstract Collection<?> getContainer();
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-27)
	 */
	@Retention(RetentionPolicy.RUNTIME)
	public static abstract @interface PropertyOrdering {
		
		public abstract String[] value();
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-16)
	 */
	@Retention(RetentionPolicy.RUNTIME)
	public static abstract @interface StringGetter {
		//
	}
	
	/**
	 * @author codistmonk (creation 2015-02-16)
	 */
	@Retention(RetentionPolicy.RUNTIME)
	public static abstract @interface PropertyGetter {
		
		public abstract String value();
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-16)
	 */
	@Retention(RetentionPolicy.RUNTIME)
	public static abstract @interface PropertySetter {
		
		public abstract String value();
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-16)
	 */
	@Retention(RetentionPolicy.RUNTIME)
	public static abstract @interface InlineList {
		
		public abstract String element();
		
		public abstract Class<?> elementClass();
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-16)
	 */
	@Retention(RetentionPolicy.RUNTIME)
	public static abstract @interface NestedList {
		
		public abstract String name();
		
		public abstract String element();
		
		public abstract Class<?> elementClass();
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-21)
	 */
	public static abstract interface Instantiator extends Serializable {
		
		public abstract <T> T newInstanceOf(Class<T> cls);
		
		/**
		 * @author codistmonk (creation 2015-02-21)
		 */
		public static final class Default implements Instantiator {
			
			@Override
			public final <T> T newInstanceOf(final Class<T> cls) {
				return CommonTools.newInstanceOf(cls);
			}
			
			private static final long serialVersionUID = -2742127011021429060L;
			
		}
		
	}
	
}
