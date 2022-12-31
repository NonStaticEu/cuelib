package eu.nonstatic.cue;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * For non-standard lines (eg ARTIST or REM-less GENRE)
 */
@Getter
@AllArgsConstructor
public class CueOther implements CueEntity {

  private final String keyword;
  private final String value;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(keyword);
    if (value != null) {
      sb.append(' ').append(value);
    }
    return sb.toString();
  }
}
