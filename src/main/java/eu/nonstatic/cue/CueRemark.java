package eu.nonstatic.cue;

import static eu.nonstatic.cue.CueTools.quote;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@EqualsAndHashCode
public class CueRemark implements CueEntity {

  public static final String KEYWORD = CueWords.REMARK;
  // Not extensive tag list
  public static final String TAG_COMMENT = "COMMENT";
  public static final String TAG_DISCID = "DISCID";
  public static final String TAG_UPC = "UPC";

  private final String tag;
  private final String value;

  public static CueRemark commentOf(String comment) {
    return new CueRemark(TAG_COMMENT, comment);
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