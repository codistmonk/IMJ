package imj.apps;

import static imj.IMJTools.blue;
import static imj.IMJTools.green;
import static imj.IMJTools.red;
import static imj.apps.modules.BigImageComponent.SOURCE_IMAGE;
import static imj.apps.modules.ClassPathProtocol.getClassesInPackage;
import static imj.apps.modules.ShowActions.ACTIONS_APPLY_MORPHOLOGICAL_OPERATION_TO_ROI;
import static imj.apps.modules.ShowActions.ACTIONS_APPLY_SIEVE;
import static imj.apps.modules.ShowActions.ACTIONS_COPY_ROI_TO_LOD;
import static imj.apps.modules.ShowActions.ACTIONS_CREATE_ANNOTATION_FROM_ROI;
import static imj.apps.modules.ShowActions.ACTIONS_EXPORT_ANNOTATIONS;
import static imj.apps.modules.ShowActions.ACTIONS_EXPORT_VIEW;
import static imj.apps.modules.ShowActions.ACTIONS_RESET_ROI;
import static imj.apps.modules.ShowActions.ACTIONS_SET_VIEW_FILTER;
import static imj.apps.modules.ShowActions.ACTIONS_TOGGLE_ANNOTATIONS;
import static imj.apps.modules.ShowActions.ACTIONS_TOGGLE_HISTOGRAM;
import static imj.apps.modules.ViewFilter.VIEW_FILTER;
import static java.lang.reflect.Modifier.isAbstract;
import static java.util.Collections.synchronizedList;
import static multij.af.AFTools.item;
import static multij.af.AFTools.newAboutItem;
import static multij.af.AFTools.newPreferencesItem;
import static multij.af.AFTools.newQuitItem;
import static multij.af.AFTools.setupSystemLookAndFeel;
import static multij.i18n.Messages.setMessagesBase;
import static multij.swing.SwingTools.menuBar;
import static multij.swing.SwingTools.packAndCenter;
import static multij.swing.SwingTools.scrollable;
import static multij.swing.SwingTools.I18N.menu;
import static multij.tools.Tools.array;
import static multij.tools.Tools.baseName;
import static multij.tools.Tools.cast;
import static multij.tools.Tools.debugPrint;
import static multij.tools.Tools.getThisPackagePath;

import imj.Image;
import imj.ImageWrangler;
import imj.apps.modules.Annotations;
import imj.apps.modules.BigImageComponent;
import imj.apps.modules.Plugin;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.ShowActions;
import imj.apps.modules.Sieve;
import imj.apps.modules.ViewFilter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import multij.af.AFConstants;
import multij.af.AFMainFrame;
import multij.af.AFTools;
import multij.af.AbstractAFAction;
import multij.context.Context;
import multij.events.Variable;
import multij.events.Variable.Listener;
import multij.events.Variable.ValueChangedEvent;
import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-02-13)
 */
public final class Show {
	
	private Show() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * {@value}.
	 */
	public static final String APPLICATION_NAME = "IMJ Show";
	
	/**
	 * {@value}.
	 */
	public static final String APPLICATION_VERSION = "0.1.0";
	
	/**
	 * {@value}.
	 */
	public static final String APPLICATION_COPYRIGHT = "(c) 2013 Codist Monk";
	
	/**
	 * {@value}.
	 */
	public static final String APPLICATION_ICON_PATH = "imj/apps/thumbnail.png";
	
	public static final Context newContext() {
		final Context result = AFTools.newContext();
		
		result.set(AFConstants.Variables.APPLICATION_NAME, APPLICATION_NAME);
		result.set(AFConstants.Variables.APPLICATION_VERSION, APPLICATION_VERSION);
		result.set(AFConstants.Variables.APPLICATION_COPYRIGHT, APPLICATION_COPYRIGHT);
		result.set(AFConstants.Variables.APPLICATION_ICON_PATH, APPLICATION_ICON_PATH);
		
		final List<Timer> timers = synchronizedList(new LinkedList<Timer>());
		
		result.set("timers", timers);
		
		new AbstractAFAction(result, AFConstants.Variables.ACTIONS_QUIT) {
			
			@Override
			public final void perform(final Object object) {
				final AFMainFrame mainFrame = result.get(AFConstants.Variables.MAIN_FRAME);
				
				mainFrame.dispose();
				
				while (!timers.isEmpty()) {
					timers.remove(0).stop();
				}
			}
			
		};
		
		ShowActions.loadInto(result);
		
		result.set(AFConstants.Variables.MAIN_MENU_BAR, menuBar(
				menu("Application",
						newAboutItem(result),
						null,
						newPreferencesItem(result),
						null,
						newQuitItem(result)),
				menu("File",
						newExportViewItem(result),
						newExportAnnotationsItem(result)),
				menu("Tools",
						newHistogramItem(result),
						newAnnotationsItem(result)),
				menu("View",
						newSetViewFilterItem(result)),
				menu("ROIs",
						newApplySieveItem(result),
						newApplyMorphologicalOperationToROIItem(result),
						null,
						newResetROIItem(result),
						newCopyROIItem(result),
						null,
						newCreateAnnotationFromROIItem(result))
		));
		
		result.set(SOURCE_IMAGE, null, Image.class);
		result.set("lod", null, Integer.class);
		result.set("scale", null, Integer.class);
		result.set("autoAdjustScale", null, Boolean.class);
		result.set("xy", null, Point.class);
		result.set("rgb", null, String.class);
		result.set("hsb", null, String.class);
		
		result.set("viewFilters", loadPlugins(result, ViewFilter.class), ViewFilter[].class);
		result.set(VIEW_FILTER, null, ViewFilter.class);
		result.set("sieves", loadPlugins(result, Sieve.class), Sieve[].class);
		result.set("sieve", null, Sieve.class);
		
		final Variable<Point> xyVariable = result.getVariable("xy");
		
		xyVariable.addListener(new Listener<Point>() {
			
			private final float[] hsbBuffer = new float[4];
			
			@Override
			public final void valueChanged(final ValueChangedEvent<Point, ?> event) {
				final Image image = ViewFilter.getCurrentImage(result);
				
				if (image != null) {
					final Point xy = event.getNewValue();
					final int rgb = image.getValue(xy.y, xy.x);
					final int red = red(rgb);
					final int green = green(rgb);
					final int blue = blue(rgb);
					
					Color.RGBtoHSB(red, green, blue, this.hsbBuffer);
					
					result.set("rgb", "(" + red + " " + green + " " + blue + ")");
					
					final int hue = (int) (this.hsbBuffer[0] * 255);
					final int saturation = (int) (this.hsbBuffer[1] * 255);
					final int brightness = (int) (this.hsbBuffer[2] * 255);
					
					result.set("hsb", "(" + hue + " " + saturation + " " + brightness + ")");
				}
			}
			
		});
		
		result.set("rois", new RegionOfInterest[0]);
		
		return result;
	}
	
	public static <T> T[] loadPlugins(final Context context, final Class<T> elementType) {
		final List<T> protoresult = new ArrayList<T>();
		
		protoresult.add(null);
		
		for (final Class<?> cls : getClassesInPackage(Plugin.class.getPackage().getName())) {
			if (!isAbstract(cls.getModifiers()) && elementType.isAssignableFrom(cls)) {
				try {
					protoresult.add((T) cls.getConstructor(Context.class).newInstance(context));
				} catch (final Exception exception) {
					debugPrint(cls);
					exception.printStackTrace();
				}
			}
		}
		
		return protoresult.toArray((T[]) Array.newInstance(elementType, protoresult.size()));
	}
	
    public static final JMenuItem newHistogramItem(final Context context) {
        return item("Histogram", context, ACTIONS_TOGGLE_HISTOGRAM);
    }
    
    public static final JMenuItem newExportAnnotationsItem(final Context context) {
    	return item("Export annotations...", context, ACTIONS_EXPORT_ANNOTATIONS);
    }
    
    public static final JMenuItem newExportViewItem(final Context context) {
    	return item("Export view...", context, ACTIONS_EXPORT_VIEW);
    }
    
    public static final JMenuItem newAnnotationsItem(final Context context) {
    	return item("Annotations", context, ACTIONS_TOGGLE_ANNOTATIONS);
    }
    
    public static final JMenuItem newSetViewFilterItem(final Context context) {
    	return item("Set filter...", context, ACTIONS_SET_VIEW_FILTER);
    }
    
    public static final JMenuItem newApplySieveItem(final Context context) {
    	return item("Apply sieve...", context, ACTIONS_APPLY_SIEVE);
    }
    
    public static final JMenuItem newApplyMorphologicalOperationToROIItem(final Context context) {
    	return item("Apply morphological operation...", context, ACTIONS_APPLY_MORPHOLOGICAL_OPERATION_TO_ROI);
    }
    
    public static final JMenuItem newResetROIItem(final Context context) {
    	return item("Reset", context, ACTIONS_RESET_ROI);
    }
    
    public static final JMenuItem newCopyROIItem(final Context context) {
    	return item("Copy to LOD...", context, ACTIONS_COPY_ROI_TO_LOD);
    }
    
    public static final JMenuItem newCreateAnnotationFromROIItem(final Context context) {
    	return item("Create annotation", context, ACTIONS_CREATE_ANNOTATION_FROM_ROI);
    }
	
	public static final String toStatusString(final Object object) {
		final Point point = cast(Point.class, object);
		
		if (point != null) {
			return "(" + point.x + " " + point.y + ")";
		}
		
		return "" + object;
	}
	
	public static final JPanel newStatusBar(final Context context) {
		final JPanel result = new JPanel();
		
		result.setLayout(new BoxLayout(result, BoxLayout.LINE_AXIS));
		
		for (final String variableName : array("lod", "scale", "autoAdjustScale", "xy", "rgb", "hsb")) {
			final JLabel label = new JLabel(toStatusString(context.get(variableName)));
			
			result.add(new JLabel(variableName.toUpperCase(Locale.ENGLISH) + ":"));
			result.add(label);
			result.add(Box.createHorizontalStrut(10));
			
			final Variable<Object> variable = context.getVariable(variableName);
			
			variable.addListener(new Listener<Object>() {
				
				@Override
				public final void valueChanged(final ValueChangedEvent<Object, ?> event) {
					label.setText(toStatusString(event.getNewValue()));
				}
				
			});
		}
		
		return result;
	}
	
	public static final JComponent centered(final Component component) {
		final JComponent result = new JPanel(new GridBagLayout());
		
		result.add(component);
		
		return result;
	}
	
	public static final AFMainFrame newMainFrame(final String imageId, final Context context) {
		final AFMainFrame result = AFMainFrame.newMainFrame(context);
		
		result.setPreferredSize(new Dimension(800, 600));
		result.setTitle(new File(imageId).getName());
		
		result.add(scrollable(centered(new BigImageComponent(context, imageId))), BorderLayout.CENTER);
		result.add(newStatusBar(context), BorderLayout.SOUTH);
		
		return result;
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		if (commandLineArguments.length != 2) {
			System.out.println("Arguments: file <imageId>");
			
			return;
		}
		
		setupSystemLookAndFeel(APPLICATION_NAME, APPLICATION_ICON_PATH);
		setMessagesBase(getThisPackagePath() + "modules/Show");
		
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imageId = arguments.get("file", "");
		
		ImageWrangler.INSTANCE.load(imageId);
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public final void run() {
				final Context context = newContext();
				
				context.set("imageId", imageId);
				context.set("annotations", Annotations.fromXML(arguments.get("annotations", baseName(imageId) + ".xml")));
				
				packAndCenter(newMainFrame(imageId, context)).setVisible(true);
			}
			
		});
	}
	
}
