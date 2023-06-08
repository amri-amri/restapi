package restapi.control.procake;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.SAXException;
import restapi.control.logic.DatabaseController;
import restapi.error.TraceNotFoundException;
import restapi.model.Retrieval;
import restapi.model.RetrievalParameters;
import restapi.model.Trace;
import restapi.service.DatabaseService;
import restapi.service.ProCAKEService;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * REST controller responsible for retrieval related acces on the ProCAKE instance.
 */
@RestController
public class RetrievalController{


    RetrievalController(){}

    /**
     * <p>Performs retrieval with the trace with the given id as query.</p>
     * <p>The xes in the request body is ignored, instead, the xes belonging to the given id is taken.</p>
     * <p>The retrieval will be performed on the filtered casebase and a JSON containing the
     * results of the retrieval is returned. Only id's and similarity values are returned.</p>
     *
     * @param id id of the query trace
     * @param parameters parameters necessary for the retrieval
     * @return JSON representation of retrieval results
     * @throws IOException todo
     * @throws ParserConfigurationException todo
     * @throws SAXException todo
     */
    @GetMapping("/procake/retrieval/retrieve/{id}")
    CollectionModel<EntityModel<Retrieval>> retrieve(@PathVariable String id, @RequestBody RetrievalParameters parameters) throws IOException, ParserConfigurationException, SAXException {
        Trace t = DatabaseService.getTraceByID(id);
        if (t == null) throw new TraceNotFoundException(id);
        List<EntityModel<Retrieval>> traces = ProCAKEService.retrieve(
                t.xes(), // xes of RetrievalParameters parameters is ignored
                parameters.globalSimilarityMeasure(),
                parameters.globalMethodInvokers(),
                parameters.localSimilarityMeasureFunc(),
                parameters.localMethodInvokersFunc(),
                parameters.localWeightFunc(),
                parameters.filterParameters(),
                parameters.numberOfResults())

                .stream().map(retrieval -> EntityModel.of(
                        retrieval,        // The result of the retrieval: trace ID & similarity value
                        linkTo(methodOn(DatabaseController.class).one(retrieval.id())).withSelfRel() // a link to the trace
                ))
                .toList();

        return CollectionModel.of(
                traces,
                linkTo(methodOn(DatabaseController.class).all()).withRel("traces") // a link to all traces
                );
    }

    /**
     * <p>Performs retrieval with the trace in the request body as query.</p>
     * <p>The retrieval will be performed on the filtered casebase and a JSON containing the
     * results of the retrieval is returned. Only id's and similarity values are returned.</p>
     *
     * @param parameters parameters necessary for the retrieval
     * @return JSON representation of retrieval results
     * @throws IOException todo
     * @throws ParserConfigurationException todo
     * @throws SAXException todo
     */
    @GetMapping("/procake/retrieval/retrieve")
    CollectionModel<EntityModel<Retrieval>> retrieve(@RequestBody RetrievalParameters parameters) throws Exception {
        List<EntityModel<Retrieval>> traces = ProCAKEService.retrieve(
                parameters.xes(),
                parameters.globalSimilarityMeasure(),
                parameters.globalMethodInvokers(),
                parameters.localSimilarityMeasureFunc(),
                parameters.localMethodInvokersFunc(),
                parameters.localWeightFunc(),
                parameters.filterParameters(),
                parameters.numberOfResults())

                .stream().map(retrieval -> EntityModel.of(
                        retrieval,        // The result of the retrieval: trace ID & similarity value
                        linkTo(methodOn(DatabaseController.class).one(retrieval.id())).withSelfRel() // a link to the trace
                ))
                .toList();

        return CollectionModel.of(
                traces,
                linkTo(methodOn(DatabaseController.class).all()).withRel("traces") // a link to all traces
        );
    }

}
