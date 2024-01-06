package de.uni_trier.wi2.control.logic;

import de.uni_trier.wi2.service.DatabaseService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static de.uni_trier.wi2.LoggingUtils.maxSubstring;
import static de.uni_trier.wi2.LoggingUtils.METHOD_CALL;
import static de.uni_trier.wi2.LoggingUtils.DIAGNOSTICS;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * REST controller responsible for database access.
 */
@RestController
public class DatabaseController {

	DatabaseController() {
	}

	@GetMapping("/log")
	@ResponseBody
	public List<Map<String, Object>> getLog() throws SQLException {
		METHOD_CALL.info("public List<Map<String, Object>> restapi.control.logic.DatabaseController.getLog()...");

		List<Map<String, Object>> logs = new ArrayList<>();
		Map<String, Object> log;
		for (String logID : DatabaseService.getLogIDs(true)) {
			log = getLog(logID);
			logs.add(log);
			DIAGNOSTICS.trace("restapi.control.logic.DatabaseController.getLog(): Added log to list of logs: {}", log);
		}


		METHOD_CALL.info("restapi.control.logic.DatabaseController.getLog(): return list of logs: {}", logs);
		return logs;
	}

	@GetMapping("/log/{logID}")
	@ResponseBody
	public Map<String, Object> getLog(@PathVariable @NotNull String logID) throws SQLException {
		METHOD_CALL.info("public Map<String, Object> restapi.control.logic.DatabaseController.getLog" +
				"(@PathVariable @NotNull String logID={})...", logID);

		// log id and links
		Map<String, Object> log;
		try{
			log = DatabaseService.getLog(logID);
		} catch (SQLException e) {
			DIAGNOSTICS.trace("restapi.control.logic.DatabaseController.getLog(String): Failed to get log belonging to logID {}", logID);
			METHOD_CALL.info("restapi.control.logic.DatabaseController.getLog(String): " +
				"throw new ResponseStatusException(HttpStatus.NOT_FOUND, {});", maxSubstring(e.getMessage()));
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}
		DIAGNOSTICS.trace("restapi.control.logic.DatabaseController.getLog(String): Got log belonging to logID {}.", logID);

		log.put("links", new Link[]{
				linkTo(methodOn(DatabaseController.class).getLog(logID)).withSelfRel(),
				linkTo(methodOn(DatabaseController.class).getLog()).withRel("all")
		});

		DIAGNOSTICS.trace("restapi.control.logic.DatabaseController.getLog(String): Added links to log: {}", log.get("links"));

		// traces that belong to log
		String[] traceIDs = DatabaseService.getTraceIDs(logID);

		DIAGNOSTICS.trace("restapi.control.logic.DatabaseController.getLog(String): Traces that belong to log: {}",
				maxSubstring(Arrays.toString(traceIDs)));

		List<Map<String, Object>> traces = new ArrayList<>();
		Map<String, Object> trace;
		for (String traceID : traceIDs) {
			trace = new HashMap<>();
			trace.put(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__traceID, traceID);
			trace.put("link", linkTo(methodOn(DatabaseController.class).getTrace(traceID)).withSelfRel());
			traces.add(trace);

			DIAGNOSTICS.trace("restapi.control.logic.DatabaseController.getLog(String): Added trace to list of traces: {}",
					maxSubstring(trace.toString()));
		}

		log.put("traces", traces);

		DIAGNOSTICS.trace("restapi.control.logic.DatabaseController.getLog(String): Added list of traces to log: {}",
				maxSubstring(traces.toString()));

		//metadata
		log.put(DatabaseService.DATABASE_NAMES.TABLENAME__metadata, DatabaseService.getLogMetadata(logID));

		DIAGNOSTICS.trace("restapi.control.logic.DatabaseController.getLog(String): Added metadata to log: {}",
				maxSubstring(log.get(DatabaseService.DATABASE_NAMES.TABLENAME__metadata).toString()));

		METHOD_CALL.info("restapi.control.logic.DatabaseController.getLog(String): return log: {}",
				maxSubstring(log.toString()));
		return log;
	}

	@GetMapping("/trace/{traceID}")
	@ResponseBody
	public Map<String, Object> getTrace(@PathVariable @NotNull String traceID) throws SQLException {
		METHOD_CALL.info("public Map<String, Object> restapi.control.logic.DatabaseController.getTrace" +
				"(@PathVariable @NotNull String traceID={})...", traceID);

		Map<String, Object> trace;
		try {
			trace = DatabaseService.getTrace(traceID);
		} catch(SQLException e){
			DIAGNOSTICS.trace("restapi.control.logic.DatabaseController.getTrace(String): Failed to get trace belonging to traceID {}", traceID);
			METHOD_CALL.info("restapi.control.logic.DatabaseController.getTrace(String): " +
					"throw new ResponseStatusException(HttpStatus.NOT_FOUND, {});", maxSubstring(e.getMessage()));
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}
		DIAGNOSTICS.trace("restapi.control.logic.DatabaseController.getTrace(String): Got trace belonging to traceID {}", traceID);

		Map<String, Object> logInfo = new HashMap<>();
		String logID = (String) trace.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__logID);
		logInfo.put(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__logID, logID);
		logInfo.put("href", linkTo(methodOn(DatabaseController.class).getLog(logID)).withSelfRel());

		Map<String, Object> response = new HashMap<>();
		response.put(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__traceID, traceID);
		response.put(DatabaseService.DATABASE_NAMES.TABLENAME__log, logInfo);
		response.put(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__xes, trace.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__xes));
		response.put(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__removed, trace.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__removed));
		response.put("href", linkTo(methodOn(DatabaseController.class).getTrace(traceID)).withSelfRel());

		//metadata
		response.put(DatabaseService.DATABASE_NAMES.TABLENAME__metadata, DatabaseService.getTraceMetadata(traceID));

		METHOD_CALL.info("restapi.control.logic.DatabaseController.getTrace(String): return trace: {}",
				maxSubstring(response.toString()));
		return response;
	}

	@PostMapping("/log")
	@ResponseBody
	public Map<String, Object> postLog(@RequestBody @NotNull String xes) throws SQLException, IOException, SAXException {
		METHOD_CALL.info("public Map<String, Object> restapi.control.logic.DatabaseController.postLog" +
				"(@RequestBody @NotNull String xes={})...", maxSubstring(xes));
		DatabaseService.startTransaction();
		DatabaseService.savepoint("putLog");
		try {

			// split log and put header and traces in database
			String[] ids = DatabaseService.putLog(xes);

			// assign metadata (date)
			String date = Instant.now().toString();
			String logID = ids[0];
			DatabaseService.putLogMetadata(logID, METADATATYPE_NAMES.DATE_OF_UPLOAD, date);

			DIAGNOSTICS.trace("restapi.control.logic.DatabaseController.postLog(String): Put log metadata: date = {}", date);

			for (int i = 1; i < ids.length; i++) {
				DatabaseService.putTraceMetadata(ids[i], METADATATYPE_NAMES.DATE_OF_UPLOAD, date);

				DIAGNOSTICS.trace("restapi.control.logic.DatabaseController.postLog(String): Put trace metadata of trace {} (same metadata as log metadata)", ids[i]);
			}

			// get log information
			Map<String, Object> logInfo = getLog(ids[0]);

			DatabaseService.commit();

			DIAGNOSTICS.trace("restapi.control.logic.DatabaseController.postLog(String): Posted Log.");

			METHOD_CALL.info("restapi.control.logic.DatabaseController.postLog(String): return logInfo: {}",
					maxSubstring(logInfo.toString()));
			// return log information
			return logInfo;

		} catch (Exception e) {
			DatabaseService.rollbackTo("putLog");
			DatabaseService.commit();

			METHOD_CALL.info("restapi.control.logic.DatabaseController.postLog(String): " +
				"throw new ResponseStatusException(HttpStatus.BAD_REQUEST, {});", maxSubstring(e.getMessage()));
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

	@DeleteMapping("/log/{logID}")
	@ResponseBody
	public Map<String, Object> deleteLog(@PathVariable @NotNull String logID) throws SQLException {
		METHOD_CALL.info("public Map<String, Object> restapi.control.logic.DatabaseController.deleteLog" +
				"(@PathVariable @NotNull String logID={})...", logID);
		DatabaseService.startTransaction();
		DatabaseService.savepoint("putLog");
		try {

			DatabaseService.removeLog(logID);

			DIAGNOSTICS.trace("restapi.control.logic.DatabaseController.deleteLog(String): Log was removed.");
			return getLog(logID);

		} catch (Exception e) {
			DatabaseService.rollbackTo("putLog");
			DatabaseService.commit();

			DIAGNOSTICS.trace("restapi.control.logic.DatabaseController.deleteLog(String): Log was not removed.");
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}
	}


	private final class METADATATYPE_NAMES {
		private static final String DATE_OF_UPLOAD = "dateOfUpload";
		private METADATATYPE_NAMES() {
		}
	}

}
