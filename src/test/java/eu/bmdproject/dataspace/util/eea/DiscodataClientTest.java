package eu.bmdproject.dataspace.util.eea;

import eu.bmdproject.dataspace.config.RestClientConfig;
import eu.bmdproject.dataspace.dao.DaoException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class DiscodataClientTest {

  private MockWebServer server;
  private DiscodataClient client;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    client = new DiscodataClient(new RestClientConfig().restClient());
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  void executeQuery_returnsResponseBody_on2xx() {
    byte[] expected = "{\"results\":[]}".getBytes();
    server.enqueue(new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(new okio.Buffer().write(expected)));
    byte[] actual = client.executeQuery(serverUrl(), "SELECT * FROM sites");
    assertThat(expected).isEqualTo(actual);
  }

  @Test
  void executeQuery_appendsSqlAsQueryParam() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
    String sql = "SELECT sitecode FROM natura2000sites WHERE sitecode = 'NL001'";
    client.executeQuery(serverUrl(), sql);
    RecordedRequest req = server.takeRequest(10, TimeUnit.MILLISECONDS);
    assertNotNull(req);
    String requestUrl = req.getPath();
    assertThat(requestUrl).contains("query=");
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

  private String serverUrl() {
    return server.url("/discodata").toString();
  }
}
