package ModelBaseInit;
import TAPConnection.MivotTAPFactory;
import TAPConnection.MivotTap;
import tap.TAPException;

import java.util.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;



public class ModelBaseInit {

    public MivotTap mivotTap = new MivotTap();

    public ModelBaseInit(){
        /** Default constructor **/

    }

    public ModelBase getModelBase(String table, String model, ArrayList<String> columns_from_query) throws TAPException {
        /** This method is used to initialize the ModelBase **/
        ModelBase modelBase = null;
        QueryBuilder queryBuilder = new QueryBuilder(table, model, columns_from_query);
        Map<String, List<String>> mappeable;
        mivotTap.init();
        try (Connection connection = getConnection()) {
            mappeable = this.getMappeableDmtype(connection, queryBuilder);
            modelBase = new ModelBase(mappeable);
            Set<String> dmtype_list = mappeable.keySet();
//            checkForFrame(connection, queryBuilder, modelBase);
            for (String dmtype : dmtype_list) {
                for (String mapped_column : mappeable.get(dmtype)) {
                    modelBase.addToDmtype(dmtype.replace("_",":"), getDmrole(connection, dmtype.replace(":","_"), mapped_column, queryBuilder).get(0).replace("_",":"), mapped_column);
                }
            }
            System.out.println("\nDictionary of dmtype: "+modelBase.dmtype_dict);
            freeConnection();
        } catch (TAPException | SQLException e) {
            e.printStackTrace();
        }
        return modelBase;
    }

    public static Connection getConnection() throws SQLException, TAPException {

        if (MivotTAPFactory.getJDBCConnection().isEmpty()) {
            throw new TAPException("Unable to get a connection to the database");
        }
        if (MivotTAPFactory.getJDBCConnection().isPresent()) {
            return MivotTAPFactory.getJDBCConnection().get().getInnerConnection();
        } else
            throw new TAPException("Unable to get a connection to the database");
    }

    public static void freeConnection() throws TAPException {
        MivotTAPFactory.freeJDBCConnection(MivotTAPFactory.getJDBCConnection().get());
    }

    public Map<String, List<String>> getMappeableDmtype(Connection connection, QueryBuilder queryBuilder) {
        /** Get a list of all dmtype that can be mapped from the table
         * mappeable format : {dmtype : [column1, column2, ...], ...} **/

        Map<String, List<String>> mappeable = new HashMap<>();
        ArrayList<String> dmtype_mappeable = new ArrayList<String>();
        ArrayList<String> dmtype_list = getAllDmtypes(connection, queryBuilder);
        ArrayList<String> dmtype_to_remove = new ArrayList<String>();
        System.out.println("classes concerned by the query : " + dmtype_list);

        for (String dmtype : dmtype_list) { // For each dmtype, we get the mandatory columns from the model and the queried columns from the table
            Boolean mandatory_column_missing = false;
            ArrayList<String> mandatory_columns = getModelColumnsFromDmtype(connection, dmtype, queryBuilder);
            ArrayList<String> table_columns = getTableColumnsFromDmtype(connection, dmtype, queryBuilder);
            System.out.println("\n-- dmtype : " + dmtype + " | mandatory columns : " + mandatory_columns + " | column queried from the table with this dmtype : " + table_columns);

            for (String column : mandatory_columns) { // We check if each mandatory columns is present in the query
                System.out.println(" - check for column : " + column);
                if (table_columns.contains(column)) {
                    System.out.println("  - mandatory column " + column + " is in the query");
                    if (!dmtype_mappeable.contains(dmtype)) {
                        dmtype_mappeable.add(dmtype);
                    }
                } else {
                    System.out.println("  - mandatory column " + column + " is NOT in the query");
                    mandatory_column_missing = true;
                }
            }
            if (mandatory_columns.isEmpty()) {
                dmtype_mappeable.add(dmtype);
            }
            if (mandatory_column_missing) {
                dmtype_to_remove.add(dmtype);
            } else {
                List<String> possibleColumns = getAllPossibleColumns(connection, dmtype, queryBuilder);
                mappeable.put(dmtype.replace("_",":"), possibleColumns);
            }

        }
        dmtype_mappeable.removeAll(dmtype_to_remove);
        System.out.println("dmtype_mappeable : " + dmtype_mappeable); // add mappeable columns
        System.out.println("mappeable : " + mappeable);
        return mappeable;
    }

    private ArrayList<String> getAllDmtypes(Connection connection, QueryBuilder queryBuilder) {
        /** Get a list of all dmtypes from the table **/
        ArrayList<String> classes_list;
        if (queryBuilder.columns_from_query.isEmpty()) {  // Get all dmtype from the table
            classes_list = executeMivotQuery(queryBuilder.getDmtypeQuery(), connection);
        } else {
            classes_list = PrepareAndExecuteStatement(queryBuilder.getDmtypeQuery(), connection, queryBuilder.columns_from_query);
        }
        return classes_list;
    }

    private ArrayList<String> getModelColumnsFromDmtype(Connection connection, String dmtype, QueryBuilder queryBuilder) {
        /** Get a list of all mandatory columns from the model for a specific dmtype **/
        return executeMivotQuery(queryBuilder.getMandatoryColumnsQuery(dmtype), connection);
    }

    private ArrayList<String> getTableColumnsFromDmtype(Connection connection, String dmtype, QueryBuilder queryBuilder) {
        /** Get a list of all columns present in the query from the table for a specific dmtype **/
        ArrayList<String> table_columns;
        if (queryBuilder.columns_from_query.isEmpty()) {  // Get all columns from the table for this dmtype
            table_columns = executeMivotQuery(queryBuilder.getMappedColumnQuery(dmtype), connection);
        } else {
            table_columns = PrepareAndExecuteStatement(queryBuilder.getMappedColumnQuery(dmtype), connection, queryBuilder.columns_from_query);
        }
        return table_columns;
    }

    private ArrayList<String> getAllPossibleColumns(Connection connection, String dmtype, QueryBuilder queryBuilder) {
        /** Get a list of all columns present in the query from the model for a specific dmtype **/
        ArrayList<String> table_columns;
        if (queryBuilder.columns_from_query.isEmpty()) {  // Get all columns from the table for this dmtype
            table_columns = executeMivotQuery(queryBuilder.getMappedColumnQuery(dmtype), connection);
        } else {
            table_columns = PrepareAndExecuteStatement(queryBuilder.getMappedColumnQuery(dmtype), connection, queryBuilder.columns_from_query);
        }
        return table_columns;
    }

    private ArrayList<String> getDmrole(Connection connection, String dmtype, String mapped_column, QueryBuilder queryBuilder) {
        /** Get a list of all dmroles from the model for a specific dmtype **/
        return executeMivotQuery(queryBuilder.getDmroleQuery(dmtype, mapped_column), connection);

    }
//    private void checkForFrame(Connection connection, QueryBuilder queryBuilder, ModelBase modelBase) {
//        /** Check if the frame is present in the query in 2 possible cases :
//         * - the frame is present in the columns of the query
//         * - the frame is not present in the query but columns of the query require a frame : LonLatPos
//         *  **/
//        if (queryBuilder.columns_from_query.contains("frame") || queryBuilder.columns_from_query.contains("equinox") || queryBuilder.columns_from_query.contains("epoch")) {
//            ArrayList<String> frame_list = executeMivotQuery(queryBuilder.getFrameQuery(), connection);
//            if (!frame_list.isEmpty()) {
//                modelBase.addToFrame("frame", frame_list.get(0));
//            }
//        } else if (queryBuilder.columns_from_query.contains("sc_ra") || queryBuilder.columns_from_query.contains("sc_dec") || queryBuilder.columns_from_query.contains("sc_err_min")) {
//            modelBase.error.add("frame");
//        }
//    }

    public static ArrayList<String> PrepareAndExecuteStatement(String query, Connection connection, ArrayList<String> parameters_from_query){
        /** Prepare and execute a query with parameters **/
        ArrayList<String> result = new ArrayList<String>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            // Set the parameter values for the placeholders
            for (int i = 0; i < parameters_from_query.size(); i++) {
                preparedStatement.setString(i + 1, parameters_from_query.get(i));
            }
            result = executeMivotQuery(preparedStatement.toString(), connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static ArrayList<String> executeMivotQuery(String querySQL, Connection connection) {
        /** Execute a query and return the result as an ArrayList **/
        ResultSet resultSet;
        ArrayList<String> listResult = new ArrayList<String>();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            if (querySQL.trim().toUpperCase().startsWith("SELECT")) {  // If it is a SELECT query,
                resultSet = statement.executeQuery(querySQL);          // we execute and treat the ResultSet
//                System.out.println("-- Executing query -- : " + querySQL);
                listResult = getListResult(resultSet);
            } else { // TODO : what to do in case of other query type ?
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
        /** Treat the ResultSet and return the result as an ArrayList **/
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

    public static void main(String[] args) throws TAPException {
        ArrayList<String> col_to_query = new ArrayList<>();
        col_to_query.add("sc_ra");
        col_to_query.add("sc_pm_ra");
        col_to_query.add("sc_dec");
        col_to_query.add("sc_err_min");
        ModelBaseInit modelbaseInit = new ModelBaseInit();
        modelbaseInit.getModelBase("epic_src","mango", col_to_query);

    }
}
