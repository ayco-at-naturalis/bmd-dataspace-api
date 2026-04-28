package eu.bmdproject.dataspace.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RDFFormatTest {

  @Test
  void forValue_null_returnsJsonLd() {
    assertThat(RDFFormat.forValue(null)).isEqualTo(RDFFormat.JSON_LD);
  }

  @Test
  void forValue_emptyString_returnsJsonLd() {
    assertThat(RDFFormat.forValue("")).isEqualTo(RDFFormat.JSON_LD);
  }

  @Test
  void forValue_mediaType_resolves() {
    assertThat(RDFFormat.forValue("application/ld+json")).isEqualTo(RDFFormat.JSON_LD);
    assertThat(RDFFormat.forValue("text/turtle")).isEqualTo(RDFFormat.TURTLE);
    assertThat(RDFFormat.forValue("application/rdf+xml")).isEqualTo(RDFFormat.RDF_XML);
  }

  @Test
  void forValue_aliases_resolve() {
    assertThat(RDFFormat.forValue("jsonld")).isEqualTo(RDFFormat.JSON_LD);
    assertThat(RDFFormat.forValue("json-ld")).isEqualTo(RDFFormat.JSON_LD);
    assertThat(RDFFormat.forValue("turtle")).isEqualTo(RDFFormat.TURTLE);
    assertThat(RDFFormat.forValue("ttl")).isEqualTo(RDFFormat.TURTLE);
    assertThat(RDFFormat.forValue("rdfxml")).isEqualTo(RDFFormat.RDF_XML);
    assertThat(RDFFormat.forValue("rdf/xml")).isEqualTo(RDFFormat.RDF_XML);
  }

  @Test
  void forValue_unrecognised_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> RDFFormat.forValue("nquads"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("nquads");
  }

  @Test
  void value_returnsOfficialMediaType() {
    assertThat(RDFFormat.JSON_LD.value()).isEqualTo("application/ld+json");
    assertThat(RDFFormat.TURTLE.value()).isEqualTo("text/turtle");
    assertThat(RDFFormat.RDF_XML.value()).isEqualTo("application/rdf+xml");
  }
}