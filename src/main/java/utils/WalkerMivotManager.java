package utils;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;

import ModelBaseInit.ModelBase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

public class WalkerMivotManager {
    /* class used to process walker using only static function */

    public static String FilesPath = "src/main/webapp/mivot_snippets/";

    public static void changeAttributeValue(TreeWalker walker, String attributeName, String attributeValue, String attributeToChange, String value) {
        /* the function lookForAttribute(attributeName, attributeValue) and change the value of attributeToChange into attributeToChange */
        Node node = walker.getCurrentNode();
        if (node != null && node.getAttributes().getNamedItem("ref").getNodeValue().equals("@@@@@")) {
            node.getAttributes().getNamedItem(attributeToChange).setNodeValue(value);
        }

    }
    public static void printWalker(TreeWalker walker) {
        walker.setCurrentNode(walker.getRoot());
        do {
            Node current = walker.getCurrentNode();
            System.out.println(current.getNodeName());
            if (current.hasAttributes()) {
                NamedNodeMap attributes = current.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    System.out.println(attributes.item(i).getNodeName() + " : " + attributes.item(i).getNodeValue());
                }
            }
        } while (walker.nextNode() != null);
        walker.setCurrentNode(walker.getRoot());
    }

    public static void cleanUpNode(TreeWalker walker) throws ParserConfigurationException, SAXException, IOException, TransformerException, URISyntaxException {

        Node parent = walker.getRoot();
        String value_attribute = "@@@@@";
        for (int i = 0; i < parent.getChildNodes().getLength(); i++) {

            Node child = parent.getChildNodes().item(i);
//            for (int j = 0; j < child.getChildNodes().getLength(); j++) {
//                Node children = child.getChildNodes().item(j);
//                if (children.getNodeName().equals("REPORT")) {
//                    children.setTextContent("Automatically generated. NotSet instance have been removed");
//                }
//                removeUnsetElements(children, value_attribute);
//            }

            if (child.getNodeName().equals("REPORT")) {
                child.setTextContent("Automatically generated. NotSet instance have been removed");
            }
            if (child.getNodeName().equals("INSTANCE") || child.getNodeName().equals("ATTRIBUTE")) {
                removeUnsetElements(child, value_attribute);
            }

        }
    }

    /**
     * look for the element with rchildef="NotSet"
     *
     * @param instanceNodeElement
     * @param value
     */
    public static void removeUnsetElements(Node instanceNodeElement, String value) {

        if (instanceNodeElement.hasChildNodes()) {

            for (int i = 0; i < instanceNodeElement.getChildNodes().getLength(); i++) {

                if (instanceNodeElement.getNodeType() == Node.ELEMENT_NODE) {
                    Node childInstanceNodeElement = instanceNodeElement.getChildNodes().item(i);

                    if (childInstanceNodeElement.getNodeName().equals("INSTANCE")) {
                        removeUnsetElements(childInstanceNodeElement, value);
                    }
                    else if (childInstanceNodeElement.getNodeName().equals("ATTRIBUTE")) {
                        String attribute_ref = ((Element) childInstanceNodeElement).getAttribute("ref");
                        String attribute_value = ((Element) childInstanceNodeElement).getAttribute("value");

                        if ((attribute_ref.startsWith(value) && attribute_value.isEmpty()) || (attribute_ref.startsWith(value) && attribute_value.equals("NotSet")) || (attribute_ref.startsWith(value) && !attribute_value.isEmpty())) {
                            Node parent = childInstanceNodeElement.getParentNode();
                            System.out.println("REMOVING " + childInstanceNodeElement.getNodeName() + " | " + childInstanceNodeElement.getAttributes().getNamedItem("dmrole") + " | " + childInstanceNodeElement.getAttributes().getNamedItem("ref") + " | " + childInstanceNodeElement.getAttributes().getNamedItem("value"));
                            parent.removeChild(childInstanceNodeElement);
                            removeElement(parent, value);
    //						parent.normalize();
                        }
                    }
                    else if(childInstanceNodeElement.getNodeName().equals("COLLECTION")) {
                        removeElement(childInstanceNodeElement, value);

                    }
                }
            }
        } else if (instanceNodeElement.getNodeName().equals("INSTANCE")) {
            removeElement(instanceNodeElement, value);
        }
    }

    /**Method allowing to remove Nodes
     * @param parentNode
     */
    public static void removeElement(Node parentNode, String value) {
        boolean noChild = true;
        for(int i =0; i<parentNode.getChildNodes().getLength();i++) {
            if(parentNode.getChildNodes().item(i).getNodeName().equals("INSTANCE") || parentNode.getChildNodes().item(i).getNodeName().equals("ATTRIBUTE") || parentNode.getChildNodes().item(i).getNodeName().equals("COLLECTION")
                    ||parentNode.getChildNodes().item(i).getNodeName().equals("REFERENCE")){
                noChild=false;
                break;
            }
            else {
                noChild=true;
            }
        }
        if(noChild) {
            System.out.println("---------------REMOVING " + parentNode.getNodeName() + " | " + parentNode.getAttributes().getNamedItem("dmrole") + " | " + parentNode.getAttributes().getNamedItem("ref") + " | " + parentNode.getAttributes().getNamedItem("value"));
            Node parent = parentNode.getParentNode();
            parent.removeChild(parentNode);
            if(parent.getNodeName().equals("INSTANCE")) {
                removeElement(parent,value);
            }
        }
        else {
            removeUnsetElements(parentNode,value);
        }
    }

    public static Document getTraversal(File fileToGet) {

        Document ourDoc = null;
        DocumentBuilderFactory factory = null;
        factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;

        try {
            builder = factory.newDocumentBuilder();
            ourDoc = builder.parse(fileToGet);
            return ourDoc;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static TreeWalker getWalker(File fileToGet, Document ourDoc) {

        DocumentTraversal traversal = (DocumentTraversal) ourDoc;
        TreeWalker walker = traversal.createTreeWalker(
                ourDoc.getDocumentElement(), NodeFilter.SHOW_ELEMENT, null, true);
        walker.getRoot();
        return walker;
    }

    public static File convertWalkerToFile(TreeWalker walker, String fileName, Document templateDoc) throws ParserConfigurationException, SAXException, IOException, TransformerException, URISyntaxException {
//        File newMangoFile = FileGetter.getXMLFile(fileName);

        File newMangoFile = new File(FilesPath + fileName);

        PrintWriter pw = new PrintWriter(newMangoFile);
        String finalString = XMLUtils.xmlToString(templateDoc); // converting the doc to a string to push it in the buffer
        pw.write(XMLUtils.toPrettyString(finalString, 2));
        pw.close();

        return newMangoFile;
    }

}
