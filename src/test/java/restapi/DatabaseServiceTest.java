package restapi;

import error.DatabaseNotEmptyException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.xml.sax.SAXException;
import restapi.service.DatabaseService;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

public class DatabaseServiceTest {

    @BeforeAll
    private static void connect() {
        connectionStatus = DatabaseService.connectToDatabase("onkocase_test");
    }

    static String connectionStatus;

    @Test
    public void testConnection() {
        assert (connectionStatus.equals("Connected to database"));
    }

    @Test
    public void testAccess() throws SQLException, IOException, SAXException, DatabaseNotEmptyException {


        String savepoint = "s";

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
            assert (log.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__log__header).equals((header + footer).replace("\n","")));
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
                t = (String) trace.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__xes) + "\n";
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

        } catch (Exception e) {
            DatabaseService.rollbackTo(savepoint);
            DatabaseService.commit();
            throw e;
        }

        DatabaseService.rollbackTo(savepoint);
        DatabaseService.commit();
    }
}
