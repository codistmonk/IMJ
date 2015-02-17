package imj3.draft.segmentation;

import static imj3.draft.segmentation.CommonTools.newInstanceOf;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import imj2.pixel3d.MouseHandler;

import imj3.draft.segmentation.CommonTools.Property;

import java.awt.Component;
import java.awt.Composite;
import java.awt.CompositeContext;
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
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-01-20)
 */
public final class CommonSwingTools {
	
	private CommonSwingTools() {
		throw new IllegalInstantiationException();
	}
	
	public static final void setModel(final JTree tree, final Object object, final String rootEdtiTitle) {
		final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		final DefaultTreeModel model = new DefaultTreeModel(root);
		final UIScaffold scaffold = new UIScaffold(object);
		
		tree.setModel(model);
		
		if (scaffold.getStringGetter() != null) {
			final TreePath path = new TreePath(model.getPathToRoot(root));
			
			model.valueForPathChanged(path, new UserObject(scaffold, rootEdtiTitle, tree, root, false));
		}
		
		for (final Map.Entry<String, Method> entry : scaffold.getNestedLists().entrySet()) {
			model.insertNodeInto(new DefaultMutableTreeNode(entry.getKey()), root, model.getChildCount(root));
		}
		
		new MouseHandler(null) {
			
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
					final Editable editable = path == null ? null : cast(Editable.class, ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject());
					
					if (editable != null) {
						final JPopupMenu popup = new JPopupMenu();
						
						popup.add(item("Edit...", e -> editable.edit()));
						
						if (editable.isRemovable()) {
							popup.add(item("Remove", e -> model.removeNodeFromParent((MutableTreeNode) path.getLastPathComponent())));
						}
						
						for (final Method inlineList : scaffold.getInlineLists()) {
							final InlineList annotation = inlineList.getAnnotation(InlineList.class);
							
							this.addListItem(popup, root, annotation.element(), annotation.elementClass());
						}
						
						{
							int i = -1;
							
							for (final Map.Entry<String, Method> entry : scaffold.getNestedLists().entrySet()) {
								final MutableTreeNode nestingNode = (MutableTreeNode) model.getChild(root, ++i);
								final NestedList annotation = entry.getValue().getAnnotation(NestedList.class);
								
								this.addListItem(popup, nestingNode, annotation.element(), annotation.elementClass());
							}
						}
						
						popup.show(tree, event.getX(), event.getY());
					}
				}
			}
			
			private final void addListItem(final JPopupMenu popup, final MutableTreeNode nestingNode, final String element, final Class<?> elementClass) {
				popup.add(item("Add " + element + "...", e -> {
					final UIScaffold newElementScaffold = new UIScaffold(newInstanceOf(elementClass));
					
					newElementScaffold.edit("New " + element, new Runnable() {
						
						@Override
						public final void run() {
							final DefaultMutableTreeNode newElementNode = new DefaultMutableTreeNode();
							
							newElementNode.setUserObject(new UserObject(newElementScaffold, element, tree, newElementNode, true));
							
							model.insertNodeInto(newElementNode, nestingNode, model.getChildCount(nestingNode));
						}
						
					});
				}));
			}
			
			private static final long serialVersionUID = -475200304537897055L;
			
		}.addTo(tree);
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
	public static final class InvertComposite implements Composite {
		
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
							buffer[i] ^= ~0;
						}
					}
					
					dstOut.setPixels(outBounds.x, outBounds.y, outBounds.width, outBounds.height, buffer);
				}
				
			};
		}
		
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
		
		public UIScaffold(final Object object) {
			this.object = object;
			this.stringGetter = new Method[1];
			this.propertyGetters = new LinkedHashMap<>();
			this.propertySetters = new LinkedHashMap<>();
			this.inlineLists = new ArrayList<>();
			this.nestedLists = new LinkedHashMap<>();
			
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
		
		private static final long serialVersionUID = -5160722477511458349L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-17)
	 */
	public static final class UserObject implements Editable {
		
		private final UIScaffold uiScaffold;
		
		private final String editTitle;
		
		private final Runnable afterEdit;
		
		private final boolean removable;
		
		public UserObject(final UIScaffold uiScaffold, final String editTitle, final JTree tree, final TreeNode node, final boolean removable) {
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
			this.removable = removable;
		}
		
		@Override
		public final boolean isRemovable() {
			return this.removable;
		}
		
		@Override
		public final void edit() {
			this.uiScaffold.edit(this.editTitle, this.afterEdit);
		}
		
		@Override
		public final String toString() {
			try {
				return (String) this.uiScaffold.getStringGetter().invoke(this.uiScaffold.getObject());
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
		
		public abstract boolean isRemovable();
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-16)
	 */
	@Retention(RetentionPolicy.RUNTIME)
	public static abstract @interface StringGetter {
		
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
	
}
