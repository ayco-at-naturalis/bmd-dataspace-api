package eu.bmdproject.dataspace.model;

import org.klojang.check.Check;

import static org.klojang.check.CommonChecks.sameAs;
import static org.klojang.check.CommonExceptions.illegalState;

/**
 * Represents an RO-Crate as it moves through the BMD Dataspace API.
 * <p>
 * This record is <em>not</em> a classical JPA/ORM model that mirrors a database table. Instead, it functions
 * as a lightweight data transfer object (DTO) between the controller, service, and DAO layers. It
 * encapsulates both the semantic identity of an RO-Crate and its serialized payload (typically JSON-LD or
 * RDF/Turtle), which may be persisted in or retrieved from a triple store such as Fuseki.
 * </p>
 *
 * <p>
 * The DAO layer is responsible for interpreting or persisting the {@code payload} according to the
 * {@link #format}. The rest of the application treats this record as an opaque carrier of crate content.
 * </p>
 *
 * @param id
 *     A stable identifier that names the RO-Crate. It doubles as the named graph URI in the triple store (for
 *     example, {@code https://bmd.example/ro-crate/{id}}). The ID is typically provided by the client or
 *     service layer and remains constant across updates to the same logical crate.
 * @param name
 *     An optional human-readable label for the crate, used for display or indexing purposes. It has no
 *     persistence semantics of its own and may be omitted or derived from crate metadata.
 * @param payload
 *     The serialized RO-Crate content. When uploading or updating, this will normally be JSON-LD. When
 *     retrieving, it may be returned as either JSON-LD or RDF/Turtle depending on the query and content
 *     negotiation performed by the DAO.
 * @param format
 *     Specifies how the {@code payload} should be interpreted by the receiver. The format distinguishes
 *     between JSON-LD and RDF/Turtle encodings and ensures that the correct content type is used when
 *     transmitting to or from the triple store.
 */
public record ROCrate(
    String id,
    String name,
    String payload,
    Format format
) {

  /**
   * Enumerates the serialization formats supported for RO-Crate payloads.
   * <ul>
   *   <li>{@link #JSON_LD} — canonical JSON-LD representation used for upserts</li>
   *   <li>{@link #TURTLE} — RDF/Turtle serialization typically returned from SPARQL CONSTRUCT queries</li>
   * </ul>
   */
  public enum Format {JSON_LD, TURTLE}

  /**
   * Returns the payload as JSON-LD, asserting that {@link #format} is {@link Format#JSON_LD}. This method is
   * intended for DAO operations that send the crate to the triple store in its JSON-LD form.
   *
   * @return the JSON-LD payload
   *
   * @throws IllegalStateException
   *     if the payload is not in JSON-LD format
   */
  public String jsonLd() {
    Check.that(format).is(sameAs(), Format.JSON_LD, illegalState("Payload is not JSON-LD"));
    return payload;
  }
}
