package restapi.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;
import restapi.error.XESnotValidException;
import restapi.model.Trace;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The service implementing all the business logic for managing the database.
 */
@Service
public class DatabaseService {

    /**
     * <p>Finds a trace in the database if it exists and returns it. If it does not exist, null is returned.</p>
     * @param id  String containing the id
     * @return  <ul>
     *     <li>a {@link Trace} object corresponding to the id, if the id exists in the database</li>
     *     <li>null, if the id does not exist in the database</li>
     * </ul>
     */
    @Nullable
    public static Trace getTraceByID(@NotNull String id){
        return repository.findById(id);
    }

    /**
     * Finds all traces in the database and returns a {@link List} of said {@link Trace}s.
     *
     * @return List object containing Trace objects
     */
    @NotNull
    public static List<Trace> getAllTraces(){
        return repository.findAll();
    }

    /**
     * <p>Connects to the database.</p>
     *
     * @return  String containing a message
     */
    @NotNull
    public static String connectToDatabase(){
        repository.loadDataBase();
        return "database loaded";
    }


    /**
     * <p>Puts a new trace in the database. The id of the new trace is generated automatically.</p>
     * <p>If the String is not a valid XES-element, an {@link XESnotValidException} is thrown.
     * We only want to save Strings in the database that start with "&lt;trace" and end with
     * "&lt;/trace&gt;".</p>
     *
     * @param trace the String to be put into the database
     * @param xsd the XSD the trace has to validate against
     * @return the automatically assigned id of the trace
     * @throws XESnotValidException if the given String does not represent a valid XES trace-element
     */
    @NotNull
    public static String put(@NotNull String trace, @NotNull XSD_SCHEMATA.XSD xsd) throws XESnotValidException {
        // We only want to save traces in the database that:
        //  1) start with "<trace" and end with "</trace>", and
        //  2) when contained in an actual XES document validate against a given SEX schema
        // That is why we have to prefix the given String with prefix and suffix, so we can use the
        // validation method (which only works on valid XML documents).
        if (!validateXmlAgainstSchema(xsd.PREFIX + trace + xsd.SUFFIX, xsd)) throw new XESnotValidException(xsd.PREFIX + trace + xsd.SUFFIX, xsd);

        // If the trace is valid, we save it to the database and return the id
        return repository.save(trace);
    }

    /**
     * <p>Puts a new trace in the database with the given id, or changes the XES, if the id already exists.</p>
     * <p>If the String is not a valid XES-element, an {@link XESnotValidException} is thrown.
     * We only want to save Strings in the database that start with "&lt;trace" and end with
     * "&lt;/trace&gt;".</p>
     *
     * @param id the id of the trace
     * @param trace the String to be put into the database
     * @param xsd the XSD the trace has to validate against
     * @throws XESnotValidException if the given String does not represent a valid XES trace-element
     */
    public static void put(@NotNull String id, @NotNull String trace, @NotNull XSD_SCHEMATA.XSD xsd) throws XESnotValidException{
        if (!validateXmlAgainstSchema(xsd.PREFIX + trace + xsd.SUFFIX, xsd)) throw new XESnotValidException(xsd.PREFIX + trace + xsd.SUFFIX, xsd);

        repository.save(id, trace);
    }

    /**
     * <p>Just a class to manage different XES schema files.</p>
     * <p>This class does not have methods and can not be instantiated, it simply owns XSD-objects
     * representing the files that can be used for validation.</p>
     * <p>An XSD-object contains four Strings:
     * <ol>
     *     <li>PATH: denoting the path of the corresponding XSD file</li>
     *     <li>SOURCE: denoting the source of the file, i.e. a download-link</li>
     *     <li>PREFIX: if a single trace element ("&lt;trace...&lt;/trace&gt;")
     *     is validated against a schema, a prefix should be put in front of it
     *     before validation</li>
     *     <li>SUFFIX: just like PREFIX but it is a suffix</li>
     * </ol></p>
     */
    public static final class XSD_SCHEMATA{
        private XSD_SCHEMATA(){}
        public static class XSD{
            public final String PATH, SOURCE, PREFIX, SUFFIX;
            XSD(String PATH, String SOURCE, String PREFIX, String SUFFIX){
                this.PATH = PATH;
                this.SOURCE = SOURCE;
                this.PREFIX = PREFIX;
                this.SUFFIX = SUFFIX;
            }
        }

        public static final XSD IEEE_APRIL_15_2020 = new XSD(
                "src/main/resources/schema/xes-ieee-1849-2016-April-15-2020.xsd",
                "http://www.xes-standard.org/downloads/xes-ieee-1849-2016-April-15-2020.xsd",
                """
                        <?xml version="1.0" encoding="utf-8"?>
                        <log xes.features="nested-attributes" xes.version="1.0">
                        """,
                "</log>");
    }

    /**
     * The XSD schema that is to be used throughout the API.
     */
    public final static XSD_SCHEMATA.XSD XSD_TO_BE_USED = XSD_SCHEMATA.IEEE_APRIL_15_2020;

    /**
     * <p>Validates a String containing an XES document against the schema defined in the method body.</p>
     *
     * @param xml  the XES document
     * @return  a boolean:<ul>
     *     <li>true, if the given XES document is valid against the schema</li>
     *     <li>false, if the given XES document is not valid against the schema
     *     or exceptions are thrown inside the method body</li>
     *     </ul>
     */
    private static boolean validateXmlAgainstSchema(@NotNull String xml, @NotNull XSD_SCHEMATA.XSD xsd) {

        try {
            SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new File(xsd.PATH));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (IOException | SAXException e) {
            return false;
        }
        return true;
    }

    /**
     * <p>Validates a String containing only an XES trace against the schema defined in the method body.</p>
     * <p>The given String should only contain a trace element ("&lt;trace&gt;...&lt;/trace&gt;")</p>
     *
     * @param xml  the XES document
     * @return  a boolean:<ul>
     *     <li>true, if the given XES document is valid against the schema</li>
     *     <li>false, if the given XES document is not valid against the schema
     *     or exceptions are thrown inside the method body</li>
     *     </ul>
     */
    public static boolean validateTraceAgainstSchema(@NotNull String xml, @NotNull XSD_SCHEMATA.XSD xsd){
        return validateXmlAgainstSchema(xsd.PREFIX + xml + xsd.SUFFIX, xsd);
    }


    // 'on-the-fly' implementation of a TraceRepository for testing purposes.
    static TraceRepository repository = new TraceRepository() {

        HashMap<String, String> traces = new HashMap<>();

        final IDFactory idFactory = new IDFactory() {
            private int c = 0;
            @Override
            public String nextID() {
                return String.format("trace:%d", c++);
            }

            @Override
            public void reset() {
                c = 0;
            }
        };


        @Override
        public String save(String id, String xes) {
            traces.put(id, xes);
            return id;
        }

        @Override
        public String save(String xes) {
            return save(idFactory.nextID(), xes);
        }

        @Override
        public Trace findById(String id) {
            String xes = traces.get(id);
            if (xes == null) return null;
            return new Trace(id, xes);
        }

        @Override
        public List<Trace> findAll() {
            List<Trace> out = new ArrayList<>();
            for (String key : traces.keySet()) out.add(new Trace(key, traces.get(key)));
            return out;
        }


        @Override
        public void loadDataBase() {
            idFactory.reset();
            traces = new HashMap<>();
        }

        public void loadTestDataBase() {
            idFactory.reset();
            traces = new HashMap<>();
            for (int i = 0; i < 20; i++) {
                save(String.format("trace:%d", i), randomTraceXES(i));
                idFactory.nextID();
            }
        }

        private String randomTraceXES(int traceIndex){
            StringBuilder trace = new StringBuilder("<trace>\n");
            trace.append(String.format("\t<string key=\"id\" value=\"trace:%d\"/>\n", traceIndex));

            int numEvents = (int) (5 + (Math.random() * 10));
            for (int i = 0; i < numEvents; i++){
                trace.append(randomEventXES(i));
            }

            trace.append("</trace>");
            return trace.toString();
        }

        private String randomEventXES(int eventIndex){
            StringBuilder event = new StringBuilder("\t<event>\n");
            event.append(String.format("\t\t<string key=\"id\" value=\"event:%d\"/>\n", eventIndex));
            int numAttributes = (int) (2 + (Math.random() * 5));
            for (int i = 0; i < numAttributes; i++){
                double attType = Math.random();
                if (attType < 0.38) {
                    //String
                    event.append(String.format("\t\t<string key=\"randomString\" value=\"%s\"/>\n", randomString()));
                } else if (attType < 0.7) {
                    //Double
                    event.append(String.format("\t\t<float key=\"randomFloat\" value=\"%f\"/>\n", Math.random()*10).replace(',','.'));
                } else {
                    //Boolean
                    event.append(String.format("\t\t<boolean key=\"randomBoolean\" value=\"%b\"/>\n", Math.random() < 0.5));
                }
            }
            event.append("\t</event>\n");
            return event.toString();
        }

        private String randomString(){
            double a = Math.random();
            if (a<0.1) return "blabla!";
            if (a<0.2) return "eins zwei drei";
            if (a<0.3) return "lol";
            if (a<0.4) return "abc";
            if (a<0.5) return "def";
            if (a<0.6) return "ghijkl";
            if (a<0.7) return "moien";
            if (a<0.8) return "42";
            if (a<0.9) return "ene mene muh";
            return "hallo";
        }


    };
}
