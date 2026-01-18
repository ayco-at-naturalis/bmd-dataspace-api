package eu.bmdproject.dataspace.util.eea;

import eu.bmdproject.dataspace.dao.DaoException;
import eu.bmdproject.dataspace.model.BiseSite;
import eu.bmdproject.dataspace.model.EunisSite;
import eu.bmdproject.dataspace.util.json.JsonUtil;
import org.klojang.check.Check;
import org.klojang.convert.NumberMethods;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.klojang.check.CommonChecks.instanceOf;
import static org.klojang.util.ClassMethods.simpleClassName;

public class SiteMetaDataUtils {

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
