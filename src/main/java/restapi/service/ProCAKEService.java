package restapi.service;


import de.uni_trier.wi2.FileToXESGraphConverter;
import de.uni_trier.wi2.XESGraphToWorkflowConverter;
import de.uni_trier.wi2.XESTraceGraph;
import de.uni_trier.wi2.procake.CakeInstance;
import de.uni_trier.wi2.procake.data.model.DataClass;
import de.uni_trier.wi2.procake.data.model.Model;
import de.uni_trier.wi2.procake.data.model.ModelFactory;
import de.uni_trier.wi2.procake.data.object.DataObject;
import de.uni_trier.wi2.procake.data.object.base.StringObject;
import de.uni_trier.wi2.procake.data.object.nest.NESTSequentialWorkflowObject;
import de.uni_trier.wi2.procake.data.objectpool.ObjectPoolFactory;
import de.uni_trier.wi2.procake.data.objectpool.ReadableObjectPool;
import de.uni_trier.wi2.procake.data.objectpool.WriteableObjectPool;
import de.uni_trier.wi2.procake.retrieval.Query;
import de.uni_trier.wi2.procake.retrieval.RetrievalResult;
import de.uni_trier.wi2.procake.retrieval.RetrievalResultList;
import de.uni_trier.wi2.procake.similarity.SimilarityMeasure;
import de.uni_trier.wi2.procake.similarity.SimilarityModel;
import de.uni_trier.wi2.procake.similarity.base.SMObjectEqual;
import de.uni_trier.wi2.procake.similarity.base.impl.SMObjectEqualImpl;
import de.uni_trier.wi2.procake.similarity.impl.SimilarityMeasureImpl;
import de.uni_trier.wi2.procake.similarity.impl.SimilarityModelImpl;
import de.uni_trier.wi2.procake.utils.exception.NameAlreadyExistsException;
import extension.retrieval.LinearRetrieverImplExt;
import extension.similarity.measure.SMBooleanXORImpl;
import extension.similarity.measure.SMStringLevenshteinImplExt;
import extension.similarity.measure.collection.*;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;
import parsing.XMLtoMethodInvokersFuncConverter;
import parsing.XMLtoSimilarityMeasureFuncConverter;
import parsing.XMLtoWeightFuncConverter;
import restapi.model.FilterParameters;
import restapi.model.Method;
import restapi.model.MethodList;
import restapi.model.Retrieval;
import utils.MethodInvoker;
import utils.MethodInvokersFunc;
import utils.SimilarityMeasureFunc;
import utils.WeightFunc;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * The service implementing all business logic for interacting with the ProCAKE instance.
 */
@Service
public class ProCAKEService {

    /**
     * The casebase used for retrieval.
     */
    static WriteableObjectPool<DataObject> casebase;

    /**
     * The similarity model the ProCAKE instance uses.
     */
    static SimilarityModel similarityModel;

    /**
     * The data model the ProCAKE instance uses.
     */
    static Model model;

    /**
     * Maps the name of a DataClass to a List of SimilarityMeasures that should be applicable to that DataClass.
     */
    static Map<String, List<SimilarityMeasureImpl>> dataClassToSimilarityMeasureMap;

    /**
     * Sets up ProCAKE instance.
     *
     * @return status message
     */
    public static String setupCake() {
        CakeInstance.start();
        setupDataModel();
        setupSimilarityModel();
        return "ProCAKE instance set up";
    }

    /**
     * Sets up data model.
     */
    private static void setupDataModel() {
        model = ModelFactory.getDefaultModel();
    }

    /**
     * Sets up similarity model.
     */
    private static void setupSimilarityModel() {
        similarityModel = new SimilarityModelImpl();

		dataClassToSimilarityMeasureMap = new HashMap<>();

        addSimilarityMeasureToSimilarityModel(new SMObjectEqualImpl(),                  model.getDataSystemClass());

        addSimilarityMeasureToSimilarityModel(new SMCollectionIsolatedMappingImplExt(), model.getDataSystemClass());
        addSimilarityMeasureToSimilarityModel(new SMCollectionMappingImplExt(),         model.getDataSystemClass());

        addSimilarityMeasureToSimilarityModel(new SMListCorrectnessImplExt(),           model.getDataSystemClass());
        addSimilarityMeasureToSimilarityModel(new SMListDTWImplExt(),                   model.getDataSystemClass());
        addSimilarityMeasureToSimilarityModel(new SMListMappingImplExt(),               model.getDataSystemClass());
        addSimilarityMeasureToSimilarityModel(new SMListSWAImplExt(),                   model.getDataSystemClass());

        addSimilarityMeasureToSimilarityModel(new SMStringLevenshteinImplExt(),         model.getDataSystemClass());
        addSimilarityMeasureToSimilarityModel(new SMBooleanXORImpl(),                   model.getDataSystemClass());

        similarityModel.setDefaultSimilarityMeasure(model.getDataSystemClass(), SMObjectEqual.NAME);
    }

    /**
     * Adds a similarity measure to the similarity model.
     *
     * @param sm        a {@link SimilarityMeasureImpl}ementation
     * @param dataClass the data class for which the similarity measure will be available
     */
    private static void addSimilarityMeasureToSimilarityModel(SimilarityMeasureImpl sm, DataClass dataClass) {

        // puts name and SimilarityMeasure-Object in cache (should be called only once per SM)
        try {
            similarityModel.registerSimilarityMeasureTemplate(sm);
        } catch (NameAlreadyExistsException ignored) {
        }

        // sets the DataClass the SM can be applied to
        sm.setDataClass(dataClass);

        //adds sm to the SMs that can be applied to 'dataClass'
        similarityModel.addSimilarityMeasure(sm, sm.getSystemName());

    }

    /**
     * Reloads the traces from the database into the casebase after converting them to {@link NESTSequentialWorkflowObject}s.
     *
     * @return status message
     */
    public static String loadCasebase() {

        try {

            // Re-instantiate the case base
            casebase = ObjectPoolFactory.newObjectPool();

            // Get the converter
            // We will convert from .xes file to 'XESTraceGraph' to 'NESTWorkflowObject' and then
            // cast to 'NESTSequentialWorkflow'.
            XESGraphToWorkflowConverter converter = new XESGraphToWorkflowConverter(model);


            // The traces are Strings starting with "<trace>" and ending with "</trace>",
            // so they are actually no valid xml documents.
            // The converter however requires for the files content not only to be a valid xml document,
            // but also to be a valid xes document, the root element of which is a log tag ("<log ...>").
            // Additionally, the files name has to end with ".xes" (see above).

            Map<String, Object> log;
            String header, prefix, suffix;
            suffix = "</log>";

            Map<String, Object> trace;
            String xes;

            // We will go through every Trace one by one, so we can set the NESTWorkflow's id's separately
            for (String logID : DatabaseService.getLogIDs(false)) {
                log = DatabaseService.getLog(logID);
                header = (String) log.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__log__header);
                prefix = header.split("</log>")[0];

                for (String traceID : DatabaseService.getTraceIDs(logID)) {
                    trace = DatabaseService.getTrace(traceID);
                    xes = (String) trace.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__xes);


                    // Convert the log containing one trace and get said trace.
                    XESTraceGraph graph = (XESTraceGraph) new FileToXESGraphConverter().convert(prefix + xes + suffix).toArray()[0];

                    // The converter is designed to convert to NESTWorkflowObjects on general, which
                    // can have arbitrary edges between their TaskNodes. Because of that we
                    // have to tell the graph to have its edges set in the document order.
                    graph.addEdgesByDocumentOrder();

                    // Now we have to convert and cast to a NESTSequentialWorkflowObject.
                    NESTSequentialWorkflowObject workflow = (NESTSequentialWorkflowObject) model.getNESTSequentialWorkflowClass().newObject();
                    workflow.transformNESTGraphToNESTSequentialWorkflow(converter.convert(graph));

                    workflow.setId(traceID);
                    casebase.store(workflow);
                }
            }

            //converter.printCreatedClasses(true);
            //for (SimilarityMeasure sm : similarityModel.getSimilarityMeasures()) System.out.println(sm.getDataClass() + ": " + sm.getSystemName() + "\n");

            return "Casebase loaded successfully!";

        } catch (SQLException e) {
            return "Failed to load casebase!" + e.getMessage();
        }
    }

    /**
     * Performs retrieval.
     *
     * @param xes                     XES log containing (at least) one trace
     * @param globalSimilarityMeasure similarity measure used on the global level
     * @param globalMethodInvokerList list of methods to be invoked on the global similarity measure
     * @param similarityMeasureFunc   XML representation of {@link SimilarityMeasureFunc}
     * @param methodInvokersFunc      XML representation of {@link MethodInvokersFunc}
     * @param weightFunc              XML representation of {@link WeightFunc}
     * @param filterParameters        parameters used to filter the casebase
     * @param numberOfResults         number of retrieval results to be returned
     * @return list of retrieval results
     * @throws ParserConfigurationException todo
     * @throws IOException                  todo
     * @throws SAXException                 todo
     */
    public static List<Retrieval> retrieve(
            String xes,
            String globalSimilarityMeasure,
            MethodList globalMethodInvokerList,
            String similarityMeasureFunc,
            String methodInvokersFunc,
            String weightFunc,
            FilterParameters filterParameters,
            int numberOfResults
    ) throws ParserConfigurationException, IOException, SAXException {

        // - preparation of retrieval - //

        // All the converters require a file as input, so instead of creating and deleting a new file
        // everytime a conversion function is called, they all just use one, which is deleted when it
        // is no longer needed.

        NESTSequentialWorkflowObject trace = convertQuery(xes);

        ArrayList<MethodInvoker> globalMethodInvokers = convertGlobalMethodInvokers(globalMethodInvokerList);

        SimilarityMeasureFunc localSimilarityMeasureFunc = XMLtoSimilarityMeasureFuncConverter.getSimilarityMeasureFunc(similarityMeasureFunc);

        MethodInvokersFunc localMethodInvokersFunc = XMLtoMethodInvokersFuncConverter.getMethodInvokersFunc(methodInvokersFunc);

        WeightFunc localWeightFunc = XMLtoWeightFuncConverter.getWeightFunc(weightFunc);


        // - retrieval - //

        LinearRetrieverImplExt linearRetrieverImplExt = new LinearRetrieverImplExt();
        linearRetrieverImplExt.setSimilarityModel(similarityModel);
        linearRetrieverImplExt.setObjectPool(getFilteredCasebase(filterParameters));
        linearRetrieverImplExt.setAddQueryToResults(false);

        linearRetrieverImplExt.setGlobalSimilarityMeasure(globalSimilarityMeasure);
        linearRetrieverImplExt.setGlobalMethodInvokers(globalMethodInvokers);
        linearRetrieverImplExt.setLocalSimilarityMeasureFunc(localSimilarityMeasureFunc);
        linearRetrieverImplExt.setLocalMethodInvokersFunc(localMethodInvokersFunc);
        linearRetrieverImplExt.setLocalWeightFunc(localWeightFunc);

        Query query = linearRetrieverImplExt.newQuery();
        query.setQueryObject(trace);
        query.setRetrieveCases(false); //we only want id's & similarity scores
        query.setNumberOfResults(numberOfResults);

        RetrievalResultList retrievalResults = linearRetrieverImplExt.perform(query);
        Iterator<RetrievalResult> retrievalResultIterator = retrievalResults.iterator();

        List<Retrieval> results = new ArrayList<>();
        RetrievalResult retrievalResult;
        while (retrievalResultIterator.hasNext()) {
            retrievalResult = retrievalResultIterator.next();
            results.add(new Retrieval(
                    retrievalResult.getObjectId(),
                    retrievalResult.getSimilarity().getValue()
            ));
        }

        return results;
    }

    /**
     * Converts a trace to a {@link NESTSequentialWorkflowObject}.
     *
     * @param xes XES log containing (at least) one trace
     * @return trace in form of a NESTGraph
     * @throws IOException todo
     */
    private static NESTSequentialWorkflowObject convertQuery(String xes) throws IOException {

        // Convert the log containing one trace and get said trace.
        XESTraceGraph graph = (XESTraceGraph) new FileToXESGraphConverter().convert(xes).toArray()[0];

        // The converter is designed to convert to NESTWorkflowObjects on general, which
        // can have arbitrary edges between their TaskNodes. Because of that we
        // have to tell the graph to have its edges set in the document order.
        graph.addEdgesByDocumentOrder();

        // Now we have to convert and cast to a NESTSequentialWorkflowObject.
        XESGraphToWorkflowConverter converter = new XESGraphToWorkflowConverter(model);
        NESTSequentialWorkflowObject workflow = (NESTSequentialWorkflowObject) ModelFactory.getDefaultModel().getNESTSequentialWorkflowClass().newObject();
        workflow.transformNESTGraphToNESTSequentialWorkflow(converter.convert(graph));

        return workflow;
    }

    /**
     * Converts a {@link MethodList} object to an {@link ArrayList} of {@link MethodInvoker}s.
     *
     * @param globalMethodInvokers the method list
     * @return list of MethodInvokers
     */
    private static ArrayList<MethodInvoker> convertGlobalMethodInvokers(MethodList globalMethodInvokers) {
        ArrayList<MethodInvoker> globalMethodInvokerList = new ArrayList<>();
        if (globalMethodInvokers == null) return globalMethodInvokerList;
        for (Method m : globalMethodInvokers.methods()) {
            Class<?> clazz;
            Object object;

            switch (m.valueType()) {
                case TYPE_NAME_STRING -> {
                    clazz = String.class;
                    object = m.value();
                }
                case TYPE_NAME_DOUBLE -> {
                    clazz = Double.class;
                    object = Double.parseDouble(m.value());
                }
                case TYPE_NAME_BOOLEAN -> {
                    clazz = Boolean.class;
                    object = Boolean.parseBoolean(m.value());
                }
                default -> {
                    clazz = null;
                    object = null;
                }
            }

            globalMethodInvokerList.add(new MethodInvoker(
                    m.name(),
                    new Class[]{clazz},
                    new Object[]{object}
            ));
        }
        return globalMethodInvokerList;
    }
    static final String TYPE_NAME_STRING = "string";
    static final String TYPE_NAME_DOUBLE = "double";
    static final String TYPE_NAME_BOOLEAN = "boolean";

    private static SimilarityMeasureFunc convertLocalSimilarityMeasureFunc(File file, String similarity_measure_func) throws ParserConfigurationException, IOException, SAXException {
        PrintWriter fw = new PrintWriter(file);
        fw.write(similarity_measure_func);
        fw.close();

        return XMLtoSimilarityMeasureFuncConverter.getSimilarityMeasureFunc(file);
    }

    private static MethodInvokersFunc convertLocalMethodInvokersFunc(File file, String method_invokers_func) throws ParserConfigurationException, IOException, SAXException {
        PrintWriter fw = new PrintWriter(file);
        fw.write(method_invokers_func);
        fw.close();

        return XMLtoMethodInvokersFuncConverter.getMethodInvokersFunc(file);
    }

    private static WeightFunc convertLocalWeightFunc(File file, String weight_func) throws ParserConfigurationException, IOException, SAXException {
        PrintWriter fw = new PrintWriter(file);
        fw.write(weight_func);
        fw.close();

        return XMLtoWeightFuncConverter.getWeightFunc(file);
    }

    private static ReadableObjectPool<DataObject> getFilteredCasebase(FilterParameters parameters) {
        return casebase; //todo
    }


    @Deprecated
    // for testing purposes only //ProCAKE service/controller should not provide method to get any cases from the casebase
    public static List<String[]> getCasebase() {
        List<String[]> cases = new ArrayList<>();
        for (DataObject trace : casebase.getCollection()) {
            cases.add(new String[]{
                    trace.getId(), ((StringObject) trace).getNativeString()});
        }
        return cases;
    }

}