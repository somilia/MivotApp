package MangoMivotBuild;
import ModelBaseInit.ModelBase;
import ModelBaseInit.ModelBaseInit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.TreeWalker;
import tap.TAPException;
import utils.WalkerMivotManager;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
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
    public boolean isMappeable = false; // Use to know if the table can be annotated in CustomVOTableFormat function
    public static String SnippetPath = "mivot_snippets/"; // Path to the snippets
    public Document doc = null;

    public MangoMivotBuilder(String table_name, ArrayList<String> col_to_query) throws TAPException, IOException, URISyntaxException, ServletException {
        /**
         * This constructor will build the annotation for the model mango, using the mapping from the ModelBase class
         * and getting from this mapping the snippets of this annotation.
         **/
        this.modelBase = ModelbaseInit.getModelBase(table_name,"mango", col_to_query);

        if (this.modelBase != null) {
            if (this.modelBase.dmtype_dict.isEmpty()) {
                System.out.println("No mapping encountered for this table.");
            } else {
                System.out.println("\nMapping encountered for this table.\n Building annotation for the table : " + table_name + " ...\n");
                buildMivotBlock();
                if (dmtype_built != null) {
                    this.isMappeable = true;
                }
            }
        }
    }

    public TreeWalker buildMivotBlock() throws IOException, URISyntaxException {
        TreeWalker snippet = null;
        Document doc = null;
        Exception e_to_show = null;
        System.out.println("All dmtype keys to map : " + modelBase.getAllDmtypeKeys());
        try {
            /* We need the first tuple (file, document, TreeWalker) to start          *
             *  - First case : we have a mango class, so we start with mango:Source *
             *  - Second case : we start with the first dmtype of the modelBase     */
            if (containsMango()) {
                File snippet_file = getFile("mango", "mango.Source");
                doc = WalkerMivotManager.getTraversal(snippet_file);
                buildVODML(doc);
                if (modelBase.getAllDmtypeKeys().contains("mango:Source")) {
                    // mango:Source could be a real dmtype in the query
                    snippet = this.buildAnnotations(snippet_file, doc, "mango:Source");
                    this.dmtype_built.add("mango:Source");
                } else {
                    /* We build our walker only using the mango.Source *
                     * file, other dmtypes will be added just after    */
                    snippet = WalkerMivotManager.getWalker(snippet_file, doc);
                }
            } else {
                File snippet_file = getFile(modelBase.getAllDmtypeKeys().get(0).split(":")[0], modelBase.getAllDmtypeKeys().get(0));
                doc = WalkerMivotManager.getTraversal(snippet_file);
                buildVODML(doc);
                snippet = this.buildAnnotations(snippet_file, doc, modelBase.getAllDmtypeKeys().get(0));
                this.dmtype_built.add(modelBase.getAllDmtypeKeys().get(0));
            }
            System.out.println("Snippet : " + snippet);
            System.out.println("Document : " + doc);

            /* We go through all the dmtype keys and build the annotation for each of them */
            for (String dmtype : modelBase.getAllDmtypeKeys()) {
                if (!this.dmtype_built.contains(dmtype) && !dmtype.equals("mango:Source")) {
                    System.out.println("--------------\n dmtype : " + dmtype + "\n--------------");
                    File new_snippet_file = getFile(dmtype.split(":")[0], dmtype);
                    Document new_doc = WalkerMivotManager.getTraversal(new_snippet_file);
                    TreeWalker new_snippet = this.buildAnnotations(new_snippet_file, new_doc, dmtype);
                    if (containsMango()) {
                        /* We add each class in mango:Source.propertyDoc, for this we get the right Node using getNodeByName */
                        Node Node_collection = WalkerMivotManager.getNodeByName(snippet, "COLLECTION", "dmrole", "mango:Source.propertyDock", doc);
                        WalkerMivotManager.addNodes(new_snippet, snippet, Node_collection);
                    } else {
                        WalkerMivotManager.addNodes(new_snippet, snippet, snippet.getRoot().getFirstChild());
                    }
                    this.dmtype_built.add(dmtype);
                }
            }
            WalkerMivotManager.cleanUpNode(snippet);
            this.buildGlobals(snippet, doc);


            //WalkerMivotManager.printWalker(snippet); // Debug

        } catch (Exception e) {
            e_to_show = e;
            System.out.println("Exception : " + e_to_show);
        }

        if (doc != null) {
            this.buildReport(snippet, doc, e_to_show);
            WalkerMivotManager.convertWalkerToFile(SnippetPath, snippet, "subfile_annoted.mango.xml", doc); // Debug and not working anymore
            this.doc = doc;
        }
        return snippet;
    }

    public void buildVODML(Document doc) {
        /**
         * This method will add the MODEL, GLOBALS, REPORT, VODML
         * at the top of the document, and change the root element to RESOURCE.
         * The order here is quite important.
         **/
        // Change the root element -> VODML
        Element rootElement = doc.getDocumentElement();
        Element VODMLParent = doc.createElement("VODML");
        VODMLParent.appendChild(rootElement);
        doc.appendChild(VODMLParent);

        Element parentElement = doc.getDocumentElement();
        Node firstChild = parentElement.getFirstChild();

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

        // Change the root element -> RESSOURCE of type "meta"
        Element VODMLrootElement = doc.getDocumentElement();
        Element ressourceElement = doc.createElement("RESOURCE");
        ressourceElement.setAttribute("type", "meta");
        ressourceElement.appendChild(VODMLrootElement);
        doc.appendChild(ressourceElement);
    }

    public TreeWalker buildAnnotations(File snippet_file, Document doc, String dmtype) throws URISyntaxException {
        /**
         * This method will build the annotation for the model
         **/
        TreeWalker snippet = WalkerMivotManager.getWalker(snippet_file, doc);
        resolve_ref(snippet, modelBase, dmtype);
        return snippet;
    }
    private File getFile(String model, String dmtype) throws URISyntaxException {
        /**
         * This method will get the snippet xml from /src/main/webapp/mivot_snippets
         **/
        return WalkerMivotManager.findSnippetFile(SnippetPath, model, this.modelBase, dmtype);
    }

    public void resolve_ref(TreeWalker walker, ModelBase modelBase, String dmtype) throws URISyntaxException {
        /**
         * This method go through the walker and resolve the ref attribute if it is present in the modelBase.
         * It also handles 2 special cases :
         * - FRAME : If the actual dmtype is a frame, we add a reference to the coordsys instance which will be set in GLOBALS.
         * - ERROR : If the actual dmtype class have an error, and it is present in the dmtype_dict, we import the error class.
         * For the rest :
         * - INSTANCE : If the actual dmtype class have an instance, and it is present in the dmtype_dict, we import the instance.
         * - REF : If the actual dmtype class have a ref, and it is present in the dmtype_dict, we resolve the ref.
         * - REF from another dmtype : If the actual dmtype class have a ref from another dmtype, and it is present in the dmtype_dict, we resolve the ref.
         * - REF from another dmtype with import : If the actual dmtype class have a ref from another dmtype, and it is not present in the dmtype_dict,
         *   we import the dmtype and resolve the ref.
         **/
        Map<String, String> targetDict = modelBase.getDictForDmtype(dmtype);
        ArrayList<String> dmtypeErrorDone = new ArrayList<>();
        do {
            Node current = walker.getCurrentNode();
            if (current.hasAttributes() && current.getAttributes().getNamedItem("dmrole") != null) { // The first node has a dmrole attribute but his value is empty
                Node CurrentDmrole = current.getAttributes().getNamedItem("dmrole");                 // -> value is check when needed (cf INSTANCE block)
                String CurrentDmroleValue = CurrentDmrole.getNodeValue();
                String CurrentDmtypeValue = "";
                if (current.getAttributes().getNamedItem("dmtype") != null) {
                    CurrentDmtypeValue = current.getAttributes().getNamedItem("dmtype").getNodeValue();
                }
                // ----------------------------------------------------------------------> FRAME
                if (CurrentDmroleValue.endsWith("coordSys")) { // This condition has to come in first ! (more specific and could be taken in the INSTANCE condition)
                    /* Add a reference to the coordsys instance which will be set in GLOBALS.
                     * We create a new Node named REFERENCE (or take the one if it already exists (cf. addStaticReference docstring)),
                     * and we append it to the parent node, the actual node is removed.*/
                    System.out.println("Resolve frame : " + CurrentDmroleValue + " : " + modelBase.frame.get(dmtype));
                    addStaticReference(walker, CurrentDmroleValue, modelBase.frame.get(dmtype));
                }
                // ----------------------------------------------------------------------> ATTRIBUTE
                else if (current.getAttributes().getNamedItem("ref") != null) { // Having a ref attribute => we are in a ATTRIBUTE (or REFERENCE?) node

                    if (targetDict.containsKey(CurrentDmroleValue) ) {
                        /* The ref is in the actual dmtype_dict, so we resolve the ref, this is the basic case. */
                        System.out.println("Resolve ref : " + CurrentDmroleValue + " : " + targetDict.get(CurrentDmroleValue));
                        WalkerMivotManager.changeAttributeValue(walker, "dmrole", CurrentDmroleValue, "ref",
                                "@@@@@", targetDict.get(CurrentDmroleValue));
                    }
                    else if (typeInDmroleIsDifferentFromParent(CurrentDmroleValue, current.getParentNode().getAttributes().getNamedItem("dmtype").getNodeValue()) && current.getParentNode().getAttributes().getNamedItem("dmtype").getNodeValue() != null &&
                            !Objects.equals(CurrentDmtypeValue.split(":")[0], dmtype.split(":")[0]) && CurrentDmroleValue.contains(".") &&
                            Objects.equals(current.getParentNode().getAttributes().getNamedItem("dmtype").getNodeValue(), CurrentDmroleValue.split("\\.")[0]) &&
                            !this.dmtype_built.contains(CurrentDmroleValue.split("\\.")[0]) &&
                            modelBase.dmtype_dict.containsKey(current.getParentNode().getAttributes().getNamedItem("dmtype").getNodeValue()) && !this.dmtype_built.contains(current.getParentNode().getAttributes().getNamedItem("dmtype").getNodeValue())) {
                        /* The ref is from another dmtype, because it is an import.
                           We call the resolve_ref method with another dmtype if we have it in the modelBase. */
                        System.out.println("Resolve ref from other dmtype : " + CurrentDmroleValue + " : " + current.getParentNode().getAttributes().getNamedItem("dmtype").getNodeValue());
                        resolve_ref(walker, modelBase, current.getParentNode().getAttributes().getNamedItem("dmtype").getNodeValue());
                        this.dmtype_built.add(current.getParentNode().getAttributes().getNamedItem("dmtype").getNodeValue());

                    }
                    /* Check for error in 2 possible cases :
                     * - The dmrole error is in the class and so in the actual dmtype_dict. -> We resolve the ref as usual.
                     * - The dmrole error is from another class, so we check if the actual dmtype is related to a dmerror
                     *   using the modelBase.error dict which give us the dmtype of the error class related to our actual dmtype.
                     *   -> it is the case just below where we import a new error class.
                     */
                    else if (CurrentDmroleValue.endsWith("error")) {
                        if (modelBase.error.containsKey(dmtype) && !dmtypeErrorDone.contains(dmtype)) {
                            importNewTreeWalker(walker, modelBase, modelBase.error.get(dmtype), CurrentDmroleValue);
                            dmtypeErrorDone.add(dmtype);
                            System.out.println("Import error : " + CurrentDmroleValue + " : " + modelBase.error.get(dmtype));
                        }
                    }
                }
                // ----------------------------------------------------------------------> INSTANCE
                /* For INSTANCE, we look for 2 possible cases :
                 * - The dmrole is in the actual dmtype_dict, so we resolve the ref.
                 * - The dmrole is not in the actual dmtype_dict AND the instance has no child, so we import this dmtype if we have it.
                 */
                else if (current.getNodeName().equals("INSTANCE") && !CurrentDmroleValue.isEmpty() && isDmtypeDifferent(CurrentDmtypeValue, dmtype)) { // We check if dmrole value is null (case for the first Node)

                    if(isDmtypeDifferent(CurrentDmroleValue, CurrentDmtypeValue)) {
                        //first part of dmrole == first part of dmtype   TODO : a line is missing here ??
                        System.out.println("Resolve instance : " + CurrentDmroleValue + " : " + CurrentDmtypeValue);
                    }
                    else if (!current.hasChildNodes() && modelBase.dmtype_dict.containsKey(CurrentDmtypeValue) && !this.dmtype_built.contains(dmtype)) {
                        //first part of dmrole != first part of dmtype
                        System.out.println("Import new instance : " + CurrentDmroleValue + " : " + CurrentDmtypeValue);
                        importNewTreeWalker(walker, modelBase, CurrentDmtypeValue, CurrentDmroleValue);
                    }
                }
            }
        } while (walker.nextNode() != null);
    }

    public boolean isDmtypeDifferent(String CurrentDmtype, String CurrentDmrole) {
        /** This method is used to compare the first part of two entity (dmrole or dmtype) **/
        return !Objects.equals(CurrentDmtype.split(":")[0], CurrentDmrole.split(":")[0]);
    }

    public boolean typeInDmroleIsDifferentFromParent(String CurrentDmroleValue, String parentDmtype) {
        /** This method will check if the dmtype of the dmrole is different from the parent dmtype.
         *  Exemple : mango:EpochPosition.coordSys become mango:EpochPosition which is not different from his parent dmtype (mango:EpochPosition).
         **/
        if (CurrentDmroleValue.contains(".") && !Objects.equals(parentDmtype, CurrentDmroleValue.split("\\.")[0])) {
            return true;
        } else return !Objects.equals(parentDmtype, CurrentDmroleValue.split("\\.")[0]); // Case where dmrole does not contain a "."
    }

    public void importNewTreeWalker(TreeWalker walker, ModelBase md, String dmtype, String dmrole) throws URISyntaxException {
        /** This method will import a new treeWalker in the actual walker.
         *  It first looks for the folder concerned by the dmtype, 
         *  then call the resolve_ref method to resolve the ref attribute in the new treeWalker,
         *  and finally, import the treeWalker in the actual walker.
         */
        if (!this.dmtype_built.contains(dmtype)){
            File new_snippet = getFile(dmtype.split(":")[0], dmtype);
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

    public void addStaticReference(TreeWalker walker, String dmrole, String dmrefValue) { //CurrentDmroleValue + " : " + modelBase.frame.get(dmtype));
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
    }

    public void buildGlobals(TreeWalker walker, Document doc) throws URISyntaxException {
        /** Add the static reference as a child of GLOBALS
         * Look for REFERENCE with dmref attribute in all the treeWalker
         * exemple with frame : <REFERENCE dmrole="mango:EpochPosition.coordSys" dmref="FK5" />
         * Look for the snippet present in dmrefValue and add it to GLOBALS
         **/
        walker.getRoot();
        do {
            Node current = walker.getCurrentNode();
            if (current.getNodeName().equals("REFERENCE")) {
                String dmrefValue = current.getAttributes().getNamedItem("dmref").getNodeValue();
                File static_ref_snippet_file = getFile(null, dmrefValue);
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
    public boolean containsMango() {
        /**
         * This method will check if the modelBase contains at least one mango class.
         * If it is the case, we will start the annotation with mango:Source.
         **/
        if (modelBase.getAllDmtypeKeys().contains("mango:Source") || modelBase.snippet.containsValue("mango:Source")) {
            System.out.println("Contains mango class");
            return true;
        } else {
            for (int i = 0; i < modelBase.getAllDmtypeKeys().size(); i++) {
                if (modelBase.getAllDmtypeKeys().get(i).startsWith("mango")) {
                    System.out.println("Contains mango class");
                    return true;
                }
            }
            return false;
        }
    }
}