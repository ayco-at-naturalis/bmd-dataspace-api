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
@RequestMapping("/rocrate")
public class ROCrateController {

  private static final Logger log = LoggerFactory.getLogger(ROCrateController.class);

  private final ROCrateDao dao;

  public ROCrateController(ROCrateDao dao) {
    this.dao = dao;
  }

  @PostMapping("/ingest")
  public ResponseEntity<ROCrateDao.IngestResult> ingest(@RequestParam("fetchFrom") URI fetchFrom) {
    log.info("Ingesting RO-Crate from URL {}", fetchFrom);
    ROCrateDao.IngestResult result = dao.ingestFromUrl(fetchFrom);
    URI location = URI.create("/rocrate/" + result.id());
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
   * Summary endpoint: triple count, etc.
   */
  @GetMapping("/{id}")
  public ResponseEntity<ROCrateDao.CrateSummary> getSummary(@PathVariable("id") String id) {
    return dao.getSummary(id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /**
   * Get the stored metadata graph back (demo-friendly). Usage: /rocrate/{id}/metadata?format=jsonld|turtle
   */
  @GetMapping("/{id}/metadata")
  public ResponseEntity<String> getMetadata(
      @PathVariable("id") String id,
      @RequestParam(value = "format", defaultValue = "jsonld") String format
  ) {
    GraphFormat graphFormat = parseFormat(format);

    return dao.getMetadata(id, graphFormat)
        .map(payload -> ResponseEntity.ok()
            .contentType(MediaType.valueOf(payload.mediaType()))
            .body(payload.body()))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /**
   * Delete an ingested RO-Crate graph.
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable("id") String id) {
    boolean deleted = dao.delete(id);
    return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  private static GraphFormat parseFormat(String format) {
    String f = format == null ? "jsonld" : format.trim().toLowerCase(Locale.ROOT);
    return switch (f) {
      case "turtle", "ttl", "text/turtle" -> GraphFormat.TURTLE;
      case "jsonld", "json-ld", "application/ld+json" -> GraphFormat.JSONLD;
      default -> throw new IllegalArgumentException("Unsupported format: " + format);
    };
  }
}
