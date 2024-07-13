package de.uni_trier.wi2.integration;

import com.fasterxml.jackson.databind.*;
import de.uni_trier.wi2.*;
import de.uni_trier.wi2.extension.retrieval.*;
import de.uni_trier.wi2.model.*;
import de.uni_trier.wi2.parsing.*;
import de.uni_trier.wi2.procake.data.object.*;
import de.uni_trier.wi2.procake.data.object.nest.*;
import de.uni_trier.wi2.procake.data.objectpool.*;
import de.uni_trier.wi2.procake.retrieval.*;
import de.uni_trier.wi2.service.*;
import de.uni_trier.wi2.utils.*;
import org.junit.*;
import org.junit.runner.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.*;
import org.springframework.http.*;
import org.springframework.test.context.junit4.*;
import org.springframework.test.web.servlet.*;
import org.xml.sax.*;

import java.io.*;
import java.sql.*;
import java.util.*;

import static de.uni_trier.wi2.RestAPILoggingUtils.*;
import static de.uni_trier.wi2.service.IOUtils.*;
import static org.junit.Assert.*;
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
public class DeterminismTest {

    final String savepoint = "spt";
    @Autowired
    private MockMvc mvc;

    @Before
    public void before() throws SQLException, IOException, SAXException {
        METHOD_CALL.trace("public void restapi.de.uni_trier.wi2.integration.DeterminismTest.before()...");

        String log = getResourceAsString("determinism_test_log.xes");

        DatabaseService.startTransaction();
        DatabaseService.deleteAll();
        DatabaseService.putLog(log);
        ProCAKEService.setupCake();
        ProCAKEService.loadCasebase();

        METHOD_CALL.trace("restapi.de.uni_trier.wi2.integration.DeterminismTest.before(): return");
    }

    @After
    public void after() throws SQLException, IOException {
        METHOD_CALL.trace("public void restapi.de.uni_trier.wi2.integration.DeterminismTest.after()...");

        //DatabaseService.deleteAll();
        DatabaseService.commit();

        METHOD_CALL.trace("restapi.de.uni_trier.wi2.integration.DeterminismTest.after(): return");
    }

    @Test
    public void controller_test() throws Exception {
        METHOD_CALL.trace("public void restapi.de.uni_trier.wi2.integration.DeterminismTest.controller_test()...");

        // get all logs

        DIAGNOSTICS.trace("restapi.de.uni_trier.wi2.integration.DeterminismTest.controller_test(): get all logs via controller...");

        String result = mvc.perform(get("/log"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ArrayList<Map<String, Object>> logs =
                new ObjectMapper().readValue(result, ArrayList.class);


        // get first traceID in first log

        String traceID = (String) ((ArrayList<LinkedHashMap>) logs.get(0).get("traces")).get(0).get("traceID");

        DIAGNOSTICS.trace("restapi.de.uni_trier.wi2.integration.DeterminismTest.controller_test(): first traceID in first log: {}", traceID);


        // define retrieval parameters

        String xes;
        String globalSimilarityMeasure;
        MethodList globalMethodInvokers;
        String localSimilarityMeasureFunc;
        String localMethodInvokersFunc;
        String localWeightFunc;
        FilterParameters filterParameters;
        int numberOfResults;

        xes = "";
        globalSimilarityMeasure = "ListDTWExt";
        Method m = new Method("setHalvingDistPercentage", List.of("double"), List.of("0.5"));
        globalMethodInvokers = new MethodList(new ArrayList<>(Collections.singleton(m)));

        localSimilarityMeasureFunc = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE similarity-measure-function SYSTEM "https://karim-amri.de/dtd/similaritymeasure-function.dtd">
                <similarity-measure-function>
                <if>
                <and>
                <equals>
                <method-return-value>
                <method-return-value>
                <q/>
                <method name="getDataClass">
                </method>
                </method-return-value>
                <method name="getName">
                </method>
                </method-return-value>
                <string value="XESEventClass"/>
                </equals>
                <equals>
                <method-return-value>
                <method-return-value>
                <c/>
                <method name="getDataClass">
                </method>
                </method-return-value>
                <method name="getName">
                </method>
                </method-return-value>
                <string value="XESEventClass"/>
                </equals>
                </and>
                <string value="CollectionIsolatedMappingExt"/>
                </if>
                </similarity-measure-function>""";


        localMethodInvokersFunc = null;
        localWeightFunc = null;
        filterParameters = new FilterParameters();
        numberOfResults = 10;

        RetrievalParameters retrievalParameters = new RetrievalParameters(
                xes,
                globalSimilarityMeasure,
                globalMethodInvokers,
                localSimilarityMeasureFunc,
                localMethodInvokersFunc,
                localWeightFunc,
                filterParameters,
                numberOfResults
        );


        // perform retrieval first time

        DIAGNOSTICS.trace("restapi.de.uni_trier.wi2.integration.DeterminismTest.controller_test(): perform retrieval first time...");

        String result_1 = mvc.perform(put("/retrieval/" + traceID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content((new ObjectMapper()).writer().withDefaultPrettyPrinter().writeValueAsString(retrievalParameters)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ArrayList<Map<String, Object>> retrieval_1 =
                new ObjectMapper().readValue(result_1, ArrayList.class);


        // perform retrieval second time

        DIAGNOSTICS.trace("restapi.de.uni_trier.wi2.integration.DeterminismTest.controller_test(): perform retrieval first time...");

        String result_2 = mvc.perform(put("/retrieval/" + traceID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content((new ObjectMapper()).writer().withDefaultPrettyPrinter().writeValueAsString(retrievalParameters)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ArrayList<Map<String, Object>> retrieval_2 =
                new ObjectMapper().readValue(result_2, ArrayList.class);

        // compare retrieval results

        DIAGNOSTICS.trace("restapi.de.uni_trier.wi2.integration.DeterminismTest.controller_test(): compare retrieval results...");

        String[] ids1 = new String[numberOfResults];
        String[] ids2 = new String[numberOfResults];
        double[] sims1 = new double[numberOfResults];
        double[] sims2 = new double[numberOfResults];
        for (int i = 0; i < numberOfResults; i++) {
            ids1[i] = (String) retrieval_1.get(i).get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__traceID);
            ids2[i] = (String) retrieval_2.get(i).get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__traceID);

            sims1[i] = (double) retrieval_1.get(i).get("similarity");
            sims2[i] = (double) retrieval_2.get(i).get("similarity");
        }

        //Arrays.sort(ids1, Comparator.naturalOrder());
        //Arrays.sort(ids2, Comparator.naturalOrder());
        Arrays.sort(sims1);
        Arrays.sort(sims2);

        for (int i = 0; i < numberOfResults; i++) {
            //assertEquals(ids1[i], ids2[i]);
            assertEquals(sims1[i], sims2[i], 0);
        }


        METHOD_CALL.trace("public void restapi.de.uni_trier.wi2.integration.DeterminismTest.controller_test(): return");
    }


    @Test
    public void service_test() throws Exception {
        METHOD_CALL.trace("public void restapi.de.uni_trier.wi2.integration.DeterminismTest.service_test()...");

        // get logID of first log

        String logID = DatabaseService.getLogIDs(true)[0];

        DIAGNOSTICS.trace("restapi.de.uni_trier.wi2.integration.DeterminismTest.service_test(): logID of first log={}", logID);


        // get first traceID and xes

        String traceID = DatabaseService.getTraceIDs(logID)[0];

        DIAGNOSTICS.trace("restapi.de.uni_trier.wi2.integration.DeterminismTest.service_test(): traceID of first traceID={}", traceID);

        String xes = (String) DatabaseService.getTrace(traceID).get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__xes);


        // define retrieval parameters

        String head = """
                <?xml version="1.0" encoding="UTF-8"?>
                <log>""";
        String foot = """
                </log>""";

        String globalSimilarityMeasure;
        MethodList globalMethodInvokers;
        String localSimilarityMeasureFunc;
        String localMethodInvokersFunc;
        String localWeightFunc;
        FilterParameters filterParameters;
        int numberOfResults;

        xes = head + xes + foot;
        globalSimilarityMeasure = "ListDTWExt";
        Method m = new Method("setHalvingDistPercentage", List.of("double"), List.of("0.5"));
        globalMethodInvokers = new MethodList(new ArrayList<>(Collections.singleton(m)));
        localSimilarityMeasureFunc = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE similarity-measure-function SYSTEM "https://karim-amri.de/dtd/similaritymeasure-function.dtd">
                <similarity-measure-function>
                <if>
                <and>
                <equals>
                <method-return-value>
                <q/>
                <method name="getDataClass">
                </method>
                </method-return-value>
                <string value="XESEventClass"/>
                </equals>
                <equals>
                <method-return-value>
                <c/>
                <method name="getDataClass">
                </method>
                </method-return-value>
                <string value="XESEventClass"/>
                </equals>
                </and>
                <string value="CollectionIsolatedMappingExt"/>
                </if>
                </similarity-measure-function>""";

        localMethodInvokersFunc = null;
        localWeightFunc = null;
        filterParameters = new FilterParameters();
        numberOfResults = 10;


        // perform retrieval first time

        DIAGNOSTICS.trace("restapi.de.uni_trier.wi2.integration.DeterminismTest.service_test(): perform retrieval first time...");

        List<Retrieval> retrieval_1 = ProCAKEService.retrieve(
                1,
                xes,
                globalSimilarityMeasure,
                globalMethodInvokers,
                localSimilarityMeasureFunc,
                localMethodInvokersFunc,
                localWeightFunc,
                filterParameters,
                numberOfResults
        );

        DIAGNOSTICS.trace("restapi.de.uni_trier.wi2.integration.DeterminismTest.service_test(): first retrieval done...");

        // perform retrieval second time

        DIAGNOSTICS.trace("restapi.de.uni_trier.wi2.integration.DeterminismTest.service_test(): perform retrieval second time...");

        List<Retrieval> retrieval_2 = ProCAKEService.retrieve(
                1,
                xes,
                globalSimilarityMeasure,
                globalMethodInvokers,
                localSimilarityMeasureFunc,
                localMethodInvokersFunc,
                localWeightFunc,
                filterParameters,
                numberOfResults
        );

        DIAGNOSTICS.trace("restapi.de.uni_trier.wi2.integration.DeterminismTest.service_test(): second retrieval done...");

        // perform retrieval third time

        DIAGNOSTICS.trace("restapi.de.uni_trier.wi2.integration.DeterminismTest.service_test(): perform retrieval third time...");

        List<Retrieval> retrieval_3 = ProCAKEService.retrieve(
                1,
                xes,
                globalSimilarityMeasure,
                globalMethodInvokers,
                localSimilarityMeasureFunc,
                localMethodInvokersFunc,
                localWeightFunc,
                filterParameters,
                numberOfResults
        );


        String[] ids1 = new String[numberOfResults];
        String[] ids2 = new String[numberOfResults];
        String[] ids3 = new String[numberOfResults];
        double[] sims1 = new double[numberOfResults];
        double[] sims2 = new double[numberOfResults];
        double[] sims3 = new double[numberOfResults];
        for (int i = 0; i < numberOfResults; i++) {
            ids1[i] = retrieval_1.get(i).id();
            ids2[i] = retrieval_2.get(i).id();
            ids3[i] = retrieval_3.get(i).id();

            sims1[i] = retrieval_1.get(i).similarityValue();
            sims2[i] = retrieval_2.get(i).similarityValue();
            sims3[i] = retrieval_3.get(i).similarityValue();
        }

        Arrays.sort(ids1, Comparator.naturalOrder());
        Arrays.sort(ids2, Comparator.naturalOrder());
        Arrays.sort(ids3, Comparator.naturalOrder());
        Arrays.sort(sims1);
        Arrays.sort(sims2);
        Arrays.sort(sims3);

        DIAGNOSTICS.trace("restapi.de.uni_trier.wi2.integration.DeterminismTest.service_test(): compare results...");

        for (int i = 0; i < numberOfResults; i++) {
            assertEquals(ids1[i], ids2[i]);
            assertEquals(ids1[i], ids3[i]);
            assertEquals(sims1[i], sims2[i], 0);
            assertEquals(sims1[i], sims3[i], 0);
        }

        METHOD_CALL.trace("public void restapi.de.uni_trier.wi2.integration.DeterminismTest.service_test(): return");
    }


    @Test
    public void extension_test() throws Exception {

        // get traceID of first trace in first log

        String logID = DatabaseService.getLogIDs(true)[0];

        String traceID = DatabaseService.getTraceIDs(logID)[0];

        // get first trace of first log

        WriteableObjectPool<DataObject> casebase = ProCAKEService.getActualCasebase();
        NESTSequentialWorkflowObject trace = (NESTSequentialWorkflowObject) casebase.getObject(traceID);


        // define retrieval parameters

        String globalSimilarityMeasure;
        ArrayList<MethodInvoker> globalMethodInvokers = new ArrayList<>();
        SimilarityMeasureFunc localSimilarityMeasureFunc;
        MethodInvokersFunc localMethodInvokersFunc;
        WeightFunc localWeightFunc;
        FilterParameters filterParameters;
        int numberOfResults;

        globalSimilarityMeasure = "ListDTWExt";
        MethodInvoker m = new MethodInvoker("setHalvingDistPercentage", new Class[]{double.class}, new Object[]{0.5d});
        globalMethodInvokers.add(m);
        localSimilarityMeasureFunc = XMLtoSimilarityMeasureFuncConverter.getSimilarityMeasureFunc("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE similarity-measure-function SYSTEM "https://karim-amri.de/dtd/similaritymeasure-function.dtd">
                <similarity-measure-function>
                <if>
                <and>
                <equals>
                <method-return-value>
                <q/>
                <method name="getDataClass">
                </method>
                </method-return-value>
                <string value="XESEventClass"/>
                </equals>
                <equals>
                <method-return-value>
                <c/>
                <method name="getDataClass">
                </method>
                </method-return-value>
                <string value="XESEventClass"/>
                </equals>
                </and>
                <string value="CollectionIsolatedMappingExt"/>
                </if>
                </similarity-measure-function>""");

        localMethodInvokersFunc = MethodInvokersFunc.getDefault();
        localWeightFunc = WeightFunc.getDefault();
        numberOfResults = 10;


        LinearRetrieverImplExt retriever = new LinearRetrieverImplExt();


        retriever.setSimilarityModel(ProCAKEService.getSimilarityModel());
        retriever.setObjectPool(casebase);
        retriever.setAddQueryToResults(false);

        retriever.setGlobalSimilarityMeasure(globalSimilarityMeasure);
        retriever.setGlobalMethodInvokers(globalMethodInvokers);
        retriever.setLocalSimilarityMeasureFunc(localSimilarityMeasureFunc);
        retriever.setLocalMethodInvokersFunc(localMethodInvokersFunc);
        retriever.setLocalWeightFunc(localWeightFunc);

        // perform retrieval first time

        Query query1 = retriever.newQuery();
        query1.setQueryObject(trace);
        query1.setRetrieveCases(false); //we only want id's & similarity scores
        query1.setNumberOfResults(numberOfResults);

        RetrievalResultList retrievalResults1 = retriever.perform(query1);


        // perform retrieval second time

        Query query2 = retriever.newQuery();
        query2.setQueryObject(trace);
        query2.setRetrieveCases(false); //we only want id's & similarity scores
        query2.setNumberOfResults(numberOfResults);

        RetrievalResultList retrievalResults2 = retriever.perform(query2);


        // perform retrieval third time

        Query query3 = retriever.newQuery();
        query3.setQueryObject(trace);
        query3.setRetrieveCases(false); //we only want id's & similarity scores
        query3.setNumberOfResults(numberOfResults);

        RetrievalResultList retrievalResults3 = retriever.perform(query3);


        // compare results

        Iterator<RetrievalResult> it1 = retrievalResults1.iterator();
        Iterator<RetrievalResult> it2 = retrievalResults1.iterator();
        Iterator<RetrievalResult> it3 = retrievalResults1.iterator();
        RetrievalResult r1;
        RetrievalResult r2;
        RetrievalResult r3;
        String[] ids1 = new String[numberOfResults];
        String[] ids2 = new String[numberOfResults];
        String[] ids3 = new String[numberOfResults];
        double[] sims1 = new double[numberOfResults];
        double[] sims2 = new double[numberOfResults];
        double[] sims3 = new double[numberOfResults];
        int idx = 0;
        while (it1.hasNext() && it2.hasNext()) {// && it3.hasNext()){
            r1 = it1.next();
            r2 = it2.next();
            r3 = it3.next();

            ids1[idx] = r1.getObjectId();
            ids2[idx] = r2.getObjectId();
            ids3[idx] = r3.getObjectId();
            sims1[idx] = r1.getSimilarity().getValue();
            sims2[idx] = r2.getSimilarity().getValue();
            sims3[idx] = r3.getSimilarity().getValue();

            idx++;
        }

        Arrays.sort(ids1, Comparator.naturalOrder());
        Arrays.sort(ids2, Comparator.naturalOrder());
        Arrays.sort(ids3, Comparator.naturalOrder());
        Arrays.sort(sims1);
        Arrays.sort(sims2);
        Arrays.sort(sims3);

        for (int i = 0; i < numberOfResults; i++) {
            assertEquals(ids1[i], ids2[i]);
            assertEquals(ids1[i], ids3[i]);
            assertEquals(sims1[i], sims2[i], 0);
            assertEquals(sims1[i], sims3[i], 0);
        }

    }


}