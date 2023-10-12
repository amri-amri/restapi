package de.uni_trier.wi2;

import de.uni_trier.wi2.service.DatabaseService;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RESTfulAPIApplication {

	public static void main(String[] args) {
		if (args.length < 3) throw new RuntimeException("Too few arguments!\n" +
				"The first argument should be the databases' URL like \"jdbc:mysql://localhost:3306/\".\n" +
				"The second argument should the username for database access like \"root\"\n" +
				"The third argument should be the password for database access.");

		String databaseUrl = args[0];
		String databaseUsername = args[1];
		String databasePassword = args[2];

		DatabaseService.setUrlUsernamePassword(databaseUrl, databaseUsername, databasePassword);

		SpringApplication.run(RESTfulAPIApplication.class, args);
		LoggerFactory.getLogger(RESTfulAPIApplication.class).info("Development server UI: http://localhost:8080/swagger-ui/index.html");
	}

}
