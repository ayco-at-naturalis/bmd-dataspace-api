package eu.bmdproject.dataspace.dao;

import eu.bmdproject.dataspace.config.DiscoDataProperties;
import eu.bmdproject.dataspace.util.eea.DiscodataClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

class DefaultSiteDaoSiteIndexTest {

  @Mock
  DiscodataClient discodataClient;

  @Mock
  DiscoDataProperties props;

  @TempDir
  Path tmp;

  private Path gpkgPath;

  @BeforeEach
  public void setup() throws Exception {
    gpkgPath = tmp.resolve("test.gpkg");
    Files.createFile(gpkgPath);
    props = mock(DiscoDataProperties.class);
    when(props.geopackagePath()).thenReturn(gpkgPath);
  }


  private DefaultSiteDao dao() {
    return new DefaultSiteDao(discodataClient, props);
  }

  @Test
  void getSiteIndex_happyPath_returnsNameCodePairs() throws Exception {
    // Given
    ResultSet rs = mock(ResultSet.class);
    when(rs.next()).thenReturn(true, true, false);
    when(rs.getString("SITECODE")).thenReturn("FI123", "NL456");
    when(rs.getString("SITENAME")).thenReturn("Sipoonkorpi", "Veluwe");

    PreparedStatement ps = mock(PreparedStatement.class);
    when(ps.executeQuery()).thenReturn(rs);

    Connection c = mock(Connection.class);
    when(c.prepareStatement(anyString())).thenReturn(ps);

    // When
    Map<String, List<List<String>>> result;
    try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
      dm.when(() -> DriverManager.getConnection(startsWith("jdbc:sqlite:"))).thenReturn(c);
      result = dao().getSiteIndex();
    }

    // Then
    List<List<String>> index = result.get("Natura2000 Site Index");
    assertThat(index).hasSize(2);
    assertThat(index.get(0)).containsExactly("Sipoonkorpi", "FI123");
    assertThat(index.get(1)).containsExactly("Veluwe", "NL456");
  }

  @Test
  void getSiteIndex_blankName_nonBlankCode_skipsEntryAndWarns() throws Exception {
    // Given — blank name but valid code: warn + skip; second row is valid
    ResultSet rs = mock(ResultSet.class);
    when(rs.next()).thenReturn(true, true, false);
    when(rs.getString("SITECODE")).thenReturn("FI000", "NL456");
    when(rs.getString("SITENAME")).thenReturn("", "Veluwe");

    PreparedStatement ps = mock(PreparedStatement.class);
    when(ps.executeQuery()).thenReturn(rs);

    Connection c = mock(Connection.class);
    when(c.prepareStatement(anyString())).thenReturn(ps);

    // When
    Map<String, List<List<String>>> result;
    try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
      dm.when(() -> DriverManager.getConnection(startsWith("jdbc:sqlite:"))).thenReturn(c);
      result = dao().getSiteIndex();
    }

    // Then — blank-name row is skipped; only the valid row survives
    List<List<String>> index = result.get("Natura2000 Site Index");
    assertThat(index).hasSize(1);
    assertThat(index.getFirst()).containsExactly("Veluwe", "NL456");
  }

  @Test
  void getSiteIndex_blankName_blankCode_skipsEntrySilently() throws Exception {
    // Given — both columns blank: silent skip (inner !isBlank(code) branch = false)
    ResultSet rs = mock(ResultSet.class);
    when(rs.next()).thenReturn(true, false);
    when(rs.getString("SITECODE")).thenReturn("");
    when(rs.getString("SITENAME")).thenReturn("");

    PreparedStatement ps = mock(PreparedStatement.class);
    when(ps.executeQuery()).thenReturn(rs);

    Connection c = mock(Connection.class);
    when(c.prepareStatement(anyString())).thenReturn(ps);

    // When
    Map<String, List<List<String>>> result;
    try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
      dm.when(() -> DriverManager.getConnection(startsWith("jdbc:sqlite:"))).thenReturn(c);
      result = dao().getSiteIndex();
    }

    // Then
    assertThat(result.get("Natura2000 Site Index")).isEmpty();
  }

  @Test
  void getSiteIndex_blankCode_nonBlankName_skipsEntryAndWarns() throws Exception {
    // Given — name present but code blank: the else-if branch
    ResultSet rs = mock(ResultSet.class);
    when(rs.next()).thenReturn(true, false);
    when(rs.getString("SITECODE")).thenReturn("");
    when(rs.getString("SITENAME")).thenReturn("Sipoonkorpi");

    PreparedStatement ps = mock(PreparedStatement.class);
    when(ps.executeQuery()).thenReturn(rs);

    Connection c = mock(Connection.class);
    when(c.prepareStatement(anyString())).thenReturn(ps);

    // When
    Map<String, List<List<String>>> result;
    try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
      dm.when(() -> DriverManager.getConnection(startsWith("jdbc:sqlite:"))).thenReturn(c);
      result = dao().getSiteIndex();
    }

    // Then
    assertThat(result.get("Natura2000 Site Index")).isEmpty();
  }

  @Test
  void getSiteIndex_sqlException_throwsDaoException() throws Exception {
    // Given
    PreparedStatement ps = mock(PreparedStatement.class);
    when(ps.executeQuery()).thenThrow(new SQLException("disk I/O error"));

    Connection c = mock(Connection.class);
    when(c.prepareStatement(anyString())).thenReturn(ps);

    // When / Then
    try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
      dm.when(() -> DriverManager.getConnection(startsWith("jdbc:sqlite:"))).thenReturn(c);
      assertThatThrownBy(() -> dao().getSiteIndex())
          .isInstanceOf(DaoException.class)
          .hasMessageContaining("site index")
          .hasCauseInstanceOf(SQLException.class);
    }
  }
}
