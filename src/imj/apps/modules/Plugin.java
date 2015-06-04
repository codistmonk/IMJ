package imj.apps.modules;

import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Math.abs;
import static javax.swing.Box.createHorizontalGlue;
import static javax.swing.Box.createVerticalGlue;
import static javax.swing.JOptionPane.showInputDialog;
import static multij.i18n.Messages.translate;
import static multij.swing.SwingTools.horizontalBox;
import static multij.tools.Tools.getResourceAsStream;
import static multij.tools.Tools.ignore;
import static multij.tools.Tools.invoke;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import multij.context.Context;
import multij.events.Variable.Listener;
import multij.events.Variable.ValueChangedEvent;
import multij.tools.CommandLineArgumentsParser;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public abstract class Plugin {
	
	private final long id;
	
	private final Context context;
	
	private final Map<String, String> parameters;
	
	protected Plugin(final Context context) {
		this.id = ++newId;
		this.context = context;
		this.parameters = new LinkedHashMap<String, String>();
	}
	
	public final long getId() {
		return this.id;
	}
	
	public final Context getContext() {
		return this.context;
	}
	
	public final Map<String, String> getParameters() {
		return this.parameters;
	}
	
	public abstract void initialize();
	
	public final void configureAndApply() {
		final Box inputBox = Box.createVerticalBox();
		final Map<String, JTextField> textFields = new HashMap<String, JTextField>();
		final JToggleButton previewButton = translate(new JToggleButton("Preview"));
		final ActionListener previewAction = new ActionListener() {
			
			@Override
			public final void actionPerformed(final ActionEvent event) {
				if (previewButton.isSelected()) {
					Plugin.this.retrieveParameters(textFields);
					Plugin.this.initialize();
					Plugin.this.apply();
				} else if (event.getSource() == previewButton) {
					Plugin.this.cancel();
				}
			}
			
		};
		
		previewButton.addActionListener(previewAction);
		
		inputBox.add(this.newInputPanel(previewAction, textFields));
		inputBox.add(createVerticalGlue());
		
		final JButton cancelButton = translate(new JButton("Cancel"));
		final JButton applyButton = translate(new JButton("Apply"));
		final JButton okButton = translate(new JButton("OK"));
		
		inputBox.add(horizontalBox(cancelButton, createHorizontalGlue(),
				previewButton, createHorizontalGlue(), applyButton, okButton));
		
		final JPanel panel = new JPanel();
		final JFrame mainFrame = this.getContext().get("mainFrame");
		final JDialog dialog = new JDialog(mainFrame, false);
		
		dialog.setTitle(this.getClass().getSimpleName());
		
		try {
			panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
			panel.add(new JLabel(new ImageIcon(ImageIO.read(getResourceAsStream("imj/apps/thumbnail.png")))));
			panel.add(inputBox);
			
			dialog.add(panel);
			
			final boolean[] finalActionPerformed = { false };
			
			cancelButton.addActionListener(new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					finalActionPerformed[0] = true;
					
					if (previewButton.isSelected()) {
						Plugin.this.cancel();
					}
					
					dialog.dispose();
				}
				
			});
			
			final ActionListener applyButtonAction = new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					finalActionPerformed[0] = true;
					
					if (!previewButton.isSelected()) {
						Plugin.this.initialize();
						Plugin.this.apply();
					}
					
					Plugin.this.clearBackup();
					Plugin.this.backup();
					
					if (previewButton.isSelected()) {
						finalActionPerformed[0] = false;
						previewAction.actionPerformed(event);
					}
				}
				
			};
			
			applyButton.addActionListener(applyButtonAction);
			
			okButton.addActionListener(new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					finalActionPerformed[0] = true;
					
					if (!previewButton.isSelected()) {
						Plugin.this.initialize();
						Plugin.this.apply();
					}
					
					dialog.dispose();
				}
				
			});
			
			dialog.addWindowListener(new WindowAdapter() {
				
				@Override
				public final void windowClosing(final WindowEvent event) {
					if (!finalActionPerformed[0]) {
						Plugin.this.cancel();
					}
					
					Plugin.this.clearBackup();
				}
				
			});
			
			Plugin.this.backup();
			
			dialog.pack();
			dialog.setVisible(true);
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}
	
	public abstract void backup();
	
	public abstract void apply();
	
	public abstract void cancel();
	
	public abstract void clearBackup();
	
	@Override
	public final String toString() {
		return this.getClass().getName();
	}
	
	public final void retrieveParameters(final Map<String, JTextField> textFields) {
		for (final Map.Entry<String, JTextField> entry : textFields.entrySet()) {
			this.getParameters().put(entry.getKey(), entry.getValue().getText());
		}
	}
	
	/**
	 * @param previewAction
	 * <br>Must not be null
	 * @param textFields
	 * <br>Must not be null
	 * <br>Used by <code>previewAction</code> to initialize this plugin's parameters
	 * @return
	 * <br>Must not be null
	 */
	protected Component newInputPanel(final ActionListener previewAction, final Map<String, JTextField> textFields) {
		final Box result = Box.createVerticalBox();
		
		for (final Map.Entry<String, String> entry : this.getParameters().entrySet()) {
			final JTextField textField = newSpinnerTextField(entry.getValue(), previewAction);
			
			textFields.put(entry.getKey(), textField);
			
			final Box horizontalBox = horizontalBox(new JLabel(entry.getKey()), textField);
			
			horizontalBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, horizontalBox.getPreferredSize().height));
			result.add(horizontalBox);
		}
		
		return result;
	}
	
	protected final int[] getIntsParameter(final String key) {
		return new CommandLineArgumentsParser(key, this.getParameters().get(key).trim()).get(key);
	}
	
	protected final int getIntParameter(final String key) {
		return parseInt(this.getParameters().get(key).trim());
	}
	
	protected final double getDoubleParameter(final String key) {
		return Double.parseDouble(this.getParameters().get(key).trim());
	}
	
	private static long newId = 0L;
	
	public static final JTextField newSpinnerTextField(final String initialText, final ActionListener action) {
		final JTextField result = new JTextField(initialText);
		
		result.addActionListener(action);
		
		result.addKeyListener(new KeyAdapter() {
			
			private String operation = "+";
			
			private double increment = +1.0;
			
			@Override
			public final void keyPressed(final KeyEvent event) {
				switch (event.getKeyCode()) {
				case KeyEvent.VK_UP:
				case KeyEvent.VK_KP_UP:
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_KP_DOWN:
					if ((event.getModifiersEx() & SHIFT_DOWN_MASK) == SHIFT_DOWN_MASK) {
						final String userInput = showInputDialog("Operation and increment", this.operation + " " + abs(this.increment));
						
						if (userInput != null) {
							final String[] operationAndIncrement = userInput.trim().split("\\s+");
							this.operation = operationAndIncrement[0];
							this.increment = parseDouble(operationAndIncrement[1]);
						}
					}
					break;
				default:
					return;
				}
				
				switch (event.getKeyCode()) {
				case KeyEvent.VK_UP:
				case KeyEvent.VK_KP_UP:
					this.increment = abs(this.increment);
					break;
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_KP_DOWN:
					this.increment = -abs(this.increment);
					break;
				default:
					break;
				}
				
				if (this.increment != 0) {
					try {
						final double value = parseDouble(result.getSelectedText());
						final double newValue;
						
						if ("+".equals(this.operation)) {
							newValue = value + this.increment;
						} else if ("*".equals(this.operation)) {
							newValue = 0 <= this.increment ? value * this.increment : value / abs(this.increment);
						} else {
							throw new IllegalArgumentException("Invalid operation: " + this.operation);
						}
						
						final String updatedNumber;
						
						if (value == (int) value && this.increment == (int) this.increment) {
							updatedNumber = "" + (int) (newValue);
						} else {
							updatedNumber = "" + (newValue);
						}
						
						final int i = result.getSelectionStart();
						final int j = result.getSelectionEnd();
						
						result.setText(result.getText().substring(0, i) + updatedNumber + result.getText().substring(j));
						result.setSelectionStart(i);
						result.setSelectionEnd(i + updatedNumber.length());
						
						event.consume();
						
						action.actionPerformed(null);
					} catch (final Exception exception) {
						ignore(exception);
					}
				}
			}
			
		});
		
		return result;
	}
	
	public static final void onChange(final Context context, final String variableName, final Object objectOrClass, final String methodName, final Object... arguments) {
		context.getVariable(variableName).addListener(new Listener<Object>() {
			
			@Override
			public final void valueChanged(final ValueChangedEvent<Object, ?> event) {
				invoke(objectOrClass, methodName, arguments);
			}
			
		});
	}
	
}
