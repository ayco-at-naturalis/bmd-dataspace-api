package eu.bmdproject.dataspace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app.discodata")
public record DiscoDataProperties(
    String endpoint,
    String eunisQueryTemplate,
    String biseQueryTemplate,
    Path geopackagePath
) {}