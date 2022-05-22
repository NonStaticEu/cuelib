package eu.nonstatic.cue;

import lombok.AllArgsConstructor;
import lombok.Data;

import static eu.nonstatic.cue.CueTools.quote;

/**
 * For non-standard lines (eg ARTIST or REM-less GENRE)
 */
@Data
@AllArgsConstructor
public class CueOther extends CueEntity {

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
}
