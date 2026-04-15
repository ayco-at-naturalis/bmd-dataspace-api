package eu.bmdproject.dataspace.exception;

import eu.bmdproject.dataspace.util.json.JsonUtil;
import org.klojang.util.ExceptionMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import java.util.Map;
import java.util.function.BiConsumer;

import static org.klojang.util.ObjectMethods.ifNull;


/**
 * Intercepts exceptions of any type and produces an appropriate HTTP response.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Handles object-not-found exceptions.
   *
   * @param ex
   *     the exception
   *
   * @return HTTP 404
   */
  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<String> handle(NotFoundException ex) {
    return error(HttpStatus.NOT_FOUND, ex, LOG::error);
  }

  /**
   * Handles invalid-user-input exceptions
   *
   * @param ex
   *     the exception
   *
   * @return HTTP 400
   */
  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<String> handle(BadRequestException ex) {
    return error(HttpStatus.BAD_REQUEST, ex, LOG::warn);
  }

  /**
   * Handles calls to specified but as yet unimplemented endpoints.
   *
   * @param ex
   *     the exception
   *
   * @return HTTP 501
   */
  @ExceptionHandler(NotImplementedException.class)
  public ResponseEntity<String> handle(NotImplementedException ex) {
    return error(HttpStatus.NOT_IMPLEMENTED, ex, LOG::error);
  }

  /**
   * Handles remaining exceptions
   *
   * @param ex
   *     the exception
   *
   * @return HTTP 500
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handle(Exception ex) {
    return error(HttpStatus.INTERNAL_SERVER_ERROR, ex, LOG::error);
  }

  private static ResponseEntity<String> error(
      HttpStatus status, Exception ex,
      BiConsumer<String, Exception> logMethod) {
    // Message to write to log file
    String internalMessage = ifNull(status.getReasonPhrase(), ex.getClass().getSimpleName());
    logMethod.accept(internalMessage, ex);
    // Message to return to client
    String clientMessage = ifNull(ex.getMessage(), ex.getClass().getSimpleName());
    Map<String, Object> error = Map.of("__ERROR__", Map.of("message", clientMessage));
    String json = JsonUtil.prettyPrint(error);
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body(json);
  }
}
