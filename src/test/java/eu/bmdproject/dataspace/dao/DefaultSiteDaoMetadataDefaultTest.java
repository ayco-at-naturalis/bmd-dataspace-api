package eu.bmdproject.dataspace.dao;

import eu.bmdproject.dataspace.config.DiscoDataProperties;
import eu.bmdproject.dataspace.exception.NotFoundException;
import eu.bmdproject.dataspace.model.DefaultSite;
import eu.bmdproject.dataspace.model.Site;
import eu.bmdproject.dataspace.model.SiteMetadataSource;
import eu.bmdproject.dataspace.util.eea.DiscodataClient;
import eu.bmdproject.dataspace.util.eea.SiteMetaDataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultSiteDaoMetadataDefaultTest {

  @TempDir
  Path tmp;

  @Mock
  Connection c;

  @Mock
  PreparedStatement ps;

  @Mock
  ResultSet rs;

  @Mock
  DiscodataClient discodataClient;

  @Mock
  DiscoDataProperties props;

  private Path gpkgPath;

  @BeforeEach
  void setup() throws Exception {
    gpkgPath = tmp.resolve("test.gpkg");
    Files.createFile(gpkgPath);
    when(props.geopackagePath()).thenReturn(gpkgPath);
    when(props.geopackagePath()).thenReturn(gpkgPath);
    when(c.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
  }

  @Test
  void getMetadataForSite_default_happyPath() throws Exception {
    // Given
    DefaultSiteDao dao = new DefaultSiteDao(discodataClient, props);
    when(rs.next()).thenReturn(true);
    Site expected = mock(DefaultSite.class);

    try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class);
         MockedStatic<SiteMetaDataUtils> utils = mockStatic(SiteMetaDataUtils.class)) {
      dm.when(() -> DriverManager.getConnection(startsWith("jdbc:sqlite:"))).thenReturn(c);
      utils.when(() -> SiteMetaDataUtils.createDefaultSite(rs)).thenReturn(expected);

      // When
      Site site = dao.getMetadataForSite("FI123", SiteMetadataSource.DEFAULT);

      // Then
      assertSame(expected, site);
      verify(ps).setString(1, "FI123");
      utils.verify(() -> SiteMetaDataUtils.createDefaultSite(rs));
    }
  }

  @Test
  void getMetadataForSite_default_notFound() throws Exception {
    // Given
    DefaultSiteDao dao = new DefaultSiteDao(discodataClient, props);
    when(rs.next()).thenReturn(false);

    try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
      dm.when(() -> DriverManager.getConnection(startsWith("jdbc:sqlite:"))).thenReturn(c);

      // When / Then
      assertThrows(NotFoundException.class,
          () -> dao.getMetadataForSite("FI-NOPE", SiteMetadataSource.DEFAULT));
    }
  }
}