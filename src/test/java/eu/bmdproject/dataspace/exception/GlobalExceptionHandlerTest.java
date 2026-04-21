package eu.bmdproject.dataspace.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
    objectMapper = new ObjectMapper();
  }

  private String extractErrorMessage(ResponseEntity<String> response) throws Exception {
    @SuppressWarnings("unchecked")
    Map<String, Object> root = objectMapper.readValue(response.getBody(), Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> errorNode = (Map<String, Object>) root.get("__ERROR__");
    assertThat(errorNode).as("__ERROR__ key must be present").isNotNull();
    return (String) errorNode.get("message");
  }

  @Test
  void notFoundException() throws Exception {
    // Given
    NotFoundException ex = new NotFoundException("Site NL0000001 not found");
    // When
    ResponseEntity<String> response = handler.handle(ex);
    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(extractErrorMessage(response)).isEqualTo("Site NL0000001 not found");
  }

  @Test
  void badRequestException() throws Exception {
    // Given
    BadRequestException ex = new BadRequestException("Invalid parameter");
    // When
    ResponseEntity<String> response = handler.handle(ex);
    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(extractErrorMessage(response)).isEqualTo("Invalid parameter");
  }

  @Test
  void notImplementedException() throws Exception {
    // Given
    NotImplementedException ex = new NotImplementedException();
    // When
    ResponseEntity<String> response = handler.handle(ex);
    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
    assertThat(extractErrorMessage(response)).isEqualTo("The requested functionality has not been implemented yet");
  }

  @Test
  void genericException() throws Exception {
    // Given
    RuntimeException ex = new RuntimeException("Unexpected failure");
    // When
    ResponseEntity<String> response = handler.handle(ex);
    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(extractErrorMessage(response)).isEqualTo("Unexpected failure");
  }

  @Test
  void exceptionWithNullMessage_fallsBackToClassName() throws Exception {
    // Given
    NullPointerException ex = new NullPointerException();
    // When
    ResponseEntity<String> response = handler.handle(ex);
    // Then
    assertThat(extractErrorMessage(response)).isEqualTo("NullPointerException");
  }

}