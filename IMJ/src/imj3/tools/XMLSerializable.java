package imj3.tools;

import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.ignore;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.io.Serializable;
import java.lang.reflect.Method;
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
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T objectFromXML(final Element element, final Map<Integer, Object> objects) {
		final String className = element.getTagName().replace("..", "$");
		
		if ("null".equals(className)) {
			return null;
		}
		
		if ("object".equals(className)) {
			return (T) objects.get(Integer.parseInt(element.getTextContent()));
		}
		
		if (String.class.getName().equals(className)) {
			return (T) element.getTextContent();
		}
		
		for (final Class<?> primitiveWrapperClass : array(Boolean.class, Byte.class, Short.class, Character.class, Integer.class, Long.class, Float.class, Double.class)) {
			if (primitiveWrapperClass.getName().equals(className)) {
				try {
					return (T) primitiveWrapperClass.getConstructor(String.class).newInstance(element.getTextContent());
				} catch (final Exception exception) {
					throw unchecked(exception);
				}
			}
		}
		
		final Class<T> resultClass;
		
		try {
			resultClass = (Class<T>) Class.forName(className);
		} catch (final ClassNotFoundException exception) {
			throw unchecked(exception);
		}
		
		try {
			final Method objectFromXML = resultClass.getDeclaredMethod("objectFromXML", Element.class, Map.class);
			
			return (T) objectFromXML.invoke(null, element, objects);
		} catch (final NoSuchMethodException exception) {
			ignore(exception);
		} catch (final Exception exception) {
			throw unchecked(exception);
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
						final Object key = objectFromXML((Element) XMLTools.getNode(entryNode, "key").getChildNodes().item(0), objects);
						final Object value = objectFromXML((Element) XMLTools.getNode(entryNode, "value").getChildNodes().item(0), objects);
						
						map.put(key, value);
					}
				}
			}
			
			return result;
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public static Element newElement(final String tagName, final Object object, final Document document, final Map<Object, Integer> ids) {
		final Element result = document.createElement(tagName);
		
		result.appendChild(objectToXML(object, document, ids));
		
		return result;
	}
	
}