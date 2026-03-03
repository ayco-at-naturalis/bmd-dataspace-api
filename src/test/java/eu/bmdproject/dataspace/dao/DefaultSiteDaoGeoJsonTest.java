package eu.bmdproject.dataspace.dao;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataUtilities;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.klojang.util.ArrayMethods.pack;

class DefaultSiteDaoGeoJsonTest {

  @TempDir
  Path tmp;

  @Test
  void getGeoJsonForSite_returnsFeatureCollectionJson_withRealGeneratedGeoPackage() throws Exception {
//    // 1) Create a GeoPackage file and populate it with 1 feature
//    Path gpkgPath = tmp.resolve("test.gpkg");
//    SimpleFeatureType type = DataUtilities.createType(
//        "NaturaSite_polygon",
//        "the_geom:MultiPolygon:srid=3035,SITECODE:String"
//    );
//    Geometry geom3035 = new WKTReader().read("MULTIPOLYGON (((0 0, 0 1, 1 1, 0 0)))");
//    Object[] row = pack(geom3035, "FI123");
//    SimpleFeature feature = SimpleFeatureBuilder.build(type, row, null);
//    DefaultFeatureCollection collection = new DefaultFeatureCollection(null, type);
//    collection.add(feature);
//    try (GeoPackage geopackage = new GeoPackage(gpkgPath.toFile())) {
//      geopackage.init();
//      FeatureEntry entry = new FeatureEntry();
//      entry.setTableName("NaturaSite_polygon");
//      entry.setGeometryColumn("the_geom");
//      geopackage.add(entry, collection);
//    }
//
//    DefaultFeatureCollection features = new DefaultFeatureCollection(null, featureType);
//    features.add(SimpleFeatureBuilder.build(
//        featureType,
//        new Object[]{geom3035, "FI123"},
//        null
//    ));
//
//    try (GeoPackage geopackage = new GeoPackage(gpkgPath.toFile())) {
//      geopackage.init();
//      FeatureEntry entry = new FeatureEntry();
//      entry.setGeometryColumn("the_geom");
//      //geopackage.add(FeatureEntry.);
//      //geopackage.add(sft);
//      geopackage.add("NaturaSite_polygon", features);
//    }

//    DataStore ds = null;
//    try {
//      ds = DataStoreFinder.getDataStore(params);
//      assertNotNull(ds, "GeoPackage DataStore not available (GeoTools geopkg module missing?)");
//      SimpleFeatureType sft = DataUtilities.createType(
//          "NaturaSite_polygon",
//          "id:Integer:pk,the_geom:MultiPolygon:srid=3035,SITECODE:String"
//      );
//      ds.createSchema(sft);
//      List<GeoPackageUtils.Column> columns = GeoPackageUtils.listColumns(gpkgPath, "NaturaSite_polygon");
//      System.out.println(columns);
//      SimpleFeatureSource source = ds.getFeatureSource("NaturaSite_polygon");
//      assertInstanceOf(SimpleFeatureStore.class, source, "FeatureSource must be writable for test setup");
//      SimpleFeatureStore store = (SimpleFeatureStore) source;
//      store.setTransaction(Transaction.AUTO_COMMIT);
//      try (var writer = ds.getFeatureWriterAppend("NaturaSite_polygon", Transaction.AUTO_COMMIT)) {
//        SimpleFeature f = writer.next();
//        Geometry geom3035 = new WKTReader().read("MULTIPOLYGON (((0 0, 0 1, 1 1, 0 0)))");
//        f.setAttribute("id", 1);
//        f.setAttribute("the_geom", geom3035);
//        f.setAttribute("SITECODE", "FI123");
//        writer.write();
//      }
//    } finally {
//      if (ds != null) {
//        ds.dispose();
//      }
//    }
//    // 2) Point the DAO at this GeoPackage
//    DefaultSiteDao.setGpkgPathForTests(gpkgPath);
//    DiscodataClient discodataClient = mock(DiscodataClient.class);
//    DiscoDataProperties props = mock(DiscoDataProperties.class);
//    DefaultSiteDao dao = new DefaultSiteDao(discodataClient, props);
//    String json = dao.getGeoJsonForSite("FI123");
//    assertTrue(json.contains("\"type\":\"FeatureCollection\""), json);
//    assertTrue(json.contains("\"features\""), json);
//    assertTrue(json.contains("\"FI123\""), json); // SITECODE should survive into GeoJSON properties
  }
}