package ModelBaseInit;

import java.util.*;

public class ModelBase {
    /**
     * This class is used to store the mapping between the columns of a table and the dmtype they are mapped to
     * dmtype_dict is a dictionary of dictionaries that stores the mapping between the columns of a table and the dmtype they are mapped to
     * dmtype_dict format : {dmtype1 : {dmrole1 : column, dmrole2 : column1, ...}, ...}
     **/
    public String model_name;
    public String model_url;
    public Map<String, Map<String, String>> dmtype_dict = new HashMap<>();
    public Map<String, String> link_id = new HashMap<>(); // Todo : remove this feature if we don't use it
    public Map<String, String> snippet = new HashMap<>();
    public Map<String, String> frame; // TODO : add frame feature
    public Map<String, String> error; // Format : {dmtype : dmerror}

    public ModelBase() {
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
    public ArrayList<String> getAllDmtypeKeys() {
        return new ArrayList<String>(dmtype_dict.keySet());
    }
    public Map<String, String> getDictForDmtype(String dmtype) {
        return dmtype_dict.get(dmtype);
    }
}

