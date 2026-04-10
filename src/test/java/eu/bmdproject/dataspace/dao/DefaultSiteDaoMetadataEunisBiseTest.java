package eu.bmdproject.dataspace.dao;

import eu.bmdproject.dataspace.config.DiscoDataProperties;
import eu.bmdproject.dataspace.model.BiseSite;
import eu.bmdproject.dataspace.model.EunisSite;
import eu.bmdproject.dataspace.model.Site;
import eu.bmdproject.dataspace.model.SiteMetadataSource;
import eu.bmdproject.dataspace.util.eea.DiscodataClient;
import eu.bmdproject.dataspace.util.eea.SiteMetaDataUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultSiteDaoMetadataEunisBiseTest {

  @Mock DiscodataClient discodataClient;
  @Mock DiscoDataProperties props;

  @Test
  void getMetadataForSite_eunis_callsDiscodataAndParses() {
    DefaultSiteDao dao = new DefaultSiteDao(discodataClient, props);

    when(props.endpoint()).thenReturn("https://example/sparql");
    when(props.eunisQueryTemplate()).thenReturn("SELECT * WHERE { '%s' }");

    byte[] bytes = "dummy".getBytes();
    when(discodataClient.executeQuery("https://example/sparql", "SELECT * WHERE { 'FI123' }"))
        .thenReturn(bytes);

    Site expected = mock(EunisSite.class);

    try (MockedStatic<SiteMetaDataUtils> utils = mockStatic(SiteMetaDataUtils.class)) {
      utils.when(() -> SiteMetaDataUtils.processEUNISResponse(bytes)).thenReturn(expected);

      Site site = dao.getMetadataForSite("FI123", SiteMetadataSource.EUNIS);

      assertSame(expected, site);
      verify(discodataClient).executeQuery("https://example/sparql", "SELECT * WHERE { 'FI123' }");
      utils.verify(() -> SiteMetaDataUtils.processEUNISResponse(bytes));
    }
  }

  @Test
  void getMetadataForSite_bise_callsDiscodataAndParses() {
    DefaultSiteDao dao = new DefaultSiteDao(discodataClient, props);

    when(props.endpoint()).thenReturn("https://example/sparql");
    when(props.biseQueryTemplate()).thenReturn("ASK { '%s' }");

    byte[] bytes = "dummy".getBytes();
    when(discodataClient.executeQuery("https://example/sparql", "ASK { 'FI123' }")).thenReturn(bytes);

    Site expected = mock(BiseSite.class);

    try (MockedStatic<SiteMetaDataUtils> utils = mockStatic(SiteMetaDataUtils.class)) {
      utils.when(() -> SiteMetaDataUtils.processBISEResponse(bytes)).thenReturn(expected);

      Site site = dao.getMetadataForSite("FI123", SiteMetadataSource.BISE);

      assertSame(expected, site);
      verify(discodataClient).executeQuery("https://example/sparql", "ASK { 'FI123' }");
      utils.verify(() -> SiteMetaDataUtils.processBISEResponse(bytes));
    }
  }
}