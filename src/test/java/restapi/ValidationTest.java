package restapi;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import restapi.service.DatabaseService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ValidationTest {

    /**
     * <p>A test to see if the validator has a problem with the '.xes' file ending.</p>
     *
     * @throws IOException
     * @throws SAXException
     */
    @Test
    public void testSuffixes() throws IOException, SAXException {
        String xml = Files.readString(Path.of("src/test/resources/xmlSuffix.xml"));
        assert (DatabaseService.logIsValid(xml));

        xml = Files.readString(Path.of("src/test/resources/xesSuffix.xes"));
        assert DatabaseService.logIsValid(xml);
    }

    @Test
    public void testValidLog() throws IOException, SAXException {
        String xml = Files.readString(Path.of("src/test/resources/valid_log.xes"));
        assert (DatabaseService.logIsValid(xml));
    }

    @Test
    public void testInvalidLogs() throws IOException, SAXException {
        String xml;

        for (int i = 0; i <= 6; i++){
            xml = Files.readString(Path.of(String.format("src/test/resources/invalid_log%d.xes",i)));
            assert (!DatabaseService.logIsValid(xml));
        }
    }

    @Test
    public void testValidTrace() throws IOException, SAXException {
        String xml = Files.readString(Path.of("src/test/resources/valid_trace.xes"));
        assert (DatabaseService.traceIsValid(xml));
    }
}
