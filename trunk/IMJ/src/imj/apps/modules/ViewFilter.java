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
	
	private ComplexFilter complexFilter;
	
	private ViewFilter backup;
	
	private boolean backingUp;
	
	protected ViewFilter(final Context context) {
		super(context);
		
		this.getParameters().put(PARAMETER_CHANNELS, "red green blue");
		
		context.getVariable("image").addListener(new Listener<Object>() {
			
			@Override
			public final void valueChanged(final ValueChangedEvent<Object, ?> event) {
				if (event.getOldValue() == event.getNewValue()) {
					return;
				}
				
				final boolean thisViewFilterIsApplying = context.get("viewFilter") == ViewFilter.this;
				
				if (thisViewFilterIsApplying) {
					ViewFilter.this.cancel();
				}
				
				if (ViewFilter.this.isBackingUp()) {
					ViewFilter.this.clearBackup();
					ViewFilter.this.backup();
				}
				
				if (thisViewFilterIsApplying) {
					ViewFilter.this.initialize();
					ViewFilter.this.apply();
				}
			}
			
		});
	}
	
	public final ComplexFilter getComplexFilter() {
		return this.complexFilter;
	}
	
	protected abstract ComplexFilter newComplexFilter();
	
	public final void initialize() {
		if (this.getComplexFilter() == null) {
			this.complexFilter = this.newComplexFilter();
		}
		
		if (this.getComplexFilter().splitInputChannels()) {
			final String[] inputChannelAsStrings = this.getParameters().get(PARAMETER_CHANNELS).split("\\s+");
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
	
	protected void doInitialize() {
		// NOP
	}
	
	@Override
	public final void apply() {
		final Context context = this.getContext();
		
		context.set("viewFilter", null);
		context.set("viewFilter", this);
		
		fireUpdate(this.getContext(), "image");
	}
	
	@Override
	public final void backup() {
		this.backup = this.getContext().get("viewFilter");
		this.backingUp = true;
	}
	
	@Override
	public final void cancel() {
		this.getContext().set("viewFilter", this.backup);
		fireUpdate(this.getContext(), "image");
	}
	
	@Override
	public final void clearBackup() {
		this.backup = null;
		this.backingUp = false;
	}
	
	public final boolean isBackingUp() {
		return this.backingUp;
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
	 * {@value}.
	 */
	public static final String PARAMETER_CHANNELS = "channels";
	
	public static final Channel parseChannel(final String string) {
		try {
			return Channel.Primitive.valueOf(string);
		} catch (final Exception exception) {
			ignore(exception);
			
			return Channel.Synthetic.valueOf(string);
		}
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
			
			if (filter instanceof StructuringElementFilter) {
				final Image image = this.getContext().get("image");
				final FilteredImage filteredImage = cast(FilteredImage.class, image);
				
				((StructuringElementFilter) filter).setImage(filteredImage != null ? filteredImage.getSource() : image);
			}
		}
		
		public final int[] parseStructuringElement() {
			return ViewFilter.parseStructuringElement(this.getParameters().get("structuringElement"));
		}
		
		@Override
		protected final ComplexFilter newComplexFilter() {
			return new ComplexFilter(this.isOutputMonochannel(), this.splitInputChannels()) {
				
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
