/*This class is used to get files
 * JSON
 * XML
 * and also used to get the path 
 * */

package utils;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class FileGetter {

	private static String JSON_LOCATION = "mappingComponent/Profiles/";
	private static String XML_LOCATION = "mappingComponent/Components/";
	
	public static File getJSONFile(String fileName) throws URISyntaxException{
		System.out.println("getting the JSONfile");
		URL resource;
	
		try {
			resource =FileGetter.class.getClassLoader().getResource(JSON_LOCATION + fileName);
			System.out.println("we got the JSONfile");
			System.out.println(resource.toURI());
			return(new File(resource.toURI()));	

		} catch (NullPointerException e) {
			System.out.println("there's no JSONFile");
			return null;
		}
	
	}
	
	public static File getXMLFile(String fileName) throws URISyntaxException{
		System.out.println("getting the XMLFile");
		URL resource = FileGetter.class.getClassLoader().getResource(XML_LOCATION+fileName);
		System.out.println("we got the XMLfile");
		System.out.println(resource.toURI());
		return(new File(resource.toURI()));	
	}
	
	public static String getFileResource(String fileName) throws URISyntaxException {
		URL resource = null;
		if(fileName.contains(".xml")) {
			 resource = FileGetter.class.getClassLoader().getResource(XML_LOCATION+fileName);
		}
		else if(fileName.contains(".json")) {
			resource =FileGetter.class.getClassLoader().getResource(JSON_LOCATION+fileName);
		}
		return resource.toURI().toString();
	}
	
}
