package ModelBase;
import java.util.ArrayList;
import java.util.Collections;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;



public class ModelBaseInit {
    public ModelBase ModelBase = new ModelBase();
    public String table;
    private ArrayList<String> columns_from_query;
    private ArrayList<String> classes_list;
    private ArrayList<String> table_columns;

    // JDBC driver name and database URL
    private static final String jdbcUrl = "jdbc:postgresql://localhost:5432/mivot_db";
    private static final String username = "saadmin";
    private static final String password = "";

    // Load the JDBC driver
    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    public ModelBaseInit(String table, ArrayList<String> columns_from_query) {
        this.table = table;
        this.columns_from_query = columns_from_query;
        ArrayList<String> mappeable_dmtype = new ArrayList<String>();

        try (Connection connection = getConnection()) {
            mappeable_dmtype = this.getMappeableDmtype(connection, table, columns_from_query);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getMappeableDmtype(Connection connection, String table, ArrayList<String> columns_from_query) {

        ArrayList<String> dmtype_mappeable = new ArrayList<String>();
        ArrayList<String> dmtype_list = getAllDmtypes(connection, table, columns_from_query);
        ArrayList<String> dmtype_to_remove = new ArrayList<String>();
        System.out.println("classes concerned by the query : " + dmtype_list);

        for (String dmtype : dmtype_list) { // For each dmtype, we get the mandatory columns from the model and the queried columns from the table
            Boolean mandatory_column_missing = false;
            ArrayList<String> mandatory_columns = getModelColumnsFromDmtype(connection, table, dmtype);
            ArrayList<String> table_columns = getTableColumnsFromDmtype(connection, table, dmtype, columns_from_query);
            System.out.println("\n-- dmtype : " + dmtype + " | mandatory columns : " + mandatory_columns + " | column queried from the table with this dmtype : " + table_columns);

            for (String column : mandatory_columns) { // We check if each mandatory columns is present in the query
                System.out.println(" - check for column : " + column);
                if (table_columns.contains(column)) {
                    System.out.println("  - mandatory column " + column + " is in the query");
                    if (!dmtype_mappeable.contains(dmtype)) {
                        dmtype_mappeable.add(dmtype);
                    }
                }
                else {
                    System.out.println("  - mandatory column " + column + " is NOT in the query");
                    mandatory_column_missing = true;
                }
            }
            if (mandatory_column_missing) {
                dmtype_to_remove.add(dmtype);
            }
            if (mandatory_columns.isEmpty()) {
                dmtype_mappeable.add(dmtype);
            }
        }
        System.out.println("dmtype_non_mappeable : " + dmtype_to_remove);
        dmtype_mappeable.removeAll(dmtype_to_remove);
        System.out.println("dmtype_mappeable : " + dmtype_mappeable);
        return dmtype_mappeable;
    }

    private ArrayList<String> getAllDmtypes(Connection connection, String table, ArrayList<String> columns_from_query) {
        ArrayList<String> classes_list = new ArrayList<String>();
        if (columns_from_query.isEmpty()) {  //classes_list = get dmtype in table
            classes_list = executeMivotQuery("SELECT dmtype FROM mango_mivot", connection);
        } else { //classes_list = get dmtype in columns_from_query | SELECT dmtype FROM table WHERE mapped_column = (columns_from_query)
            String query = "SELECT dmtype FROM mango_mivot WHERE mapped_column IN (" + String.join(", ", Collections.nCopies(columns_from_query.size(), "?")) + ")";
            classes_list = getStrings(connection, columns_from_query, classes_list, query);
        }
        return classes_list;
    }

    private ArrayList<String> getModelColumnsFromDmtype(Connection connection, String table, String dmtype) {
        //mivot_mandatory_columns = get columns from dmtype in table
        ArrayList<String> mivot_mandatory_columns = new ArrayList<String>();
        mivot_mandatory_columns = executeMivotQuery("SELECT mapped_column FROM mango_mivot WHERE mandatory = 'true' AND dmtype = '" + dmtype + "'", connection);
        return mivot_mandatory_columns;
    }

    private ArrayList<String> getTableColumnsFromDmtype(Connection connection, String table, String dmtype, ArrayList<String> columns_from_query) {
        ArrayList<String> table_columns = new ArrayList<String>();
        table_columns = executeMivotQuery("SELECT mapped_column FROM mango_mivot WHERE dmtype = '" + dmtype + "'", connection);
        if (columns_from_query.size() == 0) {
            //table_columns = get columns from dmtype in table
            table_columns = executeMivotQuery("SELECT mapped_column FROM mango_mivot WHERE dmtype = '" + dmtype + "'", connection);
        } else {
            //table_columns = get columns from dmtype in table where mapped_column = columns_from_query
            String query = "SELECT mapped_column FROM mango_mivot WHERE dmtype = '" + dmtype + "' AND mapped_column IN (" + String.join(", ", Collections.nCopies(columns_from_query.size(), "?")) + ")";
            table_columns = getStrings(connection, columns_from_query, table_columns, query);
        }
        return table_columns;
    }

    private ArrayList<String> getStrings(Connection connection, ArrayList<String> columns_from_query, ArrayList<String> table_columns, String query) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            // Set the parameter values for the placeholders
            for (int i = 0; i < columns_from_query.size(); i++) {
                preparedStatement.setString(i + 1, columns_from_query.get(i));
            }
            table_columns = executeMivotQuery(preparedStatement.toString(), connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return table_columns;
    }

    public static ArrayList<String> executeMivotQuery(String querySQL, Connection connection) {
        ResultSet resultSet = null;
        ArrayList<String> listResult = new ArrayList<String>();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            if (querySQL.trim().toUpperCase().startsWith("SELECT")) {
                // If it is a SELECT query, we execute and treat the ResultSet
                resultSet = statement.executeQuery(querySQL);
//                System.out.println("-- Executing query -- : " + querySQL);
                listResult = getListResult(resultSet);
            } else {
                int rowCount = statement.executeUpdate(querySQL);
                System.out.println("Nombre de lignes affectées : " + rowCount);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (statement != null) {  // To free resource statement
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return listResult;
    }

    private static ArrayList<String> getListResult(ResultSet resultSet) throws SQLException {
        ArrayList<String> listResult = new ArrayList<String>();
        while (resultSet.next()) {
            for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                if (!listResult.contains(resultSet.getString(i))) // remove duplicate
                    listResult.add(resultSet.getString(i));
            }
        }
        resultSet.close();
        return listResult;
    }

    public static void main(String[] args) {
        ArrayList<String> col_to_query = new ArrayList<>();
        col_to_query.add("sc_ra");
        col_to_query.add("sc_pm_ra");
        col_to_query.add("sc_pm_dec");
        col_to_query.add("sc_err_min");
        try (Connection connection = getConnection()) {

            ModelBaseInit ModelbaseInit = new ModelBaseInit("mango_mivot", col_to_query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
