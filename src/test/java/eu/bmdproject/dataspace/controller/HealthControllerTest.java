package eu.bmdproject.dataspace.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthControllerTest {

  @LocalServerPort
  int port;

  @Test
  void healthReturnsOkStub() {
    var client = RestClient.builder().baseUrl("http://localhost:" + port).build();
    ResponseEntity<String> resp = client.get().uri("/api/health").retrieve().toEntity(String.class);
    assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(resp.getBody()).contains("\"status\":\"ok\"");
  }
}