package eu.bmdproject.dataspace.controller;

import eu.bmdproject.dataspace.dao.SiteDao;
import eu.bmdproject.dataspace.exception.BadRequestException;
import eu.bmdproject.dataspace.exception.NotImplementedException;
import eu.bmdproject.dataspace.model.DefaultSite;
import eu.bmdproject.dataspace.model.Site;
import eu.bmdproject.dataspace.model.SiteMetadataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SiteControllerTest {

  private SiteDao dao;
  private SiteController controller;

  @BeforeEach
  void setUp() {
    dao = mock(SiteDao.class);
    controller = new SiteController(dao);
  }

  @Test
  void getSiteIndex_returnsOkAndDelegatesToDao() {
    Map<String, List<List<String>>> index = Map.of(
        "FI", List.of(List.of("FI1301409", "Some Site")),
        "NL", List.of(List.of("NL2000000", "Another Site"))
    );
    when(dao.getSiteIndex()).thenReturn(index);

    ResponseEntity<Map<String, List<List<String>>>> resp = controller.getSiteIndex();

    assertEquals(200, resp.getStatusCode().value());
    assertSame(index, resp.getBody());
    verify(dao).getSiteIndex();
    verifyNoMoreInteractions(dao);
  }

  @Test
  void getMetadataDefault_usesDefaultSourceAndReturnsOk() {
    String siteCode = "FI1301409";
    Site site = mock(DefaultSite.class);
    when(dao.getMetadataForSite(siteCode, SiteMetadataSource.DEFAULT)).thenReturn(site);

    ResponseEntity<Site> resp = controller.getMetadataDefault(siteCode);

    assertEquals(200, resp.getStatusCode().value());
    assertSame(site, resp.getBody());
    verify(dao).getMetadataForSite(siteCode, SiteMetadataSource.DEFAULT);
    verifyNoMoreInteractions(dao);
  }

  @Test
  void getMetadataBySource_parsesSourceAndDelegatesToDao() {
    String siteCode = "FI1301409";

    // Use a real enum constant to avoid depending on parse() specifics.
    // We feed it as lowercase to exercise the parse path.
    SiteMetadataSource expectedSource = SiteMetadataSource.values()[0];
    String sourceParam = expectedSource.name().toLowerCase();

    Site site = mock(DefaultSite.class);
    when(dao.getMetadataForSite(siteCode, expectedSource)).thenReturn(site);

    ResponseEntity<Site> resp = controller.getMetadataBySource(siteCode, sourceParam);

    assertEquals(200, resp.getStatusCode().value());
    assertSame(site, resp.getBody());

    // Capture the actual enum passed to DAO; ensures parsing occurred.
    ArgumentCaptor<SiteMetadataSource> captor = ArgumentCaptor.forClass(SiteMetadataSource.class);
    verify(dao).getMetadataForSite(eq(siteCode), captor.capture());
    assertEquals(expectedSource, captor.getValue());

    verifyNoMoreInteractions(dao);
  }

  @Test
  void getMetadataBySource_invalidSource_throwsBadRequestException_withHelpfulMessage() {
    String siteCode = "FI1301409";
    String invalid = "definitely-not-a-source";

    BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> controller.getMetadataBySource(siteCode, invalid)
    );

    // Message contract in controller: includes the invalid token and "Valid metadata sources:"
    assertTrue(ex.getMessage().contains("No such site metadata source"),
        "Should explain the source is invalid");
    assertTrue(ex.getMessage().contains("\"" + invalid + "\""),
        "Should include the invalid value");
    assertTrue(ex.getMessage().contains("Valid metadata sources:"),
        "Should include a list of valid sources");

    // No DAO calls should happen when parse fails.
    verifyNoInteractions(dao);
  }

  @Test
  void getGeoJsonForSite_returnsOkAndPassesThroughString() {
    String siteCode = "FI1301409";
    String geoJson = "{\"type\":\"FeatureCollection\",\"features\":[]}";
    when(dao.getGeoJsonForSite(siteCode)).thenReturn(geoJson);

    ResponseEntity<?> resp = controller.getGeoJsonForSite(siteCode);

    assertEquals(200, resp.getStatusCode().value());
    assertEquals(geoJson, resp.getBody());
    verify(dao).getGeoJsonForSite(siteCode);
    verifyNoMoreInteractions(dao);
  }

  @Test
  void getGeoPackageForSite_throwsNotImplementedException() {
    NotImplementedException ex = assertThrows(
        NotImplementedException.class,
        () -> controller.getGeoPackageForSite("FI1301409")
    );
    assertEquals("GeoPackage download not implemented yet", ex.getMessage());
    verifyNoInteractions(dao);
  }
}
