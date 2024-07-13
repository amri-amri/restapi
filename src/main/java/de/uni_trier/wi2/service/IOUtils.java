package de.uni_trier.wi2.service;


import java.io.*;
import java.nio.charset.*;
import java.util.*;


public class IOUtils {

    public static String getResourceAsString(String nameWithoutPackage) throws IOException {

        final String packageName = "/de/uni_trier/wi2";
        String nameWithPackage = nameWithoutPackage;
        if (nameWithPackage.charAt(0) != '/') nameWithPackage = "/" + nameWithPackage;
        nameWithPackage = packageName + nameWithPackage;
        InputStream is = IOUtils.class.getResourceAsStream(nameWithPackage);
        String out = new String(is.readAllBytes(), StandardCharsets.UTF_8);


        return out;
    }

    /*
    The Axe class implements functionality to split (event) logs, thus the name.
     */
    @Deprecated
    public static class Axe {

        private boolean inTrace;
        private boolean inTag;
        private boolean closingTag;
        private boolean lastCharWasWhitespace;
        private boolean inQuote;
        private boolean doubleQuote;

        private StringBuilder log;
        private StringBuilder trace;
        private StringBuilder current;
        private StringBuilder tagName;

        private char chr;

        private void initialize() {
            inTrace = false;
            inTag = false;
            closingTag = false;
            lastCharWasWhitespace = false;
            inQuote = false;
            doubleQuote = false;

            log = new StringBuilder();
            trace = new StringBuilder();
            current = new StringBuilder();
            tagName = new StringBuilder();
        }

        public String[] split(String xes) {
            initialize();

            Collection<String> traces = new ArrayList<>();

            for (int i = 0; i < xes.length(); i++) {
                chr = xes.charAt(i);
                if (!inQuote && Character.isWhitespace(chr) && lastCharWasWhitespace) continue;
                if (!inTag && Character.isWhitespace(chr)) continue;
                if (!inQuote && Character.isWhitespace(chr)) {
                    lastCharWasWhitespace = true;
                    chr = ' ';
                }
                if (!Character.isWhitespace(chr)) {
                    lastCharWasWhitespace = false;
                }


                switch (chr) {
                    case '<':   // start tag
                        inTag = true;
                        if (inTrace) trace.append(current);
                        else log.append(current);
                        current.setLength(0);
                        current.append(chr);
                        break;
                    case '>':   // end tag
                        inTag = false;
                        current.append(chr);
                        if (closingTag && tagName.toString().strip().startsWith("trace")) {//todo:magic string
                            inTrace = false;
                            trace.append(current);
                            traces.add(trace.toString());
                            trace.setLength(0);
                        } else if (closingTag && !tagName.toString().strip().startsWith("trace")) {
                            if (inTrace) trace.append(current);
                            else log.append(current);
                        } else if (!closingTag && tagName.toString().strip().startsWith("trace")) {
                            inTrace = true;
                            trace.append(current);
                        } else { //if !closingTag && !tagName.toString().strip().startsWith("trace"))
                            if (tagName.toString().strip().startsWith("log")) {
                                inTrace = false;
                                log.append(current);
                            } else {
                                if (inTrace) trace.append(current);
                                else log.append(current);
                            }
                        }
                        current.setLength(0);
                        tagName.setLength(0);
                        closingTag = false;
                        break;
                    case '/':   // slash
                        closingTag = !inQuote && inTag;
                        current.append(chr);
                        break;
                    case '\'':  // single quote
                        if (!inQuote) {
                            inQuote = true;
                            doubleQuote = false;
                        } else if (!doubleQuote) {
                            inQuote = false;
                        }
                        current.append(chr);
                        break;
                    case '\"':  // double quote
                        if (!inQuote) {
                            inQuote = true;
                            doubleQuote = true;
                        } else if (doubleQuote) {
                            inQuote = false;
                        }
                        current.append(chr);
                        break;
                    case '?':
                        if (!inQuote && inTag) closingTag = true;
                    default:
                        current.append(chr);
                        if (inTag) tagName.append(chr);

                }
            }

            traces.add(log.toString());
            return traces.toArray(String[]::new);
        }
    }
}
