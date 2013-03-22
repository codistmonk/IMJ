package imj.apps.modules;

import static imj.IMJTools.alpha;
import static imj.IMJTools.argb;
import static imj.IMJTools.blue;
import static imj.IMJTools.brightness;
import static imj.IMJTools.channelValue;
import static imj.IMJTools.green;
import static imj.IMJTools.hue;
import static imj.IMJTools.red;
import static imj.IMJTools.saturation;
import static imj.MorphologicalOperations.StructuringElement.newDisk;
import static imj.MorphologicalOperations.StructuringElement.newRing;
import static imj.apps.modules.BigImageComponent.SOURCE_IMAGE;
import static imj.apps.modules.ViewFilter.Channel.Primitive.ALPHA;
import static imj.apps.modules.ViewFilter.Channel.Primitive.BLUE;
import static imj.apps.modules.ViewFilter.Channel.Primitive.GREEN;
import static imj.apps.modules.ViewFilter.Channel.Primitive.RED;
import static imj.apps.modules.ViewFilter.Channel.Synthetic.BRIGHTNESS;
import static imj.apps.modules.ViewFilter.Channel.Synthetic.HUE;
import static imj.apps.modules.ViewFilter.Channel.Synthetic.SATURATION;
import static java.lang.Double.parseDouble;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.sourceforge.aprog.af.AFTools.fireUpdate;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.ignore;
import imj.Image;
import imj.Labeling.NeighborhoodShape.Distance;
import imj.apps.modules.FilteredImage.ChannelFilter;
import imj.apps.modules.FilteredImage.Filter;
import imj.apps.modules.FilteredImage.StructuringElementFilter;
import imj.apps.modules.ViewFilter.Channel.Primitive;
import imj.apps.modules.ViewFilter.Channel.Synthetic;

import java.awt.Color;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.events.Variable.Listener;
import net.sourceforge.aprog.events.Variable.ValueChangedEvent;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public abstract class ViewFilter extends Plugin {
	
	private ViewFilter source;
	
	private final FilteredImage image;
	
	protected ViewFilter(final Context context) {
		super(context);
		this.image = new FilteredImage(null);
		
		this.getParameters().put(CHANNELS, "red green blue");
		
		context.getVariable(SOURCE_IMAGE).addListener(new Listener<Object>() {
			
			@Override
			public final void valueChanged(final ValueChangedEvent<Object, ?> event) {
				if (event.getOldValue() == event.getNewValue()) {
					return;
				}
				
				if (context.get(VIEW_FILTER) == ViewFilter.this) {
					ViewFilter.this.cancel();
					ViewFilter.this.initialize();
					ViewFilter.this.apply();
				}
			}
			
		});
	}
	
	public final FilteredImage getImage() {
		return this.image;
	}
	
	final ComplexFilter getComplexFilter() {
		return (ComplexFilter) this.getImage().getFilter();
	}
	
	public final ViewFilter getSource() {
		return this.source;
	}
	
	public final void setSource(final ViewFilter source) {
		this.source = source;
		
		this.setSourceImage(source == null ? null : source.getImage());
	}
	
	public final void setSourceImage(final Image image) {
		if (this.getImage().getSource() != image) {
			this.getImage().setSource(image);
			
			this.sourceImageChanged();
		}
	}
	
	protected void sourceImageChanged() {
		// NOP
	}
	
	public final void initialize() {
		if (this.getComplexFilter() == null) {
			this.getImage().setFilter(this.newComplexFilter());
		}
		
		if (this.getComplexFilter().splitInputChannels()) {
			final String[] inputChannelAsStrings = this.getParameters().get(CHANNELS).split("\\s+");
			final int n = inputChannelAsStrings.length;
			final Collection<Channel> newInputChannels = new LinkedHashSet<Channel>();
			Class<? extends Channel> newInputChannelClass = Channel.class;
			
			for (int i = 0; i < n; ++i) {
				final Channel channel = parseChannel(inputChannelAsStrings[i].toUpperCase());
				
				if (!newInputChannelClass.isAssignableFrom(channel.getClass())) {
					debugPrint(newInputChannelClass, channel.getClass());
					throw new IllegalArgumentException("All input channels must be of the same type (primitive xor synthetic)");
				}
				
				newInputChannels.add(channel);
				
				newInputChannelClass = (Class<? extends Channel>) channel.getClass().getSuperclass();
			}
			
			this.getComplexFilter().setInputChannels(newInputChannels);
			this.getComplexFilter().setInputChannelClass(newInputChannelClass);
		} else {
			this.getComplexFilter().setInputChannels(null);
			this.getComplexFilter().setInputChannelClass(null);
		}
		
		this.doInitialize();
	}
	
	@Override
	public final void apply() {
		final Context context = this.getContext();
		
		final ViewFilter current = context.get(VIEW_FILTER);
		ViewFilter f = current;
		
		while (f != this && f != null) {
			f = f.getSource();
		}
		
		if (f == null) {
			this.setSource(current);
			context.set(VIEW_FILTER, this);
		} else {
			fireUpdate(this.getContext(), VIEW_FILTER);
		}
	}
	
	@Override
	public final void backup() {
		// NOP
	}
	
	@Override
	public final void cancel() {
		final Context context = this.getContext();
		final ViewFilter current = context.get(VIEW_FILTER);
		
		if (current == this) {
			context.set(VIEW_FILTER, this.getSource());
			
			fireUpdate(this.getContext(), VIEW_FILTER);
		} else {
			ViewFilter f = current;
			
			while (f != null && f.getSource() != this) {
				f = f.getSource();
			}
			
			if (f != null) {
				assert f.getSource() == this;
				
				f.setSource(this.getSource());
				
				fireUpdate(this.getContext(), VIEW_FILTER);
			}
		}
		
		this.setSource(null);
	}
	
	@Override
	public final void clearBackup() {
		// NOP
	}
	
	protected abstract ComplexFilter newComplexFilter();
	
	protected void doInitialize() {
		// NOP
	}
	
	/**
	 * {@value}.
	 */
	public static final String CHANNELS = "channels";
	
	/**
	 * {@value}.
	 */
	public static final String VIEW_FILTER = "viewFilter";
	
	public static final Channel parseChannel(final String string) {
		try {
			return Channel.Primitive.valueOf(string);
		} catch (final Exception exception) {
			ignore(exception);
			
			return Channel.Synthetic.valueOf(string);
		}
	}
	
	public static final Channel[] parseChannels(final String string) {
		final String[] channelAsStrings = string.trim().split("\\s+");
		final int n = channelAsStrings.length;
		final Channel[] result = new Channel[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = parseChannel(channelAsStrings[i].toUpperCase(Locale.ENGLISH));
		}
		
		return result;
	}
	
	public static final int[] parseStructuringElement(final String structuringElementParametersAsString) {
		final String[] structuringElementParameters = structuringElementParametersAsString.trim().split("\\s+");
		final String shape = structuringElementParameters[0];
		
		if ("ring".equals(shape)) {
			final double innerRadius = parseDouble(structuringElementParameters[1]);
			final double outerRadius = parseDouble(structuringElementParameters[2]);
			final Distance distance = Distance.valueOf(structuringElementParameters[3].toUpperCase(Locale.ENGLISH));
			
			return newRing(innerRadius, outerRadius, distance);
		}
		
		if ("disk".equals(shape)) {
			final double radius = parseDouble(structuringElementParameters[1]);
			final Distance distance = Distance.valueOf(structuringElementParameters[2].toUpperCase(Locale.ENGLISH));
			
			return newDisk(radius, distance);
		}
		
		throw new IllegalArgumentException("Invalid structuring element shape: " + shape);
	}
	
	public static final Image getCurrentImage(final Context context) {
		final ViewFilter viewFilter = context.get(VIEW_FILTER);
		
		return viewFilter == null ? (Image) context.get(SOURCE_IMAGE) : viewFilter.getImage();
	}
	
	/**
	 * @author codistmonk (creation 2013-02-20)
	 */
	public static interface Channel {
		
		public abstract int getIndex();
		
		public abstract int getValue(final int rgba);
		
		/**
		 * @author codistmonk (creation 2013-02-20)
		 */
		public static enum Primitive implements Channel {
			
			RED {
				
				@Override
				public final int getIndex() {
					return 0;
				}
				
				@Override
				public final int getValue(final int rgba) {
					return red(rgba);
				}
				
			}, GREEN {
				
				@Override
				public final int getIndex() {
					return 1;
				}
				
				@Override
				public final int getValue(final int rgba) {
					return green(rgba);
				}
				
			}, BLUE {
				
				@Override
				public final int getIndex() {
					return 2;
				}
				
				@Override
				public final int getValue(final int rgba) {
					return blue(rgba);
				}
				
			}, ALPHA {
				
				@Override
				public final int getIndex() {
					return 3;
				}
				
				@Override
				public final int getValue(final int rgba) {
					return alpha(rgba);
				}
				
			}, INT {
				
				@Override
				public final int getIndex() {
					return 0;
				}
				
				@Override
				public final int getValue(final int rgba) {
					return rgba;
				}
				
			};
			
		}
		
		/**
		 * @author codistmonk (creation 2013-02-20)
		 */
		public static enum Synthetic implements Channel {
			
			HUE {
				
				@Override
				public final int getIndex() {
					return 0;
				}
				
				@Override
				public final int getValue(final int rgba) {
					return hue(rgba);
				}
				
			}, SATURATION {
				
				@Override
				public final int getIndex() {
					return 1;
				}
				
				@Override
				public final int getValue(final int rgba) {
					return saturation(rgba);
				}
				
			}, BRIGHTNESS {
				
				@Override
				public final int getIndex() {
					return 2;
				}
				
				@Override
				public final int getValue(final int rgba) {
					return brightness(rgba);
				}
				
			};
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-19)
	 */
	public static abstract class FromFilter extends ViewFilter {
		
		private ChannelFilter filter;
		
		protected FromFilter(final Context context) {
			super(context);
			
			this.getParameters().put("structuringElement", "disk 1 chessboard");
		}
		
		public final ChannelFilter getFilter() {
			return this.filter;
		}
		
		public final void setFilter(final ChannelFilter filter) {
			this.filter = filter;
			
			this.sourceImageChanged();
		}
		
		@Override
		protected final void sourceImageChanged() {
			final StructuringElementFilter seFilter = cast(StructuringElementFilter.class, this.getFilter());
			
			if (seFilter != null) {
				seFilter.setImage(this.getImage().getSource());
			}
		}
		
		@Override
		protected void doInitialize() {
			super.doInitialize();
			
			this.sourceImageChanged();
		}
		
		public final int[] parseStructuringElement() {
			return ViewFilter.parseStructuringElement(this.getParameters().get("structuringElement"));
		}
		
		@Override
		protected final ComplexFilter newComplexFilter() {
			return new ComplexFilter(this.splitInputChannels(), this.isOutputMonochannel()) {
				
				@Override
				public final int getNewValue(final int index, final int oldValue, final Channel channel) {
					return FromFilter.this.getFilter().getNewValue(index, oldValue, channel);
				}
				
			};
		}
		
		protected boolean isOutputMonochannel() {
			return false;
		}
		
		protected boolean splitInputChannels() {
			return true;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-03-07)
	 */
	public static abstract class ComplexFilter implements Filter {
		
		private Collection<Channel> inputChannels;
		
		private Class<? extends Channel> inputChannelClass;
		
		private final int[] buffer;
		
		private final boolean splitInputChannels;
		
		private final boolean outputMonochannel;
		
		protected ComplexFilter() {
			this(DEFAULT_SPLIT_INPUT_CHANNELS, DEFAULT_OUTPUT_MONOCHANNEL);
		}
		
		protected ComplexFilter(final boolean splitInputChannels, final boolean outputMonochannel) {
			this.buffer = new int[4];
			this.inputChannelClass = Primitive.class;
			this.splitInputChannels = splitInputChannels;
			this.outputMonochannel = outputMonochannel;
		}
		
		public final Collection<Channel> getInputChannels() {
			return this.inputChannels;
		}
		
		public final void setInputChannels(final Collection<Channel> inputChannels) {
			this.inputChannels = inputChannels;
		}
		
		public final Class<? extends Channel> getInputChannelClass() {
			return this.inputChannelClass;
		}
		
		public final void setInputChannelClass(final Class<? extends Channel> inputChannelClass) {
			this.inputChannelClass = inputChannelClass;
		}
		
		public final int[] getBuffer() {
			return this.buffer;
		}
		
		@Override
		public final int getNewValue(final int index, final int oldValue) {
			if (!this.splitInputChannels()) {
				return this.getNewValue(index, oldValue, Channel.Primitive.INT);
			}
			
			if (this.getInputChannelClass() == Primitive.class) {
				for (int i = 0; i < 4; ++i) {
					this.getBuffer()[i] = channelValue(oldValue, i);
				}
				
				int value = 0;
				
				for (final Channel channel : this.getInputChannels()) {
					value = max(0, min(255, this.getNewValue(index, oldValue, channel)));
					this.getBuffer()[channel.getIndex()] = value;
				}
				
				if (this.isOutputMonochannel() && this.getInputChannels().size() == 1) {
					return argb(255, value, value, value);
				}
				
				return argb(this.getBuffer()[ALPHA.getIndex()],
						this.getBuffer()[RED.getIndex()], this.getBuffer()[GREEN.getIndex()], this.getBuffer()[BLUE.getIndex()]);
			} else if (this.getInputChannelClass() == Synthetic.class) {
				this.getBuffer()[0] = hue(oldValue);
				this.getBuffer()[1] = saturation(oldValue);
				this.getBuffer()[2] = brightness(oldValue);
				
				int value = 0;
				
				for (final Channel channel : this.getInputChannels()) {
					value = max(0, min(255, this.getNewValue(index, oldValue, channel)));
					this.getBuffer()[channel.getIndex()] = value;
				}
				
				if (this.isOutputMonochannel() && this.getInputChannels().size() == 1) {
					return argb(255, value, value, value);
				}
				
				return Color.HSBtoRGB(this.getBuffer()[HUE.getIndex()] / 255F,
						this.getBuffer()[SATURATION.getIndex()] / 255F, this.getBuffer()[BRIGHTNESS.getIndex()] / 255F);
			}
			
			return 0;
		}
		
		public abstract int getNewValue(final int index, final int oldValue, final Channel channel);
		
		public final boolean isOutputMonochannel() {
			return this.outputMonochannel;
		}
		
		public final boolean splitInputChannels() {
			return this.splitInputChannels;
		}
		
		/**
		 * {@value}.
		 */
		public static final boolean DEFAULT_SPLIT_INPUT_CHANNELS = true;
		
		/**
		 * {@value}.
		 */
		public static final boolean DEFAULT_OUTPUT_MONOCHANNEL = false;
		
	}
	
}
