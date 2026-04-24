package eu.bmdproject.dataspace.model;

import eu.bmdproject.dataspace.dao.ROCrateDao;

/**
 * RDF formats supported by BMD Dataspace API.
 */
public enum RDFFormat {
  JSONLD, TURTLE, RDFXML;

  public static RDFFormat parse(String value) {
    return switch (value) {
      case "turtle", "ttl", "text/turtle" -> TURTLE;
      case "jsonld", "json-ld", "application/ld+json" -> JSONLD;
      case "rdfxml", "rdf/xml", "rdf-xml", "xml", "application/rdf+xml" -> RDFXML;
      default -> throw new IllegalArgumentException("Unsupported format: \"%s\"".formatted(value));
    };

  }
}
