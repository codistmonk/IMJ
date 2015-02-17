package imj3.draft.processing;

import static imj3.draft.segmentation.CommonSwingTools.setModel;
import static net.sourceforge.aprog.swing.SwingTools.horizontalSplit;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;

import imj3.draft.segmentation.CommonSwingTools.NestedList;
import imj3.draft.segmentation.CommonSwingTools.PropertyGetter;
import imj3.draft.segmentation.CommonSwingTools.PropertySetter;
import imj3.draft.segmentation.CommonSwingTools.StringGetter;
import imj3.draft.segmentation.ImageComponent;
import imj3.tools.AwtImage2D;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

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
	
}
