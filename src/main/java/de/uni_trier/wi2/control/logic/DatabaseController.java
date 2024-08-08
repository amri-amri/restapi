package de.uni_trier.wi2.control.logic;

import de.uni_trier.wi2.service.*;
import org.jetbrains.annotations.*;
import org.springframework.hateoas.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.*;

import java.io.*;
import java.sql.*;
import java.time.*;
import java.util.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

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
        try {
            while (DatabaseService.connection == null) Thread.sleep(100); //todo delete after eval
        } catch (Exception ignored) {}


        List<Map<String, Object>> logs = new ArrayList<>();
        Map<String, Object> log;
        for (String logID : DatabaseService.getLogIDs(true)) {
            log = getLog(logID);
            logs.add(log);

        }


        return logs;
    }

    @GetMapping("/log/{logID}")
    @ResponseBody
    public Map<String, Object> getLog(@PathVariable @NotNull String logID) throws SQLException {


        // log id and links
        Map<String, Object> log;
        try {
            log = DatabaseService.getLog(logID);
        } catch (SQLException e) {

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }


        log.put("links", new Link[]{
                linkTo(methodOn(DatabaseController.class).getLog(logID)).withSelfRel(),
                linkTo(methodOn(DatabaseController.class).getLog()).withRel("all")
        });


        // traces that belong to log
        String[] traceIDs = DatabaseService.getTraceIDs(logID);


        List<Map<String, Object>> traces = new ArrayList<>();
        Map<String, Object> trace;
        for (String traceID : traceIDs) {
            trace = new HashMap<>();
            trace.put(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__traceID, traceID);
            trace.put("links", new Link[]{linkTo(methodOn(DatabaseController.class).getTrace(traceID)).withSelfRel()});
            traces.add(trace);


        }

        log.put("traces", traces);


        //metadata
        log.put(DatabaseService.DATABASE_NAMES.TABLENAME__metadata, DatabaseService.getLogMetadata(logID));


        return log;
    }

    @GetMapping("/trace/{traceID}")
    @ResponseBody
    public Map<String, Object> getTrace(@PathVariable @NotNull String traceID) throws SQLException {


        Map<String, Object> trace;
        try {
            trace = DatabaseService.getTrace(traceID);
        } catch (SQLException e) {

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }


        Map<String, Object> logInfo = new HashMap<>();
        String logID = (String) trace.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__logID);
        logInfo.put(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__logID, logID);
        logInfo.put("links", new Link[]{linkTo(methodOn(DatabaseController.class).getLog(logID)).withSelfRel()});

        Map<String, Object> response = new HashMap<>();
        response.put(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__traceID, traceID);
        response.put(DatabaseService.DATABASE_NAMES.TABLENAME__log, logInfo);
        response.put(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__xes, trace.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__xes));
        response.put(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__removed, trace.get(DatabaseService.DATABASE_NAMES.COLUMNNAME__trace__removed));
        response.put("links", new Link[]{linkTo(methodOn(DatabaseController.class).getTrace(traceID)).withSelfRel()});

        //metadata
        response.put(DatabaseService.DATABASE_NAMES.TABLENAME__metadata, DatabaseService.getTraceMetadata(traceID));


        return response;
    }

    @PostMapping("/log")
    @ResponseBody
    public Map<String, Object> postLog(@RequestBody @NotNull String xes) throws SQLException, IOException {

        DatabaseService.startTransaction();
        DatabaseService.savepoint("putLog");
        try {

            // split log and put header and traces in database
            String[] ids = DatabaseService.putLog(xes);

            // assign metadata (date)
            String date = Instant.now().toString();
            String logID = ids[0];
            DatabaseService.putLogMetadata(logID, METADATATYPE_NAMES.DATE_OF_UPLOAD, date);


            for (int i = 1; i < ids.length; i++) {
                DatabaseService.putTraceMetadata(ids[i], METADATATYPE_NAMES.DATE_OF_UPLOAD, date);
            }

            // get log information
            Map<String, Object> logInfo = getLog(ids[0]);

            DatabaseService.commit();

            // return log information
            return logInfo;

        } catch (Exception e) {
            //File file = new File("target/logs/eval.log");
            //file.createNewFile();
            //FileWriter fw = new FileWriter(file, true);
            //fw.write(e.getMessage());
            //fw.write("\n");
            //fw.close();
            DatabaseService.rollbackTo("putLog");
            DatabaseService.commit();

            //File f = new File("abcdefg12345.txt");
            //f.createNewFile();
            //PrintWriter pw = new PrintWriter(f);
            //pw.write(e.getMessage());
            //pw.close();

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/log/{logID}")
    @ResponseBody
    public Map<String, Object> deleteLog(@PathVariable @NotNull String logID) throws SQLException {

        DatabaseService.startTransaction();
        DatabaseService.savepoint("putLog");
        try {

            DatabaseService.removeLog(logID);


            return getLog(logID);

        } catch (Exception e) {
            DatabaseService.rollbackTo("putLog");
            DatabaseService.commit();


            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }


    private final class METADATATYPE_NAMES {
        private static final String DATE_OF_UPLOAD = "dateOfUpload";

        private METADATATYPE_NAMES() {
        }
    }

}
