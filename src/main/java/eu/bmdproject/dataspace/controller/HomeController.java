package eu.bmdproject.dataspace.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class HomeController {

  @GetMapping(value = "/version", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> version() throws IOException {
    ClassPathResource resource = new ClassPathResource("version-info.json");

    if (!resource.exists()) {
      return ResponseEntity.internalServerError()
          .body("{\"error\":\"version-info.json not found on classpath\"}");
    }

    String json = StreamUtils.copyToString(
        resource.getInputStream(),
        StandardCharsets.UTF_8
    );

    return ResponseEntity.ok(json);
  }
}
