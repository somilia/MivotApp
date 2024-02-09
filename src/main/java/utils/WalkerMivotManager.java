package utils;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import MangoMivotBuild.MivotTAPServlet;
import ModelBaseInit.ModelBase;
import jdk.jshell.Snippet;
import org.w3c.dom.*;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import static MangoMivotBuild.MangoMivotBuilder.SnippetPath;
import static utils.Vocabulary.EmptyRef;

public class WalkerMivotManager {
    /* class used to process walker using only static function */

    public static String FilesPath = "mivot_snippets/"; //TODO:remove the hard coded path

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
        /** This function is used to print the walker
         * Very useful for debugging **/
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

    public static void cleanUpNode(TreeWalker walker) {

        Node parent = walker.getRoot();
        for (int i = 0; i < parent.getChildNodes().getLength(); i++) {

            Node child = parent.getChildNodes().item(i);
            for (int j = 0; j < child.getChildNodes().getLength(); j++) {
                Node children = child.getChildNodes().item(j);
                if (children.getNodeName().equals("REPORT")) {
                    children.setTextContent(" NotSet instance have been removed");
                }
                removeUnsetElements(children, EmptyRef);
            }

            if (child.getNodeName().equals("REPORT")) {
                child.setTextContent(" NotSet instance have been removed");
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
            if(parentNode.getChildNodes().item(i).getNodeName().equals("INSTANCE")
                    || parentNode.getChildNodes().item(i).getNodeName().equals("ATTRIBUTE")
                    || parentNode.getChildNodes().item(i).getNodeName().equals("COLLECTION")
                    || parentNode.getChildNodes().item(i).getNodeName().equals("REFERENCE")){
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

    public static File convertWalkerToFile(String SnippetPath, TreeWalker walker, String fileName, Document templateDoc) throws IOException, URISyntaxException {

        File newMangoFile = new File(Objects.requireNonNull(getRealPath(SnippetPath, fileName)));

        PrintWriter pw = new PrintWriter(newMangoFile);
        String finalString = XMLUtils.xmlToString(templateDoc); // converting the doc to a string to push it in the buffer
        pw.write(XMLUtils.toPrettyString(finalString, 2));
        pw.close();

        return newMangoFile;
    }

    public static String getRealPath(String SnippetPath, String fileName) {
        /** This function is used to get the path of the file
         *  We need the ServletContext to get the real path of the file
         */
        String resource;
        if (fileName == null) {  // This case is used to get only the path of the folder SnippetPath "mivot_snippets/"
            resource = MivotTAPServlet.servletContext.getRealPath(SnippetPath);
        }else {
            resource = MivotTAPServlet.servletContext.getRealPath(SnippetPath+fileName);
        }
        try {
            if (resource == null) {
                System.out.println("\n !!! Resource is null, XML file not found : " + fileName + " !!!\n");
                return null;
            }else {
                return resource;
            }
        } catch (NullPointerException e) {
            System.out.println("No file found.");
            System.out.println(e.getStackTrace());
            return null;
        }
    }

    public static File findSnippetFile(String SnippetPath, String model, ModelBase modelBase, String dmtype) throws URISyntaxException {
        /** The function look for the right snippet file. The goal is to find in any case the right file, with the less time possible
         * by calling in the worst case findFilesBySuffix.
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

        ArrayList<String> model_list = new ArrayList<>();
        ArrayList<File> files = new ArrayList<>(Arrays.asList(Objects.requireNonNull(new File(Objects.requireNonNull(getRealPath(SnippetPath, model))).listFiles())));
        for (File file : files) {  // Do a list of all the model by looking the name of each folder in the SnippetPath
            if (file.isDirectory()) {
                model_list.add(file.getName());
            }
        }
        if (model==null) { // if model is null, we try to find the file in each folder -> it is the case in buildGlobals
            return WalkerMivotManager.findFilesBySuffix(SnippetPath, dmtype+".xml", model_list);
        }
        File folder = new File(Objects.requireNonNull(getRealPath(SnippetPath, model)));
        File file1 = null;
        File file2 = null;
        if(modelBase.snippet.containsKey(dmtype)){
            // Case where the snippet name is in the modelBase
            file1 = new File(Objects.requireNonNull(getRealPath(SnippetPath, model + "/" + modelBase.snippet.get(dmtype) + ".xml")));
            return file1;
        } else if (getRealPath(SnippetPath, model + "/" + dmtype.replace(":",".") + ".xml") != null) {
            // Case where the snippet name is unknown, we try to guess by looking the dmtype
            file2 = new File(Objects.requireNonNull(getRealPath(SnippetPath, model + "/" + dmtype.replace(":", ".") + ".xml")));
            return file2;
        }

        if (dmtype.contains(":")) {  // Case where the current model processed is not the right one : so we are actually doing an importation of another model
            return WalkerMivotManager.findFilesBySuffix(SnippetPath, dmtype.split(":")[1]+".xml", model_list);  // We try to find only with the dmrole
        } else {
            return WalkerMivotManager.findFilesBySuffix(SnippetPath, dmtype+".xml", model_list);
        }
    }
    public static File findFilesBySuffix(String SnippetPath, String suffix, ArrayList<String> model_list) throws URISyntaxException {
        /** The function look in each folder if a file ends with the suffix.
         * It is used to guess the snippet file name when it is not in the modelBase,
         * but also when the model in not the current one.
         */
        try {
            for (String model : model_list) {
                File folder = new File(Objects.requireNonNull(getRealPath(SnippetPath, model)));
                System.out.println("Looking for files with the extension " + suffix + " in the folder : " + folder.getAbsolutePath());
                if (folder.exists() && folder.isDirectory()) {
                    File[] files = folder.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile() && file.getName().endsWith(suffix)) {
                                System.out.println("File found : " + file.getAbsolutePath());
                                return file;
                            }
                        }
                    } else {
                        System.out.println("Empty folder.");
                    }
                } else {
                    System.out.println("Folder does not exist.");
                }
            }
        } catch (NullPointerException e) {
            System.out.println("No file found.");
            return null;
        }
        return null;
    }
}
