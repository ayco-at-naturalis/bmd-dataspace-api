package eu.bmdproject.dataspace.dao;

import eu.bmdproject.dataspace.model.ROCrate;
import eu.bmdproject.dataspace.model.ROCrate.Format;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class FusekiDao implements ROCrateDao {

  private final WebClient webClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public FusekiDao(WebClient fusekiWebClient) {
    this.webClient = fusekiWebClient;
  }

  // -------------------- ROCrate operations --------------------

  @Override
  public void upsert(ROCrate crate) {
    // Graph Store Protocol: PUT content to named graph ?graph={id}
    try {
      webClient.put()
          .uri(uriBuilder -> uriBuilder.path("/data")
              .queryParam("graph", crate.id())
              .build())
          .contentType(mediaTypeFor(crate.format()))
          .bodyValue(crate.payload().getBytes(StandardCharsets.UTF_8))
          .retrieve()
          .toBodilessEntity()
          .block();
    } catch (WebClientResponseException e) {
      throw translate("upsert", e);
    }
  }

  @Override
  public Optional<ROCrate> getById(String id) {
    try {
      String body = webClient.get()
          .uri(uriBuilder -> uriBuilder.path("/data")
              .queryParam("graph", id)
              .build())
          .accept(MediaType.valueOf("application/ld+json"))
          .retrieve()
          .bodyToMono(String.class)
          .block();

      if (body == null || body.isBlank()) {
        return Optional.empty();
      }
      // We request JSON-LD; if Fuseki returns it, wrap accordingly.
      return Optional.of(new ROCrate(id, id, body, Format.JSON_LD));
    } catch (WebClientResponseException e) {
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        return Optional.empty();
      }
      throw translate("getById", e);
    }
  }

  @Override
  public void deleteById(String id) {
    try {
      webClient.delete()
          .uri(uriBuilder -> uriBuilder.path("/data")
              .queryParam("graph", id)
              .build())
          .retrieve()
          .toBodilessEntity()
          .block();
    } catch (WebClientResponseException e) {
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        // Idempotent delete
        return;
      }
      throw translate("deleteById", e);
    }
  }

  // -------------------- SPARQL helpers --------------------

  @Override
  public List<String> searchByText(String text, int limit) {
    // Naive literal search across graphs; replace with text index if available.
    String q = """
        SELECT DISTINCT ?g WHERE {
          GRAPH ?g { ?s ?p ?o .
            FILTER(isLiteral(?o) && CONTAINS(LCASE(STR(?o)), LCASE(%s)))
          }
        }
        LIMIT %d
        """.formatted(stringLiteral(text), Math.max(1, limit));

    String json = select(q);
    try {
      JsonNode root = objectMapper.readTree(json);
      List<String> out = new ArrayList<>();
      for (JsonNode binding : root.path("results").path("bindings")) {
        JsonNode g = binding.path("g").path("value");
        if (g.isTextual()) out.add(g.asText());
      }
      return out;
    } catch (Exception e) {
      // Don’t explode the API surface because of parsing noise.
      return List.of();
    }
  }

  @Override
  public boolean ask(String sparqlAsk) {
    try {
      String json = webClient.post()
          .uri("/sparql")
          .contentType(MediaType.valueOf("application/sparql-query"))
          .accept(MediaType.valueOf("application/sparql-results+json"))
          .bodyValue(sparqlAsk)
          .retrieve()
          .bodyToMono(String.class)
          .block();

      if (json == null) return false;
      JsonNode root = objectMapper.readTree(json);
      return root.path("boolean").asBoolean(false);
    } catch (WebClientResponseException e) {
      throw translate("ask", e);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse ASK response", e);
    }
  }

  @Override
  public String select(String sparqlSelect) {
    try {
      return webClient.post()
          .uri("/sparql")
          .contentType(MediaType.valueOf("application/sparql-query"))
          .accept(MediaType.valueOf("application/sparql-results+json"))
          .bodyValue(sparqlSelect)
          .retrieve()
          .bodyToMono(String.class)
          .block();
    } catch (WebClientResponseException e) {
      throw translate("select", e);
    }
  }

  @Override
  public String construct(String sparqlConstruct) {
    try {
      return webClient.post()
          .uri("/sparql")
          .contentType(MediaType.valueOf("application/sparql-query"))
          .accept(MediaType.valueOf("text/turtle"))
          .bodyValue(sparqlConstruct)
          .retrieve()
          .bodyToMono(String.class)
          .block();
    } catch (WebClientResponseException e) {
      throw translate("construct", e);
    }
  }

  @Override
  public void update(String sparqlUpdate) {
    try {
      webClient.post()
          .uri("/update")
          .contentType(MediaType.valueOf("application/sparql-update"))
          .bodyValue(sparqlUpdate)
          .retrieve()
          .toBodilessEntity()
          .block();
    } catch (WebClientResponseException e) {
      throw translate("update", e);
    }
  }

  // -------------------- internals --------------------

  private static MediaType mediaTypeFor(Format f) {
    return switch (f) {
      case JSON_LD -> MediaType.valueOf("application/ld+json");
      case TURTLE -> MediaType.valueOf("text/turtle");
    };
  }

  /** Escape a Java string as a SPARQL string literal. */
  private static String stringLiteral(String s) {
    String escaped = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
    return "\"" + escaped + "\"";
  }

  private RuntimeException translate(String op, WebClientResponseException e) {
    return new RuntimeException("Fuseki " + op + " failed: " + e.getStatusCode() +
        " " + e.getResponseBodyAsString(), e);
  }
}
