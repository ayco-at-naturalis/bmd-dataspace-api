package eu.bmdproject.dataspace.dao;

import eu.bmdproject.dataspace.config.DiscoDataProperties;
import eu.bmdproject.dataspace.exception.NotFoundException;
import eu.bmdproject.dataspace.model.Site;
import eu.bmdproject.dataspace.model.SiteMetadataSource;
import eu.bmdproject.dataspace.util.eea.DiscodataClient;
import eu.bmdproject.dataspace.util.eea.SiteMetaDataUtils;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.klojang.check.Check;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.StringWriter;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.klojang.check.CommonChecks.file;
import static org.klojang.check.CommonChecks.notNull;
import static org.klojang.util.StringMethods.isBlank;

@Repository
public class DefaultSiteDao implements SiteDao {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultSiteDao.class);

  private static final String LAYER_NAME = "NaturaSite_polygon";
  private static final String SITECODE_COL = "SITECODE";

  private final DiscodataClient discodataClient;
  private final DiscoDataProperties discodataConfig;

  public DefaultSiteDao(
      DiscodataClient discodataClient,
      DiscoDataProperties discodataConfig) {
    this.discodataClient = discodataClient;
    this.discodataConfig = discodataConfig;
  }

  @Override
  public Map<String, List<List<String>>> getSiteIndex() {
    LOG.info("Generating Natura2000 site index");
    Path gpkgPath = discodataConfig.geopackagePath();
    Check.that(gpkgPath.toFile()).is(file(), "Missing ${arg}");
    String url = "jdbc:sqlite:" + gpkgPath.toAbsolutePath();
    String sql = "SELECT SITECODE, SITENAME FROM NATURA2000SITES ORDER BY SITENAME ASC";
    List<List<String>> index = new ArrayList<>(3000);
    try (Connection c = DriverManager.getConnection(url);
         PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        String code = rs.getString("SITECODE");
        String name = rs.getString("SITENAME");
        if (isBlank(name)) {
          if (!isBlank(code)) {
            LOG.warn("No site name for site code \"{}\"", code);
          }
          continue;
        } else if (isBlank(code)) {
          LOG.warn("No site code for site name \"{}\"", name);
          continue;
        }
        index.add(List.of(name, code));
      }
    } catch (SQLException e) {
      throw new DaoException("Failed to generate Natura 2000 site index", e);
    }
    LOG.info("{} entries in Natura2000 index", index.size());
    return Map.of("Natura2000 Site Index", index);
  }

  @Override
  public Site getMetadataForSite(String siteCode, SiteMetadataSource source) {
    LOG.info("Querying {} database using site code \"{}\"", source, siteCode);
    Check.notNull(siteCode, "site code");
    Check.notNull(source, "site metadata source");
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
      case DEFAULT -> {
        String sql = getDefaultSql();
        LOG.debug("SQL: {}", sql);
        String url = "jdbc:sqlite:" + discodataConfig.geopackagePath().toAbsolutePath();
        try (Connection c = DriverManager.getConnection(url);
             PreparedStatement ps = c.prepareStatement(sql)) {
          ps.setString(1, siteCode);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
              throw new NotFoundException("Invalid site code: \"" + siteCode + "\"");
            }
            yield SiteMetaDataUtils.createDefaultSite(rs);
          }
        } catch (SQLException e) {
          throw new DaoException("Failed to generate site metadata", e);
        }
      }
    };
  }

  @Override
  public String getGeoJsonForSite(String siteCode) {
    LOG.info("Retrieving GeoJSON for site code \"{}\"", siteCode);
    Check.notNull(siteCode, "site code");
    Path gpkgPath = discodataConfig.geopackagePath();
    Check.that(gpkgPath.toFile()).is(file(), "Missing ${arg}");
    DataStore store = null;
    try {
      store = DataStoreFinder.getDataStore(getConnectionDetails());
      Check.that(store).is(notNull(), "Could not open GeoPackage datastore (DataStoreFinder returned null)");
      SimpleFeatureSource source = store.getFeatureSource(LAYER_NAME);
      FilterFactory ff = org.geotools.factory.CommonFactoryFinder.getFilterFactory();
      Filter filter = ff.equals(ff.property(SITECODE_COL), ff.literal(siteCode));
      SimpleFeatureCollection fc = source.getFeatures(filter);
      CoordinateReferenceSystem srcCrs = CRS.decode("EPSG:3035", true);
      CoordinateReferenceSystem dstCrs = CRS.decode("EPSG:4326", true);
      var tx = CRS.findMathTransform(srcCrs, dstCrs, true);
      SimpleFeatureType schema = source.getSchema();
      DefaultFeatureCollection out = new DefaultFeatureCollection(null, schema);
      try (SimpleFeatureIterator it = fc.features()) {
        while (it.hasNext()) {
          SimpleFeature f = it.next();
          Object geomObj = f.getDefaultGeometry();
          if (geomObj instanceof Geometry g) {
            Geometry g2 = JTS.transform(g, tx);
            SimpleFeatureBuilder b = new SimpleFeatureBuilder(schema);
            b.init(f);
            b.set(schema.getGeometryDescriptor().getName(), g2);
            out.add(b.buildFeature(f.getID()));
          } else {
            out.add(f);
          }
        }
      }
      FeatureJSON fj = new FeatureJSON();
      StringWriter sw = new StringWriter();
      fj.writeFeatureCollection(out, sw);
      return sw.toString();
    } catch (Exception e) {
      throw new DaoException("Failed to produce GeoJSON for site code \"%s\"".formatted(siteCode), e);
    } finally {
      if (store != null) {
        store.dispose();
      }
    }
  }

  private Map<String, Object> getConnectionDetails() {
    return Map.of("dbtype",
        "geopkg",
        "database",
        discodataConfig.geopackagePath().toAbsolutePath().toString());
  }

  private static String getDefaultSql() {
    return """
        SELECT
          SITECODE,
          SITENAME,
          SITETYPE,
          COUNTRY_CODE,
          DATE_COMPILATION,
          DATE_UPDATE,
          DATE_SPA,
          SPA_LEGAL_REFERENCE,
          DATE_PROP_SCI,
          DATE_CONF_SCI,
          DATE_SAC,
          SAC_LEGAL_REFERENCE,
          EXPLANATIONS,
          AREAHA,
          LENGTHKM,
          MARINE_AREA_PERCENTAGE,
          DOCUMENTATION,
          QUALITY,
          DESIGNATION,
          OTHERCHARACT,
          LATITUDE,
          LONGITUDE,
          INSPIRE_ID
        FROM NATURA2000SITES
        WHERE SITECODE = ?
        LIMIT 1""";
  }
}
