package eu.bmdproject.dataspace.controller;

import eu.bmdproject.dataspace.exception.BmdRuntimeException;
import eu.bmdproject.dataspace.util.json.JsonUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

  @GetMapping(value = "/version", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> version() throws IOException {
    ClassPathResource versionResource = new ClassPathResource("version-info.json");
    if (!versionResource.exists()) {
      throw new BmdRuntimeException("version-info.json not found");
    }
    ClassPathResource appResource = new ClassPathResource("/app-info.json");
    Map<String, Object> versionInfo = JsonUtil.toMap(versionResource.getInputStream());
    Map<String, Object> appInfo = new HashMap<>(JsonUtil.toMap(appResource.getInputStream()));
    appInfo.put("version", versionInfo);
    return ResponseEntity.ok(appInfo);
  }
}
