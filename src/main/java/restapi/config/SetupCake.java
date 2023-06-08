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
class SetupCake {

    private static final Logger log = LoggerFactory.getLogger(SetupCake.class);

    @Bean
    CommandLineRunner setupProCAKE() {
        return args -> log.info(ProCAKEService.setupCake());
    }
}