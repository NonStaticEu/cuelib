package eu.nonstatic.cue;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import static eu.nonstatic.cue.CueTools.quote;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
public class CueRemark implements CueEntity {

  public static final String KEYWORD = CueWords.REMARK;
  public static final String TAG_COMMENT = "COMMENT";

  private final String tag; // COMMENT, DISCID, UPC...
  private final String value;

  public CueRemark deepCopy() {
    return new CueRemark(tag, value);
  }

  public boolean isComment() {
    return TAG_COMMENT.equalsIgnoreCase(tag);
  }

  public boolean isSeveralWords() {
    return value != null && value.split("\\s+").length > 1;
  }

  private boolean requiresQuotes() {
    return isComment() || isSeveralWords() || value.length() == 0;
  }

  @Override
  public String toSheetLine() {
    StringBuilder sb = new StringBuilder(KEYWORD);
    if (tag != null) {
      sb.append(' ').append(tag);
    }
    if (value != null) {
      sb.append(' ').append(requiresQuotes() ? quote(value) : value);
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return toSheetLine();
  }
}