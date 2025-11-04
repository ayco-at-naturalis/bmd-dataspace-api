package eu.bmdproject.dataspace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  @Bean
  public WebClient fusekiWebClient(FusekiProperties props) {
    // Allow deserializing large RO-Crate payloads by increasing WebClient’s in-memory buffer to 16 MB
    ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
        .build();

    return WebClient.builder()
        .baseUrl(props.baseUrl() + "/" + props.dataset())
        .exchangeStrategies(strategies)
        .build();
  }
}
