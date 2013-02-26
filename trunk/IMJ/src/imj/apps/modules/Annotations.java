package imj.apps.modules;

import static java.lang.Double.parseDouble;
import static java.lang.Long.parseLong;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static net.sourceforge.aprog.tools.Tools.usedMemory;

import imj.IntList;
import imj.apps.modules.Annotations.Annotation.Region;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sourceforge.aprog.tools.TicToc;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author codistmonk (creation 2013-02-26)
 */
public final class Annotations extends GenericTreeNode<imj.apps.modules.Annotations.Annotation> {;
	
	private double micronsPerPixel;
	
	public final List<Annotation> getAnnotations() {
		return this.getItems();
	}
	
	public final double getMicronsPerPixel() {
		return this.micronsPerPixel;
	}
	
	public final void setMicronsPerPixel(final double micronsPerPixel) {
		this.micronsPerPixel = micronsPerPixel;
	}
	
	public static Annotations fromXML(final String xmlFileName) {
		final File annotationsFile = new File(xmlFileName);
		
		try {
			return Annotations.fromXML(annotationsFile.exists() ? new FileInputStream(annotationsFile) : null);
		} catch (final FileNotFoundException exception) {
			throw unchecked(exception);
		}
	}
	
	public static final Annotations fromXML(final InputStream xmlInput) {
		final Annotations result = new Annotations();
		
		if (xmlInput != null) {
			try {
				final TicToc timer = new TicToc();
				
				debugPrint("Parsing XML annotations", "(", new Date(timer.tic()), ")");
				
				final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
				
				parser.parse(xmlInput, new DefaultHandler() {
					
					private Annotation annotation;
					
					private Region region;
					
					private final IntList xs;
					
					private final IntList ys;
					
					{
						this.xs = new IntList();
						this.ys = new IntList();
					}
					
					@Override
					public final void startElement(final String uri, final String localName,
							final String qName, final Attributes attributes) throws SAXException {
						if ("Annotation".equals(qName)) {
							this.annotation = result.new Annotation();
							this.annotation.setLineColor(new Color((int) parseLong(attributes.getValue("LineColor"))));
						} else if ("Region".equals(qName)) {
							this.region = this.annotation.new Region();
							this.region.setZoom(parseDouble(attributes.getValue("Zoom")));
							this.xs.clear();
							this.ys.clear();
						} else if ("Vertex".equals(qName)) {
							this.xs.add((int) (parseDouble(attributes.getValue("X"))));
							this.ys.add((int) (parseDouble(attributes.getValue("Y"))));
						}
					}
					
					@Override
					public final void endElement(final String uri, final String localName,
							final String qName) throws SAXException {
						if ("Region".equals(qName)) {
							this.region.setShape(new Polygon(this.xs.toArray(), this.ys.toArray(), this.xs.size()));
						}
					}
					
				});
				
				debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2013-02-26)
	 */
	public final class Annotation extends GenericTreeNode<Region> {
		
		private Color lineColor;
		
		public Annotation() {
			Annotations.this.getAnnotations().add(this);
			this.setUserObject(this.getClass().getSimpleName() + " " + (Annotations.this.getAnnotations().indexOf(this) + 1));
		}
		
		public final List<Region> getRegions() {
			return this.getItems();
		}
		
		public final Color getLineColor() {
			return this.lineColor;
		}
		
		public final void setLineColor(final Color lineColor) {
			this.lineColor = lineColor;
		}
		
		/**
		 * @author codistmonk (creation 2013-02-26)
		 */
		public final class Region extends DefaultMutableTreeNode {
			
			private double zoom;
			
			private double length;
			
			private double area;
			
			private double lengthInMicrons;
			
			private double areaInSquareMicrons;
			
			private Shape shape;
			
			public Region() {
				Annotation.this.getRegions().add(this);
				this.setUserObject(this.getClass().getSimpleName() + " " + (Annotation.this.getRegions().indexOf(this) + 1));
			}
			
			public final double getZoom() {
				return this.zoom;
			}
			
			public final void setZoom(final double zoom) {
				this.zoom = zoom;
			}
			
			public final double getLength() {
				return this.length;
			}
			
			public final void setLength(final double length) {
				this.length = length;
			}
			
			public final double getArea() {
				return this.area;
			}
			
			public final void setArea(final double area) {
				this.area = area;
			}
			
			public final double getLengthInMicrons() {
				return this.lengthInMicrons;
			}
			
			public final void setLengthInMicrons(final double lengthInMicrons) {
				this.lengthInMicrons = lengthInMicrons;
			}
			
			public final double getAreaInSquareMicrons() {
				return this.areaInSquareMicrons;
			}
			
			public final void setAreaInSquareMicrons(final double areaInSquareMicrons) {
				this.areaInSquareMicrons = areaInSquareMicrons;
			}
			
			public final Shape getShape() {
				return this.shape;
			}
			
			public final void setShape(final Shape shape) {
				this.shape = shape;
			}
			
		}
		
	}
	
}
