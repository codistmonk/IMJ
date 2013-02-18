package imj.apps;

import static imj.IMJTools.blue;
import static imj.IMJTools.green;
import static imj.IMJTools.red;
import static imj.IMJTools.rgba;
import static java.lang.Integer.parseInt;
import static java.lang.Integer.toBinaryString;
import static java.lang.Integer.toHexString;
import static java.lang.Math.min;
import static java.util.Collections.synchronizedList;
import static javax.swing.JOptionPane.CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.PLAIN_MESSAGE;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showOptionDialog;
import static net.sourceforge.aprog.af.AFTools.item;
import static net.sourceforge.aprog.af.AFTools.newAboutItem;
import static net.sourceforge.aprog.af.AFTools.newPreferencesItem;
import static net.sourceforge.aprog.af.AFTools.newQuitItem;
import static net.sourceforge.aprog.i18n.Messages.setMessagesBase;
import static net.sourceforge.aprog.swing.SwingTools.checkAWT;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;
import static net.sourceforge.aprog.swing.SwingTools.menuBar;
import static net.sourceforge.aprog.swing.SwingTools.packAndCenter;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.swing.SwingTools.useSystemLookAndFeel;
import static net.sourceforge.aprog.swing.SwingTools.I18N.menu;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getThisPackagePath;
import imj.IMJTools;
import imj.Image;
import imj.ImageWrangler;
import imj.apps.modules.BigImageComponent;
import imj.apps.modules.HistogramsPanel;
import imj.apps.modules.RegionOfInterest;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
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
import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

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
				final int option = showOptionDialog(null, "Select", "Set View Filter", OK_CANCEL_OPTION, PLAIN_MESSAGE, null, filters, null);
				
				if (option == JOptionPane.CLOSED_OPTION) {
					return;
				}
				
				final ViewFilter filter = filters[option];
				
				if (!filter.configure()) {
					return;
				}
				
				filter.initialize();
				
				result.set("viewFilter", null);
				result.set("viewFilter", filter);
			}
			
		};
		
		new AbstractAFAction(result, ACTIONS_APPLY_SIEVE) {
			
			@Override
			public final void perform() {
				debugPrint("TODO");
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
				final RegionOfInterest source = rois[sourceLod];
				final RegionOfInterest destination = rois[destinationLod];
				final int sourceRowCount = source.getRowCount();
				final int sourceColumnCount = source.getColumnCount();
				final int destinationRowCount = destination.getRowCount();
				final int destinationColumnCount = destination.getColumnCount();
				final boolean sourceIsSmallerThanDestination = sourceRowCount < destinationRowCount;
				
				if (sourceIsSmallerThanDestination) {
					for (int destinationRowIndex = 0; destinationRowIndex < destinationRowCount; ++destinationRowIndex) {
						final int sourceRowIndex = destinationRowIndex * sourceRowCount / destinationRowCount;
						
						for (int destinationColumnIndex = 0; destinationColumnIndex < destinationColumnCount; ++destinationColumnIndex) {
							final int sourceColumnIndex = destinationColumnIndex * sourceColumnCount / destinationColumnCount;
							
							destination.set(destinationRowIndex, destinationColumnIndex, source.get(sourceRowIndex, sourceColumnIndex));
						}
					}
				} else {
					for (int destinationRowIndex = 0; destinationRowIndex < destinationRowCount; ++destinationRowIndex) {
						for (int destinationColumnIndex = 0; destinationColumnIndex < destinationColumnCount; ++destinationColumnIndex) {
							destination.set(destinationRowIndex, destinationColumnIndex, false);
						}
					}
					
					for (int sourceRowIndex = 0; sourceRowIndex < sourceRowCount; ++sourceRowIndex) {
						final int destinationRowIndex = sourceRowIndex * destinationRowCount / sourceRowCount;
						
						for (int sourceColumnIndex = 0; sourceColumnIndex < sourceColumnCount; ++sourceColumnIndex) {
							final int destinationColumnIndex = sourceColumnIndex * destinationColumnCount / sourceColumnCount;
							
							if (source.get(sourceRowIndex, sourceColumnIndex)) {
								destination.set(destinationRowIndex, destinationColumnIndex);
							}
						}
					}
				}
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
						newCopyROIItem(result))
		));
		
		result.set("image", null, Image.class);
		result.set("lod", null, Integer.class);
		result.set("xy", null, Point.class);
		result.set("rgb", null, String.class);
		result.set("hsb", null, String.class);
		
		result.set("viewFilters", array(new RoundingViewFilter(result)), ViewFilter[].class);
		result.set("viewFilter", null, ViewFilter.class);
		result.set("sieves", array((Sieve) null), Sieve[].class);
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
	
	/**
	 * @author codistmonk (creation 2013-02-18)
	 */
	public static abstract class Plugin {
		
		private final Context context;
		
		private final Map<String, String> parameters;
		
		protected Plugin(final Context context) {
			this.context = context;
			this.parameters = new LinkedHashMap<String, String>();
		}
		
		public final Context getContext() {
			return this.context;
		}
		
		public final Map<String, String> getParameters() {
			return this.parameters;
		}
		
		public abstract void initialize();
		
		public final boolean configure() {
			final Box inputBox = Box.createVerticalBox();
			final Map<String, JTextField> textFields = new HashMap<String, JTextField>();
			
			for (final Map.Entry<String, String> entry : this.getParameters().entrySet()) {
				final JTextField textField = new JTextField(entry.getValue());
				
				textFields.put(entry.getKey(), textField);
				inputBox.add(horizontalBox(new JLabel(entry.getKey()), textField));
			}
			
			final int option = showConfirmDialog(null, inputBox, "Configure", OK_CANCEL_OPTION);
			
			if (option == CANCEL_OPTION) {
				return false;
			}
			
			for (final Map.Entry<String, JTextField> entry : textFields.entrySet()) {
				this.getParameters().put(entry.getKey(), entry.getValue().getText());
			}
			
			return true;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-18)
	 */
	public static abstract class ViewFilter extends Plugin {
		
		protected ViewFilter(final Context context) {
			super(context);
		}
		
		public abstract int getNewValue(int x, int y, int oldValue);
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-18)
	 */
	public static final class RoundingViewFilter extends ViewFilter {
		
		private int offset;
		
		private int mask;
		
		public RoundingViewFilter(final Context context) {
			super(context);
			
			this.getParameters().put("bitCount", "0");
		}
		
		@Override
		public final void initialize() {
			final int bitCount = parseInt(this.getParameters().get("bitCount"));
			
			this.offset = 1 << (bitCount - 1);
			this.mask = (~((1 << bitCount) - 1)) & 0x8FFFFFFF;
		}
		
		@Override
		public final int getNewValue(final int x, final int y, final int oldValue) {
			return rgba(255,
					this.transform(red(oldValue)),
					this.transform(green(oldValue)),
					this.transform(blue(oldValue)));
		}
		
		public final int transform(final int channelValue) {
			return min(255, (channelValue + this.offset) & this.mask);
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-18)
	 */
	public static abstract class Sieve extends Plugin {
		
		protected Sieve(final Context context) {
			super(context);
		}
		
		public abstract boolean accept(int x, int y, int value);
		
	}
	
}
