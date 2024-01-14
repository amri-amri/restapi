package de.uni_trier.wi2.restapi;

import de.uni_trier.wi2.error.DatabaseNotEmptyException;
import de.uni_trier.wi2.service.IOUtils;
import org.apache.jena.base.Sys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import de.uni_trier.wi2.service.DatabaseService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

public class DatabaseServiceTest {

    static String connectionStatus;

    @BeforeAll
    private static void connect() throws IOException, SQLException, ClassNotFoundException {
        String databaseTestInfo = IOUtils.getResourceAsString("databaseTestInfo.txt");
        String[] args = databaseTestInfo.split(System.lineSeparator());
        DatabaseService.setUrlUsernamePassword(args[0], args[1], args[2]);
        connectionStatus = DatabaseService.connectToDatabase();
    }

    @Test
    public void testConnection() {
        assert (connectionStatus.equals("Connected to database"));
    }

    @Test
    public void testAccess() throws SQLException, IOException, SAXException, DatabaseNotEmptyException {


        final String savepoint = "s";

        DatabaseService.startTransaction();
        DatabaseService.savepoint(savepoint);

        try {

            DatabaseService.deleteAll();

            String header = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <log name="testLog">
                    <string key="name" value="testLog"/>
                    """;
            String[] traces = new String[]{
                    """
                <trace>
                    <string key="name" value="trace1"/>
                </trace>
                """,
                    """
                <trace>
                    <boolean key="trace2" value="true"/>
                </trace>
                """,
                    """
                <trace>
                    <container key="attribute">
                        <string key="attributeName" value="id"/>
                        <string key="attributeValue" value="trace3"/>
                    </container>
                </trace>
                """
            };
            String footer = """
                    </log>
                    """;

            String xes = header + traces[0] + traces[1] + traces[2] + footer;

            String[] ids = DatabaseService.putLog(xes);
            assert (ids.length == 4);

            String logID = DatabaseService.getLogIDs(true)[0];
            assert (logID.equals(ids[0]));

            Map<String, Object> log = DatabaseService.getLog(logID);
            assert (log.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__log__logID).equals(logID));
            assert (
                    ((String) log.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__log__header))
                            .replace("\n", "").replace("\r", "")
                            .equals(
                                    (header + footer)
                                            .replace("\n", "").replace("\r", "")
                            )
            );
            assert (log.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__log__removed).equals(false));

            String[] traceIDs = DatabaseService.getTraceIDs(logID);
            assert (traceIDs.length == 3);

            for (String tID : traceIDs) {
                assert (Arrays.stream(ids).toList().contains(tID));
            }

            String traceID;
            String t;
            Map<String, String> metadata;
            Map<String, Object> trace;
            for (int i = 0; i < 3; i++) {
                traceID = traceIDs[i];
                trace = DatabaseService.getTrace(traceID);
                assert (trace.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__traceID).equals(traceID));
                assert (trace.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__logID).equals(logID));
                t = trace.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__xes) + "\n";
                assert (Arrays.stream(traces).toList().contains(t));
                assert (trace.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__removed).equals(false));

                DatabaseService.putTraceMetadata(traceID, "farbe", "blau");
                metadata = DatabaseService.getTraceMetadata(traceID);
                assert (metadata.get("farbe").equals("blau"));

            }

            DatabaseService.putLogMetadata(logID, "a", "b");
            metadata = DatabaseService.getLogMetadata(logID);
            assert (metadata.get("a").equals("b"));

            assert (DatabaseService.removeLog(logID) == 4);
            log = DatabaseService.getLog(logID);
            assert ((boolean) log.get("removed"));


            for (int i = 0; i < 3; i++) {
                assert ((boolean) DatabaseService.getTrace(traceIDs[0]).get("removed"));
            }

            DatabaseService.rollbackTo(savepoint);
            DatabaseService.commit();

        } catch (Exception e) {
            DatabaseService.rollbackTo(savepoint);
            DatabaseService.commit();
            throw e;
        }


    }
}
