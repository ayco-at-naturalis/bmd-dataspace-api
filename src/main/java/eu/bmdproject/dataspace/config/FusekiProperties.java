package eu.bmdproject.dataspace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection settings for Apache Fuseki.
 * <p>
 * Prefix: {@code app.fuseki}
 * <pre>
 * app:
 *   fuseki:
 *     base-url: http://localhost:3030/rocrate
 *     username: ""
 *     password: ""
 * </pre>
 */
@ConfigurationProperties(prefix = "app.fuseki")
public record FusekiProperties(
    String baseUrl,
    String username,
    String password
) {}
