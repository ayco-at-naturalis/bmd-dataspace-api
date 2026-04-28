package eu.bmdproject.dataspace.dao;

import eu.bmdproject.dataspace.config.GraphProperties;
import org.apache.jena.query.Dataset;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Plain unit tests targeting the URI construction logic in graphUriFor() and idFromGraphUri(). Both methods
 * are private but fully exercised via list() and getSummary(), which are the cheapest public entry points.
 */
class JenaROCrateDaoGraphUriTest {

  private static final Path DUMMY_PATH = Path.of("/tmp");

  private JenaROCrateDao daoWithBase(String baseIri, Dataset dataset) {
    return new JenaROCrateDao(null, new GraphProperties(baseIri, DUMMY_PATH), dataset);
  }

  /**
   * Dataset stub that returns one graph URI from listNames() and nothing else.
   */
  private Dataset datasetWithGraphUri(String graphUri) {
    Dataset ds = mock(Dataset.class);
    when(ds.listNames()).thenReturn(List.of(graphUri).iterator());
    return ds;
  }

  // graphUriFor — via getSummary() which calls graphUriFor() then containsNamedModel()

  @Test
  void graphUriFor_baseEndsWithColon_concatenatesDirectly() {
    // Given — "urn:uuid:" already ends with ':', so result is base + id
    Dataset ds = mock(Dataset.class);
    when(ds.containsNamedModel("urn:uuid:abc")).thenReturn(true);
    when(ds.getNamedModel(any(String.class))).thenReturn(createDefaultModel());
    JenaROCrateDao dao = daoWithBase("urn:uuid:", ds);

    // When
    var result = dao.getSummary("abc");

    // Then
    assertThat(result).isPresent();
    verify(ds).containsNamedModel("urn:uuid:abc");
  }

  @Test
  void graphUriFor_baseEndsWithSlash_concatenatesDirectly() {
    // Given — base ends with '/', so result is base + id
    Dataset ds = mock(Dataset.class);
    when(ds.containsNamedModel("https://example.com/graphs/abc")).thenReturn(true);
    when(ds.getNamedModel(any(String.class))).thenReturn(createDefaultModel());
    JenaROCrateDao dao = daoWithBase("https://example.com/graphs/", ds);

    // When
    var result = dao.getSummary("abc");

    // Then
    assertThat(result).isPresent();
    verify(ds).containsNamedModel("https://example.com/graphs/abc");
  }

  @Test
  void graphUriFor_baseIsUrnWithoutTrailingSeparator_appendsColon() {
    // Given — base starts with "urn:" but doesn't end with ':' or '/'
    // so result is base + ":" + id
    Dataset ds = mock(Dataset.class);
    when(ds.containsNamedModel("urn:example:abc")).thenReturn(true);
    when(ds.getNamedModel(any(String.class))).thenReturn(createDefaultModel());
    JenaROCrateDao dao = daoWithBase("urn:example", ds);

    // When
    var result = dao.getSummary("abc");

    // Then
    assertThat(result).isPresent();
    verify(ds).containsNamedModel("urn:example:abc");
  }

  @Test
  void graphUriFor_baseIsHttpWithoutTrailingSlash_appendsSlash() {
    // Given — base is an HTTP URL without trailing separator
    // so result is base + "/" + id
    Dataset ds = mock(Dataset.class);
    when(ds.containsNamedModel("https://example.com/graphs/abc")).thenReturn(true);
    when(ds.getNamedModel(any(String.class))).thenReturn(createDefaultModel());
    JenaROCrateDao dao = daoWithBase("https://example.com/graphs", ds);

    // When
    var result = dao.getSummary("abc");

    // Then
    assertThat(result).isPresent();
    verify(ds).containsNamedModel("https://example.com/graphs/abc");
  }

  // idFromGraphUri — via list(), which calls idFromGraphUri() for each graph name

  @Test
  void idFromGraphUri_graphUriUnderNormalizedBase_stripsPrefix() {
    // Given — base ends with ':', normalizedBase = "urn:uuid:"
    // graphUri = "urn:uuid:abc" → id = "abc"
    Dataset ds = datasetWithGraphUri("urn:uuid:abc");
    JenaROCrateDao dao = daoWithBase("urn:uuid:", ds);

    // When
    var refs = dao.list();

    // Then
    assertThat(refs).hasSize(1);
    assertThat(refs.getFirst().id()).isEqualTo("abc");
  }

  @Test
  void idFromGraphUri_graphUriNotUnderBase_returnsFullUri() {
    // Given — graphUri doesn't start with normalizedBase at all
    Dataset ds = datasetWithGraphUri("urn:other:xyz");
    JenaROCrateDao dao = daoWithBase("urn:uuid:", ds);

    // When
    var refs = dao.list();

    // Then — full URI returned as-is
    assertThat(refs).hasSize(1);
    assertThat(refs.getFirst().id()).isEqualTo("urn:other:xyz");
  }

  @Test
  void idFromGraphUri_graphUriEqualsNormalizedBase_returnsFullUri() {
    // Given — graphUri equals normalizedBase exactly (length not > normalizedBase.length())
    Dataset ds = datasetWithGraphUri("urn:uuid:");
    JenaROCrateDao dao = daoWithBase("urn:uuid:", ds);

    // When
    var refs = dao.list();

    // Then — not stripped, returned as-is
    assertThat(refs).hasSize(1);
    assertThat(refs.getFirst().id()).isEqualTo("urn:uuid:");
  }

  @Test
  void idFromGraphUri_graphUriIsNull_returnsNull() {
    // Given — listNames() returns null as a graph URI (defensive branch)
    Dataset ds = mock(Dataset.class);
    // Manually supply an iterator that yields null
    Iterator<String> nullIter = new Iterator<>() {
      boolean done = false;

      public boolean hasNext() {
        return !done;
      }

      public String next() {
        done = true;
        return null;
      }
    };
    when(ds.listNames()).thenReturn(nullIter);
    JenaROCrateDao dao = daoWithBase("urn:uuid:", ds);

    // When
    var refs = dao.list();

    // Then
    assertThat(refs).hasSize(1);
    assertThat(refs.getFirst().id()).isNull();
  }

  @Test
  void idFromGraphUri_baseWithoutTrailingSlash_normalizesAndStripsPrefix() {
    // Given — base "https://example.com/graphs" (no trailing slash)
    // normalizedBase becomes "https://example.com/graphs/"
    // graphUri = "https://example.com/graphs/abc" → id = "abc"
    Dataset ds = datasetWithGraphUri("https://example.com/graphs/abc");
    JenaROCrateDao dao = daoWithBase("https://example.com/graphs", ds);

    // When
    var refs = dao.list();

    // Then
    assertThat(refs).hasSize(1);
    assertThat(refs.getFirst().id()).isEqualTo("abc");
  }
}