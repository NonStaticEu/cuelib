package eu.nonstatic.cue;

import lombok.Getter;

@Getter
public class TimeCode {

  public static final int SECONDS_PER_MINUTE = 60;
  public static final int MILLIS_PER_SECOND = 1000;
  public static final int FRAMES_PER_SECOND = 75;

  private int minutes;
  private int seconds;
  private int frames;


  public TimeCode(int minutes, int seconds, int frames) {
    setTime(minutes, seconds, frames);
  }

  public TimeCode(long millis) {
    setTimeMillis(millis);
  }

  public void setTime(int minutes, int seconds, int frames) {
    setMinutes(minutes);
    setSeconds(seconds);
    setFrames(frames);
  }

  public void setMinutes(int minutes) {
    if (minutes < 0) {
      throw new IllegalArgumentException("minutes must be >= 0");
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

  public static TimeCode parse(String timeCode) {
    String[] parts = timeCode.split(":");
    return new TimeCode(Integer.valueOf(parts[0]),
        Integer.valueOf(parts[1]),
        Integer.valueOf(parts[2]));
  }

  @Override
  public String toString() {
    return String.format("%02d:%02d:%02d", minutes, seconds, frames);
  }
}
