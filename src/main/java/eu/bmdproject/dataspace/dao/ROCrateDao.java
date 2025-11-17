package eu.bmdproject.dataspace.dao;

import java.net.URI;

public interface ROCrateDao {
  void ingestFromUrl(URI url);
}
