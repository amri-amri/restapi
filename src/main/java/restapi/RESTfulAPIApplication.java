package restapi;

import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RESTfulAPIApplication {

	public static void main(String[] args) {

		SpringApplication.run(RESTfulAPIApplication.class, args);
		LoggerFactory.getLogger(RESTfulAPIApplication.class).info("Development server UI: http://localhost:8080/swagger-ui/index.html");
	}

}
