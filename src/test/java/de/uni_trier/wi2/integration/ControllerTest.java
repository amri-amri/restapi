package de.uni_trier.wi2.integration;

import com.fasterxml.jackson.databind.*;
import de.uni_trier.wi2.*;
import de.uni_trier.wi2.extension.similarity.measure.collection.*;
import de.uni_trier.wi2.model.*;
import de.uni_trier.wi2.service.*;
import org.junit.*;
import org.junit.runner.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.autoconfigure.restdocs.*;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.*;
import org.springframework.http.*;
import org.springframework.restdocs.mockmvc.*;
import org.springframework.test.context.junit4.*;
import org.springframework.test.web.servlet.*;

import java.io.*;
import java.sql.*;
import java.util.*;

import static de.uni_trier.wi2.service.IOUtils.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = RESTAPI.class,
        args = {
                "jdbc:mysql://localhost:3306/onkocase_test",
                "root",
                "pw1234"
        }
)
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "target/snippets")
public class ControllerTest {

    final String savepoint = "spt";
    @Autowired
    private MockMvc mvc;
    private int retrievalCounter;

    @Before
    public void before() throws SQLException, IOException, ClassNotFoundException {
        DatabaseService.startTransaction();
        DatabaseService.deleteAll();
        ProCAKEService.setupCake();
        ProCAKEService.loadCasebase();

        retrievalCounter = 0;
    }

    @After
    public void after() throws SQLException, IOException {
        DatabaseService.deleteAll();
        DatabaseService.commit();
    }

    @Test
    public void database_test() throws Exception {
        // GET /log     200
        mvc.perform(get("/log").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));

        // GET /log/x   404
        mvc.perform(get("/log/x").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andDo(MockMvcRestDocumentation.document("404/get/log/logID"));

        // GET /trace/x 404
        mvc.perform(get("/trace/x").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andDo(MockMvcRestDocumentation.document("404/get/trace/traceID"));

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

        // POST /log    400
        mvc.perform(post("/log").content(""))
                .andExpect(status().isBadRequest())
                .andDo(MockMvcRestDocumentation.document("400/post/log"));

        // POST /log    200
        MvcResult result = mvc.perform(post("/log")
                        .content(header + traces[0] + traces[1] + traces[2] + footer))
                .andExpect(status().isOk())
                .andDo(MockMvcRestDocumentation.document("200/post/log"))
                .andReturn();

        // GET /log     200
        mvc.perform(get("/log").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andDo(MockMvcRestDocumentation.document("200/get/log"));

        Map<String, Object> json =
                new ObjectMapper().readValue(result.getResponse().getContentAsString(), HashMap.class);


        //logID
        String logID = (String) json.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__log__logID);

        //header
        assert (((String) (json.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__log__header)))
                .replace("\n", "")
                .equals(
                        (header + footer).replace("\n", "")
                ));

        //removed
        assert (!(Boolean) json.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__log__removed));

        //links
        assert (json.get("links") != null);

        //traces
        List<Map<String, Object>> traceList = (List<Map<String, Object>>) json.get("traces");

        String[] traceIDs = new String[3];
        String[] traces2 = new String[3];

        // GET /trace/x 200
        for (int i = 0; i < 3; i++) {
            traceIDs[i] = (String) traceList.get(i).get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__traceID);
            traces[i] = traces[i].replace("\n", "");
            traces2[i] = (((String) (
                    new ObjectMapper().readValue(
                            mvc.perform(get("/trace/" + traceIDs[i]))
                                    .andExpect(status().isOk())
                                    .andDo(MockMvcRestDocumentation.document("200/get/trace/traceID_" + i))
                                    .andReturn()
                                    .getResponse()
                                    .getContentAsString(),

                            HashMap.class
                    ).get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__xes)
            )).replace("\n", "")
            );
        }

        for (int i = 0; i < 3; i++) {
            assert (!traces2[i].equals(traces2[(i + 1) % 3]));
            assert (traces2[i].equals(traces[0]) || traces2[i].equals(traces[1]) || traces2[i].equals(traces[2]));
        }

        // GET /log/x   200
        result = mvc.perform(get("/log/" + logID))
                .andExpect(status().isOk())
                .andDo(MockMvcRestDocumentation.document("200/get/log/logID"))
                .andReturn();

        json = new ObjectMapper().readValue(result.getResponse().getContentAsString(), HashMap.class);

        assert (
                ((String) json.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__log__header)).replace("\n", "").equals(
                        (header + footer).replace("\n", ""))
        );

        // DELETE /log/x    404
        mvc.perform(delete("/log/x"))
                .andExpect(status().isNotFound())
                .andDo(MockMvcRestDocumentation.document("404/delete/log/logID"));

        // DELETE /log/x    200
        result = mvc.perform(delete("/log/" + logID))
                .andExpect(status().isOk())
                .andDo(MockMvcRestDocumentation.document("200/delete/log/logID"))
                .andReturn();

        json = new ObjectMapper().readValue(result.getResponse().getContentAsString(), HashMap.class);

        assert ((Boolean) json.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__log__removed));
    }

    @Test
    public void procake_test() throws Exception {
        String trace = """
                <trace>
                        <event>
                            <string key="eventString" value="Pferd"/>
                            <boolean key="eventBoolean" value="true"/>
                            <list key="eventList">
                                <string key="listString" value="Affe"/>
                                <string key="listString" value="Hase"/>
                                <string key="listString" value="Esel"/>
                            </list>
                        </event>
                        <event>
                            <string key="eventString" value="Maultier"/>
                            <boolean key="eventBoolean" value="true"/>
                            <list key="eventList">
                                <string key="listString" value="Giraffe"/>
                                <string key="listString" value="Kaninchen"/>
                                <string key="listString" value="Hund"/>
                            </list>
                        </event>
                    </trace>""";
        String head = """
                <?xml version="1.0" encoding="UTF-8"?>
                <log>""";
        String foot = """
                </log>""";
        String log = head + """
                <string key="type" value="test log"/>""" + trace + """
                <trace>
                    <event>
                        <string key="eventString" value="Pferd"/>
                        <boolean key="eventBoolean" value="true"/>
                        <list key="eventList">
                            <string key="listString" value="Affe"/>
                            <string key="listString" value="Vase"/>
                            <string key="listString" value="Besen"/>
                        </list>
                    </event>
                    <event>
                        <string key="eventString" value="Maultier"/>
                        <boolean key="eventBoolean" value="true"/>
                        <list key="eventList">
                            <string key="listString" value="Affe"/>
                            <string key="listString" value="Hase"/>
                            <string key="listString" value="Esel"/>
                        </list>
                    </event>
                </trace>
                                   
                <trace>
                    <event>
                        <string key="eventString" value="Herd"/>
                        <boolean key="eventBoolean" value="false"/>
                        <list key="eventList">
                            <string key="listString" value="Affe"/>
                            <string key="listString" value="Vase"/>
                            <string key="listString" value="Besen"/>
                        </list>
                    </event>
                    <event>
                        <string key="eventString" value="Schaukel"/>
                        <boolean key="eventBoolean" value="false"/>
                        <list key="eventList">
                            <string key="listString" value="Waffe"/>
                            <string key="listString" value="Gabe"/>
                            <string key="listString" value="Segen"/>
                        </list>
                    </event>
                </trace>
                                   
                <trace>
                    <event>
                        <string key="eventString" value="Herd"/>
                        <boolean key="eventBoolean" value="false"/>
                        <list key="eventList">
                            <string key="listString" value="Affe"/>
                            <string key="listString" value="Vase"/>
                            <string key="listString" value="Besen"/>
                        </list>
                    </event>
                    <event>
                        <string key="eventString" value="Schaukel"/>
                        <boolean key="eventBoolean" value="false"/>
                        <list key="eventList">
                            <string key="listString" value="Waffe"/>
                            <string key="listString" value="Gabe"/>
                            <string key="listString" value="Segen"/>
                        </list>
                    </event>
                    <event>
                        <string key="eventString" value="Raupe"/>
                        <boolean key="eventBoolean" value="true"/>
                        <container key="eventList">
                            <string key="listString" value="Rabe"/>
                            <string key="listString" value="Gabe"/>
                            <string key="listString" value="Segen"/>
                            <string key="listString" value="Regen"/>
                            <string key="listString" value="Degen"/>
                        </container>
                    </event>
                </trace>""" + foot;

        MvcResult result = mvc.perform(post("/log").content(log))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json =
                new ObjectMapper().readValue(result.getResponse().getContentAsString(), HashMap.class);

        String logID = (String) json.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__log__logID);
        String traceID = (String) ((ArrayList<Map<String, Object>>) json.get("traces")).stream().map(e -> e.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__traceID)).toList().get(0);


        // GET /procake/restart 200
        mvc.perform(get("/procake/restart"))
                .andExpect(status().isOk())
                .andDo(MockMvcRestDocumentation.document("200/get/procake/restart"));

        // GET /procake/reload 200
        mvc.perform(get("/procake/reload"))
                .andExpect(status().isOk())
                .andDo(MockMvcRestDocumentation.document("200/get/procake/reload"));

        String[] ids = performRetrievalByID(traceID);

        assert ids.length == 4;

        ids = performRetrievalByXES(head + trace + foot);

        assert ids.length == 4;

        mvc.perform(delete("/log/" + logID))
                .andExpect(status().isOk());

        // GET /procake/reload 200
        // only non-removed traces should be loaded
        mvc.perform(get("/procake/reload"))
                .andExpect(status().isOk());

        ids = performRetrievalByID(traceID);

        assert ids.length == 0;
        DatabaseService.commit();
    }

    private String[] performRetrievalByID(String traceID) throws Exception {
        String xes = "";
        String globalSimilarityMeasure = SMCollectionIsolatedMappingExt.NAME;
        MethodList globalMethodInvokers = null;
        String localSimilarityMeasureFunc = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE similarity-measure-function SYSTEM "https://karim-amri.de/dtd/similaritymeasure-function.dtd">
                <similarity-measure-function>
                    <if>
                        <and>
                                        <equals>
                                <method-return-value>
                                    <method-return-value>
                                        <q/>
                                        <method name="getDataClass"></method>
                                    </method-return-value>
                                    <method name="getName"></method>
                                </method-return-value>
                                <string value="XESEventClass"/>
                            </equals>
                                        <equals>
                                <method-return-value>
                                    <method-return-value>
                                        <c/>
                                        <method name="getDataClass"></method>
                                    </method-return-value>
                                    <method name="getName"></method>
                                </method-return-value>
                                <string value="XESEventClass"/>
                            </equals>
                                    </and>
                        <string value="CollectionIsolatedMappingExt"/>
                    </if>
                        <if>
                        <and>
                            <equals>
                                <method-return-value>
                                    <method-return-value>
                                        <method-return-value>
                                            <method-return-value>
                                                <q/>
                                                <method name="getDataClass"></method>
                                            </method-return-value>
                                            <method name="getSuperClass"></method>
                                        </method-return-value>
                                        <method name="getSuperClass"></method>
                                    </method-return-value>
                                    <method name="getName"></method>
                                </method-return-value>
                                <string value="XESNaturallyNestedClass"/>
                            </equals>
                            <equals>
                                <method-return-value>
                                    <method-return-value>
                                        <method-return-value>
                                            <method-return-value>
                                                <c/>
                                                <method name="getDataClass"></method>
                                            </method-return-value>
                                            <method name="getSuperClass"></method>
                                        </method-return-value>
                                        <method name="getSuperClass"></method>
                                    </method-return-value>
                                    <method name="getName"></method>
                                </method-return-value>
                                <string value="XESNaturallyNestedClass"/>
                            </equals>
                        </and>
                        <string value="CollectionIsolatedMappingExt"/>
                    </if>
                        <if>
                        <and>
                            <equals>
                                <method-return-value>
                                    <method-return-value>
                                        <method-return-value>
                                            <q/>
                                            <method name="getDataClass"></method>
                                        </method-return-value>
                                        <method name="getSuperClass"></method>
                                    </method-return-value>
                                    <method name="getName"></method>
                                </method-return-value>
                                <string value="XESLiteralClass"/>
                            </equals>
                            <equals>
                                <method-return-value>
                                    <method-return-value>
                                        <method-return-value>
                                            <c/>
                                            <method name="getDataClass"></method>
                                        </method-return-value>
                                        <method name="getSuperClass"></method>
                                    </method-return-value>
                                    <method name="getName"></method>
                                </method-return-value>
                                <string value="XESLiteralClass"/>
                            </equals>
                        </and>
                        <string value="StringLevenshteinExt"/>
                    </if>
                        <if>
                        <and>
                            <equals>
                                <method-return-value>
                                    <method-return-value>
                                        <method-return-value>
                                            <q/>
                                            <method name="getDataClass"></method>
                                        </method-return-value>
                                        <method name="getSuperClass"></method>
                                    </method-return-value>
                                    <method name="getName"></method>
                                </method-return-value>
                                <string value="XESBooleanClass"/>
                            </equals>
                            <equals>
                                <method-return-value>
                                    <method-return-value>
                                        <method-return-value>
                                            <c/>
                                            <method name="getDataClass"></method>
                                        </method-return-value>
                                        <method name="getSuperClass"></method>
                                    </method-return-value>
                                    <method name="getName"></method>
                                </method-return-value>
                                <string value="XESBooleanClass"/>
                            </equals>
                        </and>
                        <string value="BooleanXOR"/>
                    </if>
                                
                </similarity-measure-function>""";
        String localMethodInvokersFunc = null;
        String localWeightFunc = null;
        FilterParameters filterParameters = null;
        int numberOfResults = 4;

        RetrievalParameters parameters = new RetrievalParameters(
                xes,
                globalSimilarityMeasure,
                globalMethodInvokers,
                localSimilarityMeasureFunc,
                localMethodInvokersFunc,
                localWeightFunc,
                filterParameters,
                numberOfResults
        );

        // PUT /retrieval/x 200
        String result = mvc.perform(put("/retrieval/" + traceID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content((new ObjectMapper()).writer().withDefaultPrettyPrinter().writeValueAsString(parameters)))
                .andExpect(status().isOk())
                .andDo(MockMvcRestDocumentation.document("200/put/retrieval/traceID_" + retrievalCounter++))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ArrayList<Map<String, Object>> retrieval =
                new ObjectMapper().readValue(result, ArrayList.class);


        return retrieval.stream().map(e -> e.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__traceID)).toList().toArray(String[]::new);
    }

    private String[] performRetrievalByXES(String xes) throws Exception {
        String globalSimilarityMeasure = SMCollectionIsolatedMappingExt.NAME;
        MethodList globalMethodInvokers = null;
        String localSimilarityMeasureFunc = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE similarity-measure-function SYSTEM "https://karim-amri.de/dtd/similaritymeasure-function.dtd">
                <similarity-measure-function>
                    <if>
                        <and>
                                        <equals>
                                <method-return-value>
                                    <method-return-value>
                                        <q/>
                                        <method name="getDataClass"></method>
                                    </method-return-value>
                                    <method name="getName"></method>
                                </method-return-value>
                                <string value="XESEventClass"/>
                            </equals>
                                        <equals>
                                <method-return-value>
                                    <method-return-value>
                                        <c/>
                                        <method name="getDataClass"></method>
                                    </method-return-value>
                                    <method name="getName"></method>
                                </method-return-value>
                                <string value="XESEventClass"/>
                            </equals>
                                    </and>
                        <string value="CollectionIsolatedMappingExt"/>
                    </if>
                        <if>
                        <and>
                            <equals>
                                <method-return-value>
                                    <method-return-value>
                                        <method-return-value>
                                            <method-return-value>
                                                <q/>
                                                <method name="getDataClass"></method>
                                            </method-return-value>
                                            <method name="getSuperClass"></method>
                                        </method-return-value>
                                        <method name="getSuperClass"></method>
                                    </method-return-value>
                                    <method name="getName"></method>
                                </method-return-value>
                                <string value="XESNaturallyNestedClass"/>
                            </equals>
                            <equals>
                                <method-return-value>
                                    <method-return-value>
                                        <method-return-value>
                                            <method-return-value>
                                                <c/>
                                                <method name="getDataClass"></method>
                                            </method-return-value>
                                            <method name="getSuperClass"></method>
                                        </method-return-value>
                                        <method name="getSuperClass"></method>
                                    </method-return-value>
                                    <method name="getName"></method>
                                </method-return-value>
                                <string value="XESNaturallyNestedClass"/>
                            </equals>
                        </and>
                        <string value="CollectionIsolatedMappingExt"/>
                    </if>
                        <if>
                        <and>
                            <equals>
                                <method-return-value>
                                    <method-return-value>
                                        <method-return-value>
                                            <q/>
                                            <method name="getDataClass"></method>
                                        </method-return-value>
                                        <method name="getSuperClass"></method>
                                    </method-return-value>
                                    <method name="getName"></method>
                                </method-return-value>
                                <string value="XESLiteralClass"/>
                            </equals>
                            <equals>
                                <method-return-value>
                                    <method-return-value>
                                        <method-return-value>
                                            <c/>
                                            <method name="getDataClass"></method>
                                        </method-return-value>
                                        <method name="getSuperClass"></method>
                                    </method-return-value>
                                    <method name="getName"></method>
                                </method-return-value>
                                <string value="XESLiteralClass"/>
                            </equals>
                        </and>
                        <string value="StringLevenshteinExt"/>
                    </if>
                        <if>
                        <and>
                            <equals>
                                <method-return-value>
                                    <method-return-value>
                                        <method-return-value>
                                            <q/>
                                            <method name="getDataClass"></method>
                                        </method-return-value>
                                        <method name="getSuperClass"></method>
                                    </method-return-value>
                                    <method name="getName"></method>
                                </method-return-value>
                                <string value="XESBooleanClass"/>
                            </equals>
                            <equals>
                                <method-return-value>
                                    <method-return-value>
                                        <method-return-value>
                                            <c/>
                                            <method name="getDataClass"></method>
                                        </method-return-value>
                                        <method name="getSuperClass"></method>
                                    </method-return-value>
                                    <method name="getName"></method>
                                </method-return-value>
                                <string value="XESBooleanClass"/>
                            </equals>
                        </and>
                        <string value="BooleanXOR"/>
                    </if>
                                
                </similarity-measure-function>""";
        String localMethodInvokersFunc = null;
        String localWeightFunc = null;
        FilterParameters filterParameters = null;
        int numberOfResults = 4;

        RetrievalParameters parameters = new RetrievalParameters(
                xes,
                globalSimilarityMeasure,
                globalMethodInvokers,
                localSimilarityMeasureFunc,
                localMethodInvokersFunc,
                localWeightFunc,
                filterParameters,
                numberOfResults
        );

        // PUT /retrieval/x 200
        String result = mvc.perform(put("/retrieval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content((new ObjectMapper()).writer().withDefaultPrettyPrinter().writeValueAsString(parameters)))
                .andExpect(status().isOk())
                .andDo(MockMvcRestDocumentation.document("200/put/retrieval/xes"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ArrayList<Map<String, Object>> retrieval =
                new ObjectMapper().readValue(result, ArrayList.class);


        return retrieval.stream().map(e -> e.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__traceID)).toList().toArray(String[]::new);
    }

    @Test
    public void uploadAnReloadLog_test() throws Exception {
        String[] paths = new String[]{
                "eval/sepsis.xes",
                "eval/hospital_billing.xes",
        };

        for (String path : paths) {

            System.out.printf("Starting test for log at path \"%s\"%n", path);

            long start = System.nanoTime();
            String log = getResourceAsString(path);
            mvc.perform(post("/log").content(log))
                    .andExpect(status().isOk())
                    .andReturn();

            long finish = System.nanoTime();
            long timeElapsed = finish - start;

            double totalSeconds = timeElapsed / 1E9;
            int seconds = (int) (totalSeconds % 60);
            int minutes = (int) ((totalSeconds - seconds) / 60);


            System.out.printf("Upload of log into database took %d:%d [min:sec]%n", minutes, seconds);

            start = System.nanoTime();

            mvc.perform(get("/procake/reload"))
                    .andExpect(status().isOk());

            finish = System.nanoTime();
            timeElapsed = finish - start;

            totalSeconds = timeElapsed / 1E9;
            seconds = (int) (totalSeconds % 60);
            minutes = (int) ((totalSeconds - seconds) / 60);

            System.out.printf("Reload of database into casebase took %d:%d [min:sec]%n%n", minutes, seconds);
        }
    }


}