package de.uni_trier.wi2.control.procake;

import de.uni_trier.wi2.model.RetrievalParameters;
import de.uni_trier.wi2.service.DatabaseService;
import de.uni_trier.wi2.service.ProCAKEService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static de.uni_trier.wi2.RestAPILoggingUtils.maxSubstring;
import static de.uni_trier.wi2.RestAPILoggingUtils.METHOD_CALL;
import static de.uni_trier.wi2.RestAPILoggingUtils.DIAGNOSTICS;

/**
 * REST controller responsible for retrieval related acces on the ProCAKE instance.
 */
@RestController
public class RetrievalController {


    RetrievalController() {
    }

    /**
     * <p>Performs retrieval with the trace with the given traceID as query.</p>
     * <p>The xes in the request body is ignored, instead, the xes belonging to the given traceID is taken.</p>
     * <p>The retrieval will be performed on the filtered casebase and a JSON containing the
     * results of the retrieval is returned. Only traceID's and similarity value are returned.</p>
     *
     * @param traceID    traceID of the query trace
     * @param parameters parameters necessary for the retrieval
     * @return JSON representation of retrieval results
     * @throws IOException                  todo
     * @throws ParserConfigurationException todo
     * @throws SAXException                 todo
     */
    @PutMapping(value = "/retrieval/{traceID}")
    Map<String, Object>[] retrieve(@PathVariable String traceID, @RequestBody RetrievalParameters parameters) throws Exception {
        METHOD_CALL.trace("Map<String, Object>[] restapi.control.procake.RetrievalController.retrieve" +
                "(@PathVariable String traceID={}, @RequestBody RetrievalParameters parameters={})"
                , traceID, maxSubstring(parameters));

        Map<String, Object> t;
        try {
            t = DatabaseService.getTrace(traceID);
        } catch (SQLException e) {
            DIAGNOSTICS.trace("restapi.control.procake.RetrievalController.retrieve(String, RetrievalParameters): Could not find trace belonging to traceID {}", traceID);
            METHOD_CALL.trace("ENTER: Map<String, Object>[] restapi.control.procake.RetrievalController.retrieve(@PathVariable String traceID, @RequestBody RetrievalParameters parameters)");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }


        DIAGNOSTICS.trace("restapi.control.procake.RetrievalController.retrieve" +
                "(String, RetrievalParameters): Trace belonging to traceID is {}.", maxSubstring(t));

        Map<String, Object> l =
                DatabaseService.getLog((String) t.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__logID));

        DIAGNOSTICS.trace("restapi.control.procake.RetrievalController.retrieve" +
                "(String, RetrievalParameters): Log belonging to traceID is {}.", maxSubstring(l));

        // Since the trace belongs to a log that means the log is not empty and thus the root element (log) is not
        //  self-closing which means there is a String "</log>" somewhere in the header
        String[] header = ((String) l.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__log__header)).split("</log>");
        assert (header.length > 0);
        String xes = header[0] + t.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__xes) + "</log>";

        DIAGNOSTICS.trace("restapi.control.procake.RetrievalController.retrieve" +
                "(String, RetrievalParameters): XES with log header is {}", maxSubstring(xes));

        RetrievalParameters parameters1 = new RetrievalParameters(
                xes,
                parameters.globalSimilarityMeasure(),
                parameters.globalMethodInvokers(),
                parameters.localSimilarityMeasureFunc(),
                parameters.localMethodInvokersFunc(),
                parameters.localWeightFunc(),
                parameters.filterParameters(),
                parameters.numberOfResults()
        );

        Map<String, Object>[] retrieval = retrieve(parameters1);

        METHOD_CALL.trace("restapi.control.procake.RetrievalController.retrieve" +
                "(String, RetrievalParameters): return retrieval: {}", maxSubstring(retrieval));
        return retrieval;
    }

    /**
     * <p>Performs retrieval with the trace in the request body as query.</p>
     * <p>The retrieval will be performed on the filtered casebase and a JSON containing the
     * results of the retrieval is returned. Only id's and similarity value are returned.</p>
     *
     * @param parameters parameters necessary for the retrieval
     * @return JSON representation of retrieval results
     * @throws IOException                  todo
     * @throws ParserConfigurationException todo
     * @throws SAXException                 todo
     */
    @PutMapping(value = "/retrieval")
    Map<String, Object>[] retrieve( @RequestBody RetrievalParameters parameters) throws Exception {
        METHOD_CALL.trace("Map<String, Object>[] restapi.control.procake.RetrievalController.retrieve" +
                "(@RequestBody RetrievalParameters parameters={})", maxSubstring(parameters));
        Map[] traces;
        try {
            traces = ProCAKEService.retrieve(
                            parameters.xes(), // xes of RetrievalParameters is ignored
                            parameters.globalSimilarityMeasure(),
                            parameters.globalMethodInvokers(),
                            parameters.localSimilarityMeasureFunc(),
                            parameters.localMethodInvokersFunc(),
                            parameters.localWeightFunc(),
                            parameters.filterParameters(),
                            parameters.numberOfResults())

                    .stream().map(retrieval -> {
                        Map<String, Object> out = new HashMap();
                        out.put(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__traceID, retrieval.id());
                        out.put("similarity", retrieval.similarityValue());//todo: magic String = bad!
                        return out;
                    }).toArray(Map[]::new);
        } catch (Exception e) {

            METHOD_CALL.trace("restapi.control.procake.RetrievalController.retrieve(RetrievalParameters): " +
                    "throw new ResponseStatusException(HttpStatus.BAD_REQUEST, {});", maxSubstring(e.getMessage()));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }


        METHOD_CALL.trace("restapi.control.procake.RetrievalController.retrieve(RetrievalParameters): " +
                "return traces: {}", maxSubstring(traces));
        return (Map<String, Object>[]) traces;
    }

}
