package eu.bmdproject.dataspace.controller;

import eu.bmdproject.dataspace.dao.ROCrateDao;
import eu.bmdproject.dataspace.dao.ROCrateDao.RDFFormat;
import eu.bmdproject.dataspace.validation.ROCrateUriValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Locale;

/**
 * Endpoints for dealing with RO-Creates.
 */
@RestController
@RequestMapping("/rocrates")
public class ROCrateController {

  private static final Logger LOG = LoggerFactory.getLogger(ROCrateController.class);

  private final ROCrateDao dao;
  private final ROCrateUriValidator uriValidator;

  public ROCrateController(ROCrateDao dao, ROCrateUriValidator uriValidator) {
    this.dao = dao;
    this.uriValidator = uriValidator;
  }
  /**
   * Ingest an RO-Create by fetching it from a user-specified location. This is a classic log4shell-type
   * vulnerability. We need some checks on what we pulled in.
   *
   * @param fetchFrom
   *     a URL pointing at an RO-Crate.
   *
   * @return information about the ingestion process
   */
  @PostMapping("/ingest")
  public ResponseEntity<ROCrateDao.IngestResult> ingest(@RequestParam("fetchFrom") URI fetchFrom) {
    LOG.info("Ingesting RO-Crate from URL {}", fetchFrom);
    uriValidator.validate(fetchFrom);
    ROCrateDao.IngestResult result = dao.ingestFromUrl(fetchFrom);
    URI location = URI.create("/rocrates/" + result.id());
    return ResponseEntity.created(location).body(result);
  }
  /**
   * List ingested RO-Crates (IDs + graph URIs).
   */
  @GetMapping
  public ResponseEntity<List<ROCrateDao.CrateRef>> list() {
    return ResponseEntity.ok(dao.list());
  }

  /**
   * Graph dump endpoint (default Turtle). Usage:
   * <blockquote><pre>{@code
   * /rocrates/{id}                 -> Turtle
   * /rocrates/{id}?format=rdfxml   -> RDF/XML
   * /rocrates/{id}?format=jsonld   -> JSON-LD
   * }</pre></blockquote>
   */
  @GetMapping("/{id}")
  public ResponseEntity<String> getGraph(
      @PathVariable("id") String id,
      @RequestParam(value = "format", defaultValue = "json-ld") String format
  ) {
    RDFFormat RDFFormat = parseFormat(format);
    return dao.getMetadata(id, RDFFormat)
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
   * Delete an ingested RO-Crate graph.
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable("id") String id) {
    LOG.info("Deleting RO-Crate with ID \"{}\"", id);
    boolean deleted = dao.delete(id);
    return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  private static RDFFormat parseFormat(String format) {
    String f = format == null ? "jsonld" : format.trim().toLowerCase(Locale.ROOT);
    return RDFFormat.parse(f);
  }
}
