package imj3.draft;

import static java.lang.Math.*;
import static java.util.stream.Collectors.toList;
import static multij.swing.SwingTools.*;
import static multij.tools.MathTools.square;
import static multij.tools.Tools.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Painter;
import javax.swing.SwingUtilities;

import multij.events.EventManager;
import multij.events.EventManager.AbstractEvent;
import multij.events.EventManager.Event.Listener;
import multij.swing.MouseHandler;
import multij.swing.SwingTools;
import multij.tools.Canvas;
import multij.tools.IllegalInstantiationException;
import multij.tools.MathTools.Statistics;
import multij.tools.NanoTicToc;
import multij.tools.Pair;
import multij.tools.Tools;

/**
 * @author codistmonk (creation 2015-10-06)
 */
public final class RoundObjectSegmenter {
	
	private RoundObjectSegmenter() {
		throw new IllegalInstantiationException();
	}
	
	static final AtomicBoolean debug = new AtomicBoolean();
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		speedExperiment();
		
		SwingUtilities.invokeLater(() -> {
			SwingTools.show(newMainPanel(), RoundObjectSegmenter.class.getName());
		});
	}
	
	public static final JPanel newMainPanel() {
		final JPanel mainPanel = new JPanel(new BorderLayout());
		final JTextField innerRadiusTextField = new JTextField("10");
		final JTextField outerRadiusTextField = new JTextField("30");
		final ImageComponent imageComponent = new ImageComponent();
		final Point mouse = new Point();
		
		innerRadiusTextField.addActionListener(event -> imageComponent.repaint());
		outerRadiusTextField.addActionListener(event -> imageComponent.repaint());
		
		imageComponent.addMouseMotionListener(new MouseHandler() {
			
			@Override
			public final void mouseExited(final MouseEvent event) {
				mouse.x = -1;
				imageComponent.repaint();
			}
			
			@Override
			public final void mouseMoved(final MouseEvent event) {
				mouse.setLocation(event.getX(), event.getY());
				imageComponent.repaint();
			}

			private static final long serialVersionUID = 2071608329776964978L;
			
		});
		
		final ConvolveOp horizontalGradientOp = new ConvolveOp(new Kernel(3, 1, new float[] { -1F, 0F, 1F }));
		final ConvolveOp verticalGradientOp = new ConvolveOp(new Kernel(1, 3, new float[] { -1F, 0F, 1F }));
		final int[][][] orientations = { null };
		final int[][][] gradients = { null };
		
		EventManager.getInstance().addListener(imageComponent, ImageComponent.ImageChangedEvent.class, new Serializable() {
			
			@Listener
			public final void imageChanged(final ImageComponent.ImageChangedEvent event) {
				debugPrint(event);
				
				final BufferedImage image = event.getSource().getImage();
				final int height = image.getHeight();
				final int width = image.getWidth();
				orientations[0] = new int[height][width];
				gradients[0] = new int[height][width];
				final BufferedImage horizontalGradients = horizontalGradientOp.filter(image, null);
				final BufferedImage verticalGradients = verticalGradientOp.filter(image, null);
				
				for (int y = 0; y < height; ++y) {
					for (int x = 0; x < width; ++x) {
						final Color verticalGradientRGB = new Color(verticalGradients.getRGB(x, y));
						final Color horizontalGradientRGB = new Color(horizontalGradients.getRGB(x, y));
						final int v = max(verticalGradientRGB.getRed(), max(verticalGradientRGB.getGreen(), verticalGradientRGB.getBlue()));
						final int h = max(horizontalGradientRGB.getRed(), max(horizontalGradientRGB.getGreen(), horizontalGradientRGB.getBlue()));
						
						orientations[0][y][x] = (int) (acos(cos(atan2(v, h))) * 9.0 / PI);
						gradients[0][y][x] = (int) sqrt(square(v) + square(h));
					}
				}
				
				debugPrint();
			}
			
			private static final long serialVersionUID = -5300292847450862913L;
			
		});
		
		imageComponent.setOverlay((graphics, buffer, width, height) -> {
			final int outerRadius = Integer.decode(outerRadiusTextField.getText());
			
			if (outerRadius <= mouse.x && mouse.x < width - outerRadius && outerRadius <= mouse.y && mouse.y < height - outerRadius) {
				final int innerRadius = Integer.decode(innerRadiusTextField.getText());
				
				graphics.setColor(Color.RED);
				graphics.drawOval(mouse.x - innerRadius, mouse.y - innerRadius, 2 * innerRadius, 2 * innerRadius);
				graphics.drawOval(mouse.x - outerRadius, mouse.y - outerRadius, 2 * outerRadius, 2 * outerRadius);
				
				final int[][] array = new int[32][1 + outerRadius - innerRadius];
				
				for (int i = 0; i < 32; ++i) {
					final double angle = i * PI / 16.0;
					
					for (int r = innerRadius; r <= outerRadius; ++r) {
						final int y = max(0, min((int) (mouse.y + r * sin(angle)), height - 1));
						final int x = max(0, min((int) (mouse.x + r * cos(angle)), width - 1));
						array[i][r - innerRadius] = gradients[0][y][x];
					}
				}
				
				final Pair<int[], Integer> pair = findMaximalCircularPath(array);
				final Polygon polygon = new Polygon();
				
				for (int i = 0; i < 32; ++i) {
					final double angle = i * PI / 16.0;
					final int r = innerRadius + pair.getFirst()[i];
					
					polygon.addPoint((int) (mouse.x + r * cos(angle)), (int) (mouse.y + r * sin(angle)));
				}
				
				graphics.setColor(Color.GREEN);
				graphics.draw(polygon);
			}
		});
		
		mainPanel.add(horizontalBox(
				new JLabel("Inner radius:"), innerRadiusTextField,
				new JLabel("Outer radius:"), outerRadiusTextField), BorderLayout.NORTH);
		mainPanel.add(scrollable(imageComponent), BorderLayout.CENTER);
		return mainPanel;
	}
	
	public static final void speedExperiment() {
		final int rowCount = 36;
		final int columnCount = 22;
		final int runs = 1_000;
		final long seed = 100L;
		
		{
			long checksum1 = 0L;
			long checksum2 = 0L;
			final Random random = new Random(seed);
			final Statistics statistics = new Statistics();
			final Statistics globalOptimumFitness = new Statistics();
			final Statistics globalOptimumStartEndFitness = new Statistics();
			final Statistics[] globalOptimumStartFitnesses = instances(columnCount, Statistics.FACTORY);
			final Statistics[] globalOptimumEndFitnesses = instances(columnCount, Statistics.FACTORY);
			
			findMaximalCircularPath(newRandomInts(new Random(), rowCount, columnCount));
			
			for (int i = 0; i < runs; ++i) {
				final int[][] array = newRandomInts(random, rowCount, columnCount);
				final NanoTicToc timer = new NanoTicToc();
				final Pair<int[], Integer> maximalCircular = findMaximalCircularPath(array);
				
				statistics.addValue(timer.toc());
				checksum1 += maximalCircular.getSecond();
				checksum2 += Arrays.stream(maximalCircular.getFirst()).sum();
				
				{
					final Pair<int[], Integer> maximal = findMaximalPath(array);
					
					final boolean globalOptimumIsFit = abs(maximal.getFirst()[0] - maximal.getFirst()[rowCount - 1]) <= 1;
					globalOptimumFitness.addValue(globalOptimumIsFit ? 1.0 : 0.0);
					
					final int dStart = abs(maximal.getFirst()[0] - maximalCircular.getFirst()[0]);
					final int dEnd = abs(maximal.getFirst()[rowCount - 1] - maximalCircular.getFirst()[rowCount - 1]);
					final boolean globalOptimumMatchesCircularSolution = dStart == 0 && dEnd == 0;
					globalOptimumStartEndFitness.addValue(globalOptimumMatchesCircularSolution ? 1.0 : 0.0);
					
					for (int j = 0; j < columnCount; ++j) {
						globalOptimumStartFitnesses[j].addValue(dStart == j ? 1.0 : 0.0);
						globalOptimumEndFitnesses[j].addValue(dEnd == j ? 1.0 : 0.0);
					}
					
					if (globalOptimumIsFit != globalOptimumMatchesCircularSolution && !maximalCircular.getSecond().equals(maximal.getSecond())) {
						debugError("circularScore:", maximalCircular.getSecond(), "globalScore:", maximal.getSecond());
					}
				}
			}
			
			debugPrint("checksum1:", checksum1, "checksum2:", checksum2, "meanTime:", statistics.getMean(),
					"minTime:", statistics.getMinimum(), "maxTime:", statistics.getMaximum(), "stdev:", sqrt(statistics.getVariance()));
			debugPrint("globalOptimumFitness:", globalOptimumFitness.getMean());
			debugPrint("globalOptimumStartFitnesses:", Arrays.stream(globalOptimumStartFitnesses).map(Statistics::getMean).collect(toList()));
			debugPrint("globalOptimumEndFitnesses:", Arrays.stream(globalOptimumEndFitnesses).map(Statistics::getMean).collect(toList()));
			debugPrint("globalOptimumStartEndFitness:", globalOptimumStartEndFitness.getMean());
		}
		
		{
			long checksum1 = 0L;
			long checksum2 = 0L;
			final Random random = new Random(seed);
			final Statistics statistics = new Statistics();
			
			findMaximalCircularPath(newRandomInts1(new Random(), rowCount, columnCount), rowCount);
			
			for (int i = 0; i < runs; ++i) {
				final int[] array = newRandomInts1(random, rowCount, columnCount);
				final NanoTicToc timer = new NanoTicToc();
				final Pair<int[], Integer> maximalCircular = findMaximalCircularPath(array, rowCount);
				
				statistics.addValue(timer.toc());
				checksum1 += maximalCircular.getSecond();
				checksum2 += Arrays.stream(maximalCircular.getFirst()).sum();
			}
			
			debugPrint("checksum1:", checksum1, "checksum2:", checksum2, "meanTime:", statistics.getMean(),
					"minTime:", statistics.getMinimum(), "maxTime:", statistics.getMaximum(), "stdev:", sqrt(statistics.getVariance()));
		}
	}
	
	public static final int maximum(final int[][] values) {
		int result = 0;
		
		for (final int[] row : values) {
			for (final int value : row) {
				if (result < value) {
					result = value;
				}
			}
		}
		
		return result;
	}
	
	public static final int maximum(final int[] values) {
		int result = 0;
		
		for (final int value : values) {
			if (result < value) {
				result = value;
			}
		}
		
		return result;
	}
	
	public static final Pair<int[], Integer> findMaximalCircularPath(final int[][] array) {
		final int rowCount = array.length;
		final int columnCount = array[0].length;
		Pair<int[], Integer> result = new Pair<>(new int[rowCount], 0);
		final int[][] scores = new int[rowCount][columnCount];
		final int[][] ds = new int[rowCount][columnCount];
		final int m = maximum(array);
		
		for (int origin = 0; origin < columnCount; ++origin) {
			final Pair<int[], Integer> pair = findMaximalPath(array, m, result, origin, scores, ds);
			
			if (result.getSecond() < pair.getSecond()) {
				result = pair;
			}
		}
		
		return result;
	}
	
	public static final Pair<int[], Integer> findMaximalCircularPath(final int[] array, final int rowCount) {
		final int columnCount = array.length / rowCount;
		final int[] scores = new int[array.length];
		final int[] ds = new int[array.length];
		Pair<int[], Integer> result = findMaximalPath(array, rowCount, scores, ds);
		final int f0 = result.getFirst()[0];
		final int e0 = result.getFirst()[rowCount - 1];
		
		if (abs(f0 - e0) <= 1) {
			return result;
		}
		
		result = new Pair<>(new int[rowCount], 0);
		
		final int m = maximum(array);
		final int[] origins = intRange(columnCount);
		
		{
			final int[] priorities = new int[rowCount];
			
			for (int i = 0; i < rowCount; ++i) {
				priorities[i] = min(abs(i - f0) * 3 / 2, abs(i - e0));
			}
			
			sort(origins, (i1, i2) -> Integer.compare(priorities[i1], priorities[i2]));
		}
		
		for (final int origin : origins) {
			final Pair<int[], Integer> pair = findMaximalPath(array, rowCount, m, result, origin, scores, ds);
			
			if (result.getSecond() < pair.getSecond()) {
				result = pair;
			}
		}
		
		return result;
	}
	
	public static final Pair<int[], Integer> findMaximalPath(final int[][] array, final int origin) {
		final int rowCount = array.length;
		final int columnCount = array[0].length;
		
		return findMaximalPath(array, maximum(array), new Pair<>(new int[rowCount], 0), origin, new int[rowCount][columnCount], new int[rowCount][columnCount]);
	}
	
	public static final void fill(final int[][] array, final int value) {
		for (final int[] row : array) {
			Arrays.fill(row, value);
		}
	}
	
	public static final Pair<int[], Integer> findMaximalPath(final int[][] array, final int maximum, final Pair<int[], Integer> best, final int origin, final int[][] scores, final int[][] ds) {
		final int rowCount = array.length;
		final int columnCount = array[0].length;
		
		fill(scores, -1);
		
		scores[0][origin] = array[0][origin];
		ds[0][origin] = origin;
		
		for (int i = 1; i < rowCount; ++i) {
			boolean candidateFound = false;
			
			for (int j = 0; j < columnCount; ++j) {
				int m = -1;
				
				for (int k = max(0, j - 1); k < min(j + 2, columnCount); ++k) {
					final int previousScore = scores[i - 1][k];
					
					if (m < previousScore) {
						ds[i][j] = k;
						m = previousScore;
					}
				}
				
				if (0 <= m) {
					scores[i][j] = m + array[i][j];
					
					if (best.getSecond() < scores[i][j] + (rowCount - i - 1) * maximum) {
						candidateFound = true;
					}
				}
			}
			
			if (!candidateFound) {
				return best;
			}
		}
		
		if (debug.get()) {
			debugPrint("origin:", origin);
			debugPrint("scores:");
			print(scores);
			debugPrint("ds:");
			print(ds);
		}
		
		{
			final int[] indices = new int[rowCount];
			final int lastRowIndex = rowCount - 1;
			indices[lastRowIndex] = origin;
			
//			initialize_last_index:
			for (int k = max(0, origin - 1); k < min(origin + 2, columnCount); ++k) {
				if (scores[lastRowIndex][indices[lastRowIndex]] < scores[lastRowIndex][k]) {
					indices[lastRowIndex] = k;
				}
			}
			
//			compute_indices:
			for (int i = rowCount - 2; 0 <= i; --i) {
				indices[i] = ds[i + 1][indices[i + 1]];
			}
			
			return new Pair<>(indices, scores[lastRowIndex][indices[lastRowIndex]]);
		}
	}
	
	public static final Pair<int[], Integer> findMaximalPath(final int[] array, final int rowCount, final int maximum, final Pair<int[], Integer> best, final int origin, final int[] scores, final int[] previousIndices) {
		final int bestScore = best.getSecond();
		final int columnCount = array.length / rowCount;
		
		Arrays.fill(scores, -1);
		
		scores[origin] = array[origin];
		previousIndices[origin] = origin;
		
		for (int i = 1; i < rowCount; ++i) {
			boolean candidateFound = false;
			final int delta = min(i, rowCount - i);
			final int firstJ = max(0, origin - delta);
			final int endJ = min(origin + delta + 1, columnCount);
			final int maximumPossibleRemainingValues = (rowCount - 1 - i) * maximum;
			
			for (int j = firstJ, index = i * columnCount + j; j < endJ; ++j, ++index) {
				final int firstK = max(0, j - 1);
				final int endK = min(j + 2, columnCount);
				int m = -1;
				
				for (int k = firstK; k < endK; ++k) {
					final int previousScore = scores[(i - 1) * columnCount + k];
					
					if (m < previousScore) {
						previousIndices[index] = k;
						m = previousScore;
					}
				}
				
				if (0 <= m) {
					scores[index] = m + array[index];
					
					if (bestScore < scores[index] + maximumPossibleRemainingValues) {
						candidateFound = true;
					}
				}
			}
			
			if (!candidateFound) {
				return best;
			}
		}
		
		{
			final int[] indices = new int[rowCount];
			final int lastRowIndex = rowCount - 1;
			indices[lastRowIndex] = origin;
			final int i0 = lastRowIndex * columnCount;
			
//			initialize_last_index:
			{
				final int firstK = max(0, origin - 1);
				final int endK = min(origin + 2, columnCount);
				
				for (int k = firstK; k < endK; ++k) {
					if (scores[i0 + indices[lastRowIndex]] < scores[i0 + k]) {
						indices[lastRowIndex] = k;
					}
				}
			}
			
//			compute_indices:
			for (int i = rowCount - 2; 0 <= i; --i) {
				indices[i] = previousIndices[(i + 1) * columnCount + indices[i + 1]];
			}
			
			return new Pair<>(indices, scores[i0 + indices[lastRowIndex]]);
		}
	}
	
	public static final Pair<int[], Integer> findMaximalPath(final int[][] array) {
		final int rowCount = array.length;
		final int columnCount = array[0].length;
		
		return findMaximalPath(array, new int[rowCount][columnCount], new int[rowCount][columnCount]);
	}
	
	public static final Pair<int[], Integer> findMaximalPath(final int[][] array, final int[][] scores, final int[][] ds) {
		final int rowCount = array.length;
		final int columnCount = array[0].length;
		
		System.arraycopy(array[0], 0, scores[0], 0, columnCount);
		
		for (int i = 1; i < rowCount; ++i) {
			for (int j = 0; j < columnCount; ++j) {
				int m = -1;
				
				for (int k = max(0, j - 1); k < min(j + 2, columnCount); ++k) {
					final int previousScore = scores[i - 1][k];
					
					if (m < previousScore) {
						ds[i][j] = k;
						m = previousScore;
					}
				}
				
				if (0 <= m) {
					scores[i][j] = m + array[i][j];
				}
			}
		}
		
		if (debug.get()) {
			debugPrint("scores:");
			print(scores);
			debugPrint("ds:");
			print(ds);
		}
		
		{
			final int[] indices = new int[rowCount];
			final int lastRowIndex = rowCount - 1;
			indices[lastRowIndex] = 0;
			
//			initialize_last_index:
			for (int k = 0; k < columnCount; ++k) {
				if (scores[lastRowIndex][indices[lastRowIndex]] < scores[lastRowIndex][k]) {
					indices[lastRowIndex] = k;
				}
			}
			
//			compute_indices:
			for (int i = rowCount - 2; 0 <= i; --i) {
				indices[i] = ds[i + 1][indices[i + 1]];
			}
			
			return new Pair<>(indices, scores[lastRowIndex][indices[lastRowIndex]]);
		}
	}
	
	public static final Pair<int[], Integer> findMaximalPath(final int[] array, final int rowCount, final int[] scores, final int[] ds) {
		final int columnCount = array.length / rowCount;
		
		System.arraycopy(array, 0, scores, 0, columnCount);
		
		for (int i = 1; i < rowCount; ++i) {
			for (int j = 0; j < columnCount; ++j) {
				int m = -1;
				
				for (int k = max(0, j - 1); k < min(j + 2, columnCount); ++k) {
					final int previousScore = scores[(i - 1) * columnCount + k];
					
					if (m < previousScore) {
						ds[i * columnCount +j] = k;
						m = previousScore;
					}
				}
				
				if (0 <= m) {
					scores[i * columnCount + j] = m + array[i * columnCount + j];
				}
			}
		}
		
		{
			final int[] indices = new int[rowCount];
			final int lastRowIndex = rowCount - 1;
			indices[lastRowIndex] = 0;
			
//			initialize_last_index:
			for (int k = 0; k < columnCount; ++k) {
				if (scores[lastRowIndex * columnCount + indices[lastRowIndex]] < scores[lastRowIndex * columnCount + k]) {
					indices[lastRowIndex] = k;
				}
			}
			
//			compute_indices:
			for (int i = rowCount - 2; 0 <= i; --i) {
				indices[i] = ds[(i + 1) * columnCount + indices[i + 1]];
			}
			
			return new Pair<>(indices, scores[lastRowIndex * columnCount + indices[lastRowIndex]]);
		}
	}
	
	public static final int[][] newRandomInts(final Random random, final int rowCount, final int columnCount) {
		final int[][] result = new int[rowCount][columnCount];
		
		for (final int[] row : result) {
			for (int i = 0; i < columnCount; ++i) {
				row[i] = 10 + random.nextInt(10);
			}
		}
		
		return result;
	}
	
	public static final int[] newRandomInts1(final Random random, final int rowCount, final int columnCount) {
		final int n = rowCount * columnCount;
		final int[] result = new int[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = 10 + random.nextInt(10);
		}
		
		return result;
	}
	
	public static final void print(final int[][] array) {
		for (final int[] row : array) {
			debugPrint("\t", Tools.join("\t", Arrays.stream(row).mapToObj(Integer::new).collect(toList())));
		}
	}
	
	/**
	 * @author codistmonk (creation 2015-10-06)
	 */
	public static final class ImageComponent extends JComponent {
		
		private final Canvas canvas;
		
		private BufferedImage image;
		
		private Painter<BufferedImage> overlay;
		
		public ImageComponent() {
			this.canvas = new Canvas();
			
			this.setDropTarget(new DropTarget() {
				
				@Override
				public final synchronized void drop(final DropTargetDropEvent event) {
					try {
						setImage(ImageIO.read(SwingTools.getFiles(event).get(0))).repaint();
					} catch (final IOException exception) {
						exception.printStackTrace();
					}
				}
				
				private static final long serialVersionUID = 2040965781122089929L;
				
			});
			
			this.setPreferredSize(new Dimension(256, 256));
		}
		
		public final BufferedImage getImage() {
			return this.image;
		}
		
		public final ImageComponent setImage(final BufferedImage image) {
			final BufferedImage previousImage = this.getImage();
			
			if (previousImage != image) {
				this.image = image;
				
				if (image != null) {
					this.canvas.setFormat(image.getWidth(), image.getHeight());
					this.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
					this.setSize(new Dimension(image.getWidth(), image.getHeight()));
				}
				
				this.new ImageChangedEvent(previousImage).fire();
			}
			
			return this;
		}
		
		public final Painter<BufferedImage> getOverlay() {
			return this.overlay;
		}
		
		public final ImageComponent setOverlay(final Painter<BufferedImage> overlay) {
			this.overlay = overlay;
			
			return this;
		}
		
		@Override
		protected final void paintComponent(final Graphics graphics) {
			if (this.getImage() == null) {
				super.paintComponent(graphics);
			} else {
				this.canvas.getGraphics().drawImage(this.getImage(), 0, 0, null);
				
				if (this.getOverlay() != null) {
					this.getOverlay().paint(this.canvas.getGraphics(), this.canvas.getImage(), this.getImage().getWidth(), this.getImage().getHeight());
				}
				
				graphics.drawImage(this.canvas.getImage(), 0, 0, null);
			}
		}
		
		/**
		 * @author codistmonk (creation 2015-10-06)
		 */
		public final class ImageChangedEvent extends AbstractEvent<ImageComponent> {
			
			private final BufferedImage previousImage;
			
			public ImageChangedEvent(final BufferedImage previousImage) {
				super(ImageComponent.this);
				this.previousImage = previousImage;
			}
			
			public final BufferedImage getPreviousImage() {
				return this.previousImage;
			}
			
			private static final long serialVersionUID = -872056739191889411L;
			
		}
		
		private static final long serialVersionUID = -8983748249279638251L;
		
	}
	
}
