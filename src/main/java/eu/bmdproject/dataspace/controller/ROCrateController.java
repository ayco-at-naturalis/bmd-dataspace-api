package eu.bmdproject.dataspace.controller;

import eu.bmdproject.dataspace.dao.ROCrateDao;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

@RestController
@RequestMapping("/rocrate")
public class ROCrateController {

  private static final Logger log = LoggerFactory.getLogger(ROCrateController.class);

  private final ROCrateDao roCrateDao;

  public ROCrateController(ROCrateDao roCrateDao) {
    this.roCrateDao = roCrateDao;
  }

  @PostMapping("/ingest")
  public ResponseEntity<Void> ingest(@RequestParam("fetchFrom") URI fetchFrom) {
    log.info("Ingesting RO-Crate. RO-Crate fetched from: {}", fetchFrom);
    roCrateDao.ingestFromUrl(fetchFrom);
    return ResponseEntity.ok().build();
  }
}
