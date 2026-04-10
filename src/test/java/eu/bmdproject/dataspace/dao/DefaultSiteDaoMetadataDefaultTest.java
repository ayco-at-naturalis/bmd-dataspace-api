package eu.bmdproject.dataspace.dao;

import eu.bmdproject.dataspace.config.DiscoDataProperties;
import eu.bmdproject.dataspace.exception.NotFoundException;
import eu.bmdproject.dataspace.model.DefaultSite;
import eu.bmdproject.dataspace.model.Site;
import eu.bmdproject.dataspace.model.SiteMetadataSource;
import eu.bmdproject.dataspace.util.eea.DiscodataClient;
import eu.bmdproject.dataspace.util.eea.SiteMetaDataUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DefaultSiteDaoMetadataDefaultTest {

  @Test
  void getMetadataForSite_default_happyPath() throws Exception {
    DiscodataClient discodataClient = mock(DiscodataClient.class);
    DiscoDataProperties props = mock(DiscoDataProperties.class);
    DefaultSiteDao dao = new DefaultSiteDao(discodataClient, props);

    Connection c = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    when(c.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true);

    Site expected = mock(DefaultSite.class);

    try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class);
         MockedStatic<SiteMetaDataUtils> utils = mockStatic(SiteMetaDataUtils.class)) {

      dm.when(() -> DriverManager.getConnection(startsWith("jdbc:sqlite:"))).thenReturn(c);
      utils.when(() -> SiteMetaDataUtils.createDefaultSite(rs)).thenReturn(expected);

      Site site = dao.getMetadataForSite("FI123", SiteMetadataSource.DEFAULT);

      assertSame(expected, site);
      verify(ps).setString(1, "FI123");
      utils.verify(() -> SiteMetaDataUtils.createDefaultSite(rs));
    }
  }

  @Test
  void getMetadataForSite_default_notFound() throws Exception {
    DiscodataClient discodataClient = mock(DiscodataClient.class);
    DiscoDataProperties props = mock(DiscoDataProperties.class);
    DefaultSiteDao dao = new DefaultSiteDao(discodataClient, props);

    Connection c = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    when(c.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(false);

    try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
      dm.when(() -> DriverManager.getConnection(startsWith("jdbc:sqlite:"))).thenReturn(c);

      assertThrows(NotFoundException.class,
          () -> dao.getMetadataForSite("FI-NOPE", SiteMetadataSource.DEFAULT));
    }
  }
}