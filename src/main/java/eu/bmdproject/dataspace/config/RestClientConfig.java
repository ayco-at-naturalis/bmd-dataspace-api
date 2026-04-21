package eu.bmdproject.dataspace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

  private static final int TIMEOUT_MS = (int) Duration.ofSeconds(5).toMillis();

  @Bean
  public RestClient restClient(){
    return RestClient.builder()
        .requestFactory(createRequestFactory(TIMEOUT_MS))
        .build();
  }

  private static SimpleClientHttpRequestFactory createRequestFactory(int timeout) {
    SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
    f.setConnectTimeout(timeout);
    f.setReadTimeout(timeout);
    return f;
  }


}

