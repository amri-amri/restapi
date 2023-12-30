package de.uni_trier.wi2;

import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RESTAPI {

	public static void main(String[] args) {
		if (args.length < 3) throw new RuntimeException("Too few arguments!\n" +
				"The first argument should be the databases' URL like \"jdbc:mysql://localhost:3306/onkocase\"\n" +
				"The second argument should the username for database access like \"root\"\n" +
				"The third argument should be the password for database access.");

		int portID = 8080;
		if (args.length > 3) {
			String arg = args[3];
			if (arg.substring(0,14).equals("--server.port=")) {
				portID = Integer.parseInt(arg.substring(14));
			}
		}

		SpringApplication.run(RESTAPI.class, args);
		LoggerFactory.getLogger(RESTAPI.class).info("API UI: http://localhost:" + portID + "/swagger-ui/index.html");
	}

}
