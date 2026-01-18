package eu.bmdproject.dataspace.dao;

import eu.bmdproject.dataspace.model.Site;
import eu.bmdproject.dataspace.model.SiteMetadataSource;

import java.util.Optional;

public interface SiteDao {
  Site getMetadataBySiteCode(String siteCode, SiteMetadataSource source);
}
