package restapi.model;

/**
 * Record important for communication to this API.
 * @param xes an XES trace
 * @param globalSimilarityMeasure similarity measure to be used globally
 * @param globalMethodInvokers list of methods to be invoked on the global similarity measure
 * @param localSimilarityMeasureFunc XML representation of a {@link utils.SimilarityMeasureFunc}
 * @param localMethodInvokersFunc XML representation of a {@link utils.MethodInvokersFunc}
 * @param localWeightFunc XML representation of a {@link utils.WeightFunc}
 * @param filterParameters parameters used to filter the casebase
 * @param numberOfResults number of results to be retrieved
 */
public record RetrievalParameters(
        String xes,
        String globalSimilarityMeasure,
        MethodList globalMethodInvokers,
        String localSimilarityMeasureFunc,
        String localMethodInvokersFunc,
        String localWeightFunc,
        FilterParameters filterParameters,
        int numberOfResults
) { }
