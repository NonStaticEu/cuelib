package eu.nonstatic.cue;

public class BadCharsetException extends RuntimeException {

  public BadCharsetException(Throwable cause) {
    super(cause);
  }

  public BadCharsetException(String message) {
    super(message);
  }
}
