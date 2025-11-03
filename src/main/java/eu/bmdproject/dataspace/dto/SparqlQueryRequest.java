package eu.bmdproject.dataspace.dto;

import jakarta.validation.constraints.NotBlank;

public record SparqlQueryRequest(
    @NotBlank String query
) {}
