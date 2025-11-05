package eu.bmdproject.dataspace.dao;

import java.net.URI;

public interface ROCrateDao {

  /**
   * Downloads a remote RO-Crate ZIP, extracts ro-crate-metadata.json, stores it as a named graph in Fuseki,
   * and returns the normalized ROCrate.
   *
   * @param url
   *     a URI pointing to a RO-Crate zip file (e&#46;g&#46; hosted on workflowhub.eu)
   */
  void ingestFromUrl(URI url);
}
