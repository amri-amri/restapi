package de.uni_trier.wi2.restapi;

import de.uni_trier.wi2.service.*;
import org.xml.sax.*;

import java.io.*;

import static de.uni_trier.wi2.service.IOUtils.*;


public class ValidationTest {

    /**
     * <p>A test to see if the validator has a problem with the '.xes' file ending.</p>
     *
     * @throws IOException
     * @throws SAXException
     */

    public void testSuffixes() throws IOException, SAXException {
        String xml = getResourceAsString("xmlSuffix.xml");
        assert (DatabaseService.logIsValid(xml));

        xml = getResourceAsString("xesSuffix.xes");
        assert DatabaseService.logIsValid(xml);
    }

    public void testValidLog_0() throws IOException, SAXException {
        String xml = getResourceAsString("valid_log_0.xes");
        assert (DatabaseService.logIsValid(xml));
    }

    public void testValidLog_1() throws IOException, SAXException {
        String xml = getResourceAsString("valid_log_1.xes");
        assert (DatabaseService.logIsValid(xml));
    }

    public void testValidLog_2() throws IOException, SAXException {
        String xml = getResourceAsString("valid_log_2.xes");
        assert (DatabaseService.logIsValid(xml));
    }


    public void testValidLog_3() throws IOException, SAXException {
        String xml = getResourceAsString("eval/hospital_billing.xes");
        assert (DatabaseService.logIsValid(xml));
    }


    public void testInvalidLogs() throws IOException, SAXException {
        String xml;

        for (int i = 0; i <= 6; i++) {
            xml = getResourceAsString(String.format("invalid_log%d.xes", i));
            assert (!DatabaseService.logIsValid(xml));
        }
    }

    public void testValidTrace() throws IOException, SAXException {
        String xml = getResourceAsString("valid_trace.xes");
        assert (DatabaseService.traceIsValid(xml));
    }
}
