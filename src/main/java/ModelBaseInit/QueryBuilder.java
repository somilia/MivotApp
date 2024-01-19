package ModelBaseInit;
import java.util.ArrayList;
import java.util.Collections;

public class QueryBuilder {
    /** This class is used to build the query to get the mapped column from the table
     * Columns in <model>_mivot are the followings:
     * ["instance_id", "mapped_table", "mapped_column", "dmtype", "dmrole", "dmerror", "frame", "ucd", "vocab", "mandatory"]
     **/
    String table;
    String model;
    ArrayList<String> columns_from_query;

    QueryBuilder(String table, String model, ArrayList<String> columns_from_query) {
        this.table = table;
        this.model = model;
        this.columns_from_query = columns_from_query;

    }

    public String getQuery(String column_quiered) {
        /** This method is used to build the query to get the mapped column from the table **/
        String query = "SELECT " + column_quiered;
        query += " FROM " + model + "_mivot WHERE mapped_table = '" + table + "'";
        return query;
    }

    public String getDmtypeQuery() {
        return this.getQuery("dmtype") + " AND mapped_column IN (" + String.join(", ", Collections.nCopies(columns_from_query.size(), "?")) + ")";
    }

    public String getMandatoryColumnsQuery(String dmtype) {
        return this.getQuery("mapped_column") + " AND mandatory = 'true' AND dmtype = '" + dmtype + "'";
    }

    public String getMappedColumnQuery(String dmtype) {
        return this.getQuery("mapped_column") + " AND dmtype = '" + dmtype + "' AND mapped_column IN ("
                + String.join(", ", Collections.nCopies(columns_from_query.size(), "?")) + ")";
    }

    public String getDmroleQuery(String dmtype, String mapped_column) {
        return this.getQuery("dmrole") + " AND dmtype = '" + dmtype + "' AND mapped_column = '"+ mapped_column +"'";
    }

    public String getFrameQuery() {
        return this.getQuery("frame") + " AND mapped_column IN (" + String.join(", ", Collections.nCopies(columns_from_query.size(), "?")) + ")";
    }
}
