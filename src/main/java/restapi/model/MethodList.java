package restapi.model;

import java.util.List;

/**
 * A record representing a list of {@link Method}s.
 * @param methods list of methods
 */
public record MethodList(List<Method> methods) { }
