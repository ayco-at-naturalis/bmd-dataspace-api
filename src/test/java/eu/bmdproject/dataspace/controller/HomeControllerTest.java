package eu.bmdproject.dataspace.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HomeControllerTest {

  private final HomeController controller = new HomeController();

  @Test
  void version_mergesAppInfoAndVersionInfo() throws IOException {
    //When
    ResponseEntity<Map<String, Object>> response = controller.version();
    //Then
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    Map<String, Object> body = response.getBody();
    assertThat(body).containsKey("dataSpace");
    assertThat(body).containsKey("service");
    assertThat(body).containsKey("version");
    assertThat(body.get("version")).isInstanceOf(Map.class);
  }
}