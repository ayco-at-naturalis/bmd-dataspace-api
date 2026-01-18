package eu.bmdproject.dataspace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.discodata")
public record DiscoDataProperties(String endpoint, String eunisQueryTemplate, String biseQueryTemplate) {}
