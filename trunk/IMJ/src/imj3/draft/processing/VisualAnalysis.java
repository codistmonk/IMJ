package imj3.draft.processing;

import static imj3.draft.segmentation.CommonSwingTools.item;
import static imj3.draft.segmentation.CommonSwingTools.showEditDialog;
import static net.sourceforge.aprog.swing.SwingTools.horizontalSplit;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import imj2.pixel3d.MouseHandler;
import imj3.draft.segmentation.CommonTools.Property;
import imj3.draft.segmentation.ImageComponent;
import imj3.tools.AwtImage2D;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-02-13)
 */
public final class VisualAnalysis {
	
	private VisualAnalysis() {
		throw new IllegalInstantiationException();
	}
	
	static final Preferences preferences = Preferences.userNodeForPackage(VisualAnalysis.class);
	
	public static final String IMAGE_FILE_PATH = "image.file.path";
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		SwingTools.useSystemLookAndFeel();
		
		final Context context = new Context();
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public final void run() {
				SwingTools.show(new MainPanel(context), VisualAnalysis.class.getSimpleName(), false);
				
				context.setImageFile(new File(preferences.get(IMAGE_FILE_PATH, "")));
			}
			
		});
	}
	
	/**
	 * @author codistmonk (creation 2015-02-13)
	 */
	public static final class MainPanel extends JPanel {
		
		private final JTree tree;
		
		private final JSplitPane mainSplitPane;
		
		public MainPanel(final Context context) {
			super(new BorderLayout());
			
			this.tree = new JTree();
			this.mainSplitPane = horizontalSplit(scrollable(this.tree), scrollable(new JLabel("Drop file here")));
			
			setModel(this.tree, context.setSession(new Session()).getSession(), "Session");
			
			final JToolBar toolBar = new JToolBar();
			
			toolBar.add(new JLabel("TODO"));
			
			this.add(toolBar, BorderLayout.NORTH);
			this.add(this.mainSplitPane, BorderLayout.CENTER);
			
			this.mainSplitPane.getRightComponent().setDropTarget(new DropTarget() {
				
				@Override
				public final synchronized void drop(final DropTargetDropEvent event) {
					final File file = SwingTools.getFiles(event).get(0);
					
					context.setImageFile(file);
					
					preferences.put(IMAGE_FILE_PATH, file.getPath());
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 5442000733451223725L;
				
			});
			
			this.setPreferredSize(new Dimension(800, 600));
			
			context.setMainPanel(this);
		}
		
		public final void setContents(final Component component) {
			this.mainSplitPane.setRightComponent(scrollable(component));
		}
		
		private static final long serialVersionUID = 2173077945563031333L;
		
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
	
	public static final <T> T newInstanceOf(final Class<T> cls) {
		try {
			return cls.newInstance();
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
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
							
							this.addListItem(popup, tree, root, annotation.element(), annotation.elementClass());
						}
						
						{
							int i = -1;
							
							for (final Map.Entry<String, Method> entry : scaffold.getNestedLists().entrySet()) {
								final MutableTreeNode nestingNode = (MutableTreeNode) model.getChild(root, ++i);
								final NestedList annotation = entry.getValue().getAnnotation(NestedList.class);
								
								this.addListItem(popup, tree, nestingNode, annotation.element(), annotation.elementClass());
							}
						}
						
						popup.show(tree, event.getX(), event.getY());
					}
				}
			}
			
			final void addListItem(final JPopupMenu popup, final JTree tree, final MutableTreeNode nestingNode, final String element, final Class<?> elementClass) {
				final DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
				
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
	 * @author codistmonk (creation 2015-02-13)
	 */
	public static final class Context implements Serializable {
		
		private MainPanel mainPanel;
		
		private Session session;
		
		private File imageFile;
		
		public final MainPanel getMainPanel() {
			return this.mainPanel;
		}
		
		public final void setMainPanel(final MainPanel mainPanel) {
			this.mainPanel = mainPanel;
		}
		
		public final Session getSession() {
			return this.session;
		}
		
		public final Context setSession(final Session session) {
			this.session = session;
			
			return this;
		}
		
		public final File getImageFile() {
			return this.imageFile;
		}
		
		public final void setImageFile(final File imageFile) {
			if (imageFile.isFile()) {
				this.getMainPanel().setContents(new ImageComponent(AwtImage2D.awtRead(imageFile.getPath())));
				
				this.imageFile = imageFile;
			}
		}
		
		private static final long serialVersionUID = -2487965125442868238L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-16)
	 */
	public static final class Session implements Serializable {
		
		private String name = "session";
		
		private final List<ClassDescription> classDescriptions = new ArrayList<>();
		
		@StringGetter
		@PropertyGetter("name")
		public final String getName() {
			return this.name;
		}
		
		@PropertySetter("name")
		public final Session setName(final String name) {
			this.name = name;
			
			return this;
		}
		
		@NestedList(name="classes", element="class", elementClass=ClassDescription.class)
		public final List<ClassDescription> getClassDescriptions() {
			return this.classDescriptions;
		}
		
		private static final long serialVersionUID = -4539259556658072410L;
		
		/**
		 * @author codistmonk (creation 2015-02-16)
		 */
		public static final class ClassDescription implements Serializable {
			
			private String name = "class";
			
			private int label = 0xFF000000;
			
			@StringGetter
			@PropertyGetter("name")
			public final String getName() {
				return this.name;
			}
			
			@PropertySetter("name")
			public final ClassDescription setName(final String name) {
				this.name = name;
				
				return this;
			}
			
			public final int getLabel() {
				return this.label;
			}
			
			public final ClassDescription setLabel(final int label) {
				this.label = label;
				
				return this;
			}
			
			@PropertyGetter("label")
			public final String getLabelAsString() {
				return "#" + Integer.toHexString(this.getLabel()).toUpperCase(Locale.ENGLISH);
			}
			
			@PropertySetter("label")
			public final ClassDescription setLabel(final String labelAsString) {
				return this.setLabel((int) Long.parseLong(labelAsString.substring(1), 16));
			}
			
			private static final long serialVersionUID = 4974707407567297906L;
			
		}
		
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
