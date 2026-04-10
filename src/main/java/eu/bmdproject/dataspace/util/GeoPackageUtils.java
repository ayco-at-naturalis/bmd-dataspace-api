package eu.bmdproject.dataspace.util;

import org.klojang.check.Check;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.klojang.check.CommonChecks.notNull;

public final class GeoPackageUtils {

  private GeoPackageUtils() {
  }

  public record Column(
      String cid,
      String name,
      String type,
      String notnull,
      String dflt_value,
      String pk
  ) {}

  /**
   * Returns column metadata (name + declared datatype) for a GeoPackage layer (SQLite table). Uses SQLite
   * PRAGMA table_info.
   */
  public static List<Column> listColumns(Path gpkgPath, String tableName) {
    Check.that(gpkgPath).is(notNull(), "gpkgPath");
    Check.that(tableName).is(notNull(), "tableName");

    // PRAGMA doesn't accept a bind parameter for the identifier in all drivers,
    // so we must embed the table name. We do a conservative quote escape.
    String safeTable = tableName.replace("'", "''");
    String sql = "PRAGMA table_info('" + safeTable + "')";

    String url = "jdbc:sqlite:" + gpkgPath.toAbsolutePath();

    try (Connection c = DriverManager.getConnection(url);
         PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

      List<Column> cols = new ArrayList<>();
      while (rs.next()) {
        // PRAGMA table_info returns: cid, name, type, notnull, dflt_value, pk
        cols.add(new Column(
            rs.getString("cid"),
            rs.getString("name"),
            rs.getString("type"), // may be empty/null in SQLite if not declared
            rs.getString("notnull"),
            rs.getString("dflt_value"),
            rs.getString("pk")
        ));
      }
      return cols;

    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to read GeoPackage columns for table \"" + tableName + "\" from " + gpkgPath, e);
    }
  }
}