package imj.apps;

import static imj.IMJTools.blue;
import static imj.IMJTools.green;
import static imj.IMJTools.red;
import static java.lang.Integer.parseInt;
import static java.util.Collections.synchronizedList;
import static net.sourceforge.aprog.af.AFTools.item;
import static net.sourceforge.aprog.af.AFTools.newAboutItem;
import static net.sourceforge.aprog.af.AFTools.newPreferencesItem;
import static net.sourceforge.aprog.af.AFTools.newQuitItem;
import static net.sourceforge.aprog.i18n.Messages.setMessagesBase;
import static net.sourceforge.aprog.swing.SwingTools.checkAWT;
import static net.sourceforge.aprog.swing.SwingTools.menuBar;
import static net.sourceforge.aprog.swing.SwingTools.packAndCenter;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.swing.SwingTools.useSystemLookAndFeel;
import static net.sourceforge.aprog.swing.SwingTools.I18N.menu;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.getThisPackagePath;
import imj.Image;
import imj.ImageWrangler;
import imj.apps.modules.BigImageComponent;
import imj.apps.modules.FeatureViewFilter;
import imj.apps.modules.HistogramsPanel;
import imj.apps.modules.LinearViewFilter;
import imj.apps.modules.RankViewFilter;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.RoundingViewFilter;
import imj.apps.modules.Sieve;
import imj.apps.modules.SimpleSieve;
import imj.apps.modules.StatisticsViewFilter;
import imj.apps.modules.ViewFilter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.sourceforge.aprog.af.AFConstants;
import net.sourceforge.aprog.af.AFMainFrame;
import net.sourceforge.aprog.af.AFTools;
import net.sourceforge.aprog.af.AbstractAFAction;
import net.sourceforge.aprog.af.MacOSXTools;
import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.events.Variable;
import net.sourceforge.aprog.events.Variable.Listener;
import net.sourceforge.aprog.events.Variable.ValueChangedEvent;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

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
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_TOGGLE_HISTOGRAMS = "actions.toggleHistograms";
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_SET_VIEW_FILTER = "actions.setViewFilter";
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_APPLY_SIEVE = "actions.applySieve";
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_RESET_ROI = "actions.resetROI";
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_COPY_ROI_TO_LOD = "actions.copyROIToLOD";
	
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
			public final void perform() {
				final AFMainFrame mainFrame = result.get(AFConstants.Variables.MAIN_FRAME);
				
				mainFrame.dispose();
				
				while (!timers.isEmpty()) {
					timers.remove(0).stop();
				}
			}
			
		};
		
		new AbstractAFAction(result, ACTIONS_TOGGLE_HISTOGRAMS) {
			
			@Override
			public final void perform() {
				JDialog histogramsDialog = result.get("histogramsDialog");
				
				if (histogramsDialog == null) {
					final AFMainFrame mainFrame = result.get(AFConstants.Variables.MAIN_FRAME);
					
					histogramsDialog = new JDialog(mainFrame, "Histograms");
					
					histogramsDialog.add(scrollable(new HistogramsPanel(result)));
					
					result.set("histogramsDialog", histogramsDialog);
				}
				
				histogramsDialog.setVisible(!histogramsDialog.isVisible());
			}
			
		};
		
		new AbstractAFAction(result, ACTIONS_SET_VIEW_FILTER) {
			
			@Override
			public final void perform() {
				final ViewFilter[] filters = result.get("viewFilters");
				final JComboBox input = new JComboBox(filters);
				final int option = JOptionPane.showConfirmDialog(null, input, "Select a filter", JOptionPane.OK_CANCEL_OPTION);
				
				if (option != JOptionPane.OK_OPTION) {
					return;
				}
				
				final ViewFilter filter = (ViewFilter) input.getSelectedItem();
				
				if (filter != null) {
//					final ViewFilter previousFilter = result.get("viewFilter");
					
//					if (!filter.configureAndApply()) {
//						result.set("viewFilter", previousFilter);
//						
//						return;
//					}
					filter.configureAndApply();
				} else {
					result.set("viewFilter", null);
				}
			}
			
		};
		
		new AbstractAFAction(result, ACTIONS_APPLY_SIEVE) {
			
			@Override
			public final void perform() {
				final Sieve[] sieves = result.get("sieves");
				final JComboBox input = new JComboBox(sieves);
				final int option = JOptionPane.showConfirmDialog(null, input, "Select a sieve", JOptionPane.OK_CANCEL_OPTION);
				
				if (option != JOptionPane.OK_OPTION) {
					return;
				}
				
				final Sieve sieve = (Sieve) input.getSelectedItem();
//				final RegionOfInterest roi = sieve.getROI();
//				final RegionOfInterest backup = new RegionOfInterest(roi.getRowCount(), roi.getColumnCount());
//				final Sieve previousSieve = result.get("sieve");
				
//				roi.copyTo(backup);
				
//				if (!sieve.configureAndApply()) {
//					backup.copyTo(roi);
//					result.set("sieve", previousSieve);
					
//					return;
//				}
				
				sieve.configureAndApply();
			}
			
		};
		
		new AbstractAFAction(result, ACTIONS_RESET_ROI) {
			
			@Override
			public final void perform() {
				final RegionOfInterest[] rois = result.get("rois");
				final int lod = result.get("lod");
				final RegionOfInterest roi = lod < rois.length ? rois[lod] : null;
				
				if (roi != null) {
					roi.reset();
					
					final BigImageComponent imageView = result.get("imageView");
					
					imageView.repaintAll();
				}
				
			}
			
		};
		
		new AbstractAFAction(result, ACTIONS_COPY_ROI_TO_LOD) {
			
			@Override
			public final void perform() {
				final String destinationLodAsString = JOptionPane.showInputDialog("LOD:");
				
				if (destinationLodAsString == null || destinationLodAsString.isEmpty()) {
					return;
				}
				
				final int sourceLod = result.get("lod");
				final int destinationLod = parseInt(destinationLodAsString);
				final RegionOfInterest[] rois = result.get("rois");
				
				rois[sourceLod].copyTo(rois[destinationLod]);
			}
			
		};
		
		result.set(AFConstants.Variables.MAIN_MENU_BAR, menuBar(
				menu("Application",
						newAboutItem(result),
						null,
						newPreferencesItem(result),
						null,
						newQuitItem(result)),
				menu("Tools",
						newHistogramsItem(result)),
				menu("View",
						newSetViewFilterItem(result)),
				menu("ROIs",
						newApplySieveItem(result),
						newResetROIItem(result),
						newCopyROIItem(result))
		));
		
		result.set("image", null, Image.class);
		result.set("lod", null, Integer.class);
		result.set("xy", null, Point.class);
		result.set("rgb", null, String.class);
		result.set("hsb", null, String.class);
		
		result.set("viewFilters", array(null, new RoundingViewFilter(result), new FeatureViewFilter(result),
				new StatisticsViewFilter(result), new LinearViewFilter(result), new RankViewFilter(result)), ViewFilter[].class);
		result.set("viewFilter", null, ViewFilter.class);
		result.set("sieves", array(new SimpleSieve(result)), Sieve[].class);
		result.set("sieve", null, Sieve.class);
		
		final Variable<Point> xyVariable = result.getVariable("xy");
		
		xyVariable.addListener(new Listener<Point>() {
			
			private final float[] hsbBuffer = new float[4];
			
			@Override
			public final void valueChanged(final ValueChangedEvent<Point, ?> event) {
				final Image image = result.get("image");
				
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
	
    public static final JMenuItem newHistogramsItem(final Context context) {
    	checkAWT();
    	
        return item("Histograms", context, ACTIONS_TOGGLE_HISTOGRAMS);
    }
    
    public static final JMenuItem newSetViewFilterItem(final Context context) {
    	checkAWT();
    	
    	return item("Set filter...", context, ACTIONS_SET_VIEW_FILTER);
    }
    
    public static final JMenuItem newApplySieveItem(final Context context) {
    	checkAWT();
    	
    	return item("Apply sieve...", context, ACTIONS_APPLY_SIEVE);
    }
    
    public static final JMenuItem newResetROIItem(final Context context) {
    	checkAWT();
    	
    	return item("Reset", context, ACTIONS_RESET_ROI);
    }
    
    public static final JMenuItem newCopyROIItem(final Context context) {
    	checkAWT();
    	
    	return item("Copy to LOD...", context, ACTIONS_COPY_ROI_TO_LOD);
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
		
		for (final String variableName : array("lod", "xy", "rgb", "hsb")) {
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
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		if (commandLineArguments.length != 2) {
			System.out.println("Arguments: file <imageId>");
			
			return;
		}
		
		MacOSXTools.setupUI(APPLICATION_NAME, APPLICATION_ICON_PATH);
		useSystemLookAndFeel();
		setMessagesBase(getThisPackagePath() + "modules/Show");
		
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imageId = arguments.get("file", "");
		
		ImageWrangler.INSTANCE.load(imageId);
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public final void run() {
				final Context context = newContext();
				final AFMainFrame frame = AFMainFrame.newMainFrame(context);
				
				frame.setPreferredSize(new Dimension(800, 600));
				frame.setTitle(new File(imageId).getName());
				
				frame.add(scrollable(centered(new BigImageComponent(context, imageId))), BorderLayout.CENTER);
				frame.add(newStatusBar(context), BorderLayout.SOUTH);
				
				packAndCenter(frame).setVisible(true);
			}
			
		});
	}
	
}
