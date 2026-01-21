package eu.bmdproject.dataspace.exception;

public class BmdRuntimeException extends RuntimeException {

  public BmdRuntimeException(String message) {
    super(message);
  }

  public BmdRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  public BmdRuntimeException(Throwable cause) {
    super(cause);
  }
}
