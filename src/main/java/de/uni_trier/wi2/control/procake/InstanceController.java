package de.uni_trier.wi2.control.procake;

import de.uni_trier.wi2.service.ProCAKEService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static de.uni_trier.wi2.RestAPILoggingUtils.METHOD_CALL;

/**
 * REST controller responsible for non-retrieval access to the ProCAKE instance.
 */
@RestController
public class InstanceController {


    public InstanceController(){
    }


    /**
     * <p>Restarts the ProCAKE instance.</p>
     * <p>The similarity model and data model are setup in here.</p>
     *
     * @return status message
     */
    @GetMapping("/procake/restart")
    public String restart(){
        METHOD_CALL.trace("public String restapi.control.procake.InstanceController.restart()...");
        String msg = ProCAKEService.setupCake();
        METHOD_CALL.trace("restapi.control.procake.InstanceController.restart(): {}", msg);
        return msg;
    }

    /**
     * <p>Reloads the database into the casebase.</p>
     * <p>The traces in the database are converted into NESTSequentialWorkflows.</p>
     * @return status message
     */
    @GetMapping("/procake/reload")
    String reload() {
        METHOD_CALL.trace("public String restapi.control.procake.InstanceController.reload()");
        String msg = ProCAKEService.loadCasebase();
        METHOD_CALL.trace("restapi.control.procake.InstanceController.reload(): {}", msg);
        return msg;
    }
}
