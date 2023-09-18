package restapi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import restapi.service.ProCAKEService;

/**
 * Configuration class to automatically setup ProCAKE when the {@link restapi.RESTfulAPIApplication} is started.
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