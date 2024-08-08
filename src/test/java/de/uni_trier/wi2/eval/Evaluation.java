package de.uni_trier.wi2.eval;

import com.fasterxml.jackson.databind.*;
import de.uni_trier.wi2.*;
import de.uni_trier.wi2.extension.similarity.measure.collection.*;
import de.uni_trier.wi2.model.*;
import de.uni_trier.wi2.service.*;
import org.junit.*;
import org.junit.runner.*;
import org.slf4j.*;
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
public class Evaluation {
    final static String[] listSimilarityMeasures;
    final static String localSimilarityMeasureFunc;
    final static String sepsisWeightFunc;
    final static String hosbilWeightFunc;
    final static Logger logger = LoggerFactory.getLogger(Evaluation.class);
    final static String logFilePath = "target/logs/";
    final static String logFileName = "eval.log";

    static {
        listSimilarityMeasures = new String[]{SMListDTWExt.NAME, SMCollectionMappingExt.NAME};

        localSimilarityMeasureFunc = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE similarity-measure-function SYSTEM "https://karim-amri.de/dtd/similaritymeasure-function.dtd">
                <similarity-measure-function>
                    <if>
                        <function name="qEventContainsAttribute"/>
                        <string value="CollectionIsolatedMappingExt"/>
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

        sepsisWeightFunc = """
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
                                
                            <function name="qAttributeHasKeyTypeValue" arg1="lifecycle:transition" arg2="String"/>
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
                """;

        hosbilWeightFunc = null;
    }

    @Autowired
    private MockMvc mvc;


    private static void log(String action, String evtLog, int size, String simMeasure, long nanosec) throws IOException {
        long totalSeconds = (long) (nanosec / 1E9);
        long seconds = (totalSeconds % 60);
        long minutes = ((totalSeconds - seconds) / 60);
        String msg = String.format("%s%s:%s(%d): %d ns = %d:%d min:sec | %.2f GB", action, simMeasure == null ? "" : "(" + simMeasure + ")", evtLog, size, nanosec, minutes, seconds, HeapSpace.getGBandWipe());
        logger.info(msg);
        File file = new File(logFilePath + logFileName);
        file.createNewFile();
        FileWriter fw = new FileWriter(file, true);
        fw.write(msg);
        fw.write("\n");
        fw.close();
    }

    private static void logEmpty() throws IOException {
        File file = new File(logFilePath + logFileName);
        file.createNewFile();
        FileWriter fw = new FileWriter(file, true);
        fw.write("\n");
        fw.close();
    }

    @Before
    public void before() throws SQLException, IOException, SAXException {
        logger.info(String.format("Maximum Heap Space: %.2f GB%n", Runtime.getRuntime().maxMemory()/1E9));
        DatabaseService.startTransaction();
        DatabaseService.deleteAll();
        ProCAKEService.setupCake();

        new File(logFilePath).mkdirs();

        File file = new File(logFilePath + logFileName);
        file.createNewFile();
        FileWriter fw = new FileWriter(file);
        fw.write("");
        fw.close();
    }

    @After
    public void after() throws SQLException, IOException {
        DatabaseService.deleteAll();
        DatabaseService.commit();

    }

    @Test
    @Ignore
    public void evalEasyToHard() throws Exception {
        long start, finish, timeElapsed;
        double totalSeconds;
        int seconds, minutes;
        String xes, trace;

        // SEPSIS
        xes = getResourceAsString("eval/sepsis.xes");
        trace = "<trace" + xes.split("<trace")[1].split("</trace>")[0] + "</trace>"; // first trace

        // Upload of Sepsis
        start = System.nanoTime();
        upload(xes);
        finish = System.nanoTime();
        timeElapsed = finish - start;
        log("upload", "sepsis", 1050, null, timeElapsed);

        System.gc();

        // Reload of Sepsis
        start = System.nanoTime();
        reload();
        finish = System.nanoTime();
        timeElapsed = finish - start;
        log("reload", "sepsis", 1050, null, timeElapsed);

        // Retrieval of Sepsis
        for (String similarityMeasure : listSimilarityMeasures) {
            //start = System.nanoTime();
            //retrieve(trace, similarityMeasure, sepsisWeightFunc);
            //finish = System.nanoTime();
            //timeElapsed = finish - start;
            //log("retrieval", "sepsis", 1050, similarityMeasure, timeElapsed);
        }

        // CLEAN
        logEmpty();
        DatabaseService.deleteAll();
        ProCAKEService.loadCasebase();
        System.gc();


        // HOSPITAL BILLING
        xes = getResourceAsString("eval/hospital_billing.xes");
        Axe axe = new Axe();
        String[] splitLog = axe.split(xes);
        String header = splitLog[splitLog.length - 1];
        trace = splitLog[0]; // first trace

        int[] sizes = new int[]{(int) 10E4, (int) 9E4, (int) 8E4, (int) 7E4, (int) 6E4, (int) 5E4, (int) 4E4, (int) 3E4, (int) 2.5E4, (int) 2E4, (int) 1.5E4, (int) 1E4, (int) 5E3, (int) 3E3, (int) 2.5E3, (int) 2E3, (int) 1.5E3, (int) 1E3,};

        for (int s = sizes.length - 1; s >= 0; s--) {
        //for (int s = 0; s < sizes.length; s++) {
            int size = sizes[s];
            // Downsizing of Hospital Billing
            StringBuilder smallXes = new StringBuilder();
            smallXes.append(header.split("</log>")[0]);
            for (int i = 0; i < size; i++) {
                smallXes.append(splitLog[i]);
            }
            smallXes.append("</log>");

            // Upload of Hospital Billing
            start = System.nanoTime();
            upload(smallXes.toString());
            finish = System.nanoTime();
            timeElapsed = finish - start;
            log("upload", "hosBill", size, null, timeElapsed);

            System.gc();

            // Reload of Hospital Billing
            start = System.nanoTime();
            reload();
            finish = System.nanoTime();
            timeElapsed = finish - start;
            totalSeconds = timeElapsed / 1E9;
            seconds = (int) (totalSeconds % 60);
            minutes = (int) ((totalSeconds - seconds) / 60);
            log("reload", "hosBill", size, null, timeElapsed);

            // Retrieval of Hospital Billing
            for (String similarityMeasure : listSimilarityMeasures) {
                //start = System.nanoTime();
                //retrieve(trace, similarityMeasure, hosbilWeightFunc);
                //finish = System.nanoTime();
                //timeElapsed = finish - start;
                //totalSeconds = timeElapsed / 1E9;
                //seconds = (int) (totalSeconds % 60);
                //minutes = (int) ((totalSeconds - seconds) / 60);
                //log("retrieval", "hosBill", size, similarityMeasure, timeElapsed);
            }

            // CLEAN
            logEmpty();
            DatabaseService.deleteAll();
            ProCAKEService.loadCasebase();
            System.gc();
        }

    }

    private void upload(String xes) throws Exception {
        mvc.perform(post("/log").content(xes)).andExpect(status().isOk()).andReturn();
    }

    private void reload() throws Exception {
        mvc.perform(get("/procake/reload")).andExpect(status().isOk());
    }

    private void retrieve(String trace, String similarityMeasure, String localWeightFunc) throws Exception {
        // define retrieval parameters
        MethodList globalMethodInvokers = new MethodList(new ArrayList<>());
        String localMethodInvokersFunc = null;
        FilterParameters filterParameters = new FilterParameters();
        int numberOfResults = 11;
        RetrievalParameters retrievalParameters = new RetrievalParameters(trace, similarityMeasure, globalMethodInvokers, localSimilarityMeasureFunc, localMethodInvokersFunc, localWeightFunc, filterParameters, numberOfResults);

        // perform retrieval
        mvc.perform(put("/retrieval").contentType(MediaType.APPLICATION_JSON).content((new ObjectMapper()).writer().withDefaultPrettyPrinter().writeValueAsString(retrievalParameters))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

}
