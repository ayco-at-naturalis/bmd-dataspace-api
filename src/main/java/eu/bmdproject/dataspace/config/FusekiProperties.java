package eu.bmdproject.dataspace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.fuseki")
public record FusekiProperties(
    String baseUrl,
    String username,
    String password
) {}
