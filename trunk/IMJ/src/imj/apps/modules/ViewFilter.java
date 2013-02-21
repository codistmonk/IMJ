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
	
	private Collection<Channel> inputChannels;
	
	private Class<? extends Channel> inputChannelClass;
	
	private final int[] buffer;
	
	private ViewFilter backup;
	
	protected ViewFilter(final Context context) {
		super(context);
		this.buffer = new int[4];
		
		this.getParameters().put("inputChannels", "red green blue");
		this.inputChannelClass = Primitive.class;
	}
	
	@Override
	public final int getNewValue(final int index, final int oldValue) {
		if (!this.splitInputChannels()) {
			return this.getNewValue(index, oldValue, null);
		}
		
		if (this.inputChannelClass == Primitive.class) {
			for (int i = 0; i < 4; ++i) {
				this.buffer[i] = channelValue(oldValue, i);
			}
			
			for (final Channel channel : this.inputChannels) {
				this.buffer[channel.getIndex()] = max(0, min(255, this.getNewValue(index, oldValue, channel)));
			}
			
			return argb(this.buffer[ALPHA.getIndex()],
					this.buffer[RED.getIndex()], this.buffer[GREEN.getIndex()], this.buffer[BLUE.getIndex()]);
		} else if (this.inputChannelClass == Synthetic.class) {
			this.buffer[0] = hue(oldValue);
			this.buffer[1] = saturation(oldValue);
			this.buffer[2] = brightness(oldValue);
			
			for (final Channel channel : this.inputChannels) {
				this.buffer[channel.getIndex()] = max(0, min(255, this.getNewValue(index, oldValue, channel)));
			}
			
			return Color.HSBtoRGB(this.buffer[HUE.getIndex()] / 255F,
					this.buffer[SATURATION.getIndex()] / 255F, this.buffer[BRIGHTNESS.getIndex()] / 255F);
		}
		
		return 0;
	}
	
	public abstract int getNewValue(final int index, final int oldValue, final Channel channel);
	
	protected boolean splitInputChannels() {
		return true;
	}
	
	@Override
	public final void apply() {
		{
			final String[] inputChannelAsStrings = this.getParameters().get("inputChannels").split("\\s+");
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
			
			this.inputChannels = newInputChannels;
			this.inputChannelClass = newInputChannelClass;
		}
		
		final Context context = this.getContext();
		
		context.set("viewFilter", null);
		context.set("viewFilter", this);
		fireUpdate(this.getContext(), "image");
	}
	
	@Override
	public final void backup() {
		this.backup = this.getContext().get("viewFilter");
	}
	
	@Override
	public final void cancel() {
		this.getContext().set("viewFilter", this.backup);
		fireUpdate(this.getContext(), "image");
	}
	
	@Override
	public final void clearBackup() {
		this.backup = null;
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
	
	public static final Channel parseChannel(final String string) {
		try {
			return Channel.Primitive.valueOf(string);
		} catch (final Exception exception) {
			ignore(exception);
			
			return Channel.Synthetic.valueOf(string);
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
			final String[] structuringElementParameters = this.getParameters().get("structuringElement").trim().split("\\s+");
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
		
	}
	
}
