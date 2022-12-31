package eu.nonstatic.cue;

import lombok.Data;

@Data
public class CueIndex extends CueEntity {

  public static final String KEYWORD = CueWords.INDEX;

  public static final int SECONDS_PER_MINUTE = 60;
  public static final int MILLIS_PER_SECOND = 1000;
  public static final int FRAMES_PER_SECOND = 75;


  private final int number;

  private int minutes;
  private int seconds;
  private int frames;

  public CueIndex(int number, int minutes, int seconds, int frames) {
    this.number = number;
    setTime(minutes, seconds, frames);
  }

  public CueIndex(int number, String timeCode) {
    this.number = number;
    setTimeCode(timeCode);
  }


  public void setTime(int minutes, int seconds, int frames) {
    setMinutes(minutes);
    setSeconds(seconds);
    setFrames(frames);
  }

  public void setMinutes(int minutes) {
    if (minutes < 0 || minutes > 99) {
      throw new IllegalArgumentException("minutes must be in the [0-99] range");
    }
    this.minutes = minutes;
  }

  public void setSeconds(int seconds) {
    if (seconds < 0 || seconds > 59) {
      throw new IllegalArgumentException("seconds must be in the [0-59] range");
    }
    this.seconds = seconds;
  }

  public void setFrames(int frames) {
    if (frames < 0 || frames > 75) {
      throw new IllegalArgumentException("frames must be in the [0-74] range");
    }
    this.frames = frames;
  }

  public long getTimeMillis() {
    return (minutes * SECONDS_PER_MINUTE * MILLIS_PER_SECOND)
        + (seconds * MILLIS_PER_SECOND)
        + ((frames * MILLIS_PER_SECOND + FRAMES_PER_SECOND - 1) / FRAMES_PER_SECOND); // upper rounding
  }

  public void setTimeMillis(long millis) {
    setTime((int) (millis / (SECONDS_PER_MINUTE * MILLIS_PER_SECOND)),
        (int) ((millis / MILLIS_PER_SECOND) % SECONDS_PER_MINUTE),
        (int) ((FRAMES_PER_SECOND * (millis % MILLIS_PER_SECOND)) / MILLIS_PER_SECOND));
  }

  public String getTimeCode() {
    return String.format("%02d:%02d:%02d", minutes, seconds, frames);
  }

  public void setTimeCode(String timeCode) {
    String[] parts = timeCode.split(":");
    setTime(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]), Integer.valueOf(parts[2]));
  }

  @Override
  public String toSheetLine() {
    return String.format("%s %02d %s", KEYWORD, number, getTimeCode());
  }
}
