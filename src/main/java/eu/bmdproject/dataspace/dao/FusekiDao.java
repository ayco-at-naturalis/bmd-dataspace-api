package eu.bmdproject.dataspace.dao;

import eu.bmdproject.dataspace.config.GraphProperties;
import org.klojang.check.Check;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.klojang.check.CommonChecks.notNull;

@Repository
public class FusekiDao implements ROCrateDao {

  private static final String RO_CRATE_FILE = "ro-crate-metadata.json";
  private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);

  private static final Logger log = LoggerFactory.getLogger(FusekiDao.class);


  private final WebClient fusekiClient;
  private final WebClient httpClient;
  private final GraphProperties graphProps;

  public FusekiDao(
      @Qualifier("fusekiClient") WebClient fusekiClient,
      @Qualifier("httpClient") WebClient httpClient,
      GraphProperties graphProps
  ) {
    this.fusekiClient = fusekiClient;
    this.httpClient = httpClient;
    this.graphProps = graphProps;
  }

  @Override
  public void ingestFromUrl(URI url) {
    byte[] zipBytes = download(url);
    String jsonld = getRoCrateMetadata(zipBytes);
    Check.that(jsonld)
        .is(notNull(), () -> new DaoException("RO-Crate missing " + RO_CRATE_FILE + " in ZIP: " + url));
    String id = UUID.nameUUIDFromBytes(jsonld.getBytes()).toString();
    String graphUri = graphUriFor(id);
    saveGraph(graphUri, jsonld, MediaType.valueOf("application/ld+json"));
  }

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

  private String graphUriFor(String id) {
    String base = graphProps.baseIri();
    if (base.endsWith(":") || base.endsWith("/")) {
      return base + id;
    }
    return base + (base.startsWith("urn:") ? ":" : "/") + id;
  }

  private void saveGraph(String graphUri, String body, MediaType contentType) {
    String path = "/data"; // dataset already in base-url
    try {
      log.debug("Saving ro-crate-metadata.json to triple store. Graph URI: {}", graphUri);
      fusekiClient.put()
          .uri(b -> b.path(path).queryParam("graph", graphUri).build())
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

  @SuppressWarnings({"unused"}) // might want to use it for duplicate detection
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
