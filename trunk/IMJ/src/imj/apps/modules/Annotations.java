package imj.apps.modules;

import static java.lang.Double.parseDouble;
import static java.lang.Long.parseLong;
import static java.util.Locale.ENGLISH;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.apps.modules.Annotations.Annotation.Region;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author codistmonk (creation 2013-02-26)
 */
public final class Annotations extends GenericTreeNode<imj.apps.modules.Annotations.Annotation> {
	
	private double micronsPerPixel;
	
	public Annotations() {
		this.setMicronsPerPixel(0.5);
	}
	
	public final List<Annotation> getAnnotations() {
		return this.getItems();
	}
	
	public final List<Region> collectAllRegions() {
		final List<Region> result = new ArrayList<Region>();
		
		for (final Annotation annotation : this.getAnnotations()) {
			result.addAll(annotation.getRegions());
		}
		
		return result;
	}
	
	public final double getMicronsPerPixel() {
		return this.micronsPerPixel;
	}
	
	public final void setMicronsPerPixel(final double micronsPerPixel) {
		this.micronsPerPixel = micronsPerPixel;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 6894550580896794510L;
	
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
					
					private final List<String> elements = new LinkedList<String>();
					
					@Override
					public final void startElement(final String uri, final String localName,
							final String qName, final Attributes attributes) throws SAXException {
						this.elements.add(0, qName);
						
						if ("Annotations".equals(qName)) {
							result.setMicronsPerPixel(parseDouble(attributes.getValue("MicronsPerPixel")));
						} else if ("Annotation".equals(qName)) {
							this.annotation = result.new Annotation();
							this.annotation.setLineColor(new Color((int) parseLong(attributes.getValue("LineColor"))));
							this.annotation.setSelected(parseBoolean(attributes.getValue("Selected").trim().toLowerCase(ENGLISH)));
							this.annotation.setVisible(parseBoolean(attributes.getValue("Visible").trim().toLowerCase(ENGLISH)));
						} else if ("Region".equals(qName)) {
							this.region = this.annotation.new Region();
							this.region.setZoom(parseDouble(attributes.getValue("Zoom")));
							this.region.setArea(parseDouble(attributes.getValue("Area")));
							this.region.setAreaInSquareMicrons(parseDouble(attributes.getValue("AreaMicrons")));
							this.region.setLength(parseDouble(attributes.getValue("Length")));
							this.region.setLengthInMicrons(parseDouble(attributes.getValue("LengthMicrons")));
							final String negative = attributes.getValue("NegativeROA").trim().toLowerCase(ENGLISH);
							this.region.setNegative(parseBoolean(negative));
						} else if ("Vertex".equals(qName)) {
							this.region.getVertices().add(new Point2D.Float(
									(float) (parseDouble(attributes.getValue("X"))),
									(float) (parseDouble(attributes.getValue("Y")))));
						} else if ("Attribute".equals(qName)) {
							if (startsWith(this.elements, "Attribute", "Attributes", "Annotation")) {
								final String name = attributes.getValue("Name");
								
								if (name != null && !name.trim().isEmpty()) {
									this.annotation.setUserObject(name);
								}
							}
						}
					}
					
					@Override
					public final void endElement(final String uri, final String localName,
							final String qName) throws SAXException {
						this.elements.remove(0);
					}
					
				});
				
				debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		}
		
		return result;
	}
	
	public static final <E> boolean startsWith(final Iterable<E> elements, final E... prefix) {
		int i = 0;
		
		for (final E element : elements) {
			if (prefix.length <= i) {
				break;
			}
			
			if (!Tools.equals(element, prefix[i])) {
				return false;
			}
			
			++i;
		}
		
		return true;
	}
	
	public static final void toXML(final Annotations annotations, final PrintStream out) {
		out.println("<Annotations" + attribute("MicronsPerPixel", annotations.getMicronsPerPixel()) + ">");
		
		int annotationId = 1;
		
		for (final Annotation annotation : annotations.getAnnotations()) {
			out.println("  <Annotation" +
					attribute("Id", annotationId) + 
					attribute("ReadOnly", 0) +
					attribute("NameReadOnly", 0) +
					attribute("LineColorReadOnly", 0) +
					attribute("Incremental", 0) +
					attribute("Type", 4) +
					attribute("LineColor", annotation.getLineColor().getRGB() & 0x00FFFFFF) +
					attribute("Visible", formatBoolean01(annotation.isVisible())) +
					attribute("Selected", formatBoolean01(annotation.isSelected())) +
					attribute("MarkupImagePath", "") +
					attribute("MacroName", "") +
			">");
			
			out.println("    <Attributes>");
			out.println("      <Attribute" +
					attribute("Name", "" + annotation.getUserObject()) + attribute("Id", 0) + attribute("Value", "") + "/>");
			out.println("    </Attributes>");
			
			out.println("    <Regions>");
			out.println("      <RegionAttributeHeaders>");
			out.println("        <AttributeHeader" +
					attribute("Id", 9999) + attribute("Name", "Region") + attribute("ColumnWidth", -1) + "/>");
			out.println("        <AttributeHeader" +
					attribute("Id", 9997) + attribute("Name", "Length") + attribute("ColumnWidth", -1) + "/>");
			out.println("        <AttributeHeader" +
					attribute("Id", 9996) + attribute("Name", "Area") + attribute("ColumnWidth", -1) + "/>");
			out.println("        <AttributeHeader" +
					attribute("Id", 9998) + attribute("Name", "Text") + attribute("ColumnWidth", -1) + "/>");
			out.println("        <AttributeHeader" +
					attribute("Id", 1) + attribute("Name", "Description") + attribute("ColumnWidth", -1) + "/>");
			out.println("      </RegionAttributeHeaders>");
			
			int regionId = 1;
			
			for (final Region region : annotation.getRegions()) {
				out.println("      <Region" +
						attribute("Id", regionId) +
						attribute("Zoom", 0) +
						attribute("Selected", 0) +
						attribute("ImageLocation", "") +
						attribute("ImageFocus", 0) +
						attribute("Length", region.getLength()) +
						attribute("Area", region.getArea()) +
						attribute("LengthMicrons", region.getLengthInMicrons()) +
						attribute("AreaMicrons", region.getAreaInSquareMicrons()) +
						attribute("Text", "") +
						attribute("NegativeROA", formatBoolean01(region.isNegative())) +
						attribute("InputRegionId", 0) +
						attribute("Analyze", 1) +
						attribute("DisplayId", regionId) +
				">");
				
				out.println("        <Attributes/>");
				
				out.println("        <Vertices>");
				
				for (final Point2D vertex : region.getVertices()) {
					out.println("          <Vertex" + attribute("X", vertex.getX()) + attribute("Y", vertex.getY()) + "/>");
				}
				
				out.println("        </Vertices>");
				
				out.println("      </Region>");
			}
			
			out.println("    </Regions>");
			out.println("    <Posts/>");
			out.println("  </Annotation>");
			
			++annotationId;
		}
		
		out.println("</Annotations>");
	}
	
	public static final String attribute(final String name, final Object value) {
		return " " + name + "=\"" + value + "\"";
	}
	
	public static final boolean parseBoolean(final String attributeValue) {
		return !("0".equals(attributeValue) || "false".equals(attributeValue) || "no".equals(attributeValue));
	}
	
	public static final String formatBoolean01(final boolean value) {
		return value ? "1" : "0";
	}
	
	/**
	 * Note: the <code>selected</code> attribute maps to a XML attribute and may be ignored by GUIs.
	 * 
	 * @author codistmonk (creation 2013-02-26)
	 */
	public final class Annotation extends GenericTreeNode<Region> {
		
		private Color lineColor;
		
		private boolean visible;
		
		private boolean selected;
		
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
		
		public final boolean isVisible() {
			return this.visible;
		}
		
		public final void setVisible(final boolean visible) {
			this.visible = visible;
		}
		
		public final boolean isSelected() {
			return this.selected;
		}
		
		public final void setSelected(final boolean selected) {
			this.selected = selected;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 6031044355822486034L;
		
		/**
		 * @author codistmonk (creation 2013-02-26)
		 */
		public final class Region extends DefaultMutableTreeNode {
			
			private double zoom;
			
			private double length;
			
			private double area;
			
			private double lengthInMicrons;
			
			private double areaInSquareMicrons;
			
			private final List<Point2D.Float> vertices;
			
			private boolean negative;
			
			public Region() {
				this.vertices = new ArrayList<Point2D.Float>();
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
			
			public final List<Point2D.Float> getVertices() {
				return this.vertices;
			}
			
			public final boolean isNegative() {
				return this.negative;
			}
			
			public final void setNegative(final boolean negative) {
				this.negative = negative;
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -6621209121896413465L;
			
		}
		
	}
	
}
