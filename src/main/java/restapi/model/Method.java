package restapi.model;

/**
 * A record representing a method with one parameter.
 * @param name name of the method
 * @param valueType parameter type
 * @param value value of parameter
 */
public record Method(String name, String valueType, String value) { }
