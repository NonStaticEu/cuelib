package eu.nonstatic.cue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CueRemark implements CueEntity {

  private final String tag; // COMMENT, DISCID, UPC...
  private final String value;
}
