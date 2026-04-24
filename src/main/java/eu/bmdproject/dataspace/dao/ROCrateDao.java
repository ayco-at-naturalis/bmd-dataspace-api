package eu.bmdproject.dataspace.dao;

import eu.bmdproject.dataspace.model.RDFFormat;

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

}
