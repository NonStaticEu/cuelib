package eu.nonstatic.cue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CueRemark implements CueEntity {

  public static final String KEYWORD = CueWords.REMARK;

  private final String tag; // COMMENT, DISCID, UPC...
  private final String value;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(KEYWORD);
    if (tag != null) {
      sb.append(' ').append(tag);
    }
    if (value != null) {
      sb.append(' ').append(value);
    }
    return sb.toString();
  }
}
