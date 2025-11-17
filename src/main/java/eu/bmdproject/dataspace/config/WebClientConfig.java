package eu.bmdproject.dataspace.config;

import reactor.netty.http.client.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties({FusekiProperties.class, GraphProperties.class})
public class WebClientConfig {

  @Bean
  public WebClient fusekiClient(FusekiProperties props) {
    HttpClient http = HttpClient.create().followRedirect(true);
    ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
        .build();

    WebClient.Builder b = WebClient.builder()
        .baseUrl(props.baseUrl())
        .clientConnector(new ReactorClientHttpConnector(http))
        .exchangeStrategies(strategies);

    if (props.username() != null && !props.username().isBlank()) {
      b = b.filter(ExchangeFilterFunctions.basicAuthentication(
          props.username(), props.password() == null ? "" : props.password()));
    }
    return b.build();
  }

  @Bean
  @Primary
  public WebClient httpClient() {
    HttpClient http = HttpClient.create().followRedirect(true);
    ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
        .build();

    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(http))
        .exchangeStrategies(strategies)
        .build();
  }
}
