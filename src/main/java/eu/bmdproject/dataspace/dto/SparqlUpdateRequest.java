package eu.bmdproject.dataspace.dto;

import jakarta.validation.constraints.NotBlank;

public record SparqlUpdateRequest(
    @NotBlank String update
) {}
