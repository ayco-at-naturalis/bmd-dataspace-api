package eu.bmdproject.dataspace.controller;

import eu.bmdproject.dataspace.util.json.JsonUtil;
import org.klojang.check.Check;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.klojang.check.CommonChecks.yes;
import static org.klojang.check.CommonExceptions.illegalState;

@RestController
public class HomeController {

  @GetMapping(value = "/version", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> version() throws IOException {
    ClassPathResource versionResource = new ClassPathResource("version-info.json");
    Check.that(versionResource.exists()).is(yes(), illegalState("version-info.json resource not found"));
    ClassPathResource appResource = new ClassPathResource("/app-info.json");
    Map<String, Object> versionInfo = JsonUtil.toMap(versionResource.getInputStream());
    Map<String, Object> appInfo = new HashMap<>(JsonUtil.toMap(appResource.getInputStream()));
    appInfo.put("version", versionInfo);
    return ResponseEntity.ok(appInfo);
  }
}
