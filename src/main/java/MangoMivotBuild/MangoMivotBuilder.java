package MangoMivotBuild;
import ModelBaseInit.ModelBase;
import ModelBaseInit.ModelBaseInit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.TreeWalker;
import org.xml.sax.SAXException;
import tap.TAPException;
import utils.FileGetter;
import utils.WalkerMivotManager;

import javax.servlet.ServletException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;


public class MangoMivotBuilder {
    /**
     * This class will build the annotation for the model mango, using the mapping from the ModelBase class
     * and getting from this mapping the snippets of this annotation.
     **/
    private ModelBase modelBase;
    private ModelBaseInit ModelbaseInit = new ModelBaseInit();
    private ArrayList<String> dmtype_built = new ArrayList<>();
    public boolean isMangoMappeable = false;
    public static String SnippetPath;
    public Document doc = null;

    public MangoMivotBuilder(String table_name, ArrayList<String> col_to_query) throws TAPException, ParserConfigurationException, IOException, URISyntaxException, TransformerException, SAXException, ServletException {

        SnippetPath = "src/main/webapp/mivot_snippets/";
        System.out.println(table_name);
        for (int i = 0; i < col_to_query.size(); i++){
            System.out.println("COLUMN NAME " + i + " : " + col_to_query.get(i));
        }

        this.modelBase = ModelbaseInit.getModelBase("mergedentry","mango", col_to_query);

        if (this.modelBase != null) {
            if (this.modelBase.dmtype_dict.isEmpty()) {
                System.out.println("No mapping encountered for this table.");
            } else {
                System.out.println("Mapping encountered for this table.");
                buildMivotBlock();
                if (dmtype_built != null) {
                    this.isMangoMappeable = true;
                }
            }
        }
    }

    public TreeWalker buildMivotBlock() throws ParserConfigurationException, IOException, URISyntaxException, TransformerException, SAXException {
        TreeWalker snippet = null;
        Document doc = null;
        Exception e_to_show = null;

        try {
            System.out.println("All dmtype keys : " + modelBase.getAllDmtypeKeys());
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@");
            System.out.println("Snippet path : " + WalkerMivotManager.findSnippetFile(SnippetPath, "mango", modelBase, modelBase.getAllDmtypeKeys().get(0)));
            if (modelBase.getAllDmtypeKeys().contains("mango:Source")) {
                File snippet_file = new File(getPath("mango", "mango:Source"));
                doc = WalkerMivotManager.getTraversal(snippet_file);
                // Obtention de la racine du document

                buildVODML(doc);
                snippet = this.buildAnnotations(snippet_file, doc, "mango:Source");
                this.dmtype_built.add("mango:Source");

            } else {
                File snippet_file = new File(getPath(modelBase.getAllDmtypeKeys().get(0).split(":")[0], modelBase.getAllDmtypeKeys().get(0)));

                doc = WalkerMivotManager.getTraversal(snippet_file);
                buildVODML(doc);
                System.out.println("Snippet file : " + snippet_file.getAbsolutePath());
                System.out.println("Document : " + doc);
                snippet = this.buildAnnotations(snippet_file, doc, modelBase.getAllDmtypeKeys().get(0));
                this.dmtype_built.add(modelBase.getAllDmtypeKeys().get(0));
            }
            System.out.println("Snippet : " + snippet);
            System.out.println("Document : " + doc);
            for (String dmtype : modelBase.getAllDmtypeKeys()) {
                if (!this.dmtype_built.contains(dmtype) && !Objects.equals(dmtype.split(":")[0], "mango") && !dmtype.equals("mango:Source")) {
                    System.out.println("-----------------------------------\n dmtype : " + dmtype + "\n-----------------------------------");

                    File new_snippet_file = new File(getPath(dmtype.split(":")[0], dmtype));
                    Document new_doc = WalkerMivotManager.getTraversal(new_snippet_file);
                    TreeWalker new_snippet = this.buildAnnotations(new_snippet_file, new_doc, dmtype);
                    if (modelBase.getAllDmtypeKeys().contains("mango:Source")) {
                        Node Node_collection = WalkerMivotManager.getNodeByName(snippet, "COLLECTION", "dmrole", "mango:Source.propertyDock", doc); //snippet.getRoot().getLastChild().getPreviousSibling();
                        System.out.println(Node_collection.getAttributes().getNamedItem("dmrole").getNodeValue());
                        WalkerMivotManager.addNodes(new_snippet, snippet, Node_collection);
                    } else {
                        WalkerMivotManager.addNodes(new_snippet, snippet, snippet.getRoot().getFirstChild());
                    }
                    this.dmtype_built.add(dmtype);
                }
            }
            WalkerMivotManager.cleanUpNode(snippet);
            this.buildGlobals(snippet, doc);

        } catch (Exception e) {
            e_to_show = e;
        }
        System.out.println("Exception : " + e_to_show);

        if (doc != null) {
            this.buildReport(snippet, doc, e_to_show);
            WalkerMivotManager.convertWalkerToFile(snippet, "subfile_annoted.mango.xml", doc);
            this.doc = doc;
        }
        return snippet;
    }

    public void buildVODML(Document doc) {
        /**
         * This method will add the MODEL, GLOBALS, REPORT,
         **/
        System.out.println("Document : " + doc);
        // Change the root element -> VODML
        Element rootElement = doc.getDocumentElement();
        Element VODMLParent = doc.createElement("VODML");
        VODMLParent.appendChild(rootElement);
        doc.appendChild(VODMLParent);
        System.out.println("VODMLParent : " + VODMLParent);
        // Change the root element -> RESSOURCE of type "meta"
        Element VODMLrootElement = doc.getDocumentElement();
        Element ressourceElement = doc.createElement("RESOURCE");
        ressourceElement.setAttribute("type", "meta");
        ressourceElement.appendChild(VODMLrootElement);
        doc.appendChild(ressourceElement);
        System.out.println("ressourceElement : " + ressourceElement);
        Element parentElement = doc.getDocumentElement();
        Node firstChild = parentElement.getFirstChild();
        System.out.println("parentElement : " + parentElement);
        // Add the REPORT element
        Element reportElement = doc.createElement("REPORT");
        parentElement.insertBefore(reportElement, firstChild);

        // Add the MODEL element
        Element modelElement = doc.createElement("MODEL");
        modelElement.setAttribute("name", this.modelBase.model_name);
        modelElement.setAttribute("url", this.modelBase.model_url);
        parentElement.insertBefore(modelElement, firstChild);

        // Add the GLOBALS element
        Element globalsElement = doc.createElement("GLOBALS");
        parentElement.insertBefore(globalsElement, firstChild);
    }

    public TreeWalker buildAnnotations(File snippet_file, Document doc, String dmtype) throws URISyntaxException {
        /**
         * This method will build the annotation for the model
         **/
        TreeWalker snippet = WalkerMivotManager.getWalker(snippet_file, doc);
        resolve_ref(snippet, modelBase, dmtype);
        return snippet;
    }
    private String getPath(String model, String dmtype) throws URISyntaxException {
        /**
         * This method will get the snippet xml from /src/main/webapp/mivot_snippets
         **/
        System.out.println("Snippet path : " + WalkerMivotManager.findSnippetFile(SnippetPath, model, this.modelBase, dmtype));
        return WalkerMivotManager.findSnippetFile(SnippetPath, model, this.modelBase, dmtype);
    }

    public void resolve_ref(TreeWalker walker, ModelBase modelBase, String dmtype) throws URISyntaxException {
        /**
         * This method go through the walker and resolve the ref attribute if it is present in the modelBase.
         * It also handles 3 special cases :
         * - FRAME : If the actual dmtype is a frame, we add a reference to the coordsys instance which will be set in GLOBALS.
         * - ERROR : If the actual dmtype class have an error, and it is present in
         *
         *
         **/
        Map<String, String> targetDict = modelBase.getDictForDmtype(dmtype);
        ArrayList<String> dmtypeErrorDone = new ArrayList<>();
        do {
            Node current = walker.getCurrentNode();
            if (current.hasAttributes() && current.getAttributes().getNamedItem("dmrole") != null) {
                Node CurrentDmrole = current.getAttributes().getNamedItem("dmrole");
                String CurrentDmroleValue = CurrentDmrole.getNodeValue();
                String CurrentDmtypeValue = "";
                String ParentDmtypeValue = new String();
                if (current.getAttributes().getNamedItem("dmtype") != null) {
                    CurrentDmtypeValue = current.getAttributes().getNamedItem("dmtype").getNodeValue();
                }
                if (current.getParentNode().getNodeValue() != null) {
                    ParentDmtypeValue = current.getParentNode().getAttributes().getNamedItem("dmtype").getNodeValue();
                }

                if (CurrentDmrole != null){

                    /* Add a reference to the coordsys instance which will be set in GLOBALS.
                     * We create a new Node named REFERENCE, and we append it to the parent node.
                     */
                    if (CurrentDmroleValue.endsWith("coordSys")) {
                        System.out.println("Resolve frame : " + CurrentDmroleValue + " : " + modelBase.frame.get(dmtype));
                        addStaticReference(walker, CurrentDmroleValue, modelBase.frame.get(dmtype));
//                        walker.nextNode(); // Since we added a node, we need to go to the next one.
                    }

                    else if (current.getAttributes().getNamedItem("ref") != null) {

                        if (targetDict.containsKey(CurrentDmroleValue) ) {
                            System.out.println("Resolve ref : " + CurrentDmroleValue + " : " + targetDict.get(CurrentDmroleValue));
                            WalkerMivotManager.changeAttributeValue(walker, "dmrole", CurrentDmroleValue, "ref",
                                    "@@@@@", targetDict.get(CurrentDmroleValue));
                        }
                        /* The ref is from another dmtype, because it is an import.
                         * We call the resolve_ref method with another dmtype if we have it in the modelBase.
                         */
                        else if (!Objects.equals(CurrentDmroleValue.split(":")[0], CurrentDmtypeValue.split(":")[0]) && current.getParentNode().getAttributes().getNamedItem("dmtype").getNodeValue() != null &&
                                !Objects.equals(CurrentDmtypeValue.split(":")[0], dmtype.split(":")[0]) && CurrentDmroleValue.contains(".") &&
                                Objects.equals(current.getParentNode().getAttributes().getNamedItem("dmtype").getNodeValue(), CurrentDmroleValue.split("\\.")[0]) &&
                                !this.dmtype_built.contains(CurrentDmroleValue.split("\\.")[0]) &&
                                modelBase.dmtype_dict.containsKey(current.getParentNode().getAttributes().getNamedItem("dmtype").getNodeValue()) && !this.dmtype_built.contains(current.getParentNode().getAttributes().getNamedItem("dmtype").getNodeValue())) {

                            System.out.println("Resolve ref from other dmtype : " + CurrentDmroleValue + " : " + current.getParentNode().getAttributes().getNamedItem("dmtype").getNodeValue());
                            resolve_ref(walker, modelBase, current.getParentNode().getAttributes().getNamedItem("dmtype").getNodeValue());
                            this.dmtype_built.add(current.getParentNode().getAttributes().getNamedItem("dmtype").getNodeValue());

                        }
                        /** Check for error in 2 possible cases :
                         * - The dmrole error is in the class and so in the actual dmtype_dict.
                         * - The dmrole error is from another class, so we check if the actual dmtype is related to a dmerror
                         * using the modelBase.error dict which give us the dmtype of the error class related to our actual dmtype.
                         **/
                        else if (CurrentDmroleValue.endsWith("error")) {
                            if (modelBase.error.containsKey(dmtype) && !dmtypeErrorDone.contains(dmtype)) {
                                importNewTreeWalker(walker, modelBase, modelBase.error.get(dmtype), CurrentDmroleValue);
                                dmtypeErrorDone.add(dmtype);
                                System.out.println("Import error : " + CurrentDmroleValue + " : " + modelBase.error.get(dmtype));
                            }
                        }
                    }
                    /** For INSTANCE, we look for 2 possible cases :
                     * - The dmrole is in the actual dmtype_dict, so we resolve the ref.
                     * - The dmrole is not in the actual dmtype_dict AND the instance has no child, so we import this dmtype if we have it.
                     **/
                    else if (current.getNodeName().equals("INSTANCE") && !CurrentDmroleValue.isEmpty() &&
                            !Objects.equals(CurrentDmtypeValue.split(":")[0], dmtype.split(":")[0])) {

                        if(Objects.equals(CurrentDmroleValue.split(":")[0], CurrentDmtypeValue.split(":")[0])) { //1er partie du dmrole = 1er partie du dmtype
                            System.out.println("Resolve instance : " + CurrentDmroleValue + " : " + CurrentDmtypeValue);
                        }
                        else if (!current.hasChildNodes() && modelBase.dmtype_dict.containsKey(CurrentDmtypeValue) && !this.dmtype_built.contains(dmtype)) {
                            System.out.println("Import new instance : " + CurrentDmroleValue + " : " + CurrentDmtypeValue); //1er partie du dmrole != 1er partie du dmtype
                            importNewTreeWalker(walker, modelBase, CurrentDmtypeValue, CurrentDmroleValue);
                        }
                    }
                }
            }
        } while (walker.nextNode() != null);
    }

    public void importNewTreeWalker(TreeWalker walker, ModelBase md, String dmtype, String dmrole) throws URISyntaxException {
        /** This method will import a new treeWalker in the actual walker.
         *  It first looks for the folder concerned by the dmtype, 
         *  then call the resolve_ref method to resolve the ref attribute in the new treeWalker,
         *  and finally, import the treeWalker in the actual walker.
         */

        if (!this.dmtype_built.contains(dmtype)){
            File new_snippet = new File(getPath(dmtype.split(":")[0], dmtype));
            System.out.println("New snippet : " + new_snippet.getAbsolutePath());
            Document new_doc = WalkerMivotManager.getTraversal(new_snippet);
            TreeWalker snippet_new = WalkerMivotManager.getWalker(new_snippet, new_doc);

            snippet_new.getRoot().getAttributes().getNamedItem("dmrole").setNodeValue(dmrole);
            resolve_ref(snippet_new, md, dmtype);

            WalkerMivotManager.addNodes(snippet_new, walker, walker.getCurrentNode().getParentNode());
            this.dmtype_built.add(dmtype);
        }
        else {
            System.out.println("Already built : " + dmtype);
        }

    }

    public TreeWalker addStaticReference(TreeWalker walker, String dmrole, String dmrefValue) {
        /** Add static reference as the next node
         * exemple with frame : <REFERENCE dmrole="mango:EpochPosition.coordSys" dmref="FK5" />
         **/
        Node parent = walker.getCurrentNode();
        Node ref;
        if (parent.getNodeName().equals("REFERENCE")) { // Case 1 : <REFERENCE dmref="@@@@@" dmrole="coords:Coordinate.coordSys"/>
            ref = parent.cloneNode(true);
            ref.getAttributes().getNamedItem("dmref").setNodeValue(dmrefValue);
        }else {                                         // Case 2 : <INSTANCE dmrole="mango:EpochPosition.coordSys" dmtype="coords:SpaceSys"/>
            ref = parent.getOwnerDocument().createElement("REFERENCE");
            ref.getAttributes().setNamedItem(parent.getOwnerDocument().createAttribute("dmrole"));
            ref.getAttributes().getNamedItem("dmrole").setNodeValue(dmrole);
            ref.getAttributes().setNamedItem(parent.getOwnerDocument().createAttribute("dmref"));
            ref.getAttributes().getNamedItem("dmref").setNodeValue(dmrefValue);
        }
        walker.getCurrentNode().getParentNode().appendChild(ref);
        walker.getCurrentNode().getParentNode().removeChild(parent);
        return walker;
    }

    public void buildGlobals(TreeWalker walker, Document doc) throws URISyntaxException {
        /** Add the static reference as a child of GLOBALS
         * Look for REFERENCE with dmref attribute in all the treeWalker
         * exemple with frame : <REFERENCE dmrole="mango:EpochPosition.coordSys" dmref="FK5" />
         * Look for the snippet present in dmrefValue and add it to GLOBALS
         **/
        walker.getRoot();
        WalkerMivotManager.printWalker(walker);
        do {
            Node current = walker.getCurrentNode();
            if (current.getNodeName().equals("REFERENCE")) {
                String dmrefValue = current.getAttributes().getNamedItem("dmref").getNodeValue();
                File static_ref_snippet_file = new File(getPath(null, dmrefValue));
                System.out.println("New snippet for static reference : " + static_ref_snippet_file.getAbsolutePath());
                Document new_doc = WalkerMivotManager.getTraversal(static_ref_snippet_file);
                TreeWalker static_ref_snippet = WalkerMivotManager.getWalker(static_ref_snippet_file, new_doc);
                Node global_node = WalkerMivotManager.getNodeByName(walker, "GLOBALS", null, null, doc);
                WalkerMivotManager.addNodes(static_ref_snippet, walker, global_node);
                break;

            }
        } while (walker.nextNode() != null);
    }

    public void buildReport(TreeWalker walker, Document doc, Exception e_to_show) {
        /** Add status attribute to REPORT node
         * Add element #text as a child of REPORT node
         * exemple : <REPORT status="OK">MIVOT annotations automatically generated.</REPORT>
         **/
        Node report_node = WalkerMivotManager.getNodeByName(walker, "REPORT", null, null, doc);
        report_node.getAttributes().setNamedItem(doc.createAttribute("status"));
        if (e_to_show != null) {
            report_node.getAttributes().getNamedItem("status").setNodeValue("KO");
            report_node.appendChild(doc.createTextNode(e_to_show.toString()));
        } else {
            report_node.getAttributes().getNamedItem("status").setNodeValue("OK");
            report_node.appendChild(doc.createTextNode("MIVOT annotations automatically generated."));
        }
    }

    public static void main(String[] args) throws TAPException, ParserConfigurationException, IOException, URISyntaxException, TransformerException, SAXException, ServletException {

        ArrayList<String> col_to_query = new ArrayList<>();
        col_to_query.add("SC_RA");
        col_to_query.add("sc_pm_ra");
        col_to_query.add("SC_DEC");
        col_to_query.add("sc_pm_dec");
        col_to_query.add("sc_parallax");
        col_to_query.add("sc_radial_velocity");
        col_to_query.add("sc_epoch");

        col_to_query.add("sc_err_maj_axis");
        col_to_query.add("sc_err_min_axis");
        col_to_query.add("sc_err_angle");
        col_to_query.add("sc_sym_rad");

        col_to_query.add("ident");
        col_to_query.add("prop_meas");
        col_to_query.add("phys_prop_meas");

        col_to_query.add("lon");
        col_to_query.add("lat");
        col_to_query.add("dist");
        MangoMivotBuilder mangoMivotBuilder = new MangoMivotBuilder("mergedentry",col_to_query);
    }
}
