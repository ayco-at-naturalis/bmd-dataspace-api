package eu.bmdproject.dataspace.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    assertNotNull(errorNode, "__ERROR__ key must be present");
    return (String) errorNode.get("message");
  }

  @Test
  void notFoundException_returns404() {
    ResponseEntity<String> response = handler.handle(new NotFoundException("Site NL0000001 not found"));
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void badRequestException_returns400() {
    ResponseEntity<String> response = handler.handle(new BadRequestException("Invalid parameter"));
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void notImplementedException_returns501() {
    ResponseEntity<String> response = handler.handle(new NotImplementedException());
    assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
  }

  @Test
  void genericException_returns500() {
    ResponseEntity<String> response = handler.handle(new RuntimeException("Unexpected failure"));
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  @Test
  void notFoundException_bodyContainsMessage() throws Exception {
    ResponseEntity<String> response = handler.handle(new NotFoundException("Site NL0000001 not found"));
    assertEquals("Site NL0000001 not found", extractErrorMessage(response));
  }

  @Test
  void badRequestException_bodyContainsMessage() throws Exception {
    ResponseEntity<String> response = handler.handle(new BadRequestException("Invalid parameter"));
    assertEquals("Invalid parameter", extractErrorMessage(response));
  }

  @Test
  void notImplementedException_bodyContainsDefaultMessage() throws Exception {
    ResponseEntity<String> response = handler.handle(new NotImplementedException());
    assertEquals("The requested functionality has not been implemented yet", extractErrorMessage(response));
  }

  @Test
  void genericException_bodyContainsMessage() throws Exception {
    ResponseEntity<String> response = handler.handle(new RuntimeException("Unexpected failure"));
    assertEquals("Unexpected failure", extractErrorMessage(response));
  }

  @Test
  void exceptionWithNullMessage_fallsBackToClassName() throws Exception {
    // NullPointerException() with no argument produces getMessage() == null,
    // triggering the ifNull(..., ex.getClass().getSimpleName()) branch.
    ResponseEntity<String> response = handler.handle(new NullPointerException());
    assertEquals("NullPointerException", extractErrorMessage(response));
  }

}
