package eu.bmdproject.dataspace.dao;

import eu.bmdproject.dataspace.config.DiscoDataProperties;
import eu.bmdproject.dataspace.exception.NotImplementedException;
import eu.bmdproject.dataspace.model.Site;
import eu.bmdproject.dataspace.model.SiteMetadataSource;
import eu.bmdproject.dataspace.util.eea.DiscodataClient;
import eu.bmdproject.dataspace.util.eea.SiteMetaDataUtils;
import org.klojang.check.Check;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class DefaultSiteDao implements SiteDao {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultSiteDao.class);

  private final DiscodataClient discodataClient;
  private final DiscoDataProperties discodataConfig;

  public DefaultSiteDao(DiscodataClient discodataClient, DiscoDataProperties discodataConfig) {
    this.discodataClient = discodataClient;
    this.discodataConfig = discodataConfig;
  }

  @Override
  public Site getMetadataBySiteCode(String siteCode, SiteMetadataSource source) {
    Check.notNull(siteCode, "site code");
    Check.notNull(source, "site metadata source");
    LOG.debug("Querying {} Discodata using site code \"{}\"", source, siteCode);
    return switch (source) {
      case EUNIS -> {
        String sql = discodataConfig.eunisQueryTemplate().formatted(siteCode);
        LOG.debug("SQL: {}", sql);
        byte[] response = discodataClient.executeQuery(discodataConfig.endpoint(), sql);
        yield SiteMetaDataUtils.processEUNISResponse(response);
      }
      case BISE -> {
        String sql = discodataConfig.biseQueryTemplate().formatted(siteCode);
        LOG.debug("SQL: {}", sql);
        byte[] response = discodataClient.executeQuery(discodataConfig.endpoint(), sql);
        yield SiteMetaDataUtils.processBISEResponse(response);
      }
      case EEA_NATURA2000 -> {
        throw new NotImplementedException();
      }
    };
  }

}
