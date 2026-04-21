package eu.bmdproject.dataspace.util.eea;

import eu.bmdproject.dataspace.dao.DaoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Responsible for queries against EEA's <a href="https://discodata.eea.europa.eu/">Discodata</a> API. This
 * class essentially is a {@link RestClient} wrapper specifically for the Discodata API. Note that, except for
 * a bit of data normalization, the BMD Dataspace API here simply functions as a pass-through for data coming
 * back from the Discodata API (BISE and EUNIS data). We are completely reliant on _that_ API functioning
 * properly, and we need to distinguish between "something going wrong there" and more fine-grained error
 * conditions in our own handling of the request.
 */
@Component
public class DiscodataClient {

  private static final Logger LOG = LoggerFactory.getLogger(DiscodataClient.class);

  private final RestClient restClient;

  public DiscodataClient(RestClient restClient) {
    this.restClient = restClient;
  }
  /**
   * Executes a Discodata query and returns the raw JSON response body. Throws DaoException on any non-2xx
   * response (including 404) or on timeout. Does not attempt to interpret the JSON.
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
            int status = response.getStatusCode().value();
            throw new DaoException("Request returned HTTP " + status + " for " + uri);
          })
          .body(byte[].class);
    } catch (ResourceAccessException e) {
      if (isTimeout(e)) {
        throw new DaoException("Request timed out: " + uri, e);
      }
      throw new DaoException("Request failed (I/O): " + uri, e);
    } catch (RestClientException e) {
      throw new DaoException("Request failed: " + uri, e);
    }
  }

  private static boolean isTimeout(Throwable t) {
    // Spring commonly wraps connect/read timeouts as ResourceAccessException. Also,
    // some stacks throw plain "timeout" IOExceptions without SocketTimeoutException;
    // keep this conservative to avoid misclassifying unrelated IO failures.
    for (Throwable cur = t; cur != null; cur = cur.getCause()) {
      if (cur instanceof SocketTimeoutException) {
        return true;
      }
      String msg = cur.getMessage();
      if (msg != null && msg.toLowerCase().contains("timed out")) {
        return true;
      }
    }
    return false;
  }
}
