package de.uni_trier.wi2.model;

/**
 * Record representing a result of a retrieval.
 *
 * @param id              id of trace
 * @param similarityValue similarity value of trace
 */
public record Retrieval(String id, Double similarityValue) {
}
