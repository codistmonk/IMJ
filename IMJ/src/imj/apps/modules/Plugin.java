package imj.apps.modules;

import static javax.swing.JOptionPane.CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JTextField;

import net.sourceforge.aprog.context.Context;

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
	
	@Override
	public final String toString() {
		return this.getClass().getName();
	}
	
}
