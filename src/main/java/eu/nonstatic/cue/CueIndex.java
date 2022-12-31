package eu.nonstatic.cue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@EqualsAndHashCode
public class CueIndex implements CueEntity, Comparable<CueIndex> {

  public static final String KEYWORD = CueWords.INDEX;

  @Setter
  private int number;
  private TimeCode timeCode;


  public CueIndex(int number, int minutes, int seconds, int frames) {
    this.number = number;
    this.timeCode = new TimeCode(minutes, seconds, frames);
  }

  public CueIndex(int number, String timeCode) {
    this.number = number;
    this.timeCode = TimeCode.parse(timeCode);
  }

  public int getMinutes() {
    return timeCode.getMinutes();
  }

  public int getSeconds() {
    return timeCode.getSeconds();
  }

  public int getFrames() {
    return timeCode.getFrames();
  }

  public void setMinutes(int minutes) {
    if (minutes < 0 || minutes > 99) {
      throw new IllegalArgumentException("minutes must be in the [0-99] range");
    }
    timeCode.setMinutes(minutes);
  }

  public void setSeconds(int seconds) {
    timeCode.setSeconds(seconds);
  }

  public void setFrames(int frames) {
    timeCode.setFrames(frames);
  }

  public void setTime(int minutes, int seconds, int frames) {
    timeCode.setTime(minutes, seconds, frames);
  }


  public long getTimeMillis() {
    return timeCode.getTimeMillis();
  }

  public void setTimeMillis(long millis) {
    timeCode.setTimeMillis(millis);
  }


  public String getTimeCode() {
    return timeCode.toString();
  }

  public void setTimeCode(String timeCode) {
    this.timeCode = TimeCode.parse(timeCode);
  }

  @Override
  public int compareTo(CueIndex cueIndex) {
    return Integer.compare(number, cueIndex.number);
  }

  @Override
  public String toSheetLine() {
    return String.format("%s %02d %s", KEYWORD, number, timeCode.toString());
  }

  @Override
  public String toString() {
    return toSheetLine();
  }
}
