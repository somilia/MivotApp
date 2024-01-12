package utils;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author ierrami
 *
 */
public class XMLUtils {

	public XMLUtils() {
		// TODO Auto-generated constructor stub
	}

	public static Element getNextElement(Element el) {
		Node nd = el.getNextSibling();
		while (nd != null) {
			if (nd.getNodeType() == Node.ELEMENT_NODE) {
				return (Element) nd;
			}
			nd = nd.getNextSibling();
		}
		return null;
	}

	public static Node getNode(Node parentNode, String currentNodeString, String dmrole) {

		for (int i = 0; i < parentNode.getChildNodes().getLength(); i++) {
			if (parentNode.getChildNodes().item(i).getNodeName().equals(currentNodeString)) {
				Element currentElement = (Element) parentNode.getChildNodes().item(i);
				String currentdmrole = currentElement.getAttribute("dmrole");
				if (currentdmrole.equals(dmrole)) {
					return currentElement;

				}
			}
		}
		return null;
	}

	public static Element createElement(Element parent, String name) {
		Document document;
		Element element;

		document = parent.getOwnerDocument();
		element = document.createElement(name);

		parent.appendChild(element);
		return element;
	}
	
	public static void fillAssociatedMeasureCollection(Node associated_Measure, ArrayList<String> associatedMeasure) {
		if (associated_Measure != null) {
			System.out.println("Nom de l'élément de la mesure associée " + associated_Measure.getNodeName());
			int numberOfMeasure = associatedMeasure.size();
			//System.out.println("Nombre " + numberOfMeasure);
			if (numberOfMeasure == 1) {
				Element refElement = XMLUtils.createElement((Element) associated_Measure, "REFERENCE");
				refElement.setAttribute("dmref", associatedMeasure.get(0));
			} else if (numberOfMeasure > 1) {
				for (int i = 0; i < associatedMeasure.size(); i++) {
					Element refElement = XMLUtils.createElement((Element) associated_Measure, "REFERENCE");
					refElement.setAttribute("dmref", associatedMeasure.get(i));
				}
			}
			System.out.println("Setting associated_measures");
		}
	}
	
	public static void setRefOrValue(Element attributeNode, String value) {
		if( value.startsWith("@") == true) {
			attributeNode.setAttribute("ref", value.replace("@", ""));
			attributeNode.removeAttribute("value");
		} else {
			attributeNode.setAttribute("value", value);
			attributeNode.removeAttribute("ref");
			
		}
	}
	
	/**this method is used to convert XML to string
	 * @param doc
	 * @return
	 */
	public static String xmlToString(Document doc) {

		String xmlString = null;

		try {
			Source source = new DOMSource(doc);
			StringWriter stringWriter = new StringWriter();
			Result result = new StreamResult(stringWriter);
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.transform(source, result);
			xmlString = stringWriter.getBuffer().toString();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		return xmlString;
	}


	/**
	 * @param xml    the XML string we want to beautify
	 * @param indent the number of indentations we want
	 * @return a beautified XML string
	 */
	public static String toPrettyString(String xml, int indent) {
		try {
			// Turn xml string into a document
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
					.parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));

			// Remove whitespaces outside tags
			document.normalize();
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']", document,
					XPathConstants.NODESET);

			for (int i = 0; i < nodeList.getLength(); ++i) {
				Node node = nodeList.item(i);
				node.getParentNode().removeChild(node);
			}

			// Setup pretty print options
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", indent);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			// Return pretty print xml string
			StringWriter stringWriter = new StringWriter();
			transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
			return stringWriter.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
