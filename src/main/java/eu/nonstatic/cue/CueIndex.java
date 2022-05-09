package eu.nonstatic.cue;

import lombok.Getter;

@Getter
public class CueIndex implements CueEntity {

  private final int number;

  private final int minutes;
  private final int seconds;
  private final int frames;

  public CueIndex(int number, String timeCode) {
    this.number = number;

    String[] parts = timeCode.split(":");
    this.minutes = Integer.valueOf(parts[0]);
    this.seconds = Integer.valueOf(parts[1]);
    this.frames = Integer.valueOf(parts[2]);
  }

  public long getMillis() {
    return (minutes * 60L * 1000L) + (seconds * 1000L) + (frames * 1000L / 75L);
  }

  public String getTimeCode() {
    return String.format("%02d:%02d:%02d", minutes, seconds, frames);
  }
}
