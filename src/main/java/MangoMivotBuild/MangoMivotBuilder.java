package MangoMivotBuild;
import ModelBaseInit.ModelBase;
import ModelBaseInit.ModelBaseInit;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.TreeWalker;
import org.xml.sax.SAXException;
import tap.TAPException;
import utils.WalkerMivotManager;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;


public class MangoMivotBuilder {
    /**
     * This class will build the annotation for the model mango, using the mapping from the ModelBase class
     * and getting from this mapping the snippets of this annotation.
     **/
    private ModelBase modelBase;
    private ModelBaseInit ModelbaseInit = new ModelBaseInit();

    public String SnippetPath;

    MangoMivotBuilder() throws TAPException, ParserConfigurationException, IOException, URISyntaxException, TransformerException, SAXException {
        ArrayList<String> col_to_query = new ArrayList<>();
        col_to_query.add("sc_ra");
        col_to_query.add("sc_pm_ra");
        col_to_query.add("sc_dec");
        col_to_query.add("sc_pm_dec");
        col_to_query.add("sc_err_min");
        this.modelBase = ModelbaseInit.getModelBase("epic_src","mango", col_to_query);
        this.SnippetPath = "src/main/webapp/mivot_snippets/";
        for (String dmtype : modelBase.getAllDmtypeKeys()) {
            buildAnnotations("mango", dmtype);
        }

    }
    public TreeWalker buildAnnotations(String model, String dmtype) throws ParserConfigurationException, IOException, URISyntaxException, TransformerException, SAXException {
        /**
         * This method will build the annotation for the model
         **/
        Document doc = WalkerMivotManager.getTraversal(new File(getPath(model, dmtype)));
        TreeWalker snippet = WalkerMivotManager.getWalker(new File(getPath(model, dmtype)), doc);
        resolve_ref(snippet, modelBase, dmtype);

        WalkerMivotManager.cleanUpNode(snippet);
//        WalkerMivotManager.printWalker(snippet);

        WalkerMivotManager.convertWalkerToFile(snippet, "subfile_annoted.mango.xml", doc);

        return snippet;
    }
    private String getPath(String model, String dmtype) {
        /**
         * This method will get the snippet xml from /src/main/webapp/mivot_snippets
         **/
        return (new File(this.SnippetPath + model + "/" + dmtype.replace(":",".") + ".xml").getAbsolutePath());
    }

    public static void resolve_ref(TreeWalker walker, ModelBase md, String dmtype) {
        Map<String, String> targetDict = md.getDictForDmtype(dmtype);
        do {
            Node current = walker.getCurrentNode();
            if (current.hasAttributes()) {
                if (current.getAttributes().getNamedItem("dmrole") != null){
                    if (current.getAttributes().getNamedItem("dmrole").getNodeValue().endsWith("coordSys")) {
                        System.out.println("we found the frame : " + current.getAttributes().getNamedItem("dmrole").getNodeValue());
                        walker.getCurrentNode().getParentNode().appendChild(addStaticReference(walker, current.getAttributes().getNamedItem("dmrole").getNodeValue(), md.frame));
                        walker.nextNode();
                    }
                    else
                    if (targetDict.containsKey(current.getAttributes().getNamedItem("dmrole").getNodeValue())) {
                        WalkerMivotManager.changeAttributeValue(walker, "dmrole",
                                current.getAttributes().getNamedItem("dmrole").getNodeValue(), "ref",
                                targetDict.get(current.getAttributes().getNamedItem("dmrole").getNodeValue()));
                    }
                }
            }
        } while (walker.nextNode() != null);
    }

    public static Node addStaticReference(TreeWalker walker, String dmrole, String dmrefValue) {
        /** Add static reference as the next node
         * exemple with frame : <REFERENCE dmrole="mango:EpochPosition.coordSys" dmref="FK5" />
         **/
        Node parent = walker.getCurrentNode();
        Node ref = parent.getOwnerDocument().createElement("REFERENCE");
        ref.getAttributes().setNamedItem(parent.getOwnerDocument().createAttribute("dmrole"));
        ref.getAttributes().getNamedItem("dmrole").setNodeValue(dmrole);
        ref.getAttributes().setNamedItem(parent.getOwnerDocument().createAttribute("dmref"));
        ref.getAttributes().getNamedItem("dmref").setNodeValue(dmrefValue);
        return ref;
    }

    public static void main(String[] args) throws TAPException, ParserConfigurationException, IOException, URISyntaxException, TransformerException, SAXException {
        MangoMivotBuilder mangoMivotBuilder = new MangoMivotBuilder();
    }
}
