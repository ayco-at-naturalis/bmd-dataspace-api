package eu.bmdproject.dataspace.util.eea;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bmdproject.dataspace.dao.DaoException;
import eu.bmdproject.dataspace.model.BiseSite;
import eu.bmdproject.dataspace.model.DefaultSite;
import eu.bmdproject.dataspace.model.EunisSite;
import org.junit.jupiter.api.Test;
import org.klojang.util.CollectionMethods;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SiteMetaDataUtilsTest {

  // ---------------------------------------------------------------------------
  // createDefaultSite
  // ---------------------------------------------------------------------------

  @Test
  void createDefaultSite_mapsAllPopulatedColumns() throws Exception {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getString("SITECODE")).thenReturn("NL2000001");
    when(rs.getString("SITENAME")).thenReturn("Boschplaat");
    when(rs.getString("SITETYPE")).thenReturn("B");
    when(rs.getString("COUNTRY_CODE")).thenReturn("NL");
    when(rs.getDouble("AREAHA")).thenReturn(4540.0);
    when(rs.wasNull()).thenReturn(false);
    when(rs.getString("DATE_COMPILATION")).thenReturn("1998-06-01T00:00:00");
    when(rs.getString("DATE_SAC")).thenReturn("2004-05-07T00:00:00");
    when(rs.getString("DATE_CONF_SCI")).thenReturn(null);
    when(rs.getString("DATE_PROP_SCI")).thenReturn(null);
    when(rs.getString("DATE_SPA")).thenReturn(null);
    when(rs.getString("DOCUMENTATION")).thenReturn("Some documentation.");
    when(rs.getString("DESIGNATION")).thenReturn("SAC");
    when(rs.getString("QUALITY")).thenReturn("Good");
    when(rs.getString("INSPIRE_ID")).thenReturn("NL.EEA.NL2000001");
    when(rs.getString("EXPLANATIONS")).thenReturn(null);
    when(rs.getString("SAC_LEGAL_REFERENCE")).thenReturn("Decree 2004");
    when(rs.getString("OTHERCHARACT")).thenReturn(null);
    when(rs.getDouble("LENGTHKM")).thenReturn(0.0);
    when(rs.getDouble("MARINE_AREA_PERCENTAGE")).thenReturn(0.0);
    when(rs.getDouble("LATITUDE")).thenReturn(53.45);
    when(rs.getDouble("LONGITUDE")).thenReturn(5.21);

    DefaultSite site = SiteMetaDataUtils.createDefaultSite(rs);

    assertEquals("NL2000001", site.getSiteCode());
    assertEquals("Boschplaat", site.getSiteName());
    assertEquals("B", site.getSiteType());
    assertEquals("NL", site.getCountryCode());
    assertEquals(4540.0, site.getAreaHa());
    assertEquals(
        OffsetDateTime.of(1998, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC),
        site.getDateCompilation()
    );
    assertEquals(
        OffsetDateTime.of(2004, 5, 7, 0, 0, 0, 0, ZoneOffset.UTC),
        site.getDateSac()
    );
    assertNull(site.getDateConfSci());
    assertNull(site.getDatePropSci());
    assertNull(site.getDateSpa());
    assertEquals("Some documentation.", site.getDocumentation());
    assertEquals("SAC", site.getDesignation());
    assertEquals("Good", site.getQuality());
    assertEquals("NL.EEA.NL2000001", site.getInspireId());
    assertNull(site.getExplanations());
    assertEquals("Decree 2004", site.getSacLegalReference());
    assertNull(site.getOthercharact());
    assertEquals(53.45, site.getLatitude());
    assertEquals(5.21, site.getLongitude());
  }

  @Test
  void createDefaultSite_nullableDoubles_returnNull_whenSqlWasNull() throws Exception {
    ResultSet rs = mock(ResultSet.class);
    // Provide the minimum non-nullable strings
    when(rs.getString(anyString())).thenReturn(null);
    when(rs.getDouble(anyString())).thenReturn(0.0);
    when(rs.wasNull()).thenReturn(true); // every getDouble() was SQL NULL

    DefaultSite site = SiteMetaDataUtils.createDefaultSite(rs);

    assertNull(site.getAreaHa());
    assertNull(site.getLengthKm());
    assertNull(site.getMarineAreaPercentage());
    assertNull(site.getLatitude());
    assertNull(site.getLongitude());
  }

  // ---------------------------------------------------------------------------
  // processEUNISResponse
  // ---------------------------------------------------------------------------

  /** Builds the expected {"results":[{...}]} wire format and verifies all fields. */
  @Test
  void processEUNISResponse_mapsAllFields() throws Exception {
    Map<String, Object> row = CollectionMethods.initializedMap(
        "code_site", "FI0100065",
        "site_name", "Sipoonkorpi",
        "country_name", "Finland",
        "site_type", "B",
        "area_km2", 55.3,
        "area_ha", 5530.0,
        "eunis_area_code", "FI",
        "spa_date", "2004-01-01",
        "nuts", "FI1B",
        "admin_region", "Uusimaa",
        "marine_percent", 0.0,
        "source_db", "EUNIS",
        "national_code", "FI001",
        "natura_2000", true
    );
    byte[] json = buildResponse(row);

    EunisSite site = SiteMetaDataUtils.processEUNISResponse(json);

    assertEquals("FI0100065", site.getSiteCode());
    assertEquals("Sipoonkorpi", site.getSiteName());
    assertEquals("Finland", site.getCountryName());
    assertEquals("B", site.getSiteType());
    assertEquals(55.3, site.getAreaKm2());
    assertEquals(5530.0, site.getAreaHa());
    assertEquals("FI", site.getEunisAreaCode());
    assertEquals("2004-01-01", site.getSpaDate());
    assertEquals("FI1B", site.getNuts());
    assertEquals("Uusimaa", site.getAdminRegion());
    assertEquals(0.0, site.getMarinePercent());
    assertEquals("EUNIS", site.getSourceDb());
    assertEquals("FI001", site.getNationalCode());
    assertTrue(site.getNatura2000());
  }

  @Test
  void processEUNISResponse_nullableFields_returnNull_whenAbsentFromJson() throws Exception {
    // Minimal row — only the keys the parser does a null-safe get() on
    Map<String, Object> row = Map.of("code_site", "FI0100065");
    byte[] json = buildResponse(row);

    EunisSite site = SiteMetaDataUtils.processEUNISResponse(json);

    assertEquals("FI0100065", site.getSiteCode());
    assertNull(site.getSiteName());
    assertNull(site.getCountryName());
    assertNull(site.getEunisAreaCode());
    assertNull(site.getNatura2000());
  }

  @Test
  void processEUNISResponse_throwsDaoException_onMalformedJson() {
    byte[] garbage = "not-json-at-all".getBytes();
    assertThrows(DaoException.class, () -> SiteMetaDataUtils.processEUNISResponse(garbage));
  }

  @Test
  void processEUNISResponse_throwsDaoException_whenFieldHasWrongType() throws Exception {
    // "area_km2" is expected to be a Number; supply a String to trigger the type check
    Map<String, Object> row = Map.of(
        "code_site", "FI0100065",
        "area_km2", "not-a-number"
    );
    byte[] json = buildResponse(row);
    assertThrows(DaoException.class, () -> SiteMetaDataUtils.processEUNISResponse(json));
  }

  // ---------------------------------------------------------------------------
  // processBISEResponse
  // ---------------------------------------------------------------------------

  @Test
  void processBISEResponse_mapsAllFields() throws Exception {
    Map<String, Object> row = CollectionMethods.initializedMap(
        "site_code", "NL2000015",
        "site_name", "Veluwe",
        "country_name", "Netherlands",
        "site_type", "C",
        "area_km2", 910.0,
        "area_ha", 91000.0,
        "site_description", "Large forested area.",
        "designation", "SAC",
        "cdda_designation_national_language", "Habitatrichtlijngebied",
        "major_ecosystem_type", "Forest",
        "year_stablished", 2004,   // sic — matches the intentional typo in BISE
        "number_protected_habitat_types", 12,
        "number_protected_species", 37,
        "regions", "Gelderland",
        "management_plan", "Beheerplan 2020"
    );
    byte[] json = buildResponse(row);

    BiseSite site = SiteMetaDataUtils.processBISEResponse(json);

    assertEquals("NL2000015", site.getSiteCode());
    assertEquals("Veluwe", site.getSiteName());
    assertEquals("Netherlands", site.getCountryName());
    assertEquals("C", site.getSiteType());
    assertEquals(910.0, site.getAreaKm2());
    assertEquals(91000.0, site.getAreaHa());
    assertEquals("Large forested area.", site.getSiteDescription());
    assertEquals("SAC", site.getDesignation());
    assertEquals("Habitatrichtlijngebied", site.getCddaDesignationNationalLanguage());
    assertEquals("Forest", site.getMajorEcosystemType());
    assertEquals(2004, site.getYearEstablished());
    assertEquals(12, site.getNumberProtectedHabitatTypes());
    assertEquals(37, site.getNumberProtectedSpecies());
    assertEquals("Gelderland", site.getRegions());
    assertEquals("Beheerplan 2020", site.getManagementPlan());
  }

  @Test
  void processBISEResponse_nullableFields_returnNull_whenAbsentFromJson() throws Exception {
    Map<String, Object> row = Map.of("site_code", "NL2000015");
    byte[] json = buildResponse(row);

    BiseSite site = SiteMetaDataUtils.processBISEResponse(json);

    assertEquals("NL2000015", site.getSiteCode());
    assertNull(site.getSiteName());
    assertNull(site.getYearEstablished());
    assertNull(site.getNumberProtectedSpecies());
  }

  @Test
  void processBISEResponse_throwsDaoException_onMalformedJson() {
    byte[] garbage = "[{\"results\": \"oops\"}]".getBytes();
    assertThrows(DaoException.class, () -> SiteMetaDataUtils.processBISEResponse(garbage));
  }

  @Test
  void processBISEResponse_throwsDaoException_whenIntegerFieldHasWrongType() throws Exception {
    Map<String, Object> row = Map.of(
        "site_code", "NL2000015",
        "year_stablished", "not-an-int"
    );
    byte[] json = buildResponse(row);
    assertThrows(DaoException.class, () -> SiteMetaDataUtils.processBISEResponse(json));
  }

  // ---------------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------------

  /** Wraps a row map in the {"results":[row]} envelope that getPayload() expects. */
  private static byte[] buildResponse(Map<String, Object> row) throws Exception {
    Map<String, Object> envelope = Map.of("results", List.of(row));
    return new ObjectMapper().writeValueAsBytes(envelope);
  }
}