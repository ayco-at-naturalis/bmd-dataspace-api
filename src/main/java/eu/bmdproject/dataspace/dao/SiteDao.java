package eu.bmdproject.dataspace.dao;

import eu.bmdproject.dataspace.model.Site;
import eu.bmdproject.dataspace.model.SiteMetadataSource;

import java.util.List;
import java.util.Map;


public interface SiteDao {

  Map<String, List<List<String>>> getSiteIndex();

  Site getMetadataForSite(String siteCode, SiteMetadataSource source);

  // Or should we try to use geojson-jackson???
  String getGeoJsonForSite(String siteCode);
}
