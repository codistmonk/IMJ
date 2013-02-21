package imj.apps.modules;

import static javax.swing.Box.createHorizontalGlue;
import static javax.swing.Box.createVerticalGlue;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;
import static net.sourceforge.aprog.tools.Tools.cast;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
		}
		
		final JButton cancelButton = new JButton("Cancel");
		final JButton okButton = new JButton("OK");
		
		inputBox.add(createVerticalGlue());
		inputBox.add(horizontalBox(cancelButton, createHorizontalGlue(), previewButton, okButton));
		
		final JPanel panel = new JPanel();
		final JFrame mainFrame = this.getContext().get("mainFrame");
		final JDialog dialog = new JDialog(mainFrame, false);
		
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
