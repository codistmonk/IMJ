package imj3.tools;

import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.ignore;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import imj3.tools.AwtGeometryTools.PathElement;

import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.aprog.tools.Tools;
import net.sourceforge.aprog.xml.XMLTools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author codistmonk (creation 2015-03-15)
 */
public abstract interface XMLSerializable extends Serializable {
	
	public default Document toXML() {
		final Document result = XMLTools.newDocument();
		
		result.appendChild(this.toXML(result, new HashMap<>()));
		
		return result;
	}
	
	public default Element toXML(final Document document, final Map<Object, Integer> ids) {
		final Element result = document.createElement(this.getClass().getName().replace("$", ".."));
		final Integer id = ids.computeIfAbsent(this, o -> ids.size());
		
		result.setAttribute("id", id.toString());
		
		return result;
	}
	
	public default XMLSerializable fromXML(final Element xml, final Map<Integer, Object> objects) {
		final Integer id = Integer.decode(xml.getAttribute("id"));
		
		if (null != objects.put(id, this)) {
			Tools.debugError("Id clash detected");
		}
		
		return this;
	}
	
	public static final String ENCLOSING_INSTANCE_ID = "enclosingInstanceId";
	
	public static Element getFirstElement(final Node node) {
		Node result = node.getFirstChild();
		
		while (!(result instanceof Element)) {
			result = result.getNextSibling();
		}
		
		return (Element) result;
	}
	
	public static Element newElement(final String tagName, final Object object, final Document document, final Map<Object, Integer> ids) {
		final Element result = document.createElement(tagName);
		
		result.appendChild(XMLSerializable.objectToXML(object, document, ids));
		
		return result;
	}
	
	public static Element objectToXML(final Object object) {
		return objectToXML(object, XMLTools.newDocument(), new HashMap<>());
	}
	
	public static Element objectToXML(final Object object, final Document document, final Map<Object, Integer> ids) {
		Integer id = ids.get(object);
		
		if (id != null) {
			final Element result = document.createElement("object");
			
			result.setTextContent(id.toString());
			
			return result;
		}
		
		if (object == null) {
			return document.createElement("null");
		}
		
		ids.put(object, id = ids.size());
		
		if (object instanceof Enum<?>) {
			final Element result = document.createElement(object.getClass().getEnclosingClass().getName().replace("$", ".."));
			
			result.setAttribute("id", id.toString());
			result.setTextContent(object.toString());
			
			return result;
		}
		
		final XMLSerializable serializable = cast(XMLSerializable.class, object);
		
		if (serializable != null) {
			return serializable.toXML(document, ids);
		}
		
		final Element result = document.createElement(object.getClass().getName().replace("$", ".."));
		
		result.setAttribute("id", id.toString());
		
		{
			final Map<?, ?> map = cast(Map.class, object);
			
			if (map != null) {
				for (final Map.Entry<?, ?> entry : map.entrySet()) {
					final Element entryElement = (Element) result.appendChild(document.createElement("entry"));
					final Element keyElement = (Element) entryElement.appendChild(document.createElement("key"));
					final Element valueElement = (Element) entryElement.appendChild(document.createElement("value"));
					
					keyElement.appendChild(objectToXML(entry.getKey(), document, ids));
					valueElement.appendChild(objectToXML(entry.getValue(), document, ids));
				}
			}
		}
		
		{
			final String string = cast(String.class, object);
			
			if (string != null) {
				result.setTextContent(string);
			}
		}
		
		{
			final Number number = cast(Number.class, object);
			
			if (number != null) {
				result.setTextContent(number.toString());
			}
		}
		
		{
			final Area area = cast(Area.class, object);
			
			if (area != null) {
				final PathIterator pathIterator = area.getPathIterator(new AffineTransform());
				
				result.setAttribute("windingRule", "" + pathIterator.getWindingRule());
				
				for (final PathElement pathElement : AwtGeometryTools.iterable(pathIterator)) {
					result.appendChild(pathElement.toXML(document, ids));
				}
			}
		}
		
		return result;
	}
	
	public static <T> T objectFromXML(final File file) {
		try (final InputStream input = new FileInputStream(file)) {
			return objectFromXML(XMLTools.parse(input).getDocumentElement(), new HashMap<>());
		} catch (final IOException exception) {
			throw unchecked(exception);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T objectFromXML(final Element element, final Map<Integer, Object> objects) {
		final String className = element.getTagName().replace("..", "$");
		
		if ("null".equals(className)) {
			return null;
		}
		
		if ("object".equals(className)) {
			return (T) objects.get(Integer.decode(element.getTextContent()));
		}
		
		final Integer id = Integer.decode(element.getAttribute("id"));
		
		if (String.class.getName().equals(className)) {
			final T result = (T) element.getTextContent();
			
			objects.put(id, result);
			
			return result;
		}
		
		for (final Class<?> primitiveWrapperClass : array(Boolean.class, Byte.class, Short.class, Character.class, Integer.class, Long.class, Float.class, Double.class)) {
			if (primitiveWrapperClass.getName().equals(className)) {
				try {
					final T result = (T) primitiveWrapperClass.getConstructor(String.class).newInstance(element.getTextContent());
					
					objects.put(id, result);
					
					return result;
				} catch (final Exception exception) {
					throw unchecked(exception);
				}
			}
		}
		
		if (Area.class.getName().equals(className)) {
			final Path2D path = new Path2D.Float();
			
			for (final Node child : XMLTools.getNodes(element, "*")) {
				if (child instanceof Element) {
					((PathElement) objectFromXML((Element) child, objects)).update(path);
				}
			}
			
			return (T) new Area(path);
		}
		
		final Class<T> resultClass;
		
		try {
			resultClass = (Class<T>) Class.forName(className);
		} catch (final ClassNotFoundException exception) {
			throw unchecked(exception);
		}
		
		try {
			final Method objectFromXML = resultClass.getMethod("objectFromXML", Element.class, Map.class);
			
			return (T) objectFromXML.invoke(null, element, objects);
		} catch (final NoSuchMethodException exception) {
			ignore(exception);
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
		
		if (element.getFirstChild() != null && element.getFirstChild().getNodeType() == Node.TEXT_NODE) {
			try {
				final T result = resultClass.getConstructor(String.class).newInstance(element.getTextContent());
				
				objects.put(id, result);
				
				return result;
			} catch (final Exception exception) {
				ignore(exception);
			}
			
			try {
				final Number number = new BigDecimal(element.getTextContent());
				
				for (final Constructor<?> constructor : resultClass.getConstructors()) {
					if (constructor.getParameterCount() != 1) {
						continue;
					}
					
					final Class<?> parameterType = constructor.getParameterTypes()[0];
					final T result;
					
					if (parameterType == byte.class || parameterType == Byte.class) {
						result = (T) constructor.newInstance(number.byteValue());
					} else if (parameterType == short.class || parameterType == Short.class) {
						result = (T) constructor.newInstance(number.shortValue());
					} else if (parameterType == int.class || parameterType == Integer.class) {
						result = (T) constructor.newInstance(number.intValue());
					} else if (parameterType == long.class || parameterType == Long.class) {
						result = (T) constructor.newInstance(number.intValue());
					} else if (parameterType == float.class || parameterType == Float.class) {
						result = (T) constructor.newInstance(number.floatValue());
					} else if (parameterType == double.class || parameterType == Double.class) {
						result = (T) constructor.newInstance(number.doubleValue());
					} else {
						continue;
					}
					
					objects.put(id, result);
					
					return result;
				}
			} catch (final Exception exception) {
				ignore(exception);
			}
		}
		
		try {
			final String enclosingInstanceIdAsString = element.getAttribute(ENCLOSING_INSTANCE_ID);
			final T result;
			
			if (enclosingInstanceIdAsString != null && !enclosingInstanceIdAsString.isEmpty()) {
				final Object enclosingInstance = objects.get(Integer.decode(enclosingInstanceIdAsString));
				result = resultClass.getConstructor(enclosingInstance.getClass()).newInstance(enclosingInstance);
			} else if (resultClass.isEnum()) {
				result = (T) Enum.valueOf((Class<Enum>) resultClass, element.getTextContent());
			} else {
				result = resultClass.newInstance();
			}
			
			final XMLSerializable serializable = cast(XMLSerializable.class, result);
			
			if (serializable != null) {
				return (T) serializable.fromXML(element, objects);
			}
			
			{
				final Map<Object, Object> map = cast(Map.class, result);
				
				if (map != null) {
					for (final Node entryNode : XMLTools.getNodes(element, "entry")) {
						final Object key = objectFromXML(XMLSerializable.getFirstElement(XMLTools.getNode(entryNode, "key")), objects);
						final Object value = objectFromXML(XMLSerializable.getFirstElement(XMLTools.getNode(entryNode, "value")), objects);
						
						map.put(key, value);
					}
				}
			}
			
			objects.put(id, result);
			
			return result;
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
}