package eu.nonstatic.cue;

import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CueHiddenTrack {

  private CueFile file;
  private Duration duration; //from index 0 till index 1 on track 1

  public CueTrack getTrack() {
    return file.getNumberOneTrack();
  }

  public CueIndex getPreGapIndex() {
    return getTrack().getPreGapIndex();
  }

  public CueIndex getStartIndex() {
    return getTrack().getStartIndex();
  }
}
