package eu.bmdproject.dataspace.dao;

import eu.bmdproject.dataspace.config.DiscoDataProperties;
import eu.bmdproject.dataspace.util.eea.DiscodataClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ByteOrderValues;
import org.locationtech.jts.io.WKBWriter;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultSiteDaoGeoJsonTest {

  @TempDir
  Path tmp;

  @Test
  void getGeoJsonForSite_returnsFeatureCollectionJson() throws Exception {
    // Given
    Path gpkgPath = tmp.resolve("test.gpkg");
    DiscoDataProperties config = mock(DiscoDataProperties.class);
    when(config.geopackagePath()).thenReturn(gpkgPath);
    createMinimalGeoPackage(gpkgPath, "FI123");
    DefaultSiteDao dao = new DefaultSiteDao(mock(DiscodataClient.class), config);

    // When
    String json = dao.getGeoJsonForSite("FI123");

    // Then
    assertThat(json).contains("\"type\":\"FeatureCollection\"");
    assertThat(json).contains("\"features\"");
    assertThat(json).contains("FI123");
  }

  /**
   * Builds a minimal but spec-compliant GeoPackage (SQLite file) containing one MultiPolygon
   * feature in EPSG:3035 in the NaturaSite_polygon layer. Uses only the sqlite-jdbc driver —
   * no GeoTools write API involved.
   */
  private static void createMinimalGeoPackage(Path gpkgPath, String siteCode) throws Exception {
    String url = "jdbc:sqlite:" + gpkgPath.toAbsolutePath();
    try (Connection c = DriverManager.getConnection(url)) {
      c.setAutoCommit(false);

      try (var st = c.createStatement()) {

        st.execute("""
            CREATE TABLE gpkg_spatial_ref_sys (
              srs_name TEXT NOT NULL,
              srs_id INTEGER NOT NULL PRIMARY KEY,
              organization TEXT NOT NULL,
              organization_coordsys_id INTEGER NOT NULL,
              definition TEXT NOT NULL,
              description TEXT
            )""");

        st.execute("""
            INSERT INTO gpkg_spatial_ref_sys
              (srs_name, srs_id, organization, organization_coordsys_id, definition, description)
            VALUES
              ('EPSG:3035', 3035, 'EPSG', 3035, 'undefined', 'ETRS89-LAEA')""");

        st.execute("""
            CREATE TABLE gpkg_contents (
              table_name TEXT NOT NULL PRIMARY KEY,
              data_type TEXT NOT NULL,
              identifier TEXT,
              description TEXT,
              last_change DATETIME,
              min_x REAL, min_y REAL, max_x REAL, max_y REAL,
              srs_id INTEGER
            )""");

        st.execute("""
            INSERT INTO gpkg_contents
              (table_name, data_type, identifier, last_change, srs_id)
            VALUES
              ('NaturaSite_polygon', 'features', 'NaturaSite_polygon', '2024-01-01T00:00:00.000Z', 3035)""");

        st.execute("""
            CREATE TABLE gpkg_geometry_columns (
              table_name TEXT NOT NULL,
              column_name TEXT NOT NULL,
              geometry_type_name TEXT NOT NULL,
              srs_id INTEGER NOT NULL,
              z TINYINT NOT NULL,
              m TINYINT NOT NULL,
              CONSTRAINT pk_geom_cols PRIMARY KEY (table_name, column_name)
            )""");

        st.execute("""
            INSERT INTO gpkg_geometry_columns
              (table_name, column_name, geometry_type_name, srs_id, z, m)
            VALUES
              ('NaturaSite_polygon', 'the_geom', 'MULTIPOLYGON', 3035, 0, 0)""");

        st.execute("""
            CREATE TABLE NaturaSite_polygon (
              fid INTEGER PRIMARY KEY AUTOINCREMENT,
              the_geom BLOB,
              SITECODE TEXT
            )""");
      }

      byte[] gpkgBlob = toGeoPackageBlob(makeMultiPolygon(), 3035);
      try (PreparedStatement ps = c.prepareStatement(
          "INSERT INTO NaturaSite_polygon (the_geom, SITECODE) VALUES (?, ?)")) {
        ps.setBytes(1, gpkgBlob);
        ps.setString(2, siteCode);
        ps.executeUpdate();
      }

      c.commit();
    }
  }

  private static MultiPolygon makeMultiPolygon() {
    GeometryFactory gf = new GeometryFactory();
    Coordinate[] ring = {
        new Coordinate(3000000, 1000000),
        new Coordinate(3000000, 1100000),
        new Coordinate(3100000, 1100000),
        new Coordinate(3100000, 1000000),
        new Coordinate(3000000, 1000000)
    };
    Polygon polygon = gf.createPolygon(ring);
    return gf.createMultiPolygon(new Polygon[]{polygon});
  }

  /**
   * Wraps a JTS geometry in a GeoPackage Standard Binary Header (GPB).
   * Spec: OGC 12-128r18, Annex B.
   *
   * Header layout (8 bytes):
   *   [0-1]  magic: 0x47 0x50  ("GP")
   *   [2]    version: 0x00
   *   [3]    flags:  bit 0 = byte order (1=little-endian), bits 1-3 = envelope type (0=none)
   *   [4-7]  srs_id: int32 (little-endian)
   * Followed immediately by ISO WKB of the geometry.
   */
  private static byte[] toGeoPackageBlob(Geometry geom, int srsId) throws Exception {
    byte[] wkb = new WKBWriter(2, ByteOrderValues.LITTLE_ENDIAN).write(geom);
    ByteArrayOutputStream baos = new ByteArrayOutputStream(8 + wkb.length);
    baos.write(0x47); // 'G'
    baos.write(0x50); // 'P'
    baos.write(0x00); // version
    baos.write(0x01); // flags: little-endian, no envelope
    ByteBuffer srsBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(srsId);
    baos.write(srsBytes.array());
    baos.write(wkb);
    return baos.toByteArray();
  }
}
