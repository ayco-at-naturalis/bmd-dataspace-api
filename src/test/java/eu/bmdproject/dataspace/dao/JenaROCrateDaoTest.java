package eu.bmdproject.dataspace.dao;

import eu.bmdproject.dataspace.config.GraphProperties;
import eu.bmdproject.dataspace.model.RDFFormat;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JenaROCrateDaoTest {

  private static final String ZIP_RESOURCE =
      "/eu/bmdproject/dataspace/dao/ro-crate-test-001.zip";

  static Path JENA_DATA_DIR = createTempDir();

  static MockWebServer server;

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("app.graph.jena-data-dir", () -> JENA_DATA_DIR.toString());
    registry.add("app.graph.base-iri", () -> "urn:uuid:");
  }

  @BeforeAll
  static void startServer() throws Exception {
    server = new MockWebServer();
    server.start();
  }

  @AfterAll
  static void stopServer() throws Exception {
    server.shutdown();
    try (var walk = java.nio.file.Files.walk(JENA_DATA_DIR)) {
      walk.sorted(java.util.Comparator.reverseOrder())
          .forEach(p -> {
            try { java.nio.file.Files.deleteIfExists(p); } catch (Exception ignored) {}
          });
    }
  }

  @Autowired
  JenaROCrateDao dao;

  @Autowired
  GraphProperties graphProperties;

  @Autowired
  Dataset dataset;

  @Test
  void ingestFromUrl_persists_named_graph_and_returns_deterministic_id() throws Exception {
    byte[] zipBytes = readResourceBytes(ZIP_RESOURCE);
    server.enqueue(new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/zip")
        .setBody(new okio.Buffer().write(zipBytes)));

    URI url = server.url("/rocrate.zip").uri();

    // Compute expected ID the same way the DAO does: UUID.nameUUIDFromBytes(jsonldBytes)
    String jsonld = extractRoCrateMetadata(zipBytes);
    assertThat(jsonld).isNotNull();
    String expectedId = UUID.nameUUIDFromBytes(jsonld.getBytes(StandardCharsets.UTF_8)).toString();
    String expectedGraphUri = graphUriFor(expectedId, graphProperties.baseIri());

    ROCrateDao.IngestResult result = dao.ingestFromUrl(url);

    assertThat(result.id()).isEqualTo(expectedId);
    assertThat(result.graphUri()).isEqualTo(expectedGraphUri);

    dataset.begin(ReadWrite.READ);
    try {
      assertThat(dataset.containsNamedModel(expectedGraphUri)).isTrue();
      assertThat(dataset.getNamedModel(expectedGraphUri).size()).isGreaterThan(0);
    } finally {
      dataset.end();
    }
  }

  @Test
  void list_summary_metadata_delete_roundtrip() throws Exception {
    byte[] zipBytes = readResourceBytes(ZIP_RESOURCE);
    server.enqueue(new MockResponse().setResponseCode(200).setBody(new okio.Buffer().write(zipBytes)));
    URI url = server.url("/rocrate.zip").uri();

    String jsonld = extractRoCrateMetadata(zipBytes);
    String id = UUID.nameUUIDFromBytes(jsonld.getBytes(StandardCharsets.UTF_8)).toString();
    String graphUri = graphUriFor(id, graphProperties.baseIri());

    dao.ingestFromUrl(url);

    // list()
    assertThat(dao.list())
        .anySatisfy(ref -> {
          assertThat(ref.id()).isEqualTo(id);
          assertThat(ref.graphUri()).isEqualTo(graphUri);
        });

    // getSummary()
    var summaryOpt = dao.getSummary(id);
    assertThat(summaryOpt).isPresent();
    long countFromSummary = summaryOpt.get().tripleCount();

    // getMetadata() for each format – parse-free sanity + media type check
    assertThat(dao.getMetadata(id, RDFFormat.JSON_LD))
        .hasValueSatisfying(p -> {
          assertThat(p.graphUri()).isEqualTo(graphUri);
          assertThat(p.mediaType()).isEqualTo(MediaType.valueOf("application/ld+json").toString());
          assertThat(p.body()).isNotBlank();
        });

    assertThat(dao.getMetadata(id, RDFFormat.TURTLE))
        .hasValueSatisfying(p -> {
          assertThat(p.mediaType()).isEqualTo("text/turtle");
          assertThat(p.body()).isNotBlank();
        });

    assertThat(dao.getMetadata(id, RDFFormat.RDF_XML))
        .hasValueSatisfying(p -> {
          assertThat(p.mediaType()).isEqualTo("application/rdf+xml");
          assertThat(p.body()).isNotBlank();
        });

    // triple count should be stable across retrieval
    assertThat(countFromSummary).isGreaterThan(0);

    // delete() idempotency
    assertThat(dao.delete(id)).isTrue();
    assertThat(dao.delete(id)).isFalse();
    assertThat(dao.getSummary(id)).isEmpty();
    assertThat(dao.getMetadata(id, RDFFormat.JSON_LD)).isEmpty();
  }

  @Test
  void ingestFromUrl_throws_if_missing_ro_crate_metadata_entry() throws Exception {
    byte[] zipBytes = createZipWithoutMetadata();
    server.enqueue(new MockResponse().setResponseCode(200).setBody(new okio.Buffer().write(zipBytes)));
    URI url = server.url("/broken.zip").uri();

    assertThatThrownBy(() -> dao.ingestFromUrl(url))
        .isInstanceOf(DaoException.class)
        .hasMessageContaining("missing ro-crate-metadata.json");
  }

  // --- helpers ---

  private static byte[] readResourceBytes(String path) throws Exception {
    try (var in = JenaROCrateDaoTest.class.getResourceAsStream(path)) {
      assertThat(in).as("resource " + path).isNotNull();
      return in.readAllBytes();
    }
  }

  private static String extractRoCrateMetadata(byte[] zipBytes) throws Exception {
    try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
      ZipEntry e;
      while ((e = zin.getNextEntry()) != null) {
        if (!e.isDirectory() && e.getName().endsWith("ro-crate-metadata.json")) {
          return new String(zin.readAllBytes(), StandardCharsets.UTF_8);
        }
      }
      return null;
    }
  }

  private static String graphUriFor(String id, String base) {
    if (base.endsWith(":") || base.endsWith("/")) return base + id;
    return base + (base.startsWith("urn:") ? ":" : "/") + id;
  }

  private static byte[] createZipWithoutMetadata() throws Exception {
    // If you already prefer fixtures, just add a second zip in resources and read it instead.
    var out = new java.io.ByteArrayOutputStream();
    try (var zout = new java.util.zip.ZipOutputStream(out)) {
      zout.putNextEntry(new ZipEntry("something-else.txt"));
      zout.write("nope".getBytes(StandardCharsets.UTF_8));
      zout.closeEntry();
    }
    return out.toByteArray();
  }

  private static Path createTempDir() {
    try {
      return java.nio.file.Files.createTempDirectory("jena-tdb2-test-");
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
  }



}