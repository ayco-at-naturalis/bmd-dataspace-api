package eu.bmdproject.dataspace.component;

import eu.bmdproject.dataspace.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

class ROCrateUriValidatorTest {

  private ROCrateUriValidator validator;

  @BeforeEach
  void setUp() {
    validator = new ROCrateUriValidator();
  }

  @Test
  void validate_httpScheme_throwsBadRequestException() {
    //Given
    URI uri = URI.create("http://example.com/crate.zip");

    //When/Then
    assertThatThrownBy(() -> validator.validate(uri))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("https");

  }

  @Test
  void validate_fileScheme_throwsBadRequestException() {
    //Given
    URI uri = URI.create("file:///etc/passwd");

    //When/Then
    assertThatThrownBy(() -> validator.validate(uri))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("https");

  }

  @Test
  void validate_loopbackAddress_throwsBadRequestException() {
    //Given
    URI uri = URI.create("https://127.0.0.1/crate.zip");
    //When/Then
    assertThatThrownBy(() -> validator.validate(uri))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("127.0.0.1");
  }

  @Test
  void validate_localhost_throwsBadRequestException() {
    //Given — localhost always resolves to loopback
    URI uri = URI.create("https://localhost/crate.zip");

    //When/Then
    assertThatThrownBy(() -> validator.validate(uri))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void validate_privateSiteLocalAddress_throwsBadRequestException() {
    //Given
    URI uri = URI.create("https://192.168.1.1/crate.zip");

    //When/Then
    assertThatThrownBy(() -> validator.validate(uri))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("192.168.1.1");
  }

  @Test
  void validate_publicHost_doesNotThrow() {
    //Given — if bmd-project.eu goes away, no one will care about this test
    URI uri = URI.create("https://bmd-project.eu/crate.zip");

    //When/Then
    assertThatNoException().isThrownBy(() -> validator.validate(uri));
  }
}