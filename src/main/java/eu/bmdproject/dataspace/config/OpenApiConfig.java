package eu.bmdproject.dataspace.config;

import eu.bmdproject.dataspace.exception.BmdRuntimeException;
import eu.bmdproject.dataspace.util.json.JsonUtil;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Map;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() throws IOException {
    ClassPathResource versionResource = new ClassPathResource("version-info.json");
    String version;
    if (!versionResource.exists()) {
      // We are running in IntelliJ / the CI/CD build scripts have not run yet
      version = "unknown";
    } else {
      Map<String, Object> versionInfo = JsonUtil.toMap(versionResource.getInputStream());
      version = (String) versionInfo.get("api");
    }
    return new OpenAPI()
        .info(new Info()
            .title("BMD Dataspace API")
            .version(version)
            .description("REST API for interacting with the BMD Dataspace"));
  }
}
