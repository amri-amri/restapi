package de.uni_trier.wi2.service;


import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static de.uni_trier.wi2.RestAPILoggingUtils.METHOD_CALL;
import static de.uni_trier.wi2.RestAPILoggingUtils.maxSubstring;

public class IOUtils {

    public static String getResourceAsString(String nameWithoutPackage) throws IOException {
        METHOD_CALL.info("public static String restapi.service.IOUtils.getResourceAsString(String nameWithoutPackage={})...",
                nameWithoutPackage);

        final String packageName = "/de/uni_trier/wi2";
        String nameWithPackage = nameWithoutPackage;
        if (nameWithPackage.charAt(0) != '/') nameWithPackage = "/" + nameWithPackage;
        nameWithPackage = packageName + nameWithPackage;
        InputStream is = IOUtils.class.getResourceAsStream(nameWithPackage);
        String out = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        METHOD_CALL.info("restapi.service.IOUtils.getResourceAsString(String): return {}", maxSubstring(out));
        return out;
    }
}
