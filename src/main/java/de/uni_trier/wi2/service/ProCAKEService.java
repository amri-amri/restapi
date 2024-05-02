package de.uni_trier.wi2.service;


import de.uni_trier.wi2.conversion.sax.XEStoNESTsAXConverter;
import de.uni_trier.wi2.conversion.sax.XEStoNESTsAXparallelConverter;
import de.uni_trier.wi2.extension.retrieval.LinearRetrieverImplExt;
import de.uni_trier.wi2.extension.similarity.measure.SMBooleanXORImpl;
import de.uni_trier.wi2.extension.similarity.measure.SMStringLevenshteinImplExt;
import de.uni_trier.wi2.extension.similarity.measure.collection.*;
import de.uni_trier.wi2.model.FilterParameters;
import de.uni_trier.wi2.model.Method;
import de.uni_trier.wi2.model.MethodList;
import de.uni_trier.wi2.model.Retrieval;
import de.uni_trier.wi2.parsing.XMLtoMethodInvokersFuncConverter;
import de.uni_trier.wi2.parsing.XMLtoSimilarityMeasureFuncConverter;
import de.uni_trier.wi2.parsing.XMLtoWeightFuncConverter;
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
import de.uni_trier.wi2.procake.similarity.SimilarityModel;
import de.uni_trier.wi2.procake.similarity.base.SMObjectEqual;
import de.uni_trier.wi2.procake.similarity.base.impl.SMObjectEqualImpl;
import de.uni_trier.wi2.procake.similarity.impl.SimilarityMeasureImpl;
import de.uni_trier.wi2.procake.similarity.impl.SimilarityModelImpl;
import de.uni_trier.wi2.procake.utils.exception.NameAlreadyExistsException;
import de.uni_trier.wi2.utils.MethodInvoker;
import de.uni_trier.wi2.utils.MethodInvokersFunc;
import de.uni_trier.wi2.utils.SimilarityMeasureFunc;
import de.uni_trier.wi2.utils.WeightFunc;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import static de.uni_trier.wi2.RestAPILoggingUtils.*;

/**
 * The service implementing all business logic for interacting with the ProCAKE instance.
 */
@Service
public class ProCAKEService {

    static final String TYPE_NAME_STRING = "string";
    static final String TYPE_NAME_DOUBLE = "double";
    static final String TYPE_NAME_BOOLEAN = "boolean";

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
        METHOD_CALL.trace("public static String restapi.service.ProCAKEService.setupCake()...");
        CakeInstance.start();
        setupDataModel();
        setupSimilarityModel();
        METHOD_CALL.trace("service.ProCAKEService.setupCake(): return \"{}\"", "ProCAKE instance set up");
        return "ProCAKE instance set up";
    }

    /**
     * Sets up data model.
     */
    private static void setupDataModel() {
        METHOD_CALL.trace("private static void restapi.service.ProCAKEService.setupDataModel()...");
        model = ModelFactory.getDefaultModel();
    }

    /**
     * Sets up similarity model.
     */
    private static void setupSimilarityModel() {
        METHOD_CALL.trace("private static void restapi.service.ProCAKEService.setupSimilarityModel()...");

        similarityModel = new SimilarityModelImpl();

        dataClassToSimilarityMeasureMap = new HashMap<>();

        addSimilarityMeasureToSimilarityModel(new SMObjectEqualImpl(), model.getDataSystemClass());

        addSimilarityMeasureToSimilarityModel(new SMCollectionIsolatedMappingImplExt(), model.getDataSystemClass());
        addSimilarityMeasureToSimilarityModel(new SMCollectionMappingImplExt(), model.getDataSystemClass());

        addSimilarityMeasureToSimilarityModel(new SMListCorrectnessImplExt(), model.getDataSystemClass());
        addSimilarityMeasureToSimilarityModel(new SMListDTWImplExt(), model.getDataSystemClass());
        addSimilarityMeasureToSimilarityModel(new SMListMappingImplExt(), model.getDataSystemClass());
        addSimilarityMeasureToSimilarityModel(new SMListSWAImplExt(), model.getDataSystemClass());

        addSimilarityMeasureToSimilarityModel(new SMStringLevenshteinImplExt(), model.getDataSystemClass());
        addSimilarityMeasureToSimilarityModel(new SMBooleanXORImpl(), model.getDataSystemClass());

        similarityModel.setDefaultSimilarityMeasure(model.getDataSystemClass(), SMObjectEqual.NAME);
    }

    /**
     * Adds a similarity measure to the similarity model.
     *
     * @param sm        a {@link SimilarityMeasureImpl}ementation
     * @param dataClass the data class for which the similarity measure will be available
     */
    private static void addSimilarityMeasureToSimilarityModel(SimilarityMeasureImpl sm, DataClass dataClass) {
        METHOD_CALL.trace("private static void restapi.service.ProCAKEService.addSimilarityMeasureToSimilarityModel" + "(SimilarityMeasureImpl sm={}, DataClass dataClass={})...", sm, dataClass);

        // puts name and SimilarityMeasure-Object in cache (should be called only once per SM)
        try {
            similarityModel.registerSimilarityMeasureTemplate(sm);

            DIAGNOSTICS.trace("restapi.service.ProCAKEService.addSimilarityMeasureToSimilarityModel(SimilarityMeasureImpl, DataClass): " + "similarityModel.registerSimilarityMeasureTemplate(sm); successful!");
        } catch (NameAlreadyExistsException ignored) {
            DIAGNOSTICS.trace("restapi.service.ProCAKEService.addSimilarityMeasureToSimilarityModel(SimilarityMeasureImpl, DataClass): " + "similarityModel.registerSimilarityMeasureTemplate(sm); failed!");
        }

        // sets the DataClass the SM can be applied to
        sm.setDataClass(dataClass);

        DIAGNOSTICS.trace("restapi.service.ProCAKEService.addSimilarityMeasureToSimilarityModel(SimilarityMeasureImpl, DataClass): " + "sm.setDataClass(dataClass); successful!");

        //adds sm to the SMs that can be applied to 'dataClass'
        similarityModel.addSimilarityMeasure(sm, sm.getSystemName());

        DIAGNOSTICS.trace("restapi.service.ProCAKEService.addSimilarityMeasureToSimilarityModel(SimilarityMeasureImpl, DataClass): " + "similarityModel.addSimilarityMeasure(sm, sm.getSystemName()); successful!");

    }


    /**
     * Reloads the traces from the database into the casebase after converting them to {@link NESTSequentialWorkflowObject}s.
     *
     * @return status message
     */
    public static String loadCasebase() {

        // Re-instantiate the case base
        casebase = ObjectPoolFactory.newObjectPool();

        try {
            // The traces are Strings starting with "<trace" and ending with "</trace>",
            // so they are actually no valid xml documents.
            // The converter however requires for the files content not only to be a valid xml document,
            // but also to be a valid xes document, the root element of which is a log tag ("<log ...>").

            Map<String, Object> log;
            String header, prefix, suffix;
            suffix = "</log>";

            Map<String, Object> trace;
            String xes;

            StringBuilder completeLog;

            for (String logID : DatabaseService.getLogIDs(false)) {

                log = DatabaseService.getLog(logID);
                header = (String) log.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__log__header);
                prefix = header.split(suffix)[0];

                String[] ids = DatabaseService.getTraceIDs(logID);

                completeLog = new StringBuilder(prefix);

                for (String traceID : ids) {
                    trace = DatabaseService.getTrace(traceID);
                    xes = (String) trace.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__xes);
                    completeLog.append(xes);
                }

                completeLog.append(suffix);

                XEStoNESTsAXparallelConverter converter = new XEStoNESTsAXparallelConverter(model, 7);
                converter.configure(false, false, null, ids);
                casebase.storeAll((Collection) converter.convert(completeLog.toString()));

            }

            return "Casebase loaded successfully!";
        } catch (SQLException e) {
            return ":(";
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
    public static List<Retrieval> retrieve(String xes, String globalSimilarityMeasure, MethodList globalMethodInvokerList, String similarityMeasureFunc, String methodInvokersFunc, String weightFunc, FilterParameters filterParameters, int numberOfResults) throws ParserConfigurationException, IOException, SAXException, ClassNotFoundException {
        METHOD_CALL.trace("public static List<Retrieval> retrieve" + "(String xes={}" + ", String globalSimilarityMeasure={}" + ", MethodList globalMethodInvokerList={}" + ", String similarityMeasureFunc={}" + ", String methodInvokersFunc={}" + ", String weightFunc={}" + ", FilterParameters filterParameters={}" + ", int numberOfResult={})...", maxSubstring(xes), maxSubstring(globalSimilarityMeasure), maxSubstring(globalMethodInvokerList), maxSubstring(similarityMeasureFunc), maxSubstring(methodInvokersFunc), maxSubstring(weightFunc), maxSubstring(filterParameters), numberOfResults);

        // - preparation of retrieval - //

        NESTSequentialWorkflowObject trace = convertQuery(xes);

        ArrayList<MethodInvoker> globalMethodInvokers = convertGlobalMethodInvokers(globalMethodInvokerList);

        SimilarityMeasureFunc localSimilarityMeasureFunc = SimilarityMeasureFunc.getDefault();
        MethodInvokersFunc localMethodInvokersFunc = MethodInvokersFunc.getDefault();
        WeightFunc localWeightFunc = WeightFunc.getDefault();

        if (similarityMeasureFunc != null)
            localSimilarityMeasureFunc = XMLtoSimilarityMeasureFuncConverter.getSimilarityMeasureFunc(similarityMeasureFunc);

        if (methodInvokersFunc != null)
            localMethodInvokersFunc = XMLtoMethodInvokersFuncConverter.getMethodInvokersFunc(methodInvokersFunc);

        if (weightFunc != null) localWeightFunc = XMLtoWeightFuncConverter.getWeightFunc(weightFunc);


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
            results.add(new Retrieval(retrievalResult.getObjectId(), retrievalResult.getSimilarity().getValue()));
        }

        METHOD_CALL.trace("restapi.service.ProCAKEService.retrieve" + "(String, String, MethodList, String, String, String, FilterParameters, int): return results={}", maxSubstring(results));
        return results;
    }

    /**
     * Converts a trace to a {@link NESTSequentialWorkflowObject}.
     *
     * @param xes XES log containing (at least) one trace
     * @return trace in form of a NESTSequentialWorkflowObject
     * @throws java.lang.IndexOutOfBoundsException if there are no traces present
     */
    private static NESTSequentialWorkflowObject convertQuery(String xes) throws java.lang.IndexOutOfBoundsException {
        XEStoNESTsAXConverter converter = new XEStoNESTsAXConverter(model);
        converter.configure(false, false, null, null);
        NESTSequentialWorkflowObject workflow = converter.convert(xes).get(0);
        workflow.setId("CONVERTED_WORKFLOW");
        return workflow;
    }

    /**
     * Converts a {@link MethodList} object to an {@link ArrayList} of {@link MethodInvoker}s.
     *
     * @param globalMethodInvokers the method list
     * @return list of MethodInvokers
     */
    private static ArrayList<MethodInvoker> convertGlobalMethodInvokers(MethodList globalMethodInvokers) throws ClassNotFoundException {
        METHOD_CALL.trace("private static ArrayList<MethodInvoker> restapi.service.ProCAKEService.convertGlobalMethodInvokers" + "(MethodList globalMethodInvokers={})...", maxSubstring(globalMethodInvokers));

        ArrayList<MethodInvoker> globalMethodInvokerList = new ArrayList<>();
        if (globalMethodInvokers == null) {
            DIAGNOSTICS.trace("service.ProCAKEService.convertGlobalMethodInvokers(MethodList): return []");
            return globalMethodInvokerList;
        }

        for (Method m : globalMethodInvokers.methods()) {
            DIAGNOSTICS.trace("service.ProCAKEService.convertGlobalMethodInvokers(MethodList): Method m={}", maxSubstring(m));

            int numOfArgs = m.valueTypes().size();

            Class<?>[] classes = new Class<?>[numOfArgs];
            Object[] objects = new Object[numOfArgs];

            for (int i = 0; i < numOfArgs; i++) {
                String valueType = m.valueTypes().get(i);
                String value = m.values().get(i);
                switch (valueType) {
                    case TYPE_NAME_STRING -> {
                        classes[i] = String.class;
                        objects[i] = value;
                    }
                    case TYPE_NAME_DOUBLE -> {
                        classes[i] = Double.class;
                        objects[i] = Double.parseDouble(value);
                    }
                    case TYPE_NAME_BOOLEAN -> {
                        classes[i] = Boolean.class;
                        objects[i] = Boolean.parseBoolean(value);
                    }
                    default -> {
                        throw new ClassNotFoundException(valueType);
                    }
                }
            }

            globalMethodInvokerList.add(new MethodInvoker(m.name(), classes, objects));
        }


        METHOD_CALL.trace("service.ProCAKEService.convertGlobalMethodInvokers(MethodList): return {}", maxSubstring(globalMethodInvokerList));
        return globalMethodInvokerList;
    }

    private static ReadableObjectPool<DataObject> getFilteredCasebase(FilterParameters parameters) {
        METHOD_CALL.trace("private static ReadableObjectPool<DataObject> restapi.service.ProCAKEService.getFilteredCasebase" + "(FilterParameters parameters={})...", parameters);

        return casebase; //todo
    }


    @Deprecated
    // for testing purposes only //ProCAKE service/controller should not provide method to get any cases from the casebase
    public static List<String[]> getCasebase() {
        List<String[]> cases = new ArrayList<>();
        for (DataObject trace : casebase.getCollection()) {
            cases.add(new String[]{trace.getId(), ((StringObject) trace).getNativeString()});
        }
        return cases;
    }

    @Deprecated
    // for testing purposes only //ProCAKE service/controller should not provide method to get any cases from the casebase
    public static WriteableObjectPool<DataObject> getActualCasebase() {
        return casebase;
    }

    @Deprecated
    // for testing purposes only //ProCAKE service/controller should not provide method to get similarity model
    public static SimilarityModel getSimilarityModel() {
        return similarityModel;
    }

}