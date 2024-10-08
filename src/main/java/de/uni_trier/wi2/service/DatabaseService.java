package de.uni_trier.wi2.service;

import de.uni_trier.wi2.*;
import de.uni_trier.wi2.error.*;
import org.jetbrains.annotations.*;
import org.springframework.stereotype.*;
import org.xml.sax.*;

import javax.xml.*;
import javax.xml.transform.stream.*;
import javax.xml.validation.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

import static de.uni_trier.wi2.service.IOUtils.*;


/**
 * The service implementing all business logic for managing the database.
 */
@Service
public class DatabaseService {

    // ------------------------------------------------- Connection ------------------------------------------------- //
    private static String url = null;
    private static String username = null;
    private static String password = null;
    public static Connection connection;

    public static void setUrlUsernamePassword(String url, String username, String password) {
        DatabaseService.url = url;
        DatabaseService.username = username;
        DatabaseService.password = password;
    }

    /**
     * <p>Connects to the database.</p>
     *
     * @return String containing a status message
     */
    @NotNull
    public static String connectToDatabase() throws ClassNotFoundException, SQLException {

        Class.forName("com.mysql.cj.jdbc.Driver");
        connection = DriverManager.getConnection(url, username, password);
        connection.prepareStatement("SHOW TABLES").execute();

        return "Connected to database";
    }


    // --------------------------------------------------- Service -------------------------------------------------- //

    /**
     * <p>
     * Inserts a log into the database and returns an array of Strings containing the
     * log's id (at index 0) and the traces' id's.
     * </p>
     *
     * @param xes String containing an XES-log
     * @return String array of UUID's
     * @throws XESnotValidException
     * @throws SQLException
     * @throws IOException
     * @throws SAXException
     */
    public static String[] putLog(String xes) throws XESnotValidException, SQLException, IOException, SAXException {

        //HeapSpace.measure();

        // validate the XES
        //if (!logIsValid(xes)) throw new XESnotValidException(xes);

        // Split the log into the header and the traces
        String[] splitXES = xes.split("<trace");
        StringBuilder header = new StringBuilder(splitXES[0]);

        // Put traces in list
        // Complete header
        ArrayList<String> traces = new ArrayList<>();
        String[] splitTrace;
        String trace;
        for (int i = 1; i < splitXES.length; i++) {
            splitTrace = splitXES[i].split("</trace>");
            trace = "<trace" + splitTrace[0] + "</trace>";
            traces.add(trace);
            if (splitTrace.length > 1) {
                header.append(splitTrace[1]);
            }
            //HeapSpace.measure();
        }

        // create logID
        String logID = UUID.randomUUID().toString();

        // insert log
        insertInto(
                DATABASE_NAMES.TABLENAME__log,

                new String[]{
                        DATABASE_NAMES.COLUMNNAME__log__logID,
                        DATABASE_NAMES.COLUMNNAME__log__header,
                        DATABASE_NAMES.COLUMNNAME__log__removed,
                },
                new Object[]{
                        logID,
                        header.toString().replace(System.lineSeparator(), ""),
                        false});

        // array for memorizing id's to be returned
        String[] ids = new String[traces.size() + 1];
        ids[0] = logID;


        // insert traces
        Object[][] values = new Object[traces.size()][4];
        for (int i = 0; i < traces.size(); i++) {

            String traceID = UUID.randomUUID().toString();
            values[i] = new Object[]{
                    traceID,
                    logID,
                    traces.get(i),
                    false
            };
            ids[i + 1] = traceID;

        }
        insertIntoValues(
                DATABASE_NAMES.TABLENAME__trace,

                new String[]{
                        DATABASE_NAMES.COLUMNNAME__trace__traceID,
                        DATABASE_NAMES.COLUMNNAME__trace__logID,
                        DATABASE_NAMES.COLUMNNAME__trace__xes,
                        DATABASE_NAMES.COLUMNNAME__trace__removed,
                },
                values
        );

        //HeapSpace.measure();

        return ids;
    }

    /**
     * <p>Returns a {@link Map} object assigning objects to {@link String}s, representing the log's database entry.</p>
     *
     * <p>The value in that map are
     * <ol>
     *     <li>the log's id (String),</li>
     *     <li>the log's header (String) &</li>
     *     <li>the removed flag (Boolean).</li>
     * </ol></p>
     *
     * @param logID UUID of log
     * @return {@link Map} representing the database entry
     * @throws SQLException if the log does not exist in the database or if there was a problem with the sql query
     */
    public static Map<String, Object> getLog(String logID) throws SQLException {


        ResultSet resultSet = selectFrom(DATABASE_NAMES.TABLENAME__log,
                new String[]{DATABASE_NAMES.COLUMNNAME__log__header, DATABASE_NAMES.COLUMNNAME__log__removed},
                DATABASE_NAMES.COLUMNNAME__log__logID + " = '" + logID + "'");

        if (!resultSet.next()) {

            throw new SQLException("Log not found in database.");
        }

        Map<String, Object> log = new HashMap<>();
        log.put(DATABASE_NAMES.COLUMNNAME__log__logID, logID);
        log.put(DATABASE_NAMES.COLUMNNAME__log__header, resultSet.getString(1));
        log.put(DATABASE_NAMES.COLUMNNAME__log__removed, resultSet.getBoolean(2));

        //HeapSpace.measure();

        return log;
    }

    /**
     * <p>Sets removed flag of requested log and all traces belonging to it to 'true'.</p>
     *
     * @param logID UUID of log
     * @return number of database entries updated (1 + number of traces belonging to that log)
     * @throws SQLException if no row was updated or if there was a problem with the sql query
     */
    public static int removeLog(String logID) throws SQLException {


        int rowsUpdated = update(
                DATABASE_NAMES.TABLENAME__log,
                new String[]{DATABASE_NAMES.COLUMNNAME__log__removed},
                new Object[]{true},
                DATABASE_NAMES.COLUMNNAME__log__logID + " = '" + logID + "'"
        );

        if (rowsUpdated < 1) {

            throw new SQLException("Log not found in database.");
        }

        rowsUpdated += update(
                DATABASE_NAMES.TABLENAME__trace,
                new String[]{DATABASE_NAMES.COLUMNNAME__trace__removed},
                new Object[]{true},
                DATABASE_NAMES.COLUMNNAME__trace__logID + " = '" + logID + "'"
        );


        return rowsUpdated;
    }

    /**
     * <p>Returns a {@link Map} object assigning objects to {@link String}s, representing the trace's database entry.</p>
     *
     * <p>The value in that map are
     * <ol>
     *     <li>the traces's id (String),</li>
     *     <li>the id of the log the trace belongs to (String),</li>
     *     <li>the traces's xes data (String)&</li>
     *     <li>the removed flag (Boolean).</li>
     * </ol></p>
     *
     * @param traceID UUID of trace
     * @return {@link Map} representing the database entry
     * @throws SQLException if the trace does not exist in the database or if there was a problem with the sql query
     */
    public static Map<String, Object> getTrace(String traceID) throws SQLException {


        ResultSet resultSet = selectFrom(
                DATABASE_NAMES.TABLENAME__trace,
                new String[]{
                        DATABASE_NAMES.COLUMNNAME__trace__logID,
                        DATABASE_NAMES.COLUMNNAME__trace__xes,
                        DATABASE_NAMES.COLUMNNAME__trace__removed
                },
                DATABASE_NAMES.COLUMNNAME__trace__traceID + " = '" + traceID + "'");

        if (!resultSet.next()) {

            throw new SQLException("Trace not found in database.");
        }

        Map<String, Object> trace = new HashMap<>();
        trace.put(DATABASE_NAMES.COLUMNNAME__trace__traceID, traceID);
        trace.put(DATABASE_NAMES.COLUMNNAME__trace__logID, resultSet.getString(1));
        trace.put(DATABASE_NAMES.COLUMNNAME__trace__xes, resultSet.getString(2));
        trace.put(DATABASE_NAMES.COLUMNNAME__trace__removed, resultSet.getBoolean(3));

        //HeapSpace.measure();

        return trace;
    }

    /**
     * Returns all id's of traces that belong to the log with the given log-id
     *
     * @param logID UUID of log
     * @return {@link String} array of trace id's
     * @throws SQLException if the log does not exist in the database
     */
    public static String[] getTraceIDs(String logID) throws SQLException {


        ResultSet resultSet = selectFrom(
                DATABASE_NAMES.TABLENAME__trace,
                new String[]{DATABASE_NAMES.COLUMNNAME__trace__traceID},
                DATABASE_NAMES.COLUMNNAME__trace__logID + " = '" + logID + "'");

        if (!resultSet.next()) {

            return new String[0];
        }

        List<String> traceIDs = new ArrayList<>();
        String traceID = resultSet.getString(1);
        traceIDs.add(traceID);
        while (resultSet.next()) {
            traceID = resultSet.getString(1);
            traceIDs.add(traceID);
        }

        String[] traceIDsArray = traceIDs.toArray(new String[]{});

        //HeapSpace.measure();

        return traceIDsArray;
    }

    /**
     * Returns {@link List} of {@link Map}s representing traces belonging to log with given log id.
     *
     * @param logID UUID of log
     * @return {@link List} of {@link Map}s representing traces belonging to log
     * @throws SQLException if the log does not exist in the database or if there was a problem with the sql query
     */
    public static List<Map<String, Object>> getTraces(String logID) throws SQLException {


        String[] traceIDs = getTraceIDs(logID);
        List<Map<String, Object>> traces = new ArrayList<>();
        for (String traceID : traceIDs) traces.add(getTrace(traceID));

        //HeapSpace.measure();

        return traces;
    }

    /**
     * Returns id's of all logs stored in the database.
     *
     * @return {@link String} array of all log id's
     * @throws SQLException if there was a problem with the sql query
     */
    public static String[] getLogIDs(boolean includeRemoved) throws SQLException {


        String condition = "true";
        if (!includeRemoved) condition = DATABASE_NAMES.COLUMNNAME__log__removed + "= false";

        ResultSet resultSet = selectFrom(
                DATABASE_NAMES.TABLENAME__log,
                new String[]{DATABASE_NAMES.COLUMNNAME__log__logID},
                condition);

        if (!resultSet.next()) {

            return new String[0];
        }

        List<String> logIDs = new ArrayList<>();
        String logID = resultSet.getString(1);
        logIDs.add(logID);
        while (resultSet.next()) {
            logID = resultSet.getString(1);
            logIDs.add(logID);
        }

        String[] logIDsArray = logIDs.toArray(new String[]{});

        //HeapSpace.measure();

        return logIDsArray;
    }


    /**
     * <p>Puts a new metadata entry in the database.</p>
     *
     * <p>The metadata entry belongs to the trace whose id is given as an argument and if the given metadata type does
     * not exist yet, it is created.</p>
     *
     * @param traceID       UUID of trace
     * @param metadataType  name of metadata type
     * @param metadataValue value of metadata
     * @return id of metadata
     * @throws SQLException if the trace does not exist in the database or if there was a problem with the sql query
     */
    public static String putTraceMetadata(String traceID, String metadataType, String metadataValue) throws SQLException {


        // calling this function will throw an exception if the trace does not exist in the database
        getTrace(traceID);

        // insert new metadata
        ResultSet insertResult = insertInto(
                DATABASE_NAMES.TABLENAME__metadata,
                new String[]{DATABASE_NAMES.COLUMNNAME__metadata__value},
                new String[]{metadataValue});

        if (!insertResult.next()) throw new SQLException("No autogenerated key was returned.");
        String metadataID = insertResult.getString(1);

        // get metadataTypeID
        ResultSet typeIDs = selectFrom(
                DATABASE_NAMES.TABLENAME__metadataType,
                new String[]{DATABASE_NAMES.COLUMNNAME__metadataType__metadataTypeID},
                DATABASE_NAMES.COLUMNNAME__metadataType__name + " = '" + metadataType + "'");

        String typeID;
        if (typeIDs.next()) {
            typeID = typeIDs.getString(1);
        } else {
            // type does not exist yet
            insertResult = insertInto(
                    DATABASE_NAMES.TABLENAME__metadataType,
                    new String[]{DATABASE_NAMES.COLUMNNAME__metadataType__name},
                    new String[]{metadataType});

            if (!insertResult.next()) throw new SQLException("No autogenerated key was returned.");
            typeID = insertResult.getString(1);
        }

        insertInto(
                DATABASE_NAMES.TABLENAME__metadata_hasType,
                new String[]{
                        DATABASE_NAMES.COLUMNNAME__metadata_hasType__metadataID,
                        DATABASE_NAMES.COLUMNNAME__metadata_hasType__metadataTypeID
                },
                new String[]{
                        metadataID,
                        typeID});
        insertInto(
                DATABASE_NAMES.TABLENAME__metadata_belongsTo_trace,
                new String[]{
                        DATABASE_NAMES.COLUMNNAME__metadata_belongsTo_trace__metadataID,
                        DATABASE_NAMES.COLUMNNAME__metadata_belongsTo_trace__traceID
                },
                new String[]{
                        metadataID,
                        traceID});


        return metadataID;
    }

    /**
     * <p>Puts a new metadata entry in the database.</p>
     *
     * <p>The metadata entry belongs to the log whose id is given as an argument and if the given metadata type does
     * not exist yet, it is created.</p>
     *
     * @param logID         UUID of log
     * @param metadataType  name of metadata type
     * @param metadataValue value of metadata
     * @return id of metadata
     * @throws SQLException if the log does not exist in the database or if there was a problem with the sql query
     */
    public static String putLogMetadata(String logID, String metadataType, String metadataValue) throws SQLException {


        // calling this function will throw an exception if the log does not exist in the database
        getLog(logID);

        // insert new metadata
        ResultSet insertResult = insertInto(
                DATABASE_NAMES.TABLENAME__metadata,
                new String[]{DATABASE_NAMES.COLUMNNAME__metadata__value},
                new String[]{metadataValue});

        if (!insertResult.next()) throw new SQLException("No autogenerated key was returned.");
        String metadataID = insertResult.getString(1);

        // get metadataTypeID
        ResultSet typeIDs = selectFrom(
                DATABASE_NAMES.TABLENAME__metadataType,
                new String[]{DATABASE_NAMES.COLUMNNAME__metadataType__metadataTypeID},
                DATABASE_NAMES.COLUMNNAME__metadataType__name + " = '" + metadataType + "'");

        String typeID;
        if (typeIDs.next()) {
            typeID = typeIDs.getString(1);
        } else {
            // type does not exist yet
            insertResult = insertInto(
                    DATABASE_NAMES.TABLENAME__metadataType,
                    new String[]{DATABASE_NAMES.COLUMNNAME__metadataType__name},
                    new String[]{metadataType});

            if (!insertResult.next()) throw new SQLException("No autogenerated key was returned.");
            typeID = insertResult.getString(1);
        }

        insertInto(
                DATABASE_NAMES.TABLENAME__metadata_hasType,
                new String[]{
                        DATABASE_NAMES.COLUMNNAME__metadata_hasType__metadataID,
                        DATABASE_NAMES.COLUMNNAME__metadata_hasType__metadataTypeID
                },
                new String[]{
                        metadataID,
                        typeID});
        insertInto(
                DATABASE_NAMES.TABLENAME__metadata_belongsTo_log,
                new String[]{
                        DATABASE_NAMES.COLUMNNAME__metadata_belongsTo_log__metadataID,
                        DATABASE_NAMES.COLUMNNAME__metadata_belongsTo_log__logID
                },
                new String[]{
                        metadataID,
                        logID});


        return metadataID;
    }

    /**
     * <p>Returns a {@link Map} object representing metadata of the requested trace.</p>
     *
     * <p>The map's keys are {@link String}s denoting the type of the metadata. The respective value are Strings
     * denoting the metadata's value.</p>
     *
     * @param traceID UUID of trace
     * @return {@link Map} representing metadata
     * @throws SQLException if the trace does not exist in the database or if there was a problem with the sql query
     */
    public static Map<String, String> getTraceMetadata(String traceID) throws SQLException {


        // calling this function will throw an exception if the trace does not exist in the database
        getTrace(traceID);

        String join = String.format(
                "%s join %s join %s on %s.%s = %s.%s join %s on %s.%s = %s.%s join %s",
                DATABASE_NAMES.TABLENAME__trace,
                DATABASE_NAMES.TABLENAME__metadata_belongsTo_trace,
                DATABASE_NAMES.TABLENAME__metadata,
                DATABASE_NAMES.TABLENAME__metadata,
                DATABASE_NAMES.COLUMNNAME__metadata__metadataID,
                DATABASE_NAMES.TABLENAME__metadata_belongsTo_trace,
                DATABASE_NAMES.COLUMNNAME__metadata_belongsTo_trace__metadataID,
                DATABASE_NAMES.TABLENAME__metadata_hasType,
                DATABASE_NAMES.TABLENAME__metadata,
                DATABASE_NAMES.COLUMNNAME__metadata__metadataID,
                DATABASE_NAMES.TABLENAME__metadata_hasType,
                DATABASE_NAMES.COLUMNNAME__metadata_hasType__metadataID,
                DATABASE_NAMES.TABLENAME__metadataType
        );
        String[] columns = new String[]{
                DATABASE_NAMES.TABLENAME__metadataType + "." + DATABASE_NAMES.COLUMNNAME__metadataType__name,
                DATABASE_NAMES.TABLENAME__metadata + "." + DATABASE_NAMES.COLUMNNAME__metadata__value
        };
        String condition = DATABASE_NAMES.TABLENAME__trace + "." + DATABASE_NAMES.COLUMNNAME__trace__traceID + " = '" + traceID + "'";

        ResultSet resultSet = selectFrom(
                join,
                columns,
                condition);

        Map<String, String> metadata = new HashMap<>();
        while (resultSet.next()) {
            metadata.put(resultSet.getString(1), resultSet.getString(2));
        }


        return metadata;
    }

    /**
     * <p>Returns a {@link Map} object representing metadata of the requested log.</p>
     *
     * <p>The map's keys are {@link String}s denoting the type of the metadata. The respective value are Strings
     * denoting the metadata's value.</p>
     *
     * @param logID UUID of log
     * @return {@link Map} representing metadata
     * @throws SQLException if the log does not exist in the database or if there was a problem with the sql query
     */
    public static Map<String, String> getLogMetadata(String logID) throws SQLException {


        // calling this function will throw an exception if the log does not exist in the database
        getLog(logID);

        String join = String.format(
                "%s join %s join %s on %s.%s = %s.%s join %s on %s.%s = %s.%s join %s",
                DATABASE_NAMES.TABLENAME__log,
                DATABASE_NAMES.TABLENAME__metadata_belongsTo_log,
                DATABASE_NAMES.TABLENAME__metadata,
                DATABASE_NAMES.TABLENAME__metadata,
                DATABASE_NAMES.COLUMNNAME__metadata__metadataID,
                DATABASE_NAMES.TABLENAME__metadata_belongsTo_log,
                DATABASE_NAMES.COLUMNNAME__metadata_belongsTo_log__metadataID,
                DATABASE_NAMES.TABLENAME__metadata_hasType,
                DATABASE_NAMES.TABLENAME__metadata,
                DATABASE_NAMES.COLUMNNAME__metadata__metadataID,
                DATABASE_NAMES.TABLENAME__metadata_hasType,
                DATABASE_NAMES.COLUMNNAME__metadata_hasType__metadataID,
                DATABASE_NAMES.TABLENAME__metadataType
        );
        String[] columns = new String[]{
                DATABASE_NAMES.TABLENAME__metadataType + "." + DATABASE_NAMES.COLUMNNAME__metadataType__name,
                DATABASE_NAMES.TABLENAME__metadata + "." + DATABASE_NAMES.COLUMNNAME__metadata__value
        };
        String condition = DATABASE_NAMES.TABLENAME__log + "." + DATABASE_NAMES.COLUMNNAME__log__logID + " = '" + logID + "'";

        ResultSet resultSet = selectFrom(
                join,
                columns,
                condition);

        Map<String, String> metadata = new HashMap<>();
        while (resultSet.next()) {
            metadata.put(resultSet.getString(1), resultSet.getString(2));
        }


        return metadata;
    }


    // ----------------------------------------------------- SQL ---------------------------------------------------- //


    // - standard database operations -

    private static ResultSet selectFrom(String tableName, String[] attributeNames, String condition) throws SQLException {


        assert (tableName != null &&
                attributeNames != null &&
                condition != null);

        StringBuilder select = new StringBuilder("SELECT ");
        if (attributeNames.length > 0) select.append(attributeNames[0]);
        else select.append("*");
        for (int i = 1; i < attributeNames.length; i++) select.append(",").append(attributeNames[i]);
        select.append("\nFROM ").append(tableName).append("\nWHERE ").append(condition).append(";");
        ResultSet resultSet = connection.prepareStatement(select.toString()).executeQuery();


        return resultSet;
    }

    private static ResultSet insertInto(String tableName, String[] attributeNames, Object[] values) throws SQLException {


        assert (tableName != null &&
                attributeNames != null &&
                values != null);
        assert (attributeNames.length == values.length);

        StringBuilder insert = new StringBuilder("INSERT INTO " + tableName + " (");

        if (attributeNames.length > 0) insert.append(attributeNames[0]);
        for (int i = 1; i < attributeNames.length; i++) insert.append(",").append(attributeNames[i]);
        insert.append(")\nVALUES (");

        if (values.length > 0) insert.append("?");
        for (int i = 1; i < values.length; i++) insert.append(",?");
        insert.append(");");

        PreparedStatement insertStatement = connection.prepareStatement(insert.toString(), Statement.RETURN_GENERATED_KEYS);


        for (int i = 0; i < attributeNames.length; i++) {
            insertStatement.setObject(i + 1, values[i]);
        }


        insertStatement.executeUpdate();
        ResultSet generatedKeys = insertStatement.getGeneratedKeys();


        return generatedKeys;
    }

    private static ResultSet insertIntoValues(String tableName, String[] attributeNames, Object[][] values) throws SQLException {


        assert (tableName != null &&
                attributeNames != null &&
                values != null);
        // assert (attributeNames.length == values[0].length);

        StringBuilder insert = new StringBuilder("INSERT INTO " + tableName + " (");

        if (attributeNames.length > 0) insert.append(attributeNames[0]);
        for (int i = 1; i < attributeNames.length; i++) insert.append(",").append(attributeNames[i]);
        insert.append(")\nVALUES ");

        if (values.length > 0) {
            insert.append("(?");
            for (int i = 1; i < values[0].length; i++) insert.append(",?");
            insert.append(")");
        }

        for (int i = 1; i < values.length; i++){
            insert.append(", (");
            if (values[i].length > 0) insert.append("?");
            for (int j = 1; j < values[i].length; j++) insert.append(",?");
            insert.append(")");
        }
        insert.append(";");

        PreparedStatement insertStatement = connection.prepareStatement(insert.toString(), Statement.RETURN_GENERATED_KEYS);


        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values[i].length; j++){
                insertStatement.setObject(attributeNames.length*i+j + 1, values[i][j]);
            }
        }


        insertStatement.executeUpdate();
        ResultSet generatedKeys = insertStatement.getGeneratedKeys();


        return generatedKeys;
    }

    private static int deleteFrom(String tableName, String conditionString) throws SQLException {


        assert (tableName != null &&
                conditionString != null);

        int rows = connection.prepareStatement("DELETE FROM " + tableName + "\nWHERE " + conditionString + ";").executeUpdate();


        return rows;
    }

    private static int update(String tableName, String[] attributeNames, Object[] values, String condition) throws SQLException {


        assert (tableName != null &&
                attributeNames != null &&
                condition != null);

        assert (!tableName.isEmpty() &&
                attributeNames.length > 0 &&
                values.length > 0);

        assert (attributeNames.length == values.length);

        String update = String.format("UPDATE %s%nSET ", tableName);
        update = update + attributeNames[0] + " = " + "?";
        for (int i = 1; i < attributeNames.length; i++) {
            update = update + attributeNames[i] + " = " + ", ?";
        }
        update = update + "\nWHERE " + condition + ";";
        PreparedStatement updateStatement = connection.prepareStatement(update);
        for (int i = 0; i < values.length; i++) {
            updateStatement.setObject(i + 1, values[i]);
        }
        int rows = updateStatement.executeUpdate();


        return rows;
    }


    // - transaction operations to ensure consistency -

    public static void startTransaction() throws SQLException {

        connection.prepareStatement("start transaction;").execute();
    }

    public static void savepoint(String identifier) throws SQLException {

        connection.prepareStatement("savepoint " + identifier + ";").execute();
    }

    public static void rollbackTo(String identifier) throws SQLException {

        connection.prepareStatement("rollback to savepoint " + identifier + ";").execute();
    }

    public static void commit() throws SQLException {

        connection.prepareStatement("commit;").execute();
    }


    /**
     * ONLY FOR TESTING PURPOSES!!!
     */
    @Deprecated
    public static void deleteAll() throws SQLException, IOException {

        String sql = getResourceAsString("/sql/deleteAll.sql");
        for (String create : sql.split("--")) {
            connection.prepareStatement(create).execute();
        }
    }


    // ------------------------------------------------- Validation ------------------------------------------------- //


    /**
     * <p>Validates a String containing an XES document against the OCv1.xsd schema (see resources/schema).</p>
     *
     * @param xml the XES document
     * @return a boolean:<ul>
     * <li>true, if the given XES document is valid against the schema</li>
     * <li>false, if the given XES document is not valid against the schema
     * or exceptions are thrown inside the method body</li>
     * </ul>
     */
    public static boolean logIsValid(@NotNull String xml) throws SAXException, IOException {
        if (true) return true; //todo: build valiator that works


        // by default, the validator ignores this declaration, but we want to include it
        if (!xml.contains("<?xml")) {

            return false;
        }

        // prepare validation
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        URL url = DatabaseService.class.getResource("/de/uni_trier/wi2/schema/OCv1.xsd");

        Schema schema = factory.newSchema(url); // if there is a problem with the xsd an exception will be thrown

        Validator validator = schema.newValidator();

        try {
            // check if xml is valid
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (SAXException e) {
            // SAXException means that xml is not valid
            // we probably have to add certain necessary attributes to the log element of the xml

            // check if the xml contains a log element
            if (!xml.contains("<log")) {

                return false;
            }

            // take out the beginning of the log element
            String[] split = xml.split("<log");

            // check if the xml contains too many log elements (length > 2)
            // or if the cutoff string "<log" was at the end of the xml (length = 1)
            if (split.length != 2) {

                return false;
            }

            // add log-attributes necessary for validation
            String logTag = "<log xmlns=\"https://www.w3schools.com\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"https://www.w3schools.com ../../main/resources/schema/OCv1.xsd\">";
            String xes = split[0] + logTag + split[1].replace(split[1].split(">")[0],"");

            // try validating the altered xml
            try {
                validator.validate(new StreamSource(new StringReader(xes)));
            } catch (SAXException f) {
                // neither forms of the xml is valid

                return false;
            }
        }

        // one of the forms of the xml is valid

        return true;

    }

    /**
     * <p>Validates a String containing only an XES trace.</p>
     * <p>The given String should only contain a trace element ("&lt;trace&gt;...&lt;/trace&gt;")</p>
     *
     * @param xml the XES document
     * @return a boolean:<ul>
     * <li>true, if the given XES document is valid</li>
     * <li>false, if the given XES document is not valid</li>
     * </ul>
     * @throws IOException
     * @throws SAXException
     */
    public static boolean traceIsValid(@NotNull String xml) throws IOException, SAXException {

        String prefix = "<?xml version=\"1.0\" encoding=\"utf-8\" ?> <log>";
        String suffix = "</log>";
        return logIsValid(prefix + xml + suffix);
    }


    // ------------------------------------------- Table and Column Names ------------------------------------------- //

    // to make the DatabaseService robust against name changes
    public static final class DATABASE_NAMES {

        // variable names are in the forms of:
        //  - TABLENAME__<table>
        //  - COLUMNNAME__<table>__<column>

        // - log -
        public static final String TABLENAME__log = "log";
        public static final String COLUMNNAME__log__logID = "logID";
        public static final String COLUMNNAME__log__header = "header";
        public static final String COLUMNNAME__log__removed = "removed";
        // - trace -
        public static final String TABLENAME__trace = "trace";
        public static final String COLUMNNAME__trace__traceID = "traceID";
        public static final String COLUMNNAME__trace__logID = COLUMNNAME__log__logID;
        public static final String COLUMNNAME__trace__xes = "xes";
        public static final String COLUMNNAME__trace__removed = "removed";
        // - metadata -
        public static final String TABLENAME__metadata = "metadata";
        public static final String COLUMNNAME__metadata__metadataID = "metadataID";
        public static final String COLUMNNAME__metadata__value = "value";
        // - metadataType -
        public static final String TABLENAME__metadataType = "metadataType";
        public static final String COLUMNNAME__metadataType__metadataTypeID = "metadataTypeID";
        public static final String COLUMNNAME__metadataType__name = "name";
        // - metadata_belongsTo_log -
        public static final String TABLENAME__metadata_belongsTo_log = "metadata_belongsTo_log";
        public static final String COLUMNNAME__metadata_belongsTo_log__metadataID = COLUMNNAME__metadata__metadataID;
        public static final String COLUMNNAME__metadata_belongsTo_log__logID = COLUMNNAME__log__logID;
        // - metadata_belongsTo_trace -
        public static final String TABLENAME__metadata_belongsTo_trace = "metadata_belongsTo_trace";
        public static final String COLUMNNAME__metadata_belongsTo_trace__metadataID = COLUMNNAME__metadata__metadataID;
        public static final String COLUMNNAME__metadata_belongsTo_trace__traceID = COLUMNNAME__trace__traceID;
        // - metadata_hasType -
        public static final String TABLENAME__metadata_hasType = "metadata_hasType";
        public static final String COLUMNNAME__metadata_hasType__metadataID = COLUMNNAME__metadata__metadataID;
        public static final String COLUMNNAME__metadata_hasType__metadataTypeID = COLUMNNAME__metadataType__metadataTypeID;

        private DATABASE_NAMES() {
        }
    }


}
