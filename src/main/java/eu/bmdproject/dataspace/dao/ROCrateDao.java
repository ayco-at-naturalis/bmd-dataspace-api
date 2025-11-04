package eu.bmdproject.dataspace.dao;

import eu.bmdproject.dataspace.model.ROCrate;

import java.util.List;
import java.util.Optional;

/**
 * Data-access facade for interacting with the triple store (e.g., Apache Fuseki).
 * <p>
 * This interface mixes crate-centric operations (CRUD on named graphs) with generic SPARQL helpers that
 * higher layers may use as escape hatches.
 */
public interface ROCrateDao {

  // ----- Crate operations on named graphs -----

  /**
   * Create or replace a crate in a named graph (id → graph IRI/URI).
   */
  void upsert(ROCrate crate);

  /**
   * Fetch a crate by its graph id. Returns empty if the graph is missing.
   */
  Optional<ROCrate> getById(String id);

  /**
   * Delete a crate graph by id. Should be idempotent.
   */
  void deleteById(String id);

  // ----- Generic SPARQL helpers -----

  /**
   * Very simple text search over literal values; returns distinct graph IRIs that contain the text.
   * Implementations may be naive (e.g., FILTER/CONTAINS) unless a text index is configured.
   */
  List<String> searchByText(String text, int limit);

  /**
   * Execute an ASK query and return its boolean result.
   */
  boolean ask(String sparqlAsk);

  /**
   * Execute a SELECT query and return SPARQL Results JSON (raw).
   */
  String select(String sparqlSelect);

  /**
   * Execute a CONSTRUCT/DESCRIBE and return RDF (e.g., Turtle).
   */
  String construct(String sparqlConstruct);

  /**
   * Execute a SPARQL UPDATE.
   */
  void update(String sparqlUpdate);
}
