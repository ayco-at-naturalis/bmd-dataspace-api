package eu.bmdproject.dataspace.dao;

import eu.bmdproject.dataspace.config.DiscoDataProperties;
import eu.bmdproject.dataspace.util.eea.DiscodataClient;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.data.DataUtilities;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.referencing.CRS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DefaultSiteDaoGeoJsonTest {

  @TempDir Path tmp;

  @SuppressWarnings({"unchecked"})
  @Test
  void getGeoJsonForSite_returnsFeatureCollectionJson_evenWithoutRealGpkg() throws Exception {
    // Make a dummy file to satisfy Check.file()
    Path gpkg = tmp.resolve("dummy.gpkg");
    gpkg.toFile().createNewFile();
    setStaticFinalPath(DefaultSiteDao.class, "GPKG_PATH", gpkg);

    DiscodataClient discodataClient = mock(DiscodataClient.class);
    DiscoDataProperties props = mock(DiscoDataProperties.class);
    DefaultSiteDao dao = new DefaultSiteDao(discodataClient, props);

    // Build a real feature collection (1 feature)
    var sft = DataUtilities.createType("NaturaSite_polygon",
        "the_geom:MultiPolygon:srid=3035," +
            "SITECODE:String");

    Geometry geom = new WKTReader().read("MULTIPOLYGON (((0 0, 0 1, 1 1, 0 0)))");

    var fb = new SimpleFeatureBuilder(sft);
    fb.add(geom);
    fb.add("FI123");
    var feature = fb.buildFeature("id-1");

    DefaultFeatureCollection fc = new DefaultFeatureCollection(null, sft);
    fc.add(feature);

    DataStore store = mock(DataStore.class);
    SimpleFeatureSource source = mock(SimpleFeatureSource.class);

    when(store.getFeatureSource("NaturaSite_polygon")).thenReturn(source);
    when(source.getSchema()).thenReturn(sft);
    when(source.getFeatures((Query) any())).thenReturn(fc);

    // Mock datastore open + CRS transform to identity
    try (MockedStatic<DataStoreFinder> dsf = mockStatic(DataStoreFinder.class);
         MockedStatic<CRS> crs = mockStatic(CRS.class)) {

      dsf.when(() -> DataStoreFinder.getDataStore(any(Map.class))).thenReturn(store);

      CoordinateReferenceSystem src = mock(CoordinateReferenceSystem.class);
      CoordinateReferenceSystem dst = mock(CoordinateReferenceSystem.class);
      MathTransform tx = mock(MathTransform.class); // identity-ish; JTS.transform will accept

      crs.when(() -> CRS.decode("EPSG:3035", true)).thenReturn(src);
      crs.when(() -> CRS.decode("EPSG:4326", true)).thenReturn(dst);
      crs.when(() -> CRS.findMathTransform(src, dst, true)).thenReturn(tx);

      // We’re not asserting reprojection numerics here; just “we emit GeoJSON FeatureCollection”
      String json = dao.getGeoJsonForSite("FI123");

      assertTrue(json.contains("\"type\":\"FeatureCollection\""));
      assertTrue(json.contains("\"features\""));
      verify(store).dispose();
    }
  }

  // --- reflection helper ---
  private static void setStaticFinalPath(Class<?> clazz, String fieldName, Path value) throws Exception {
    Field f = clazz.getDeclaredField(fieldName);
    f.setAccessible(true);

    Field modifiers = Field.class.getDeclaredField("modifiers");
    modifiers.setAccessible(true);
    modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);

    f.set(null, value);
  }
}