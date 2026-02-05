package eu.bmdproject.dataspace.dao;

import eu.bmdproject.dataspace.config.DiscoDataProperties;
import eu.bmdproject.dataspace.util.eea.DiscodataClient;
import org.geotools.api.data.*;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataUtilities;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DefaultSiteDaoGeoJsonTest {

  @TempDir
  Path tmp;

  @Test
  void getGeoJsonForSite_returnsFeatureCollectionJson_withRealGeneratedGeoPackage() throws Exception {

    // 1) Create a GeoPackage file and populate it with 1 feature
    Path gpkgPath = tmp.resolve("test.gpkg");

    // Layer schema must match what DefaultSiteDao expects:
    // - layer/table: NaturaSite_polygon
    // - geometry column: the_geom
    // - attribute: SITECODE
    SimpleFeatureType sft = DataUtilities.createType(
        "NaturaSite_polygon",
        "the_geom:MultiPolygon:srid=3035,SITECODE:String"
    );

    Geometry geom3035 = new WKTReader().read("MULTIPOLYGON (((0 0, 0 1, 1 1, 0 0)))");

    SimpleFeature feature = SimpleFeatureBuilder.build(sft, new Object[]{geom3035, "FI123"}, "id-1");

    DefaultFeatureCollection features = new DefaultFeatureCollection(null, sft);
    features.add(feature);

    // Create datastore + schema + insert feature
    Map<String, Object> params = new HashMap<>();
    params.put("dbtype", "geopkg");
    params.put("database", gpkgPath.toFile());

    DataStore ds = null;
    try {
      ds = DataStoreFinder.getDataStore(params);
      // Some GeoTools setups require createNewDataStore() via the factory.
      // If ds == null here, see the note below (GeoPackage module missing).
      assertNotNull(ds, "GeoPackage DataStore not available (GeoTools geopkg module missing?)");

      ds.createSchema(sft);

      SimpleFeatureSource source = ds.getFeatureSource("NaturaSite_polygon");
      assertInstanceOf(SimpleFeatureStore.class, source, "FeatureSource must be writable for test setup");

      SimpleFeatureStore store = (SimpleFeatureStore) source;
      store.setTransaction(Transaction.AUTO_COMMIT);
      store.addFeatures(features);
    } finally {
      if (ds != null) {
        ds.dispose();
      }
    }

    // 2) Point the DAO at this GeoPackage
    DefaultSiteDao.setGpkgPathForTests(gpkgPath);

    DiscodataClient discodataClient = mock(DiscodataClient.class);
    DiscoDataProperties props = mock(DiscoDataProperties.class);
    DefaultSiteDao dao = new DefaultSiteDao(discodataClient, props);

    // 3) Exercise
    String json = dao.getGeoJsonForSite("FI123");

    // 4) Assert (lightweight checks; you can parse JSON later if you want)
    assertTrue(json.contains("\"type\":\"FeatureCollection\""), json);
    assertTrue(json.contains("\"features\""), json);
    assertTrue(json.contains("\"FI123\""), json); // SITECODE should survive into GeoJSON properties
  }
}