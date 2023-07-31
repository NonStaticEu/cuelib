package eu.nonstatic.cue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter @AllArgsConstructor
@ToString
public class CueSheetIssue {

  private final String message;
  private final Throwable cause;

  public CueSheetIssue(String message) {
    this(message, null);
  }

  public CueSheetIssue(Throwable cause) {
    this(cause.getClass().getName() + ": " + cause.getMessage(), cause);
  }
}
