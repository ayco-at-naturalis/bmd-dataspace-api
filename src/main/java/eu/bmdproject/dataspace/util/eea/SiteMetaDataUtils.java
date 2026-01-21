package eu.bmdproject.dataspace.util.eea;

import eu.bmdproject.dataspace.dao.DaoException;
import eu.bmdproject.dataspace.model.BiseSite;
import eu.bmdproject.dataspace.model.DefaultSite;
import eu.bmdproject.dataspace.model.EunisSite;
import eu.bmdproject.dataspace.util.json.JsonUtil;
import org.klojang.check.Check;
import org.klojang.convert.NumberMethods;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.klojang.check.CommonChecks.instanceOf;
import static org.klojang.util.ClassMethods.simpleClassName;

public class SiteMetaDataUtils {

  public static DefaultSite createDefaultSite(ResultSet rs) throws SQLException {
    DefaultSite site = new DefaultSite();
    site.setSiteCode(rs.getString("SITECODE"));
    site.setSiteName(rs.getString("SITENAME"));
    site.setSiteType(rs.getString("SITETYPE"));
    site.setCountryCode(rs.getString("COUNTRY_CODE"));
    site.setAreaHa(getNullableDouble(rs, "AREAHA"));
    site.setDateCompilation(getDate(rs, "DATE_COMPILATION"));
    site.setDateSac(getDate(rs, "DATE_SAC"));
    site.setDateConfSci(getDate(rs, "DATE_CONF_SCI"));
    site.setDatePropSci(getDate(rs, "DATE_PROP_SCI"));
    site.setDateSpa(getDate(rs, "DATE_SPA"));
    site.setDocumentation(rs.getString("DOCUMENTATION"));
    site.setDesignation(rs.getString("DESIGNATION"));
    site.setQuality(rs.getString("QUALITY"));
    site.setInspireId(rs.getString("INSPIRE_ID"));
    site.setExplanations(rs.getString("EXPLANATIONS"));
    site.setSacLegalReference(rs.getString("SAC_LEGAL_REFERENCE"));
    site.setOthercharact(rs.getString("OTHERCHARACT"));
    site.setLengthKm(getNullableDouble(rs, "LENGTHKM"));
    site.setMarineAreaPercentage(getNullableDouble(rs, "MARINE_AREA_PERCENTAGE"));
    site.setLatitude(getNullableDouble(rs, "LATITUDE"));
    site.setLongitude(getNullableDouble(rs, "LONGITUDE"));
    return site;
  }

  public static EunisSite processEUNISResponse(byte[] jsonResponse) {
    try {
      Map<String, Object> row = getPayload(jsonResponse);
      EunisSite site = new EunisSite();
      // Common denominator
      site.setSiteCode(getString(row, "code_site"));
      site.setSiteName(getString(row, "site_name"));
      site.setCountryName(getString(row, "country_name"));
      site.setSiteType(getString(row, "site_type"));
      site.setAreaKm2(getDouble(row, "area_km2"));
      site.setAreaHa(getDouble(row, "area_ha"));
      // EUNIS-specific
      site.setEunisAreaCode(getString(row, "eunis_area_code"));
      site.setSpaDate(getString(row, "spa_date"));
      site.setNuts(getString(row, "nuts"));
      site.setAdminRegion(getString(row, "admin_region"));
      site.setMarinePercent(getDouble(row, "marine_percent"));
      site.setSourceDb(getString(row, "source_db"));
      site.setNationalCode(getString(row, "national_code"));
      site.setNatura2000(getBoolean(row, "natura_2000"));
      return site;
    } catch (IOException e) {
      String response = new String(jsonResponse, StandardCharsets.UTF_8);
      String message = "Unable to extract EUNIS site metadata from response: " + response;
      throw new DaoException(message, e);
    }
  }

  public static BiseSite processBISEResponse(byte[] jsonResponse) {
    try {
      Map<String, Object> row = getPayload(jsonResponse);
      BiseSite site = new BiseSite();
      // Common denominator
      site.setSiteCode(getString(row, "site_code"));
      site.setSiteName(getString(row, "site_name"));
      site.setCountryName(getString(row, "country_name"));
      site.setSiteType(getString(row, "site_type"));
      site.setAreaKm2(getDouble(row, "area_km2"));
      site.setAreaHa(getDouble(row, "area_ha"));
      // BISE-specific
      site.setSiteDescription(getString(row, "site_description"));
      site.setDesignation(getString(row, "designation"));
      site.setCddaDesignationNationalLanguage(getString(row, "cdda_designation_national_language"));
      site.setMajorEcosystemType(getString(row, "major_ecosystem_type"));
      site.setYearEstablished(getInt(row, "year_stablished")); // sic
      site.setNumberProtectedHabitatTypes(getInt(row, "number_protected_habitat_types"));
      site.setNumberProtectedSpecies(getInt(row, "number_protected_species"));
      site.setRegions(getString(row, "regions"));
      site.setManagementPlan(getString(row, "management_plan"));
      return site;
    } catch (IOException e) {
      String response = new String(jsonResponse, StandardCharsets.UTF_8);
      String message = "Unable to extract BISE site metadata from response: " + response;
      throw new DaoException(message, e);
    }
  }

  private static Map<String, Object> getPayload(byte[] jsonResponse) throws IOException {
    Map<String, Object> root = JsonUtil.toMap(jsonResponse);
    Object results = root.get("results");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> rows = (List<Map<String, Object>>) results;
    return rows.getFirst();
  }

  private static String getString(Map<String, Object> m, String key) {
    Object val = m.get(key);
    if (val == null) {
      return null;
    }
    Check.that(val).is(instanceOf(), String.class, typeError(key, val, String.class));
    return (String) val;
  }

  private static Double getDouble(Map<String, Object> m, String key) {
    Object val = m.get(key);
    if (val == null) {
      return null;
    }
    Check.that(val).is(instanceOf(), Number.class, typeError(key, val, double.class));
    return ((Number) val).doubleValue();
  }

  private static Double getNullableDouble(ResultSet rs, String column) throws SQLException {
    double val = rs.getDouble(column);
    return rs.wasNull() ? null : val;
  }

  private static OffsetDateTime getDate(ResultSet rs, String column) throws SQLException {
    String dateString = rs.getString(column);
    if (dateString == null) {
      return null;
    }
    return LocalDateTime.parse(dateString).atOffset(ZoneOffset.UTC);
  }


  private static Integer getInt(Map<String, Object> m, String key) {
    Object v = m.get(key);
    if (v == null) {
      return null;
    }
    Check.that(v.getClass()).is(NumberMethods::isIntegral, typeError(key, v, int.class));
    Check.that((Number) v).is(NumberMethods::fitsInto, Integer.class, typeError(key, v, int.class));
    return NumberMethods.convert((Number) v, Integer.class);
  }

  private static Boolean getBoolean(Map<String, Object> m, String key) {
    Object val = m.get(key);
    if (val == null) {
      return null;
    }
    Check.that(val).is(instanceOf(), Boolean.class, typeError(key, val, boolean.class));
    return (Boolean) val;
  }

  private static Supplier<DaoException> typeError(String key, Object val, Class<?> expected) {
    return unexpected("Field \"%s\": %s expected, but was %s", key, simpleClassName(expected), typeOf(val));
  }

  private static Supplier<DaoException> unexpected(String reason, Object... args) {
    String message = "Unexpected site metadata layout. " + reason.formatted(args);
    return () -> new DaoException(message);
  }

  private static String typeOf(Object v) {
    if (v == null) {
      return "null";
    }
    return v + " (" + simpleClassName(v) + ")";
  }
}
