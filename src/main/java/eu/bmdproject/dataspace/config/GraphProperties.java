package eu.bmdproject.dataspace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app.graph")
public record GraphProperties(String baseIri, Path jenaDataDir) {}
