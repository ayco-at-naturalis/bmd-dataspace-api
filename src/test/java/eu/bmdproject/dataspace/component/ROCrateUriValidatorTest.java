package eu.bmdproject.dataspace.component;

import eu.bmdproject.dataspace.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

class ROCrateUriValidatorTest {

  private IPValidator ipValidator;
  private ROCrateUriValidator validator;

  @BeforeEach
  void setUp() {
    ipValidator = mock(IPValidator.class);
    validator = new ROCrateUriValidator(ipValidator);
  }

  @Test
  void validate_httpsUri_delegatesToIpValidator() {
    //Given
    URI uri = URI.create("https://example.com/crate.zip");

    //When
    assertThatNoException().isThrownBy(() -> validator.validate(uri));

    //Then
    verify(ipValidator).validate(uri);
  }

  @Test
  void validate_httpScheme_throwsBadRequestException() {
    //Given
    URI uri = URI.create("http://example.com/crate.zip");

    //When/Then
    assertThatThrownBy(() -> validator.validate(uri))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("https");

    verifyNoInteractions(ipValidator);
  }

  @Test
  void validate_fileScheme_throwsBadRequestException() {
    //Given
    URI uri = URI.create("file:///etc/passwd");

    //When/Then
    assertThatThrownBy(() -> validator.validate(uri))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("https");

    verifyNoInteractions(ipValidator);
  }

  @Test
  void validate_ipValidatorRejects_exceptionPropagates() {
    //Given
    URI uri = URI.create("https://internal.example.com/crate.zip");
    doThrow(new BadRequestException("private address")).when(ipValidator).validate(uri);

    //When/Then
    assertThatThrownBy(() -> validator.validate(uri))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("private address");
  }
}