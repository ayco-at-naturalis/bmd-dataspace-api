package eu.bmdproject.dataspace.dao;

import eu.bmdproject.dataspace.config.GraphProperties;
import eu.bmdproject.dataspace.model.RDFFormat;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.klojang.check.Check;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.klojang.check.CommonChecks.notNull;

@Repository
public class JenaROCrateDao implements ROCrateDao {

  private static final Logger LOG = LoggerFactory.getLogger(JenaROCrateDao.class);

  private static final String RO_CRATE_FILE = "ro-crate-metadata.json";
  private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);


  private final WebClient httpClient;
  private final GraphProperties graphProps;
  private final Dataset dataset;

  public JenaROCrateDao(
      @Qualifier("httpClient") WebClient httpClient,
      GraphProperties graphProps,
      Dataset dataset
  ) {
    this.httpClient = httpClient;
    this.graphProps = graphProps;
    this.dataset = dataset;
  }

  @Override
  public IngestResult ingestFromUrl(URI url) {
    LOG.debug("Ingesting RO-CRATE from URL {}", url);
    byte[] zipBytes = download(url);
    String jsonld = getRoCrateMetadata(zipBytes);
    Check.that(jsonld).is(notNull(),
        () -> new DaoException("RO-Crate missing " + RO_CRATE_FILE + " in ZIP: " + url));
    byte[] bytes = jsonld.getBytes(StandardCharsets.UTF_8);
    String id = UUID.nameUUIDFromBytes(bytes).toString();
    // Future use, when using audit database:
    String sha256 = sha256Hex(bytes);
    LOG.debug("---> ID: {}", id);
    LOG.debug("---> SHA-256: {}", sha256);
    String graphUri = graphUriFor(id);
    saveGraph(graphUri, jsonld, MediaType.valueOf("application/ld+json"));
    return new IngestResult(id, graphUri);
  }

  @Override
  public List<CrateRef> list() {
    dataset.begin(ReadWrite.READ);
    try {
      List<CrateRef> out = new ArrayList<>();
      Iterator<String> names = dataset.listNames();
      while (names.hasNext()) {
        String graphUri = names.next();
        String id = idFromGraphUri(graphUri);
        out.add(new CrateRef(id, graphUri));
      }
      return out;
    } catch (Exception e) {
      throw new DaoException("Failed to list RO-Crate graphs", e);
    } finally {
      dataset.end();
    }
  }

  @Override
  public Optional<CrateSummary> getSummary(String id) {
    String graphUri = graphUriFor(id);
    dataset.begin(ReadWrite.READ);
    try {
      if (!dataset.containsNamedModel(graphUri)) {
        return Optional.empty();
      }
      Model model = dataset.getNamedModel(graphUri);
      long tripleCount = model.size();
      return Optional.of(new CrateSummary(id, graphUri, tripleCount));
    } catch (Exception e) {
      throw new DaoException("Failed to read summary for graph " + graphUri, e);
    } finally {
      dataset.end();
    }
  }

  @Override
  public Optional<GraphPayload> getMetadata(String id, RDFFormat format) {
    LOG.debug("Retrieving metadata for graph {} (format={})", id, format);
    String graphUri = graphUriFor(id);
    dataset.begin(ReadWrite.READ);
    try {
      if (!dataset.containsNamedModel(graphUri)) {
        return Optional.empty();
      }
      Model model = dataset.getNamedModel(graphUri);
      Serialized serialized = serialize(model, format);
      return Optional.of(new GraphPayload(graphUri, serialized.body(), serialized.mediaType()));
    } catch (Exception e) {
      throw new DaoException("Failed to serialize metadata for graph " + graphUri, e);
    } finally {
      dataset.end();
    }
  }

  @Override
  public boolean delete(String id) {
     String graphUri = graphUriFor(id);
    LOG.debug("Graoh URI for RO-Crate: \"{}\"", graphUri);
    dataset.begin(ReadWrite.WRITE);
    try {
      if (!dataset.containsNamedModel(graphUri)) {
        LOG.debug("Graph {} not found. Nothing deleted.", id);
        dataset.abort();
        return false;
      }
      dataset.removeNamedModel(graphUri);
      LOG.debug("Successfully deleted graph with ID {}", id);
      dataset.commit();
      return true;
    } catch (Exception e) {
      dataset.abort();
      throw new DaoException("Failed to delete graph " + graphUri, e);
    } finally {
      dataset.end();
    }
  }

  private byte[] download(URI url) {
    LOG.debug("Downloading RO-Crate from {}", url);
    try {
      return httpClient.get()
          .uri(url)
          .retrieve()
          .bodyToMono(byte[].class)
          .timeout(HTTP_TIMEOUT)
          .block();
    } catch (Exception e) {
      throw new DaoException("Failed to download RO-Crate ZIP: " + url, e);
    }
  }

  private String graphUriFor(String id) {
    String base = graphProps.baseIri();
    if (base.endsWith(":") || base.endsWith("/")) {
      return base + id;
    }
    return base + (base.startsWith("urn:") ? ":" : "/") + id;
  }

  /**
   * Best-effort reverse mapping for listing. If the graph URI is not under the configured base IRI, we return
   * the full URI as the "id". This keeps the MVP usable even if someone stored graphs outside the base.
   */
  private String idFromGraphUri(String graphUri) {
    String base = graphProps.baseIri();
    String normalizedBase = (base.endsWith(":") || base.endsWith("/"))
        ? base
        : base + (base.startsWith("urn:") ? ":" : "/");
    if (graphUri != null && graphUri.startsWith(normalizedBase) && graphUri.length() > normalizedBase.length()) {
      return graphUri.substring(normalizedBase.length());
    }
    return graphUri;
  }

  private void saveGraph(String graphUri, String body, MediaType contentType) {
    if (!MediaType.valueOf("application/ld+json").isCompatibleWith(contentType)) {
      throw new DaoException("Unsupported content type for RO-Crate metadata: " + contentType);
    }
    LOG.debug("Saving ro-crate-metadata.json to Jena Dataset. Graph URI: {}", graphUri);
    Model model = ModelFactory.createDefaultModel();
    try (InputStream in = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))) {
      RDFDataMgr.read(model, in, Lang.JSONLD);
    } catch (Exception e) {
      throw new DaoException("Failed to parse RO-Crate JSON-LD", e);
    }
    dataset.begin(ReadWrite.WRITE);
    try {
      if (dataset.containsNamedModel(graphUri)) {
        dataset.removeNamedModel(graphUri);
      }
      dataset.addNamedModel(graphUri, model);
      dataset.commit();
    } catch (Exception e) {
      dataset.abort();
      throw new DaoException("Failed to upsert graph " + graphUri + " into Jena Dataset", e);
    } finally {
      dataset.end();
    }
  }

  private static String getRoCrateMetadata(byte[] zipBytes) {
    try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
      ZipEntry entry;
      while ((entry = zin.getNextEntry()) != null) {
        if (!entry.isDirectory() && entry.getName().endsWith(RO_CRATE_FILE)) {
          byte[] bytes = StreamUtils.copyToByteArray(zin);
          return new String(bytes, StandardCharsets.UTF_8);
        }
      }
      return null;
    } catch (IOException e) {
      throw new DaoException("Failed to read RO-Crate zip file", e);
    }
  }

  private static Serialized serialize(Model model, RDFFormat format) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Lang lang;
      String mediaType;
      switch (format) {
        case TURTLE -> {
          lang = Lang.TURTLE;
          mediaType = "text/turtle";
        }
        case JSON_LD -> {
          lang = Lang.JSONLD;
          mediaType = "application/ld+json";
        }
        case RDF_XML -> {
          lang = Lang.RDFXML;
          mediaType = "application/rdf+xml";
        }
        default -> throw new IllegalArgumentException("Unsupported graph format: " + format);
      }
      RDFDataMgr.write(out, model, lang);
      return new Serialized(out.toString(StandardCharsets.UTF_8), mediaType);
    } catch (Exception e) {
      throw new DaoException("Failed to serialize graph", e);
    }
  }

  private record Serialized(String body, String mediaType) {}

  private static String sha256Hex(byte[] data) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(data);
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      throw new DaoException("Unable to compute SHA-256", e);
    }
  }

}
