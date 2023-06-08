package restapi.control.logic;

import org.apache.commons.lang.NullArgumentException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.*;
import restapi.error.TraceNotFoundException;
import restapi.error.XESnotValidException;
import restapi.model.DatabaseUploadResponse;
import restapi.model.FilterParameters;
import restapi.model.Trace;
import restapi.model.assembling.TraceModelAssembler;
import restapi.service.DatabaseService;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * REST controller responsible for access to the database.
 */
@RestController
public class DatabaseController {

    private final TraceModelAssembler assembler;

    DatabaseController(TraceModelAssembler assembler){
        this.assembler = assembler;
    }

    /**
     * Returns the trace with the given id, if it exists.
     *
     * @param id the id of the trace
     * @return  a JSON containing the id, the XES String, a link to the trace and a link to all traces
     */
    @NotNull
    @GetMapping("/database/{id}")
    public EntityModel<Trace> one(@PathVariable @NotNull String id){
        Trace t = DatabaseService.getTraceByID(id);
        if (t == null) throw new TraceNotFoundException(id);

        return assembler.toModel(t);
    }

    /**
     * Returns all traces in the database.
     *
     * @return JSON containing:<ul>
     *     <li>a list of traces (id, XES & link)</li>
     *     <li>a link to all traces</li>
     * </ul>
     */
    @NotNull
    @GetMapping("/database")
    public CollectionModel<EntityModel<Trace>> all(){
        List<EntityModel<Trace>> traces = DatabaseService.getAllTraces().stream() //
                .map(assembler::toModel) //
                .toList();

        return CollectionModel.of(traces, linkTo(methodOn(DatabaseController.class).all()).withSelfRel());
    }

    /**
     * Returns a filtered version of the current casebase.
     *
     * @param filterParameters  parameters used to define the filtering process
     * @return  filtered casebase
     */
    @NotNull
    @GetMapping("/database/filter")
    public CollectionModel<EntityModel<Trace>> filtered(@RequestBody @Nullable FilterParameters filterParameters){
        //todo
        return all();
    }

    /**
     * <p>Expects a String being a valid XES document containing a (perhaps empty) log of traces.</p>
     * <p>For every trace element the following happens:
     * <ol>
     *     <li>the trace is validated against an XSD schema</li>
     *     <li>if the trace is valid, it is put in the database and a list of 'successful' traces,
     *     if it is not valid, it is put in a list of 'failed' traces along with a message</li>
     * </ol></p>
     * <p>
     * The {@link DatabaseUploadResponse} contains:
     * <ol>
     *     <li>the successful traces along with their new id's and http links</li>
     *     <li>the failed traces along with a message</li>
     * </ol>
     * </p>
     *
     * @param log the XES document containing the log
     * @return a JSON representation of above-mentioned DatabaseUploadResponse
     */
    @NotNull
    @PutMapping("/database")
    public EntityModel<DatabaseUploadResponse> put(@RequestBody @Nullable String log){
        List<EntityModel<Trace>> traceList = new ArrayList<>();
        List<DatabaseUploadResponse.DatabaseFailedUploadResponse> failed = new ArrayList<>();

        if (log != null) {
            // Split traces
            String[] traces = log.split("<trace");
            for (int i = 0; i < traces.length; i++) traces[i] = "<trace" + traces[i].split("</trace>")[0] + "</trace>";


            // Put traces in database
            String id;
            for (String trace : traces) {
                if (trace.contains("<?xml")) continue;
                try {
                    id = DatabaseService.put(trace, DatabaseService.XSD_SCHEMATA.IEEE_APRIL_15_2020);
                    traceList.add(assembler.toModel(new Trace(id, trace)));
                } catch (XESnotValidException e) {
                    failed.add(new DatabaseUploadResponse.DatabaseFailedUploadResponse(trace, e.getMessage()));
                }
            }
        }


        return EntityModel.of(new DatabaseUploadResponse(traceList, failed));
    }

    /**
     * <p>Instead of a valid XES document containing a whole log, only the String representation
     * of a trace element in an XES file is required. The String should start with "&lt;trace" and
     * end with "&lt;/trace&gt;". If it was part of an otherwise valid XES document, the document should
     * still be valid.</p>
     * <p>If the given id exists in the database, the corresponding XES String is overridden.
     * If the id does not exist, a new trace with given id is created.</p>
     * <p>The first thing this method does is to check if the given trace is valid (see above).
     * If the trace is not valid, an {@link XESnotValidException} is thrown and the method returns, so
     * no changes will have been made to the database.</p>
     *
     * @param id the id of the trace
     * @param trace the XES element representing the trace
     * @return a JSON representation of the new Trace
     * @throws NullArgumentException if trace is null
     * @throws XESnotValidException if the trace is not valid
     */
    @NotNull
    @PutMapping("/database/{id}")
    public EntityModel<Trace> put(@PathVariable @NotNull String id, @RequestBody @Nullable String trace) throws NullArgumentException, XESnotValidException{
        if (trace == null) throw new NullArgumentException("trace");

        DatabaseService.XSD_SCHEMATA.XSD xsd = DatabaseService.XSD_TO_BE_USED;
        if (!DatabaseService.validateTraceAgainstSchema(trace, xsd)) throw new XESnotValidException(xsd.PREFIX + trace + xsd.SUFFIX, xsd);

        DatabaseService.put(id, trace, xsd);

        return assembler.toModel(DatabaseService.getTraceByID(id));
    }
}
