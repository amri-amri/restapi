package integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import extension.similarity.measure.collection.SMCollectionIsolatedMappingExt;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import restapi.RESTfulAPIApplication;
import restapi.model.FilterParameters;
import restapi.model.MethodList;
import restapi.model.RetrievalParameters;
import restapi.service.DatabaseService;
import restapi.service.ProCAKEService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = RESTfulAPIApplication.class)
@AutoConfigureMockMvc
public class ControllerTest {

    final String savepoint = "spt";
    @Autowired
    private MockMvc mvc;

    @Before
    public void before() throws SQLException, IOException {
        DatabaseService.connectToDatabase("onkocase_test");
        DatabaseService.deleteAll();
        ProCAKEService.setupCake();
        ProCAKEService.loadCasebase();
    }

    @After
    public void after() throws SQLException, IOException {
        DatabaseService.deleteAll();
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
                .andExpect(status().isNotFound());

        // GET /trace/x 404
        mvc.perform(get("/trace/x").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

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
                .andExpect(status().isBadRequest());

        // POST /log    200
        MvcResult result = mvc.perform(post("/log").content(header + traces[0] + traces[1] + traces[2] + footer))
                .andExpect(status().isOk())
                .andReturn();

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
                .andReturn();

        json = new ObjectMapper().readValue(result.getResponse().getContentAsString(), HashMap.class);

        assert (
                ((String) json.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__log__header)).replace("\n", "").equals(
                        (header + footer).replace("\n", ""))
        );

        // DELETE /log/x    404
        mvc.perform(delete("/log/x"))
                .andExpect(status().isNotFound());

        // DELETE /log/x    200
        result = mvc.perform(delete("/log/" + logID))
                .andExpect(status().isOk())
                .andReturn();

        json = new ObjectMapper().readValue(result.getResponse().getContentAsString(), HashMap.class);

        assert ((Boolean) json.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__log__removed));


    }

    @Test
    public void procake_test() throws Exception {
        String log = """
                <?xml version="1.0" encoding="UTF-8"?>
                <log>
                    <string key="type" value="test log"/>
                   \s
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
                    </trace>
                   \s
                   \s
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
                   \s
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
                   \s
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
                    </trace>
                </log>
                """;

        MvcResult result = mvc.perform(post("/log").content(log))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json =
                new ObjectMapper().readValue(result.getResponse().getContentAsString(), HashMap.class);

        String logID = (String) json.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__log__logID);
        String traceID = (String) ((ArrayList<Map<String, Object>>)json.get("traces")).stream().map(e -> e.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__traceID)).toList().get(0);


        // GET /procake/restart 200
        mvc.perform(get("/procake/restart"))
                .andExpect(status().isOk());

        // GET /procake/reload 200
        mvc.perform(get("/procake/reload"))
                .andExpect(status().isOk());

        String[] ids = performRetrieval(traceID);

        assert ids.length == 4;

        mvc.perform(delete("/log/" + logID))
                .andExpect(status().isOk());

        // GET /procake/reload 200
        // only non-removed traces should be loaded
        mvc.perform(get("/procake/reload"))
                .andExpect(status().isOk());

        ids = performRetrieval(traceID);

        assert ids.length == 0;
    }

    private String[] performRetrieval(String traceID) throws Exception {
        String xes = "";
        String globalSimilarityMeasure = SMCollectionIsolatedMappingExt.NAME;
        MethodList globalMethodInvokers = null;
        String localSimilarityMeasureFunc = """
                <?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE similarity-measure-function SYSTEM \"../procake-extension/src/main/resources/schema/similaritymeasure-function.dtd\">\n<similarity-measure-function>\n    <if>\n        <and>\n            \n            <equals>\n                <method-return-value>\n                    <method-return-value>\n                        <q/>\n                        <method name=\"getDataClass\"></method>\n                    </method-return-value>\n                    <method name=\"getName\"></method>\n                </method-return-value>\n                <string value=\"XESEventClass\"/>\n            </equals>\n            \n            <equals>\n                <method-return-value>\n                    <method-return-value>\n                        <c/>\n                        <method name=\"getDataClass\"></method>\n                    </method-return-value>\n                    <method name=\"getName\"></method>\n                </method-return-value>\n                <string value=\"XESEventClass\"/>\n            </equals>\n            \n        </and>\n        <string value=\"CollectionIsolatedMappingExt\"/>\n    </if>\n    \n    <if>\n        <and>\n            <equals>\n                <method-return-value>\n                    <method-return-value>\n                        <method-return-value>\n                            <method-return-value>\n                                <q/>\n                                <method name=\"getDataClass\"></method>\n                            </method-return-value>\n                            <method name=\"getSuperClass\"></method>\n                        </method-return-value>\n                        <method name=\"getSuperClass\"></method>\n                    </method-return-value>\n                    <method name=\"getName\"></method>\n                </method-return-value>\n                <string value=\"XESNaturallyNestedClass\"/>\n            </equals>\n            <equals>\n                <method-return-value>\n                    <method-return-value>\n                        <method-return-value>\n                            <method-return-value>\n                                <c/>\n                                <method name=\"getDataClass\"></method>\n                            </method-return-value>\n                            <method name=\"getSuperClass\"></method>\n                        </method-return-value>\n                        <method name=\"getSuperClass\"></method>\n                    </method-return-value>\n                    <method name=\"getName\"></method>\n                </method-return-value>\n                <string value=\"XESNaturallyNestedClass\"/>\n            </equals>\n        </and>\n        <string value=\"CollectionIsolatedMappingExt\"/>\n    </if>\n    \n    <if>\n        <and>\n            <equals>\n                <method-return-value>\n                    <method-return-value>\n                        <method-return-value>\n                            <q/>\n                            <method name=\"getDataClass\"></method>\n                        </method-return-value>\n                        <method name=\"getSuperClass\"></method>\n                    </method-return-value>\n                    <method name=\"getName\"></method>\n                </method-return-value>\n                <string value=\"XESLiteralClass\"/>\n            </equals>\n            <equals>\n                <method-return-value>\n                    <method-return-value>\n                        <method-return-value>\n                            <c/>\n                            <method name=\"getDataClass\"></method>\n                        </method-return-value>\n                        <method name=\"getSuperClass\"></method>\n                    </method-return-value>\n                    <method name=\"getName\"></method>\n                </method-return-value>\n                <string value=\"XESLiteralClass\"/>\n            </equals>\n        </and>\n        <string value=\"StringLevenshteinExt\"/>\n    </if>\n    \n    <if>\n        <and>\n            <equals>\n                <method-return-value>\n                    <method-return-value>\n                        <method-return-value>\n                            <q/>\n                            <method name=\"getDataClass\"></method>\n                        </method-return-value>\n                        <method name=\"getSuperClass\"></method>\n                    </method-return-value>\n                    <method name=\"getName\"></method>\n                </method-return-value>\n                <string value=\"XESBooleanClass\"/>\n            </equals>\n            <equals>\n                <method-return-value>\n                    <method-return-value>\n                        <method-return-value>\n                            <c/>\n                            <method name=\"getDataClass\"></method>\n                        </method-return-value>\n                        <method name=\"getSuperClass\"></method>\n                    </method-return-value>\n                    <method name=\"getName\"></method>\n                </method-return-value>\n                <string value=\"XESBooleanClass\"/>\n            </equals>\n        </and>\n        <string value=\"BooleanXOR\"/>\n    </if>\n\n</similarity-measure-function>""";
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

        String result = mvc.perform(put("/retrieval/" + traceID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content((new ObjectMapper()).writer().withDefaultPrettyPrinter().writeValueAsString(parameters)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ArrayList<Map<String, Object>> retrieval =
                new ObjectMapper().readValue(result, ArrayList.class);


        return retrieval.stream().map(e -> e.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__traceID)).toList().toArray(String[]::new);
    }


}
