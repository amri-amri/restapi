package de.uni_trier.wi2.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static de.uni_trier.wi2.LoggingUtils.maxSubstring;

public class IOUtils {

    public static final Logger METHOD_CALL = LoggerFactory.getLogger("method-call");

    public static String getResourceAsString(String nameWithoutPackage) throws IOException {
        METHOD_CALL.info("public static String service.IOUtils.getResourceAsString(String nameWithoutPackage={})...",
                nameWithoutPackage);

        final String packageName = "/de/uni_trier/wi2";
        String nameWithPackage = nameWithoutPackage;
        if (nameWithPackage.charAt(0) != '/') nameWithPackage = "/" + nameWithPackage;
        nameWithPackage = packageName + nameWithPackage;
        InputStream is = IOUtils.class.getResourceAsStream(nameWithPackage);
        String out = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        METHOD_CALL.info("service.IOUtils.getResourceAsString(String): return {}", maxSubstring(out));
        return out;
    }
}
