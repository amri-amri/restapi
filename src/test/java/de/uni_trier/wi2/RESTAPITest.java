package de.uni_trier.wi2;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.*;

@SpringBootTest(
        classes = RESTAPI.class,
        args = {
                "jdbc:mysql://localhost:3306/onkocase_test",
                "root",
                "pw1234"
        }
)
class RESTAPITest {


    @Test
    void contextLoads() {

    }


}
