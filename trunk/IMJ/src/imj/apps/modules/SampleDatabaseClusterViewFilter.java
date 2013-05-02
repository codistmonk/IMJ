package imj.apps.modules;

import static imj.database.IMJDatabaseTools.RGB;
import static imj.database.IMJDatabaseTools.newBKDatabase;
import static imj.database.Sample.processTile;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import imj.Image;
import imj.database.BKSearch.BKDatabase;
import imj.database.LinearSampler;
import imj.database.Sample;
import imj.database.Sample.Collector;
import imj.database.Sample.SampleMetric;
import imj.database.Sampler;
import imj.database.PatchDatabase;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Map;

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class SampleDatabaseClusterViewFilter extends ViewFilter {
	
	private String databaseFile;
	
	private Image source;
	
	private int tileSize;
	
	private int tileStride;
	
	private PatchDatabase<Sample> tileDatabase;
	
	private BKDatabase<Sample> bkDatabase;
	
	private final Collector collector;
	
	private Sampler sampler;
	
	private int previousTileRowIndex;
	
	private int previousTileColumnIndex;
	
	private int previousTileColor;
	
	public SampleDatabaseClusterViewFilter(final Context context) {
		super(context);
		this.collector = new Collector();
		
		this.getParameters().clear();
		
		this.getParameters().put("databaseRoot", "");
		this.getParameters().put("tileSize", "3");
		this.getParameters().put("tileStride", "3");
		this.getParameters().put("references", "45656");
	}
	
	@Override
	protected final void sourceImageChanged() {
		this.updateParameters();
	}
	
	@Override
	protected final void doInitialize() {
		this.updateParameters();
	}
	
	@Override
	protected final ComplexFilter newComplexFilter() {
		return new ComplexFilter(false, true) {
			
			@Override
			public final int getNewValue(final int index, final int oldValue, final Channel channel) {
				return SampleDatabaseClusterViewFilter.this.getClusterColor(index);
			}
			
		};
	}
	
	final int getClusterColor(final int pixel) {
		if (this.source == null) {
			return 0;
		}
		
		assert this.source == this.sampler.getImage();
		
		final int imageRowCount = this.source.getRowCount();
		final int imageColumnCount = this.source.getColumnCount();
		final int pixelRowIndex = pixel / imageColumnCount;
		final int pixelColumnIndex = pixel % imageColumnCount;
		final int tileRowIndex = pixelRowIndex - (pixelRowIndex % this.tileStride);
		final int tileColumnIndex = pixelColumnIndex - (pixelColumnIndex % this.tileStride);
		
		if (this.previousTileRowIndex == tileRowIndex && this.previousTileColumnIndex == tileColumnIndex) {
			return this.previousTileColor;
		}
		
		this.previousTileRowIndex = tileRowIndex;
		this.previousTileColumnIndex = tileColumnIndex;
		final int tileEndRowIndex = tileRowIndex + this.tileSize;
		final int tileEndColumnIndex = tileColumnIndex + this.tileSize;
		
		if (imageRowCount < tileEndRowIndex || imageColumnCount < tileEndColumnIndex) {
			return this.previousTileColor = 0;
		}
		
		processTile(this.sampler, tileRowIndex, tileColumnIndex, this.tileSize, this.tileSize);
		
		assert null != this.collector.getSample().getKey();
		
		final Sample match = this.bkDatabase.findClosest(this.collector.getSample());
		final String cls = match == null || match.getClasses().isEmpty() ? null : match.getClasses().iterator().next();
		
		this.previousTileColor = cls == null ? 0 : 0xFF000000 | cls.hashCode();
		
		return this.previousTileColor;
	}
	
	private final boolean updateParameters() {
		this.tileSize = this.getIntParameter("tileSize");
		this.tileStride = this.getIntParameter("tileStride");
		final int lod = this.getContext().get("lod");
		
		this.updateSource();
		
		final String oldDatabaseFile = this.databaseFile;
		this.databaseFile = this.getParameters().get("databaseRoot") + "/" + this.getParameters().get("references") +
				".lod" + lod + ".sdb.rgb.h" + this.tileSize + "w" + this.tileSize + "dx" + this.tileStride + "dy" + this.tileStride + ".jo";
		
		if (Tools.equals(oldDatabaseFile, this.databaseFile)) {
			return false;
		}
		
		debugPrint(oldDatabaseFile, this.databaseFile);
		
		this.previousTileRowIndex = -1;
		this.previousTileColumnIndex = -1;
		
		try {
			final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(this.databaseFile));
			
			try {
				final Map<Object, Object> database = (Map<Object, Object>) ois.readObject();
				this.tileDatabase = (PatchDatabase<Sample>) database.get("samples");
				this.bkDatabase = newBKDatabase(this.tileDatabase, SampleMetric.EUCLIDEAN);
			} finally {
				ois.close();
			}
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
		
		return true;
	}
	
	private final void updateSource() {
		this.source = this.getImage().getSource();
		
		if (this.source != null) {
			debugPrint(this.source.getRowCount(), this.source.getColumnCount());
			this.sampler = new LinearSampler(this.source, null, RGB, this.collector);
		}
	}
	
}
