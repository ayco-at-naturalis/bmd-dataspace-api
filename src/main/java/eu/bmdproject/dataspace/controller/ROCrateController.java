package eu.bmdproject.dataspace.controller;

import eu.bmdproject.dataspace.dao.ROCrateDao;
import eu.bmdproject.dataspace.dao.ROCrateDao.GraphFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/rocrates")
public class ROCrateController {

  private static final Logger LOG = LoggerFactory.getLogger(ROCrateController.class);

  private final ROCrateDao dao;

  public ROCrateController(ROCrateDao dao) {
    this.dao = dao;
  }

  @PostMapping("/ingest")
  public ResponseEntity<ROCrateDao.IngestResult> ingest(@RequestParam("fetchFrom") URI fetchFrom) {
    LOG.info("Ingesting RO-Crate from URL {}", fetchFrom);
    ROCrateDao.IngestResult result = dao.ingestFromUrl(fetchFrom);
    URI location = URI.create("/rocrates/" + result.id());
    return ResponseEntity.created(location).body(result);
  }

  // Temporary MVP/demo alias (browser-friendly). Remove afterwards.
  @GetMapping("/ingest")
  public ResponseEntity<ROCrateDao.IngestResult> ingestGet(@RequestParam("fetchFrom") URI fetchFrom) {
    return ingest(fetchFrom);
  }

  /**
   * List ingested RO-Crates (IDs + graph URIs).
   */
  @GetMapping
  public ResponseEntity<List<ROCrateDao.CrateRef>> list() {
    return ResponseEntity.ok(dao.list());
  }

  /**
   * Graph dump endpoint (default Turtle).
   * Usage:
   *   /rocrates/{id}                 -> Turtle
   *   /rocrates/{id}?format=rdfxml   -> RDF/XML
   *   /rocrates/{id}?format=jsonld   -> JSON-LD
   */
  @GetMapping("/{id}")
  public ResponseEntity<String> getGraph(
      @PathVariable("id") String id,
      @RequestParam(value = "format", defaultValue = "turtle") String format
  ) {
    GraphFormat graphFormat = parseFormat(format);

    return dao.getMetadata(id, graphFormat)
        .map(payload -> ResponseEntity.ok()
            .contentType(MediaType.valueOf(payload.mediaType()))
            .body(payload.body()))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /**
   * Summary endpoint: triple count, etc.
   */
  @GetMapping("/{id}/summary")
  public ResponseEntity<ROCrateDao.CrateSummary> getSummary(@PathVariable("id") String id) {
    return dao.getSummary(id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /**
   * Backwards-compatible alias. Consider removing once clients switch to GET /rocrates/{id}.
   */
  @GetMapping("/{id}/metadata")
  public ResponseEntity<String> getMetadataAlias(
      @PathVariable("id") String id,
      @RequestParam(value = "format", defaultValue = "jsonld") String format
  ) {
    return getGraph(id, format);
  }

  /**
   * Delete an ingested RO-Crate graph.
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable("id") String id) {
    LOG.info("Deleting RO-Crate with ID \"{}\"", id);
    boolean deleted = dao.delete(id);
    return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  private static GraphFormat parseFormat(String format) {
    String f = format == null ? "turtle" : format.trim().toLowerCase(Locale.ROOT);
    return switch (f) {
      case "turtle", "ttl", "text/turtle" -> GraphFormat.TURTLE;
      case "jsonld", "json-ld", "application/ld+json" -> GraphFormat.JSONLD;
      case "rdfxml", "rdf/xml", "rdf-xml", "xml", "application/rdf+xml" -> GraphFormat.RDFXML;
      default -> throw new IllegalArgumentException("Unsupported format: " + format);
    };
  }
}
