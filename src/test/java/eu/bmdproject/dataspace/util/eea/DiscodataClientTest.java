package eu.bmdproject.dataspace.util.eea;

import eu.bmdproject.dataspace.config.RestClientConfig;
import eu.bmdproject.dataspace.dao.DaoException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

  @SuppressWarnings("resource")
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

  // --- isTimeout / ResourceAccessException branches ---
  // These use a deep-stub mock RestClient to inject specific exception cause chains
  // without needing a slow real network timeout.

  @Test
  void executeQuery_throwsDaoException_withTimeoutMessage_whenSocketTimeoutExceptionInCauseChain() {
    // Given — ResourceAccessException wrapping a SocketTimeoutException (direct cause)
    RestClient mockRestClient = mockRestClientThrowing(new ResourceAccessException("timeout",
        new SocketTimeoutException("Read timed out")));
    DiscodataClient timeoutClient = new DiscodataClient(mockRestClient);

    // When / Then
    DaoException ex = assertThrows(DaoException.class,
        () -> timeoutClient.executeQuery("http://example.com/query", "SELECT 1"));
    assertThat(ex.getMessage()).contains("timed out");
  }

  @Test
  void executeQuery_throwsDaoException_withTimeoutMessage_whenTimedOutMessageInCauseChain() {
    // Given — ResourceAccessException wrapping a plain IOException whose message contains "timed out"
    // This exercises the msg.toLowerCase().contains("timed out") branch in isTimeout()
    ResourceAccessException cause = new ResourceAccessException("I/O error",
        new IOException("connection timed out"));
    RestClient mockRestClient = mockRestClientThrowing(cause);
    DiscodataClient timeoutClient = new DiscodataClient(mockRestClient);

    // When / Then
    DaoException ex = assertThrows(DaoException.class,
        () -> timeoutClient.executeQuery("http://example.com/query", "SELECT 1"));
    assertThat(ex.getMessage()).contains("timed out");
  }

  @Test
  void executeQuery_throwsDaoException_withIoMessage_whenResourceAccessExceptionIsNotTimeout() {
    // Given — ResourceAccessException whose cause chain contains no SocketTimeoutException
    // and no "timed out" message: exercises the non-timeout ResourceAccessException branch
    RestClient mockRestClient = mockRestClientThrowing(new ResourceAccessException("I/O error",
        new IOException("Connection refused")));
    DiscodataClient ioClient = new DiscodataClient(mockRestClient);
    // When / Then
    DaoException ex = assertThrows(DaoException.class,
        () -> ioClient.executeQuery("http://example.com/query", "SELECT 1"));
    assertThat(ex.getMessage()).contains("I/O");
  }

  @Test
  void executeQuery_throwsDaoException_whenRestClientExceptionIsNotResourceAccessException() {
    // Given — a RestClientException that is not a ResourceAccessException:
    // exercises the final catch (RestClientException e) branch
    RestClient mockRestClient = mockRestClientThrowing(new RestClientException("Unexpected error"));
    DiscodataClient brokenClient = new DiscodataClient(mockRestClient);

    // When / Then
    DaoException ex = assertThrows(DaoException.class,
        () -> brokenClient.executeQuery("http://example.com/query", "SELECT 1"));
    assertThat(ex.getMessage()).contains("Request failed");
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private RestClient mockRestClientThrowing(Exception exc) {
    RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class,
        RETURNS_DEEP_STUBS);
    RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class, RETURNS_DEEP_STUBS);
    RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class, RETURNS_DEEP_STUBS);
    RestClient mockRestClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
    when(mockRestClient.get()).thenReturn(uriSpec);
    when(uriSpec.uri(any(URI.class))).thenReturn(headersSpec);
    when(headersSpec.accept(MediaType.APPLICATION_JSON)).thenReturn(headersSpec);
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    when(responseSpec.body(byte[].class)).thenThrow(exc);
    return mockRestClient;
  }

  private String serverUrl() {
    return server.url("/discodata").toString();
  }
}
