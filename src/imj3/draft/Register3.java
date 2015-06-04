package imj3.draft;

import static imj3.draft.Register.lerp;
import static imj3.draft.Register.middle;
import static imj3.draft.Register.newPatchOffsets;
import static imj3.draft.Register2.copyTo;
import static imj3.draft.Register2.overlay;
import static imj3.draft.Register2.scalarize;
import static multij.swing.SwingTools.show;
import static multij.tools.Tools.baseName;
import static multij.tools.Tools.debugError;
import static multij.tools.Tools.debugPrint;

import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import imj3.core.Image2D;
import imj3.draft.Register2.ParticleGrid;
import imj3.draft.Register2.Warping;
import imj3.tools.IMJTools;
import imj3.tools.Image2DComponent;
import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;
import multij.tools.MathTools.Statistics;
import multij.tools.Pair;

/**
 * @author codistmonk (creation 2015-04-28)
 */
public final class Register3 {
	
	private Register3() {
		throw new IllegalInstantiationException();
	}
	
	static final Point2D ZERO = new Point2D.Double();
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final int lod = arguments.get("lod", 5)[0];
		final Image2D source = IMJTools.read(arguments.get("source", ""), lod);
		final int iterations = arguments.get("iterations", 200)[0];
		final int patchSize0 = arguments.get("patchSize", 25)[0];
		final int regularization = arguments.get("regularization", 20)[0];
		final String outputPrefix = arguments.get("outputPrefix", baseName(source.getId()));
		final boolean show = arguments.get("show", 0)[0] != 0;
		
		debugPrint("sourceId:", source.getId());
		debugPrint("sourceWidth:", source.getWidth(), "sourceHeight:", source.getHeight(), "sourceChannels:", source.getChannels());
		
		final Image2D target = IMJTools.read(arguments.get("target", ""), lod);
		
		debugPrint("targetId:", target.getId());
		debugPrint("targetWidth:", target.getWidth(), "targetHeight:", target.getHeight(), "targetChannels:", target.getChannels());
		
		final Warping warping = new Warping(16, 16);
		
		if (true) {
			final Image2DComponent sourceComponent = new Image2DComponent(scalarize(source));
			final Image2DComponent targetComponent = new Image2DComponent(scalarize(target));
			
			for (int i = 0; i < 20; ++i) {
				debugPrint(i);
				
				move(warping.getTargetGrid(), target, 15, 5);
				
				for (int j = 0; j < 20; ++j) {
					regularize(warping.getTargetGrid());
				}
			}
			
			copyTo(warping.getSourceGrid(), warping.getTargetGrid());
			
			for (int i = 0; i < 20; ++i) {
				debugPrint(i);
				
				move(warping.getSourceGrid(), source, 15, 5);
				
				for (int j = 0; j < 20; ++j) {
					regularize(warping.getSourceGrid());
				}
			}
			
			overlay(warping.getSourceGrid(), sourceComponent);
			overlay(warping.getTargetGrid(), targetComponent);
			
			show(targetComponent, "scalarized target", false);
			show(sourceComponent, "scalarized source", false);
			
			return;
		}
	}
	
	public static final void move(final ParticleGrid grid, final Image2D image, final int windowSize, final int patchSize) {
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final Point2D[] patchOffsets = newPatchOffsets(windowSize);
		
		grid.forEach((x, y) -> {
			final Point2D normalizedPixel = grid.get(x, y);
			double bestSaliency = 0.0;
			Point2D bestOffset = null;
			
			for (final Point2D offset : patchOffsets) {
				final double saliency = Register2.saliency(image,
						(int) (normalizedPixel.getX() * imageWidth + offset.getX()),
						(int) (normalizedPixel.getY() * imageHeight + offset.getY()), patchSize);
				
				if (bestSaliency < saliency) {
					bestSaliency = saliency;
					bestOffset = offset;
				}
			}
			
			if (bestOffset != null) {
				final double d = bestOffset.distance(ZERO);
				
				if (0.0 < d) {
					final double s = 0.5 / d;
					
					normalizedPixel.setLocation(normalizedPixel.getX() + s * bestOffset.getX() / imageWidth,
							normalizedPixel.getY() + s * bestOffset.getY() / imageHeight);
				}
			}
			
			return true;
		});
	}
	
	public static final void regularize(final ParticleGrid grid) {
		final int gridWidth = grid.getWidth();
		final int gridHeight = grid.getHeight();
		final Statistics statistics = new Statistics();
		
		for (int y = 0; y < gridHeight; ++y) {
			for (int x = 0; x + 1 < gridWidth; ++x) {
				statistics.addValue(grid.get(x, y).distance(grid.get(x + 1, y)));
			}
		}
		
		for (int y = 0; y + 1 < gridHeight; ++y) {
			for (int x = 0; x < gridWidth; ++x) {
				statistics.addValue(grid.get(x, y).distance(grid.get(x, y + 1)));
			}
		}
		
		final double m = statistics.getMean();
		
		for (int y = 0; y < gridHeight; ++y) {
			for (int x = 0; x + 1 < gridWidth; ++x) {
				final Point2D p1 = grid.get(x, y);
				final Point2D p2 = grid.get(x + 1, y);
				final double d = p1.distance(p2);
				final double k = lerp(d, m, 0.25) / d;
				final double x1 = p1.getX();
				final double y1 = p1.getY();
				final double x2 = p2.getX();
				final double y2 = p2.getY();
				final double mx = middle(x1, x2);
				final double my = middle(y1, y2);
				
				p1.setLocation(lerp(mx, x1, k), lerp(my, y1, k));
				p2.setLocation(lerp(mx, x2, k), lerp(my, y2, k));
			}
		}
	}
	
}
