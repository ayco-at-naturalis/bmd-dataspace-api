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

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<String>  handle(NotFoundException ex) {
    return error(HttpStatus.NOT_FOUND, ex, LOG::error);
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<String> handle(BadRequestException ex) {
    return error(HttpStatus.BAD_REQUEST, ex, LOG::warn);
  }

  @ExceptionHandler(NotImplementedException.class)
  public ResponseEntity<String>  handle(NotImplementedException ex) {
    return error(HttpStatus.NOT_IMPLEMENTED, ex, LOG::error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String>  handle(Exception ex) {
    return error(HttpStatus.INTERNAL_SERVER_ERROR, ex, LOG::error);
  }

  private static ResponseEntity<String> error(
      HttpStatus status, Exception ex,
      BiConsumer<String, Exception> logMethod) {
    String internalMessage = ifNull(status.getReasonPhrase(), ex.getClass().getSimpleName());
    logMethod.accept(internalMessage, ex);
    String clientMessage = ex.getMessage();
    Map<String, Object> error = Map.of("__ERROR__", Map.of("message", clientMessage));
    String json = JsonUtil.prettyPrint(error);
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body(json);
  }
}
