package ModelBaseInit;

import java.util.*;

public class ModelBase {
    /**
     * This class is used to store the mapping between the columns of a table and the dmtype they are mapped to
     * dmtype_dict is a dictionary of dictionaries that stores the mapping between the columns of a table and the dmtype they are mapped to
     * dmtype_dict format : {dmtype1 : {dmrole1 : column, dmrole2 : column1, ...}, ...}
     **/

    public Map<String, Map<String, String>> dmtype_dict = new HashMap<>();
    Map<String, List<String>> mappeable = new HashMap<String, List<String>>();

    // frame format : {frame : "FK5", equinox : "2000.0", epoch : "2000.0"}
    public String frame = "FK5";
    public ArrayList<String> error = new ArrayList<String>();

    public ModelBase(Map<String, List<String>> mappeable) {
        this.mappeable = mappeable;
    }

    public void addToDmtype(String dmtype, String dmrole, String column) {
        /** This method is used to initialize the dmtype_dict for dmtype as a key, it adds new element to his dictionary **/
        Map<String, String> dmtype_dict1;
        if (dmtype_dict.containsKey(dmtype)) {
            dmtype_dict1 = (Map<String, String>) dmtype_dict.get(dmtype);
        } else {
            dmtype_dict1 = new HashMap<>();
        }
        dmtype_dict1.put(dmrole, column);
        dmtype_dict.put(dmtype, dmtype_dict1);
    }

//    public void addToFrame(String caracteristic, String value) {
//        /** This method is used to add a column to the frame list **/
//        frame.put(caracteristic, value);
//    }

    public ArrayList<String> getAllDmtypeKeys() {
        return new ArrayList<String>(dmtype_dict.keySet());
    }

    public Map<String, String> getDictForDmtype(String dmtype) {
        return dmtype_dict.get(dmtype);
    }
    public boolean DmrolePresentInDict(String dmtype, String dmrole) {
        return dmtype_dict.get(dmtype).containsKey(dmrole);
    }


}

