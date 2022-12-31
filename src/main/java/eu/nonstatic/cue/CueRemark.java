package eu.nonstatic.cue;

import lombok.AllArgsConstructor;
import lombok.Getter;

import static eu.nonstatic.cue.CueTools.quote;

@Getter
@AllArgsConstructor
public class CueRemark extends CueEntity {

  public static final String KEYWORD = CueWords.REMARK;
  public static final String TAG_COMMENT = "COMMENT";

  private final String tag; // COMMENT, DISCID, UPC...
  private final String value;


  private boolean isComment() {
    return TAG_COMMENT.equalsIgnoreCase(tag);
  }

  private boolean isSeveralWords() {
    return value != null && value.split("\\s+").length > 1;
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

  private boolean requiresQuotes() {
    return isComment() || isSeveralWords() || value.length() == 0;
  }
}