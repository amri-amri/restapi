package de.uni_trier.wi2.config;

import de.uni_trier.wi2.*;
import de.uni_trier.wi2.service.*;
import org.slf4j.*;
import org.springframework.boot.*;
import org.springframework.context.annotation.*;

/**
 * Configuration class to automatically connect to the database when the {@link RESTAPI} is started.
 */
@Configuration
class ConnectToDatabase {

    private static final Logger log = LoggerFactory.getLogger(ConnectToDatabase.class);


    @Bean
    CommandLineRunner connect() {
        return args -> {
            String databaseUrl = args[0];
            String databaseUsername = args[1];
            String databasePassword = args[2];

            DatabaseService.setUrlUsernamePassword(databaseUrl, databaseUsername, databasePassword);

            log.info(DatabaseService.connectToDatabase());
        };
    }
}