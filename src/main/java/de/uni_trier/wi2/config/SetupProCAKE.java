package de.uni_trier.wi2.config;

import de.uni_trier.wi2.*;
import de.uni_trier.wi2.service.*;
import org.slf4j.*;
import org.springframework.boot.*;
import org.springframework.context.annotation.*;

/**
 * Configuration class to automatically setup ProCAKE when the {@link RESTAPI} is started.
 */
@Configuration
class SetupProCAKE {

    private static final Logger log = LoggerFactory.getLogger(SetupProCAKE.class);

    @Bean
    CommandLineRunner setupInstance() {
        return args -> {
            log.info(ProCAKEService.setupCake());
            log.info(ProCAKEService.loadCasebase());
        };
    }
}