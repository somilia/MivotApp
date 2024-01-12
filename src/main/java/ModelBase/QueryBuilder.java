package ModelBase;
import java.util.ArrayList;
import java.util.Collections;

public class QueryBuilder {
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
    //"SELECT mapped_column FROM mango_mivot WHERE mapped_table = ? AND dmtype = '" + dmtype + "' AND mapped_column IN (" + String.join(", ", Collections.nCopies(columns_from_query.size(), "?")) + ")";
    public String getMappedColumnQuery(String dmtype) {
        return this.getQuery("mapped_column") + " AND dmtype = '" + dmtype + "' AND mapped_column IN ("
                + String.join(", ", Collections.nCopies(columns_from_query.size(), "?")) + ")";
    }
}
