package eu.bmdproject.dataspace.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Skeleton for the ingest API.
 * Later we can add fields for graphBaseIri override, credentials, options, etc.
 */
public record IngestRequest(
    /**
     * Could be: "zip", "jsonld", "s3", "http", etc.
     * For now this is just a placeholder signal.
     */
    @NotBlank String mode,

    /**
     * Source location or inline payload reference (to be refined).
     * e.g., file path, URL, or an identifier for an uploaded asset.
     */
    @NotBlank String source
) {}
