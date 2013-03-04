package imj.apps.modules;

import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.lang.Double.parseDouble;
import static java.lang.Math.abs;
import static javax.swing.Box.createHorizontalGlue;
import static javax.swing.JOptionPane.showInputDialog;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.ignore;

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

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.events.AtomicVariable;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public abstract class Plugin {
	
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
	
	public final void configureAndApply() {
		final Box inputBox = Box.createVerticalBox();
		final Map<String, JTextField> textFields = new HashMap<String, JTextField>();
		final JToggleButton previewButton = new JToggleButton("Preview");
		final ActionListener applyAction = new ActionListener() {
			
			@Override
			public final void actionPerformed(final ActionEvent event) {
				if (previewButton.isSelected()) {
					for (final Map.Entry<String, JTextField> entry : textFields.entrySet()) {
						Plugin.this.getParameters().put(entry.getKey(), entry.getValue().getText());
					}
					
					Plugin.this.initialize();
					Plugin.this.apply();
				} else if (event.getSource() == previewButton) {
					Plugin.this.cancel();
				}
			}
			
		};
		
		previewButton.addActionListener(applyAction);
		
		for (final Map.Entry<String, String> entry : this.getParameters().entrySet()) {
			final JTextField textField = new JTextField(entry.getValue());
			
			textFields.put(entry.getKey(), textField);
			inputBox.add(horizontalBox(new JLabel(entry.getKey()), textField));
			
			textField.addActionListener(applyAction);
			
			textField.addKeyListener(new KeyAdapter() {
				
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
							final String userInput = showInputDialog("Operation and increment", "+ 1");
							
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
							final double value = parseDouble(textField.getSelectedText());
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
							
							final int i = textField.getSelectionStart();
							final int j = textField.getSelectionEnd();
							
							textField.setText(textField.getText().substring(0, i) + updatedNumber + textField.getText().substring(j));
							textField.setSelectionStart(i);
							textField.setSelectionEnd(i + updatedNumber.length());
							
							event.consume();
							
							applyAction.actionPerformed(null);
						} catch (final Exception exception) {
							ignore(exception);
						}
					}
				}
				
			});
		}
		
		final JButton cancelButton = new JButton("Cancel");
		final JButton okButton = new JButton("OK");
		
		inputBox.add(horizontalBox(cancelButton, createHorizontalGlue(), previewButton, okButton));
		
		final JPanel panel = new JPanel();
		final JFrame mainFrame = this.getContext().get("mainFrame");
		final JDialog dialog = new JDialog(mainFrame, false);
		
		dialog.setTitle(this.getClass().getSimpleName());
		
		try {
			panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
			panel.add(new JLabel(new ImageIcon(ImageIO.read(Tools.getResourceAsStream("imj/apps/thumbnail.png")))));
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
	
	public static final void fireUpdate(final Context context, final String variableName) {
		final Object value = context.get(variableName);
		final AtomicVariable<Object> variable = cast(AtomicVariable.class, context.getVariable(variableName));
		
		if (variable != null) {
			variable.new ValueChangedEvent(value, value).fire();
		}
	}
	
}
