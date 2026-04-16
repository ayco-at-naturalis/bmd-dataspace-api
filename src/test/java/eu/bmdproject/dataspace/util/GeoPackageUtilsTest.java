package eu.bmdproject.dataspace.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeoPackageUtilsTest {

  @TempDir
  Path tmp;

  @Test
  void listColumns_returnsColumnMetadata_forExistingTable() throws Exception {
    Path dbPath = tmp.resolve("test.gpkg");
    createTable(dbPath, "test_layer",
        "id        INTEGER PRIMARY KEY",
        "site_code TEXT NOT NULL",
        "area_ha   REAL",
        "geom      BLOB",
        "is_active INTEGER DEFAULT 1"
    );

    List<GeoPackageUtils.Column> cols = GeoPackageUtils.listColumns(dbPath, "test_layer");

    assertEquals(5, cols.size());

    GeoPackageUtils.Column id = cols.getFirst();
    assertThat("id").isEqualTo(id.name());
    assertEquals("INTEGER", id.type());
    assertEquals("1", id.pk());

    GeoPackageUtils.Column siteCode = cols.get(1);
    assertEquals("site_code", siteCode.name());
    assertEquals("TEXT", siteCode.type());
    assertEquals("1", siteCode.notnull());
    assertEquals("0", siteCode.pk());

    GeoPackageUtils.Column areaHa = cols.get(2);
    assertEquals("area_ha", areaHa.name());
    assertEquals("REAL", areaHa.type());
    assertNull(areaHa.dflt_value());

    GeoPackageUtils.Column geom = cols.get(3);
    assertEquals("geom", geom.name());
    assertEquals("BLOB", geom.type());

    GeoPackageUtils.Column isActive = cols.get(4);
    assertEquals("is_active", isActive.name());
    assertEquals("INTEGER", isActive.type());
    assertEquals("1", isActive.dflt_value());
  }

  @Test
  void listColumns_returnsEmptyList_forNonExistentTable() throws Exception {
    Path dbPath = tmp.resolve("empty.gpkg");
    // Create a valid SQLite file with no tables of interest
    createTable(dbPath, "some_other_table", "id INTEGER");
    List<GeoPackageUtils.Column> cols = GeoPackageUtils.listColumns(dbPath, "no_such_table");
    assertTrue(cols.isEmpty());
  }

  @Test
  void listColumns_handlesTableNameWithSingleQuote_withoutThrowing() throws Exception {
    // The safeTable escaping replaces ' with '' — this should not cause an exception,
    // and since no such table exists it should return an empty list.
    Path dbPath = tmp.resolve("test.gpkg");
    createTable(dbPath, "dummy", "id INTEGER");
    List<GeoPackageUtils.Column> cols = GeoPackageUtils.listColumns(dbPath, "it's_a_table");
    assertThat(cols).isEmpty();
  }

  private static void createTable(Path dbPath, String tableName, String... columnDefs) throws Exception {
    String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
    String ddl = "CREATE TABLE " + tableName + " (" + String.join(", ", columnDefs) + ")";
    try (Connection c = DriverManager.getConnection(url);
         Statement st = c.createStatement()) {
      st.execute(ddl);
    }
  }
}