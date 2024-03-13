package de.uni_trier.wi2.model;

import java.util.List;

/**
 * A record representing a method with one parameter.
 * @param name name of the method
 * @param valueTypes parameter type
 * @param values values of parameter
 */
public record Method(String name, List<String> valueTypes, List<String> values) { }
