package imj.apps.modules;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.JTextField;

import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class PipelineViewFilter extends ViewFilter {
	
	public PipelineViewFilter(final Context context) {
		super(context);
		
		this.getParameters().clear();
	}
	
	@Override
	public final void initialize() {
		// TODO
	}
	
	@Override
	public final int getNewValue(final int index, final int oldValue, final Channel channel) {
		return oldValue;
	}
	
	@Override
	protected final boolean splitInputChannels() {
		return false;
	}
	
	@Override
	protected final Component newInputPanel(final ActionListener previewAction, final Map<String, JTextField> textFields) {
		// TODO Auto-generated method stub
		return super.newInputPanel(previewAction, textFields);
	}
	
}
