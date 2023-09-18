package restapi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import restapi.service.DatabaseService;

/**
 * Configuration class to automatically connect to the database when the {@link restapi.RESTfulAPIApplication} is started.
 */
@Configuration
class ConnectToDatabase {

    private static final Logger log = LoggerFactory.getLogger(ConnectToDatabase.class);


    @Bean
    CommandLineRunner connect() {
        return args -> log.info(DatabaseService.connectToDatabase("onkocase"));
    }
}