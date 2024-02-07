package utils;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import ModelBaseInit.ModelBase;
import org.w3c.dom.*;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import static utils.Vocabulary.EmptyRef;

public class WalkerMivotManager {
    /* class used to process walker using only static function */

    public static String FilesPath = "src/main/webapp/mivot_snippets/"; //TODO:remove the hard coded path

    public static void changeAttributeValue(TreeWalker walker, String attributeNameToCheck, String attributeValueToCheck, String attributeToChange, String attributeValueToChange, String valueToSet) {
        /* the function lookForAttribute(attributeNameToCheck, attributeValueToCheck) and change the value of attributeToChange into attributeToChange */
        Node node = walker.getCurrentNode();
        if (node != null) {
            if (node.getAttributes().getNamedItem(attributeToChange) != null && node.getAttributes().getNamedItem(attributeToChange).getNodeValue().equals(attributeValueToChange) &&
                    node.getAttributes().getNamedItem(attributeNameToCheck).getNodeValue().equals(attributeValueToCheck)) {
                node.getAttributes().getNamedItem(attributeToChange).setNodeValue(valueToSet);
            }
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

    public static Node getNodeByTag(TreeWalker walker, String tagName, Document doc) {
        NodeList nodeList = doc.getElementsByTagName(tagName);
        System.out.println("Node list length : " + nodeList.getLength());
        Node targetNode = null;
        if (nodeList.getLength() > 0) {
            targetNode = nodeList.item(0);
        }
        else {
            System.out.println("No unique node found with the tag " + tagName);
            targetNode = walker.getRoot().getFirstChild();
        }
        System.out.println("Target node : " + targetNode.getNodeName());
        return targetNode;
    }

    public static Node getNodeByName(TreeWalker walker, String tagName, String attributeName, String attributeValue, Document doc) {
        if (doc != null) {
            NodeList nodeList = doc.getElementsByTagName(tagName);
            Node targetNode = null;
            if (nodeList.getLength() == 1) {
                targetNode = nodeList.item(0);
            } else if (nodeList.getLength() > 1) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    if (nodeList.item(i).getAttributes().getNamedItem(attributeName).getNodeValue().equals(attributeValue)) {
                        targetNode = nodeList.item(i);
                    }
                }
                if (targetNode == null) {
                    System.out.println("No node found with the attribute " + attributeName + " and the value " + attributeValue);
                    targetNode = walker.getRoot().getFirstChild();
                }
            }
            return targetNode;
        }else {
            return null;
        }
    }

    public static void cleanUpNode(TreeWalker walker) throws ParserConfigurationException, SAXException, IOException, TransformerException, URISyntaxException {

        Node parent = walker.getRoot();
        for (int i = 0; i < parent.getChildNodes().getLength(); i++) {

            Node child = parent.getChildNodes().item(i);
            for (int j = 0; j < child.getChildNodes().getLength(); j++) {
                Node children = child.getChildNodes().item(j);
                if (children.getNodeName().equals("REPORT")) {
                    children.setTextContent("Automatically generated. NotSet instance have been removed");
                }
                removeUnsetElements(children, EmptyRef);
            }

            if (child.getNodeName().equals("REPORT")) {
                child.setTextContent("Automatically generated. NotSet instance have been removed");
            }
            if (child.getNodeName().equals("INSTANCE") || child.getNodeName().equals("ATTRIBUTE")) {
                removeUnsetElements(child, EmptyRef);
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

    public static void addNodes(TreeWalker walkerToAdd, TreeWalker targetWalker, Node targetParentNode) {
        Node currentNode = walkerToAdd.getRoot();
        targetWalker.setCurrentNode(targetParentNode);
        do {
            Node newNode = targetWalker.getRoot().getOwnerDocument().importNode(currentNode, true);
            targetParentNode.appendChild(newNode);
            targetWalker.nextNode();
            currentNode = walkerToAdd.getCurrentNode();
        }while (walkerToAdd.nextSibling() != null);
    }

    public static File convertWalkerToFile(TreeWalker walker, String fileName, Document templateDoc) throws ParserConfigurationException, SAXException, IOException, TransformerException, URISyntaxException {


        File newMangoFile = new File(FilesPath + fileName);

        PrintWriter pw = new PrintWriter(newMangoFile);
        String finalString = XMLUtils.xmlToString(templateDoc); // converting the doc to a string to push it in the buffer
        pw.write(XMLUtils.toPrettyString(finalString, 2));
        pw.close();

        return newMangoFile;
    }
    public static String findSnippetFile(String SnippetPath, String model, ModelBase modelBase, String dmtype) throws URISyntaxException {
        /** The function look for the right snippet file.
         * First case :
         * - The current model processed is the right one
         * - The snippet name is in the modelBase
         * Second case :
         * - The current model processed is the right one
         * - The snippet name is unknown, we try to guess by looking the dmtype
         * Third case :
         * - The current model processed is not the right one : so we are actually doing an importation of another model
         * - The snippet name is in the modelBase
         * Fourth case :
         * - The current model processed is not the right one : so we are actually doing an importation of another model
         * - The snippet name is unknown, we try to guess by looking the dmtype
         */
        System.out.println("Looking for the snippet file for the model " + model + " and the dmtype " + dmtype);
        File folder = new File(SnippetPath + model);
        File file1 = new File((SnippetPath + model + "/" + modelBase.snippet.get(dmtype) + ".xml")); // Case where the snippet name is in the modelBase
        File file2 = new File((SnippetPath + model + "/" + dmtype + ".xml")); // Case where the snippet name is unknown, we try to guess by looking the dmtype
        System.out.println("File 1 : " + file1.getAbsolutePath());
        System.out.println("File 2 : " + file2.getAbsolutePath());

        System.out.println("Model list creating");
        ArrayList<String> model_list = new ArrayList<>();
        // Do a list of all the model by looking the name of each folder in the SnippetPath
//        System.out.println(FileGetter.getXMLFile("mivot_snippets").getAbsolutePath());
        for (File file : Objects.requireNonNull(new File(String.valueOf(FileGetter.getXMLFile("subfile_annoted.mango.xml"))).listFiles())) {
            if (file.isDirectory()) {
                model_list.add(file.getName());
            }
        }
        System.out.println("Model list : " + model_list);
        if (modelBase.snippet.containsKey(dmtype)) {
            if (folder.exists() && folder.isDirectory() &&
                file1.isFile() && file1.getName().endsWith(modelBase.snippet.get(dmtype)+".xml")) {
                System.out.println("File found : " + file1.getAbsolutePath());
                return (new File(SnippetPath + model + "/" + modelBase.snippet.get(dmtype) + ".xml").getAbsolutePath());
            } else if (modelBase.snippet.containsKey(dmtype) && folder.exists() && folder.isDirectory() &&
                    file2.isFile() && file2.getName().endsWith(dmtype+".xml")) {
                System.out.println("File found : " + file2.getAbsolutePath());
                return (new File(SnippetPath + model + "/" + dmtype + ".xml").getAbsolutePath());
            } else {
                System.out.println("No file found.");
                return WalkerMivotManager.findFilesBySuffix(SnippetPath, modelBase.snippet.get(dmtype)+".xml", model_list);
            }
        } else {
            if (dmtype.contains(":")) {
                System.out.println("No file found.");
                return WalkerMivotManager.findFilesBySuffix(SnippetPath, dmtype.split(":")[1]+".xml", model_list);
            } else {
                System.out.println("No file found.");
                return WalkerMivotManager.findFilesBySuffix(SnippetPath, dmtype+".xml", model_list);
            }
        }
    }
    public static String findFilesBySuffix(String folderPath, String suffix, ArrayList<String> model_list) {
        /** The function look in each folder if a file ends with the suffix.
         * It is used to guess the snippet file name when it is not in the modelBase,
         * but also when the model in not the current one.
         */

        for (String model : model_list) {
            File folder = new File(folderPath + model);
            System.out.println("Looking for files with the extension " + suffix + " in the folder : " + folder.getAbsolutePath());
            if (folder.exists() && folder.isDirectory()) {
                File[] files = folder.listFiles();

                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && file.getName().endsWith(suffix)) {
                            System.out.println("File found : " + file.getAbsolutePath());
                            return (new File(folderPath + model + "/" + file.getName()).getAbsolutePath());
                        }
                    }
                } else {
                    System.out.println("Empty folder.");

                }
            } else {
                System.out.println("Folder does not exist.");
            }
        }
        System.out.println("No file found.");
        return null;
    }

}
