package de.uni_trier.wi2.eval;

import com.fasterxml.jackson.databind.*;
import de.uni_trier.wi2.*;
import de.uni_trier.wi2.extension.similarity.measure.collection.*;
import de.uni_trier.wi2.model.*;
import de.uni_trier.wi2.service.*;
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

import static de.uni_trier.wi2.service.IOUtils.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = RESTAPI.class, args = {"jdbc:mysql://localhost:3306/onkocase_test", "root", "pw1234"})
@AutoConfigureMockMvc
public class EvalSepsis {
    final static String[] listSimilarityMeasures;
    final static String[] localWeightFuncs;

    static {
        listSimilarityMeasures = new String[]{SMCollectionIsolatedMappingExt.NAME, SMCollectionMappingExt.NAME, SMListCorrectnessExt.NAME, SMListDTWExt.NAME, SMListSWAExt.NAME, SMListMappingExt.NAME,};

        localWeightFuncs = new String[]{null, """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE weight-function SYSTEM "https://karim-amri.de/dtd/weight-function.dtd">
                <weight-function>
                    <if>
                        <or>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="?"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="D"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="E"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="F"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="G"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="H"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="I"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="J"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="K"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="M"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="N"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="O"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="P"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="Q"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="R"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="S"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="T"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="U"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="V"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="W"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="X"/>
                            <function name="qEventContainsAttribute" arg1="org:group" arg2="String" arg3="Y"/>
                
                            <function name="qEventContainsAttribute" arg1="concept:name" arg2="String"
                                arg3="Admission IC"/>
                            <function name="qEventContainsAttribute" arg1="concept:name" arg2="String"
                                arg3="Admission NC"/>
                            <function name="qEventContainsAttribute" arg1="concept:name" arg2="String"
                                arg3="IV Antibiotics"/>
                            <function name="qEventContainsAttribute" arg1="concept:name" arg2="String"
                                arg3="Release A"/>
                            <function name="qEventContainsAttribute" arg1="concept:name" arg2="String"
                                arg3="Release B"/>
                            <function name="qEventContainsAttribute" arg1="concept:name" arg2="String"
                                arg3="Release C"/>
                            <function name="qEventContainsAttribute" arg1="concept:name" arg2="String"
                                arg3="Release D"/>
                            <function name="qEventContainsAttribute" arg1="concept:name" arg2="String"
                                arg3="Release E"/>
                            <function name="qEventContainsAttribute" arg1="concept:name" arg2="String"
                                arg3="Return ER"/>
                
                            <function name="qAttributeHasKeyTypeValue" arg1="concept:name" arg2="String"/>
                            <function name="qAttributeHasKeyTypeValue" arg1="lifecycle:transition" arg2="String"/>
                            <function name="qAttributeHasKeyTypeValue" arg1="org:group" arg2="String"/>
                            <function name="qAttributeHasKeyTypeValue" arg1="time:timestamp" arg2="String"/>
                        </or>
                        <double value="0"/>
                    </if>
                
                    <if>
                        <or>
                            <function name="qEventContainsAttribute" arg1="concept:name" arg2="String"
                                arg3="ER Registration"/>
                            <function name="qEventContainsAttribute" arg1="concept:name" arg2="String"
                                arg3="ER Triage"/>
                        </or>
                        <double value="1"/>
                    </if>
                    <if>
                        <function name="qEventContainsAttribute" arg1="concept:name" arg2="String"
                            arg3="ER Sepsis Triage"/>
                        <double value="0.99904764"/>
                    </if>
                    <if>
                        <function name="qEventContainsAttribute" arg1="concept:name" arg2="String" arg3="Leucocytes"/>
                        <double value="0.96380955"/>
                    </if>
                    <if>
                        <function name="qEventContainsAttribute" arg1="concept:name" arg2="String" arg3="CRP"/>
                        <double value="0.9590476"/>
                    </if>
                    <if>
                        <function name="qEventContainsAttribute" arg1="concept:name" arg2="String" arg3="LacticAcid"/>
                        <double value="0.81904763"/>
                    </if>
                    <if>
                        <function name="qEventContainsAttribute" arg1="concept:name" arg2="String"
                            arg3="IV Antibiotics"/>
                        <double value="0.78380954"/>
                    </if>
                
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="Leucocytes" arg2="Float"/>
                        <double value="0.22091495"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="CRP" arg2="Float"/>
                        <double value="0.20527147"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="LacticAcid" arg2="Float"/>
                        <double value="0.09556987"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="Age" arg2="Integer"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="DiagnosticArtAstrup" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="DiagnosticBlood" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="DiagnosticECG" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="DiagnosticIC" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="DiagnosticLacticAcid" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="DiagnosticLiquor" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="DiagnosticOther" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="DiagnosticSputum" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="DiagnosticUrinaryCulture" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="DiagnosticUrinarySediment" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="DiagnosticXthorax" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="DisfuncOrg" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="Hypotensie" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="Hypoxie" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="InfectionSuspected" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="Infusion" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="Oligurie" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="SIRSCritHeartRate" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="SIRSCritLeucos" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="SIRSCritTachypnea" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="SIRSCritTemperature" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="SIRSCriteria2OrMore" arg2="Boolean"/>
                        <double value="0.06901538"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue" arg1="Diagnose" arg2="String	"/>
                        <double value="0.05238596"/>
                    </if>
                </weight-function>
                """};
    }

    final String savepoint = "spt";
    @Autowired
    private MockMvc mvc;

    @Before
    public void before() throws SQLException, IOException, SAXException {

        String log = getResourceAsString("eval/sepsis.xes");

        DatabaseService.startTransaction();
        DatabaseService.deleteAll();
        DatabaseService.putLog(log);
        ProCAKEService.setupCake();
        ProCAKEService.loadCasebase();

    }

    @After
    public void after() throws SQLException, IOException {

        //DatabaseService.deleteAll();
        DatabaseService.commit();

    }

    @Test
    public void performRetrieval() throws Exception {

        int c = 1;
        int max = listSimilarityMeasures.length * listSimilarityMeasures.length * localWeightFuncs.length;

        if (false) performRetrieval(c++, max, listSimilarityMeasures[0], listSimilarityMeasures[1], null);

        if (false) {
            for (String globalSimilarityMeasure : listSimilarityMeasures) {
                for (String eventSimilarityMeasure : listSimilarityMeasures) {
                    for (String localWeightFunc : localWeightFuncs) {
                        performRetrieval(c++, max, globalSimilarityMeasure, eventSimilarityMeasure, localWeightFunc);
                    }
                }
            }
        }
    }


    public void performRetrieval(int iteration, int maxIterations, String globalSimilarityMeasure, String eventSimilarityMeasure, String localWeightFunc) throws Exception {

        System.out.printf("%n--------------------------------------------------------------------------------%nIteration %d of %d%nTrace Sim: %s%nEvent Sim: %s%nWeightFunc: %s%n", iteration, maxIterations, globalSimilarityMeasure, eventSimilarityMeasure, localWeightFunc == null ? "null" : "complex");


        long start = System.nanoTime();


        // get all logs

        String result = mvc.perform(get("/log")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        ArrayList<Map<String, Object>> logs = new ObjectMapper().readValue(result, ArrayList.class);


        // get first traceID in first log

        String traceID = (String) ((ArrayList<LinkedHashMap>) logs.get(0).get("traces")).get(0).get("traceID");


        // define retrieval parameters

        String xes;
        MethodList globalMethodInvokers;
        String localSimilarityMeasureFunc;
        String localMethodInvokersFunc;
        FilterParameters filterParameters;
        int numberOfResults;

        xes = "";
        globalMethodInvokers = new MethodList(new ArrayList<>());
        localSimilarityMeasureFunc = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE similarity-measure-function SYSTEM "https://karim-amri.de/dtd/similaritymeasure-function.dtd">
                <similarity-measure-function>
                    <if>
                        <function name="qEventContainsAttribute"/>
                        <string value=""" + "\"" + eventSimilarityMeasure + "\"" + """
                />
                    </if>
                    <if>
                        <function name="qcAttributesHaveSameKeyAndType" arg2="String"/>
                        <string value="StringLevenshteinExt"/>
                    </if>
                    <if>
                        <or>
                            <function name="qcAttributesHaveSameKeyAndType" arg2="Float"/>
                            <function name="qcAttributesHaveSameKeyAndType" arg2="Integer"/>
                        </or>
                        <string value="ChronologicalOrNumericComparison100"/>
                    </if>
                    <if>
                        <function name="qcAttributesHaveSameKeyAndType" arg2="Boolean"/>
                        <string value="BooleanEquivalence"/>
                    </if>
                    <if>
                        <function name="qAttributeHasKeyTypeValue"/>
                        <string value="ObjectEqual"/>
                    </if>
                </similarity-measure-function>
                """;

        localMethodInvokersFunc = null;
        filterParameters = new FilterParameters();
        numberOfResults = 11;

        RetrievalParameters retrievalParameters = new RetrievalParameters(xes, globalSimilarityMeasure, globalMethodInvokers, localSimilarityMeasureFunc, localMethodInvokersFunc, localWeightFunc, filterParameters, numberOfResults);


        // perform retrieval

        result = mvc.perform(put("/retrieval/" + traceID).contentType(MediaType.APPLICATION_JSON).content((new ObjectMapper()).writer().withDefaultPrettyPrinter().writeValueAsString(retrievalParameters))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        long finish = System.nanoTime();
        long timeElapsed = finish - start;

        double totalSeconds = timeElapsed / 1E9;
        int seconds = (int) (totalSeconds % 60);
        int minutes = (int) ((totalSeconds - seconds) / 60);


        System.out.printf("Time elapsed: %d:%d [min:sec]%n--------------------------------------------------------------------------------%n", minutes, seconds);

        ArrayList<?> retrieval = new ObjectMapper().readValue(result, ArrayList.class);

        for (int i = 1; i < retrieval.size(); i++) {
            Map<?, ?> map = (Map<?, ?>) retrieval.get(i);
            String caseID = (String) map.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__traceID);
            Double simVal = (Double) map.get("similarity");
            System.out.printf("%d) %s%nsimilarity: %f%n%n", i, caseID, simVal);
        }
    }
}
