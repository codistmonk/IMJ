package imj3.draft.processing;

import static imj3.draft.segmentation.CommonSwingTools.item;
import static net.sourceforge.aprog.swing.SwingTools.horizontalSplit;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import imj2.pixel3d.MouseHandler;

import imj3.draft.segmentation.CommonSwingTools;
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
			
			setModel(this.tree, context.setSession(new Session()).getSession());
			
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
	
	public static final void setModel(final JTree tree, final Object object) {
		final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		final DefaultTreeModel model = new DefaultTreeModel(root);
		final Method[] stringGetter = { null };
		final Map<String, Method> propertyGetters = new LinkedHashMap<>();
		final Map<String, Method> propertySetters = new LinkedHashMap<>();
		final List<Method> inlineLists = new ArrayList<>();
		final Map<String, Method> nestedLists = new LinkedHashMap<>();
		
		tree.setModel(model);
		
		for (final Method method : object.getClass().getMethods()) {
			for (final Annotation annotation : method.getAnnotations()) {
				final StringGetter stringGetter0 = cast(StringGetter.class, annotation);
				final PropertyGetter propertyGetter = cast(PropertyGetter.class, annotation);
				final PropertySetter propertySetter = cast(PropertySetter.class, annotation);
				final InlineList inlineList = cast(InlineList.class, annotation);
				final NestedList nestedList = cast(NestedList.class, annotation);
				
				if (stringGetter0 != null) {
					stringGetter[0] = method;
				}
				
				if (propertyGetter != null) {
					propertyGetters.put(propertyGetter.value(), method);
				}
				
				if (propertySetter != null) {
					propertySetters.put(propertySetter.value(), method);
				}
				
				if (inlineList != null) {
					inlineLists.add(method);
				}
				
				if (nestedList != null) {
					nestedLists.put(nestedList.value(), method);
				}
			}
		}
		
		if (stringGetter[0] != null) {
			final TreePath path = new TreePath(model.getPathToRoot(root));
			
			model.valueForPathChanged(path, new EditableUserObject() {
				
				@Override
				public final String toString() {
					try {
						return (String) stringGetter[0].invoke(object);
					} catch (final Exception exception) {
						throw Tools.unchecked(exception);
					}
				}
				
				@Override
				public final void edit() {
					final List<Property> properties = new ArrayList<>();
					
					for (final Map.Entry<String, Method> entry : propertyGetters.entrySet()) {
						final Method setter = propertySetters.get(entry.getKey());
						
						if (setter != null) {
							properties.add(new Property(entry.getKey(), () -> {
								try {
									return entry.getValue().invoke(object);
								} catch (final Exception exception) {
									throw unchecked(exception);
								}
							}, string -> {
								try {
									return setter.invoke(object, string);
								} catch (final Exception exception) {
									throw unchecked(exception);
								}
							}));
						}
					}
					
					CommonSwingTools.showEditDialog("Session", new Runnable() {
						
						@Override
						public final void run() {
							model.valueForPathChanged(path, root.getUserObject());
							tree.getRootPane().validate();
						}
						
					}, properties.toArray(new Property[properties.size()]));
				}
				
				private static final long serialVersionUID = 3506822059086373426L;
				
			});
		}
		
		Tools.debugPrint(propertyGetters.keySet());
		Tools.debugPrint(propertySetters.keySet());
		
		for (final Map.Entry<String, Method> entry : nestedLists.entrySet()) {
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
					final EditableUserObject editable = path == null ? null : cast(EditableUserObject.class, ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject());
					
					if (editable != null) {
						final JPopupMenu popup = new JPopupMenu();
						
						popup.add(item("Edit...", e -> editable.edit()));
						
						for (final Method inlineList : inlineLists) {
							popup.add(item("Add " + "?" + "...", e -> {}));
						}
						
						for (final Map.Entry<String, Method> entry : nestedLists.entrySet()) {
							popup.add(item("Add " + "?" + "...", e -> {}));
						}
						
						popup.show(tree, event.getX(), event.getY());
					}
				}
			}
			
			private static final long serialVersionUID = -475200304537897055L;
			
		}.addTo(tree);
	}
	
	/**
	 * @author codistmonk (creation 2015-02-16)
	 */
	public static abstract interface EditableUserObject extends Serializable {
		
		public abstract void edit();
		
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
		
		@NestedList("classes")
		public final List<ClassDescription> getClassDescriptions() {
			return this.classDescriptions;
		}
		
		private static final long serialVersionUID = -4539259556658072410L;
		
		/**
		 * @author codistmonk (creation 2015-02-16)
		 */
		public static final class ClassDescription implements Serializable {
			
			private String name;
			
			private int label;
			
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
				return this.setLabel(Integer.parseInt(labelAsString.substring(1), 16));
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
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-16)
	 */
	@Retention(RetentionPolicy.RUNTIME)
	public static abstract @interface NestedList {
		
		public abstract String value();
		
	}
	
}
