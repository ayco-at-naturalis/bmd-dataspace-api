package eu.bmdproject.dataspace.dao;

import eu.bmdproject.dataspace.config.GraphProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Repository
public class FusekiDao implements ROCrateDao {

  private static final String RO_CRATE_FILE = "ro-crate-metadata.json";
  private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);

  private final WebClient fusekiClient;
  private final WebClient httpClient;
  private final GraphProperties graphProps;

  public FusekiDao(
      WebClient fusekiClient,
      WebClient httpClient,
      GraphProperties graphProps) {
    this.fusekiClient = fusekiClient;
    this.httpClient = httpClient;
    this.graphProps = graphProps;
  }

  @Override
  public void ingestFromUrl(URI url) {
    byte[] zipBytes = download(url);
    String jsonld = extractJsonLd(zipBytes)
        .orElseThrow(() -> new DaoException("RO-Crate missing " + RO_CRATE_FILE + " in ZIP: " + url));
    // Derive a stable content-addressed ID from the JSON-LD payload
    String id = sha256Hex(jsonld.getBytes(StandardCharsets.UTF_8));
    String graphUri = graphUriFor(id);
    // PUT JSON-LD to named graph (Graph Store Protocol)
    putToGraph(graphUri, jsonld, MediaType.valueOf("application/ld+json"));
  }

  /* -------------------- helpers -------------------- */

  private byte[] download(URI url) {
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

  private Optional<String> extractJsonLd(byte[] zipBytes) {
    try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
      ZipEntry entry;
      while ((entry = zin.getNextEntry()) != null) {
        if (!entry.isDirectory() && entry.getName().endsWith(RO_CRATE_FILE)) {
          byte[] bytes = StreamUtils.copyToByteArray(zin);
          return Optional.of(new String(bytes, StandardCharsets.UTF_8));
        }
      }
      return Optional.empty();
    } catch (IOException e) {
      throw new DaoException("Failed to read RO-Crate ZIP", e);
    }
  }

  private String graphUriFor(String id) {
    String base = graphProps.baseIri();
    if (base.endsWith(":") || base.endsWith("/")) {
      return base + id;
    }
    // Prefer ':' if base looks like a URN; otherwise '/'
    return base + (base.startsWith("urn:") ? ":" : "/") + id;
  }

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

  private void putToGraph(String graphUri, String body, MediaType contentType) {
    // Since baseUrl already points at the dataset, use the Graph Store Protocol endpoint /data
    String path = "/data";
    try {
      fusekiClient.put()
          .uri(uriBuilder -> uriBuilder
              .path(path)
              .queryParam("graph", graphUri)
              .build())
          .contentType(contentType)
          .bodyValue(body)
          .retrieve()
          .bodyToMono(Void.class)
          .timeout(HTTP_TIMEOUT)
          .block();
    } catch (Exception e) {
      throw new DaoException("Failed to upsert graph " + graphUri, e);
    }
  }
}
