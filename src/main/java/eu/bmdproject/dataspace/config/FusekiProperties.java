package eu.bmdproject.dataspace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for connecting to the Apache Fuseki triple store.
 * <p>
 * These values are typically defined in {@code application.yml} or {@code application.properties} under the
 * {@code fuseki.*} namespace, for example:
 *
 * <pre>
 * fuseki:
 *   base-url: http://localhost:3030
 *   dataset: bmd
 * </pre>
 * <p>
 * The {@link #baseUrl()} and {@link #dataset()} values can then be injected wherever needed (e.g. when
 * constructing the Fuseki client or building SPARQL endpoints).
 *
 * @param baseUrl
 *     the base URL of the Fuseki server, excluding the dataset path
 * @param dataset
 *     the name of the dataset within the Fuseki server to target
 */
@ConfigurationProperties(prefix = "fuseki")
public record FusekiProperties(
    String baseUrl,
    String dataset
) {}
