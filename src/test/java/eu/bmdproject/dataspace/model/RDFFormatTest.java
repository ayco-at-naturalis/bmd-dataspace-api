package eu.bmdproject.dataspace.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

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

  @ParameterizedTest
  @MethodSource("mediaTypeToFormat")
  void forValue_mediaType_resolves(String mediaType, RDFFormat format) {
    assertThat(RDFFormat.forValue(mediaType)).isEqualTo(format);
  }

  static Stream<Arguments> mediaTypeToFormat() {
    return Stream.of(
        Arguments.of("application/ld+json", RDFFormat.JSON_LD),
        Arguments.of("text/turtle", RDFFormat.TURTLE),
        Arguments.of("application/rdf+xml", RDFFormat.RDF_XML)
    );
  }

  @ParameterizedTest
  @MethodSource("aliases")
  void forValue_alias_resolves(String alias, RDFFormat expected) {
    assertThat(RDFFormat.forValue(alias)).isEqualTo(expected);
  }

  static Stream<Arguments> aliases() {
    return Stream.of(
        Arguments.of("jsonld", RDFFormat.JSON_LD),
        Arguments.of("json-ld", RDFFormat.JSON_LD),
        Arguments.of("turtle", RDFFormat.TURTLE),
        Arguments.of("ttl", RDFFormat.TURTLE),
        Arguments.of("rdfxml", RDFFormat.RDF_XML),
        Arguments.of("rdf/xml", RDFFormat.RDF_XML)
    );
  }

  @Test
  void forValue_unrecognised_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> RDFFormat.forValue("nquads"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("nquads");
  }

  @ParameterizedTest
  @MethodSource("formatToMediatype")
  void value_returnsOfficialMediaType(RDFFormat format, String expected) {
    assertThat(format.value()).isEqualTo(expected);
  }

  static Stream<Arguments> formatToMediatype() {
    return Stream.of(
        Arguments.of(RDFFormat.JSON_LD, "application/ld+json"),
        Arguments.of(RDFFormat.TURTLE, "text/turtle"),
        Arguments.of(RDFFormat.RDF_XML, "application/rdf+xml")
    );
  }


}