package eu.bmdproject.dataspace.util.eea;

import eu.bmdproject.dataspace.dao.DaoException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DiscodataClientTest {

  private MockWebServer server;
  private DiscodataClient client;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    client = new DiscodataClient(500);
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  void executeQuery_returnsResponseBody_on2xx() throws Exception {
    byte[] expected = "{\"results\":[]}".getBytes();
    server.enqueue(new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(new okio.Buffer().write(expected)));
    byte[] actual = client.executeQuery(serverUrl(), "SELECT * FROM sites");
    assertArrayEquals(expected, actual);
  }

  @Test
  void executeQuery_appendsSqlAsQueryParam() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
    String sql = "SELECT sitecode FROM natura2000sites WHERE sitecode = 'NL001'";
    client.executeQuery(serverUrl(), sql);
    RecordedRequest req = server.takeRequest(10, TimeUnit.MILLISECONDS);
    assertNotNull(req);
    String requestUrl = req.getPath();
    assertTrue(requestUrl.contains("query="), "Expected 'query' param in: " + requestUrl);
    assertTrue(requestUrl.contains("NL001"), "Expected SQL content in URL: " + requestUrl);
  }

  @Test
  void executeQuery_sendsAcceptApplicationJson() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
    client.executeQuery(serverUrl(), "SELECT 1");
    RecordedRequest req = server.takeRequest(10, TimeUnit.MILLISECONDS);
    assertNotNull(req);
    String accept = req.getHeader("Accept");
    assertNotNull(accept, "Accept header should be set");
    assertTrue(accept.contains("application/json"), "Expected application/json in Accept: " + accept);
  }

  @Test
  void executeQuery_throwsDaoException_on404() {
    server.enqueue(new MockResponse().setResponseCode(404).setBody("Not Found"));
    DaoException ex = assertThrows(DaoException.class, () -> client.executeQuery(serverUrl(), "SELECT 1"));
    assertTrue(ex.getMessage().contains("404"), "Message should include the status code: " + ex.getMessage());
  }

  @Test
  void executeQuery_throwsDaoException_on500() {
    server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));
    DaoException ex = assertThrows(DaoException.class, () -> client.executeQuery(serverUrl(), "SELECT 1"));
    assertTrue(ex.getMessage().contains("500"), "Message should include the status code: " + ex.getMessage());
  }

  @Test
  void executeQuery_throwsDaoException_on503() {
    server.enqueue(new MockResponse().setResponseCode(503));
    DaoException ex = assertThrows(DaoException.class, () -> client.executeQuery(serverUrl(), "SELECT 1"));
    assertTrue(ex.getMessage().contains("503"));
  }

  @Test
  void executeQuery_throwsDaoException_withTimeoutMessage_onReadTimeout() {
    // Delay the response body longer than the 500 Ms client timeout.
    // We use a short delay in tests to keep the suite fast; the point is the
    // exception type and message, not the exact duration.
    server.enqueue(new MockResponse()
        .setResponseCode(200)
        .setBodyDelay(1, TimeUnit.SECONDS)
        .setHeadersDelay(1, TimeUnit.SECONDS)
        .setBody("{}"));
    DaoException ex = assertThrows(DaoException.class, () -> client.executeQuery(serverUrl(), "SELECT 1"));
    assertTrue(ex.getMessage().contains("timed out") || ex.getMessage().contains("I/O"),
        "Expected timeout or I/O message, got: " + ex.getMessage());
    // The cause chain must include a ResourceAccessException (Spring) or SocketTimeoutException
    assertNotNull(ex.getCause());
  }

  private String serverUrl() {
    return server.url("/discodata").toString();
  }
}
