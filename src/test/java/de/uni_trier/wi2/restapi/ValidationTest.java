package de.uni_trier.wi2.restapi;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import de.uni_trier.wi2.service.DatabaseService;

import java.io.IOException;

import static de.uni_trier.wi2.service.IOUtils.getResourceAsString;

public class ValidationTest {

    /**
     * <p>A test to see if the validator has a problem with the '.xes' file ending.</p>
     *
     * @throws IOException
     * @throws SAXException
     */
    @Test
    public void testSuffixes() throws IOException, SAXException {
        String xml = getResourceAsString("xmlSuffix.xml");
        assert (DatabaseService.logIsValid(xml));

        xml = getResourceAsString("xesSuffix.xes");
        assert DatabaseService.logIsValid(xml);
    }

    @Test
    public void testValidLog() throws IOException, SAXException {
        String xml = getResourceAsString("valid_log.xes");
        assert (DatabaseService.logIsValid(xml));
    }

    @Test
    public void testInvalidLogs() throws IOException, SAXException {
        String xml;

        for (int i = 0; i <= 6; i++){
            xml = getResourceAsString(String.format("invalid_log%d.xes",i));
            assert (!DatabaseService.logIsValid(xml));
        }
    }

    @Test
    public void testValidTrace() throws IOException, SAXException {
        String xml = getResourceAsString("valid_trace.xes");
        assert (DatabaseService.traceIsValid(xml));
    }
}
