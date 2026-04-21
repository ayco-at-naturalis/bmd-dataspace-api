package eu.bmdproject.dataspace.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeoPackageUtilsTest {

  @TempDir
  Path tmp;

  @Test
  void listColumns_returnsColumnMetadata_forExistingTable() throws Exception {
    // Given
    Path dbPath = tmp.resolve("test.gpkg");
    createTable(dbPath, "test_layer",
        "id        INTEGER PRIMARY KEY",
        "site_code TEXT NOT NULL",
        "area_ha   REAL",
        "geom      BLOB",
        "is_active INTEGER DEFAULT 1"
    );

    // When
    List<GeoPackageUtils.Column> cols = GeoPackageUtils.listColumns(dbPath, "test_layer");

    // Then
    assertThat(cols).hasSize(5);

    GeoPackageUtils.Column id = cols.getFirst();
    assertThat(id.name()).isEqualTo("id");
    assertThat(id.type()).isEqualTo("INTEGER");
    assertThat(id.pk()).isEqualTo("1");

    GeoPackageUtils.Column siteCode = cols.get(1);
    assertThat(siteCode.name()).isEqualTo("site_code");
    assertThat(siteCode.type()).isEqualTo("TEXT");
    assertThat(siteCode.notnull()).isEqualTo("1");
    assertThat(siteCode.pk()).isEqualTo("0");

    GeoPackageUtils.Column areaHa = cols.get(2);
    assertThat(areaHa.name()).isEqualTo("area_ha");
    assertThat(areaHa.type()).isEqualTo("REAL");
    assertThat(areaHa.dflt_value()).isNull();

    GeoPackageUtils.Column geom = cols.get(3);
    assertThat(geom.name()).isEqualTo("geom");
    assertThat(geom.type()).isEqualTo("BLOB");

    GeoPackageUtils.Column isActive = cols.get(4);
    assertThat(isActive.name()).isEqualTo("is_active");
    assertThat(isActive.type()).isEqualTo("INTEGER");
    assertThat(isActive.dflt_value()).isEqualTo("1");
  }

  @Test
  void listColumns_returnsEmptyList_forNonExistentTable() throws Exception {
    // Given
    Path dbPath = tmp.resolve("empty.gpkg");
    createTable(dbPath, "some_other_table", "id INTEGER");

    // When
    List<GeoPackageUtils.Column> cols = GeoPackageUtils.listColumns(dbPath, "no_such_table");

    // Then
    assertThat(cols).isEmpty();
  }

  @Test
  void listColumns_handlesTableNameWithSingleQuote_withoutThrowing() throws Exception {
    // Given
    Path dbPath = tmp.resolve("test.gpkg");
    createTable(dbPath, "dummy", "id INTEGER");

    // When
    List<GeoPackageUtils.Column> cols = GeoPackageUtils.listColumns(dbPath, "it's_a_table");

    // Then
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