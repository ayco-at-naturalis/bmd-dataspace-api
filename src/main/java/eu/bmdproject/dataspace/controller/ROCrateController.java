package eu.bmdproject.dataspace.controller;

import eu.bmdproject.dataspace.dao.ROCrateDao;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * REST endpoints for RO-Crate operations.
 */
@RestController
@RequestMapping("/rocrate")
public class ROCrateController {

  private final ROCrateDao dao;

  public ROCrateController(ROCrateDao dao) {
    this.dao = dao;
  }
  /**
   * Stub endpoint that (eventually) ingests an RO-Crate by fetching it from the given URL.
   * For now, it returns HTTP 200 with no content.
   *
   * Example: POST /rocrate/ingest?fetchFrom=https://example.org/mycrate.zip
   *
   * @param fetchFrom the source URL to fetch the RO-Crate from
   * @return 200 OK with empty body
   */
  @PostMapping("/ingest")
  public ResponseEntity<Void> ingest(@RequestParam("fetchFrom") URI fetchFrom) {
    dao.ingestFromUrl(fetchFrom);
    return ResponseEntity.ok().build();
  }
}
