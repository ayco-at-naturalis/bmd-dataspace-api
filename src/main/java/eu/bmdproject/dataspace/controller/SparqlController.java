package eu.bmdproject.dataspace.controller;


import eu.bmdproject.dataspace.dto.SparqlQueryRequest;
import eu.bmdproject.dataspace.dto.SparqlUpdateRequest;
import jakarta.validation.Valid;


import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sparql")
public class SparqlController {

  @PostMapping(
      path = "/query",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public ResponseEntity<Map<String, Object>> query(@Valid @RequestBody SparqlQueryRequest body) {
    // TODO: call Fuseki query endpoint and return results
    return ResponseEntity.ok(Map.of(
        "stub", true,
        "type", "query",
        "query", body.query()
    ));
  }

  @PostMapping(
      path = "/update",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public ResponseEntity<Map<String, Object>> update(@Valid @RequestBody SparqlUpdateRequest body) {
    // TODO: call Fuseki update endpoint and return status
    return ResponseEntity.ok(Map.of(
        "stub", true,
        "type", "update",
        "update", body.update()
    ));
  }
}