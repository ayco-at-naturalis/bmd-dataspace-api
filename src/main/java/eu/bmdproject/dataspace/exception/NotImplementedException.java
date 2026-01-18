package eu.bmdproject.dataspace.exception;

public class NotImplementedException extends RuntimeException {

  public NotImplementedException() {
    super("The requested functionality has not been implemented yet");
  }

  public NotImplementedException(String message) {
    super(message);
  }

}
