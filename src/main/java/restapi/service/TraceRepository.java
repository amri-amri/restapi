package restapi.service;


import restapi.model.Trace;

import java.util.List;

// Just a placeholder interface for testing...
public interface TraceRepository {
    // Connection to the DataBase containing all the XES traces

    String save(String id, String xes);

    String save(String xes);

    void loadDataBase();

    Trace findById(String id);
    List<Trace> findAll();
}
