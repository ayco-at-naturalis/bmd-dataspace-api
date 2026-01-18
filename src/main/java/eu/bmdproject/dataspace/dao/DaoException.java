package eu.bmdproject.dataspace.dao;

/**
 * Generic runtime exception for error conditions arising in the DAO layer.
 */
public class DaoException extends RuntimeException {

  public DaoException(String message) {
    super(message);
  }

  public DaoException(Throwable cause) {
    super(cause);
  }

  public DaoException(String message, Throwable cause) {
    super(message, cause);
  }
}
