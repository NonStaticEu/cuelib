package eu.nonstatic.cue;

import static eu.nonstatic.cue.CueTools.quote;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * For non-standard lines (eg ARTIST or REM-less GENRE)
 */
@Getter @Setter
@AllArgsConstructor
@EqualsAndHashCode
public class CueOther implements CueEntity {

  private final String keyword;
  private final String value;

  @Override
  public String toSheetLine() {
    StringBuilder sb = new StringBuilder(keyword);
    if (value != null) {
      sb.append(' ').append(quote(value));
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return toSheetLine();
  }
}
