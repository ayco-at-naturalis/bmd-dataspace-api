package eu.bmdproject.dataspace.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.SequencedSet;

/**
 * RDF formats supported by BMD Dataspace API.
 */
public enum RDFFormat {
  JSON_LD("application/ld+json", "jsonld", "json-ld"),
  TURTLE("text/turtle", "turtle", "ttl"),
  RDF_XML("application/rdf+xml", "rdfxml", "rdf/xml"),
  ;

  // User inputs that we recognize and accept as specifying this enum constant. The
  // first string in the set must be the official media type.
  private final SequencedSet<String> values;

  RDFFormat(String... values) {
    this.values = new LinkedHashSet<>(Arrays.asList(values));
  }

  @JsonCreator
  public static RDFFormat forValue(String value) {
    if (value == null || value.isEmpty()) {
      return JSON_LD;
    }
    for (RDFFormat format : RDFFormat.values()) {
      if (format.values.contains(value)) {
        return format;
      }
    }
    throw new IllegalArgumentException("Unsupported format: \"%s\"".formatted(value));
  }

  @JsonValue
  public String value() {
    return values.getFirst();
  }
}
