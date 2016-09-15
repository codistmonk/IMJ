package imj3.draft;

import static imj3.tools.CommonSwingTools.newRandomColor;
import static multij.tools.Tools.debugError;

import imj3.tools.AwtImage2D;
import imj3.tools.Image2DComponent;
import imj3.tools.Image2DComponent.Overlay;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.SwingUtilities;

import multij.swing.SwingTools;

/**
 * @author codistmonk (creation 2016-09-15)
 */
public final class SVGOverlay implements Overlay {
	
	private final Image2DComponent component;
	
	private final Map<String, List<Area>> regions;
	
	private final Map<String, Color> classColors;
	
	public SVGOverlay(final Image2DComponent component) {
		this(component, new TreeMap<>(), new HashMap<>());
	}
	
	public SVGOverlay(final Image2DComponent component, final Map<String, List<Area>> regions, final Map<String, Color> classColors) {
		this.regions = regions;
		this.classColors = classColors;
		this.component = component;
	}
	
	public final Image2DComponent getComponent() {
		return this.component;
	}
	
	public final Map<String, List<Area>> getRegions() {
		return this.regions;
	}
	
	public final Map<String, Color> getClassColors() {
		return this.classColors;
	}
	
	@Override
	public final void update(final Graphics2D graphics, final Rectangle region) {
		{
			final AffineTransform transform = graphics.getTransform();
			
			graphics.setTransform(this.getComponent().getView());
			
			for (final Map.Entry<String, List<Area>> entry : this.getRegions().entrySet()) {
				graphics.setColor(this.getClassColors().getOrDefault(entry.getKey(), new Color(0x60000000, true)));
				
				for (final Area r : entry.getValue()) {
					graphics.fill(r);
				}
			}
			
			graphics.setTransform(transform);
		}
	}
	
	private static final long serialVersionUID = 5364538837277111690L;
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String... commandLineArguments) {
		SwingTools.useSystemLookAndFeel();
		
		SwingUtilities.invokeLater(() -> SwingTools.show(
				setup(new Image2DComponent(new AwtImage2D("", 256, 256))), "View"));
	}
	
	public static final Image2DComponent setup(final Image2DComponent component) {
		final SVGOverlay svgOverlay = new SVGOverlay(component);
		
		component.setOverlay(svgOverlay);
		
		component.setDropImageEnabled(true);
		
		component.setDropTarget(new DropTarget() {
			
			private final Map<String, List<Area>> regions = svgOverlay.getRegions();
			
			private final Map<String, Color> classColors = svgOverlay.getClassColors();
			
			private final DropTarget originalDropTarget = component.getDropTarget();
			
			@Override
			public final synchronized void drop(final DropTargetDropEvent event) {
				final File file = SwingTools.getFiles(event).get(0);
				
				try {
					this.regions.clear();
					this.regions.putAll(SVGTools.getRegions(SVGTools.readXML(file)));
					
					this.regions.keySet().forEach(k -> this.classColors.putIfAbsent(k, newRandomColor()));
					
					component.repaint();
				} catch (final Exception exception) {
					debugError(exception);
					
					if (this.originalDropTarget != null) {
						this.originalDropTarget.drop(event);
					}
				}
			}
			
			private static final long serialVersionUID = 3099468766376236640L;
			
		});
		
		return component;
	}
	
}
