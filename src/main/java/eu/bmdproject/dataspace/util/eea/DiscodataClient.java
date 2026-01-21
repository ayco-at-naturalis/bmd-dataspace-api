package eu.bmdproject.dataspace.util.eea;

import eu.bmdproject.dataspace.dao.DaoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class DiscodataClient {

  private static final Logger LOG = LoggerFactory.getLogger(DiscodataClient.class);

  private static final int TIMEOUT_MS = (int) Duration.ofSeconds(5).toMillis();

  private final RestClient restClient;

  public DiscodataClient() {
    this.restClient = RestClient.builder()
        .requestFactory(createRequestFactory())
        .build();
  }

  /**
   * Executes a DISCODATA SQL query and returns the raw JSON response body.
   *
   * <p>Throws DaoException on non-2xx responses (including 404) or on timeout.
   * Does not attempt to interpret the JSON.</p>
   */
  public byte[] executeQuery(String endpoint, String sql) {
    final URI uri = UriComponentsBuilder
        .fromUriString(endpoint)
        .queryParam("query", sql)
        .build()
        .encode(StandardCharsets.UTF_8)
        .toUri();
    LOG.debug("Executing HTTP request: {}", uri);
    try {
      return restClient.get()
          .uri(uri)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .onStatus(HttpStatusCode::isError, (_, response) -> {
            // Any HTTP error is unexpected in DAO-land for this integration at MVP stage.
            // Specifically: 404 should NOT map to our REST NotFoundException.
            int status = response.getStatusCode().value();
            throw new DaoException("Request returned HTTP " + status + " for " + uri);
          })
          .body(byte[].class);
    } catch (ResourceAccessException e) {
      // Spring commonly wraps connect/read timeouts as ResourceAccessException.
      if (isTimeout(e)) {
        throw new DaoException("Request timed out after " + (TIMEOUT_MS / 1000.0) + "s: " + uri, e);
      }
      throw new DaoException("Request failed (I/O): " + uri, e);
    } catch (RestClientException e) {
      // Covers other client-side failures (conversion issues, etc.). We treat as DAO failure.
      throw new DaoException("Request failed: " + uri, e);
    }
  }

  private static SimpleClientHttpRequestFactory createRequestFactory() {
    SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
    f.setConnectTimeout(TIMEOUT_MS);
    f.setReadTimeout(TIMEOUT_MS);
    return f;
  }

  private static boolean isTimeout(Throwable t) {
    for (Throwable cur = t; cur != null; cur = cur.getCause()) {
      if (cur instanceof SocketTimeoutException) {
        return true;
      }
      // Some stacks throw plain "timeout" IOExceptions without SocketTimeoutException;
      // keep this conservative to avoid misclassifying unrelated IO failures.
      String msg = cur.getMessage();
      if (msg != null && msg.toLowerCase().contains("timed out")) {
        return true;
      }
    }
    return false;
  }
}
