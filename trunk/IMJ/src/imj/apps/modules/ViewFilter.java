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
import net.sourceforge.aprog.events.Variable;
import net.sourceforge.aprog.events.Variable.Listener;
import net.sourceforge.aprog.events.Variable.ValueChangedEvent;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public abstract class ViewFilter extends Plugin implements Filter {
	
	/**
	 * @author codistmonk (creation 2013-03-07)
	 */
	public static final class ComplexFilter {
		
		private Collection<Channel> inputChannels;
		
		private Class<? extends Channel> inputChannelClass;
		
		private final int[] buffer;
		
		public ComplexFilter() {
			this.buffer = new int[4];
			this.inputChannelClass = Primitive.class;
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
		
	}
	
	private final ComplexFilter complexFilter;
	
	private ViewFilter backup;
	
	private boolean backingUp;
	
	protected ViewFilter(final Context context) {
		super(context);
		this.complexFilter = new ComplexFilter();
		
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
	
	protected boolean isOutputMonochannel() {
		return false;
	}
	
	@Override
	public final int getNewValue(final int index, final int oldValue) {
		if (!this.splitInputChannels()) {
			return this.getNewValue(index, oldValue, Channel.Primitive.INT);
		}
		
		if (this.complexFilter.getInputChannelClass() == Primitive.class) {
			for (int i = 0; i < 4; ++i) {
				this.complexFilter.getBuffer()[i] = channelValue(oldValue, i);
			}
			
			int value = 0;
			
			for (final Channel channel : this.complexFilter.getInputChannels()) {
				value = max(0, min(255, this.getNewValue(index, oldValue, channel)));
				this.complexFilter.getBuffer()[channel.getIndex()] = value;
			}
			
			if (this.isOutputMonochannel() && this.complexFilter.getInputChannels().size() == 1) {
				return argb(255, value, value, value);
			}
			
			return argb(this.complexFilter.getBuffer()[ALPHA.getIndex()],
					this.complexFilter.getBuffer()[RED.getIndex()], this.complexFilter.getBuffer()[GREEN.getIndex()], this.complexFilter.getBuffer()[BLUE.getIndex()]);
		} else if (this.complexFilter.getInputChannelClass() == Synthetic.class) {
			this.complexFilter.getBuffer()[0] = hue(oldValue);
			this.complexFilter.getBuffer()[1] = saturation(oldValue);
			this.complexFilter.getBuffer()[2] = brightness(oldValue);
			
			int value = 0;
			
			for (final Channel channel : this.complexFilter.getInputChannels()) {
				value = max(0, min(255, this.getNewValue(index, oldValue, channel)));
				this.complexFilter.getBuffer()[channel.getIndex()] = value;
			}
			
			if (this.isOutputMonochannel() && this.complexFilter.getInputChannels().size() == 1) {
				return argb(255, value, value, value);
			}
			
			return Color.HSBtoRGB(this.complexFilter.getBuffer()[HUE.getIndex()] / 255F,
					this.complexFilter.getBuffer()[SATURATION.getIndex()] / 255F, this.complexFilter.getBuffer()[BRIGHTNESS.getIndex()] / 255F);
		}
		
		return 0;
	}
	
	public abstract int getNewValue(final int index, final int oldValue, final Channel channel);
	
	public final void initialize() {
		if (this.splitInputChannels()) {
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
			
			this.complexFilter.setInputChannels(newInputChannels);
			this.complexFilter.setInputChannelClass(newInputChannelClass);
		} else {
			this.complexFilter.setInputChannels(null);
			this.complexFilter.setInputChannelClass(null);
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
	
	protected boolean splitInputChannels() {
		return true;
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
			
			final Variable<Image> imageVariable = context.getVariable("image");
			
			imageVariable.addListener(new Listener<Image>() {
				
				@Override
				public final void valueChanged(final ValueChangedEvent<Image, ?> event) {
					FromFilter.this.setFilter(FromFilter.this.getFilter());
				}
				
			});
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
		
		@Override
		public final int getNewValue(final int index, final int oldValue, final Channel channel) {
			return this.getFilter().getNewValue(index, oldValue, channel);
		}
		
		public final int[] parseStructuringElement() {
			return ViewFilter.parseStructuringElement(this.getParameters().get("structuringElement"));
		}
		
	}
	
}
