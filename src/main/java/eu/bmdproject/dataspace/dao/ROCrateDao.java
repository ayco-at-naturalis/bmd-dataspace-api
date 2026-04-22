package eu.bmdproject.dataspace.dao;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public interface ROCrateDao {

  IngestResult ingestFromUrl(URI url);

  List<CrateRef> list();

  Optional<CrateSummary> getSummary(String id);

  Optional<GraphPayload> getMetadata(String id, RDFFormat format);

  boolean delete(String id);

  record IngestResult(String id, String graphUri) {}

  record CrateRef(String id, String graphUri) {}

  record CrateSummary(String id, String graphUri, long tripleCount) {}

  record GraphPayload(String graphUri, String body, String mediaType) {}

  /**
   * RDF formats supported by BMD Dataspace API.
   */
  enum RDFFormat {
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
}
