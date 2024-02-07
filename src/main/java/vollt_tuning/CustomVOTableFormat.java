package vollt_tuning;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import MangoMivotBuild.MangoMivotBuilder;
import adql.query.SelectAllColumns;
import adql.query.SelectItem;
import adql.query.operand.ADQLColumn;
import org.xml.sax.SAXException;
import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPExecutionReport;
import tap.config.ConfigurableServiceConnection;
import tap.formatter.VOTableFormat;
//import model.AnnotationBuilder;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOSerializer;
import uk.ac.starlink.votable.VOTableVersion;
import utils.FileGetter;
import utils.XMLUtils;
import uws.service.log.UWSLog.LogLevel;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

public class CustomVOTableFormat extends VOTableFormat {

	/* **********************************************************************
	   * NOTE:                                                              *
	   *   Attribut de classe de VOTableFormat intéressant pour accéder à   *
	   *   la base de données ou métadonnées:                               *
	   *                                                                    *
	   *                     ServiceConnection service;                     *
	   *                                                                    *
	   ********************************************************************** */

	/**
	 * NOTE:
	 *   Il doit y avoir au moins un constructeur avec un seul paramètre de type
	 *   ServiceConnection. Il est possible d'avoir d'autres constructeurs, mais
	 *   en passant seulement par le fichier de configuration, seul celui
	 *   ci-dessous sera appelé.
	 */
	
	//MappingBuilder mapBuild;
	public CustomVOTableFormat(final ServiceConnection service) throws NullPointerException {
		//super(service);                                             // Serialisation et version par défaut: BINARY et 1.3
		super(service, DataFormat.TABLEDATA);                     // Pour préciser une sérialisation différente
		//super(service, null, VOTableVersion.V12);                 // Serialisation par défaut (BINARY) et version précise
		//super(service, DataFormat.TABLEDATA, VOTableVersion.V12); // Serialisation et version personnalisées
		
		// Si vous voulez aussi changer le type MIME associé avec ce format:
		//setMimeType(mimeType, shortForm);
	}

	@Override
	
	//remettre en protected
	public void writeHeader(final VOTableVersion votVersion, final TAPExecutionReport execReport, final BufferedWriter out) throws IOException, TAPException {
        this.service.getLogger().log(LogLevel.INFO, "IHSANE", "test 1 : affiché? ", null);
        /* ******************************************************************
         *                                                                *
         * NOTE:                                                          *
         *   Tout ce qui suit est un copier-coller de la fonction         *
         *   VOTableFormat.writeHeader(...). A changer selon les besoins  *
         *   mais attention à respecter le schéma VOTable et les headers  *
         *   TAP (surtout la description des colonnes). Le plus simple    *
         *   étant d'ajouter les headers nécessaires à la fin (cf NOTE    *
         *   plus bas).                                                   *
         *                                                                *
         ****************************************************************** */
        //récupération de la requete à annote

        String query = execReport.parameters.getQuery();
        // définir le noeud racine votable
        out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        out.newLine();
        out.write("<VOTABLE" + VOSerializer.formatAttribute("version", votVersion.getVersionNumber()) + VOSerializer.formatAttribute("xmlns", votVersion.getXmlNamespace()) + VOSerializer.formatAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance") + VOSerializer.formatAttribute("xsi:schemaLocation", votVersion.getXmlNamespace() + " " + votVersion.getSchemaLocation()) + ">");
        out.newLine();

        // The RESOURCE note MUST have a type "results":	[REQUIRED]
        out.write("<RESOURCE type=\"results\">");
        out.newLine();

        String tableName = "";
        ArrayList<String> column_names = new ArrayList<String>();
        this.service.getLogger().log(LogLevel.INFO, "IHSANE", "test 2 affiché?", null); //affiché

        MangoMivotBuilder mangoMivotBuilder = null;
        try {

            this.service.getLogger().log(LogLevel.INFO, "IHSANE", "test 3 : affiché? ", null);

            tableName = this.service.getFactory().createADQLParser().parseQuery(query).getFrom().getName();
            this.service.getLogger().log(LogLevel.INFO, "IHSANE", tableName, null);
//			boolean tableExist = AnnotationBuilder.tableExist(tableName);
            boolean tableExist = true;

            //si la table existe
            if (tableExist == true) {

                this.service.getLogger().log(LogLevel.INFO, "IHSANE", "hello", null);

                for (SelectItem selectItems : this.service.getFactory().createADQLParser().parseQuery(query).getSelect()) {
                    if (selectItems.getOperand() instanceof ADQLColumn) {
                        System.out.println("we add column_name in its arraylist :" + selectItems.getOperand().toString());
                        this.service.getLogger().log(LogLevel.INFO, "IHSANE", "we add column_name in its arraylist :" + selectItems.getOperand().toString(), null);
                        column_names.add(selectItems.getOperand().toString());
                    } else if (selectItems instanceof SelectAllColumns) {
                        // SearchColumnList starColumns = ((ADQLColumn) selectItems.getOperand()).getAdqlTable().getDBColumns();
                        this.service.getLogger().log(LogLevel.INFO, "IHSANE", "we add all columns :" + selectItems.getName(), null);
                        column_names.add(selectItems.getName());
                    }
                }
                this.service.getLogger().log(LogLevel.INFO, "IHSANE", "test 4 : affiché? ", null);

				System.out.println(FileGetter.getXMLFile("subfile_annoted.mango.xml"));
				System.out.println(Arrays.toString(new File(String.valueOf(FileGetter.getXMLFile("subfile_annoted.mango.xml"))).listFiles()));
//				mangoMivotBuilder = new MangoMivotBuilder(tableName, column_names);

                System.out.println("ca marche ");
                System.out.println(column_names);


                //Faire des test pour voir si ca marche toujours pas
                //this.service.getLogger().log(LogLevel.INFO, "Generate XML block", annotationBuilder.generateXMLblock(), null); //affiché
                this.service.getLogger().log(LogLevel.INFO, "IHSANE", "test 5 : affiché? ", null);
            } else {
                this.service.getLogger().log(LogLevel.INFO, "IHSANE", "la table n'existe pas ", null);
                //out.write(AnnotationBuilder.buildFailedTableAnnotation(tableName));
            }

        } catch (Exception e1) {
            // TODO Auto-generated catch block
            //report avec buildFailedAnnotation.
            StringWriter errors = new StringWriter();
            e1.printStackTrace(new PrintWriter(errors));
            out.write(CustomVOTableFormat.buildFailedAnnotation(errors.toString()));

        }

        //out.newLine();
        if (mangoMivotBuilder == null) {
			out.write(CustomVOTableFormat.buildFailedAnnotation("Couldn't resolve annotation : no profiles for table :" + tableName + "."));
		}
		else if (mangoMivotBuilder.isMangoMappeable) {
			out.write(XMLUtils.xmlToString(mangoMivotBuilder.doc));
        } else {
            out.write(CustomVOTableFormat.buildFailedAnnotation("Couldn't resolve annotation : no profiles for table :" + tableName + "."));
        }
        // Indicate that the query has been successfully processed:	[REQUIRED]
//        out.write("<INFO name=\"QUERY_STATUS\" value=\"OK\"/>");
//        out.newLine();
//
//        // Append the PROVIDER information (if any):	[OPTIONAL]
//        if (service.getProviderName() != null) {
//            out.write("<INFO name=\"PROVIDER\"" + VOSerializer.formatAttribute("value", service.getProviderName()) + ">" + ((service.getProviderDescription() == null) ? "" : VOSerializer.formatText(service.getProviderDescription())) + "</INFO>");
//            out.newLine();
//        }
//
//        // Append the ADQL query at the origin of this result:	[OPTIONAL]
//        String adqlQuery = execReport.parameters.getQuery();
//        if (adqlQuery != null) {
//            out.write("<INFO name=\"QUERY\"" + VOSerializer.formatAttribute("value", adqlQuery) + "/>");
//            out.newLine();
//        }
//
//        // Append the fixed ADQL query, if any:	[OPTIONAL]
//        String fixedQuery = execReport.fixedQuery;
//        if (fixedQuery != null) {
//            out.write("<INFO name=\"QUERY_AFTER_AUTO_FIX\"" + VOSerializer.formatAttribute("value", fixedQuery) + "/>");
//            out.newLine();
//        }


        // Insert the definition of all used coordinate systems:
		/*HashSet<String> insertedCoosys = new HashSet<String>(10);
		for(DBColumn col : execReport.resultingColumns) {
			// ignore columns with no coossys:
			if (col instanceof TAPColumn && ((TAPColumn)col).getCoosys() != null) {
				// get its coosys:
				TAPCoosys coosys = ((TAPColumn)col).getCoosys();
				// insert the coosys definition ONLY if not already done because of another column:
				if (!insertedCoosys.contains(coosys.getId())) {
					// write the VOTable serialization of this coordinate system definition:
					out.write("<COOSYS" + VOSerializer.formatAttribute("ID", coosys.getId()));
					if (coosys.getSystem() != null)
						out.write(VOSerializer.formatAttribute("system", coosys.getSystem()));
					if (coosys.getEquinox() != null)
						out.write(VOSerializer.formatAttribute("equinox", coosys.getEquinox()));
					if (coosys.getEpoch() != null)
						out.write(VOSerializer.formatAttribute("epoch", coosys.getEpoch()));
					out.write(" />");
					out.newLine();
					// remember this coosys has already been written:
					insertedCoosys.add(coosys.getId());
				}
			}
		}*/


        /* ******************************************************************
         *                                                                *
         * NOTE:                                                          *
         *   Ajouter les nouveaux header ici!                             *
         *                                                                *
         ****************************************************************** */

        out.flush();
    }



	/**
	 * @param doc the document to convert
	 * @return a string that matches the content of the document
	 * 
	 * This method is used to convert an xml Document to a String
	 */
	/**
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
	        
	        System.out.println(doc.getClass());
	        
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
	 * @param xml the XML string we want to beautify
	 * @param indent the number of indentations we want
	 * @return a beautified XML string
	 */
/**
	public static String toPrettyString(String xml, int indent) {
	    try {
	        // Turn xml string into a document
	        Document document = DocumentBuilderFactory.newInstance()
	                .newDocumentBuilder()
	                .parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));

	        // Remove whitespaces outside tags
	        document.normalize();
	        XPath xPath = XPathFactory.newInstance().newXPath();
	        NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']",
	                                                      document,
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
	}**/
	
	public static String buildFailedAnnotation(Object object) {
		//String mappBuildError = new MappingBuilder().getMapping();
		String vodml = "<VODML xmlns=\"http://ivoa.net/xml/merged-synthax\">\n";
		String report = "<REPORT status=\"KO\">";
		
		String endOfMapp = "</REPORT>\n</VODML>\n";
		//mappBuildError += vodml + report + object + endOfMapp;
		String mappBuildError = vodml + report + object + endOfMapp;

		return mappBuildError;
	}

}


