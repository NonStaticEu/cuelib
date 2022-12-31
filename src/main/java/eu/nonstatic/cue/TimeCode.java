package eu.nonstatic.cue;

import static java.lang.Integer.parseInt;

import java.time.Duration;
import lombok.Getter;

/**
 * Immutable class
 */
@Getter
public final class TimeCode {

  public static final int SECONDS_PER_MINUTE = 60;
  public static final int MILLIS_PER_SECOND = 1000;
  public static final int FRAMES_PER_SECOND = 75;

  private int minutes;
  private int seconds;
  private int frames;


  public TimeCode(int minutes, int seconds, int frames) {
    reset(minutes, seconds, frames);
  }

  public TimeCode(long millis) {
    setMillis(millis);
  }

  public TimeCode(TimeCode timeCode) {
    this(timeCode.minutes, timeCode.seconds, timeCode.frames);
  }


  private void reset(int minutes, int seconds, int frames) {
    setMinutes(minutes);
    setSeconds(seconds);
    setFrames(frames);
  }

  private void setMillis(long millis) {
    reset((int) (millis / (SECONDS_PER_MINUTE * MILLIS_PER_SECOND)),
        (int) ((millis / MILLIS_PER_SECOND) % SECONDS_PER_MINUTE),
        (int) ((FRAMES_PER_SECOND * (millis % MILLIS_PER_SECOND)) / MILLIS_PER_SECOND));
  }

  private void setMinutes(int minutes) {
    if (minutes < 0) {
      throw new IllegalArgumentException("minutes must be >= 0");
    }
    this.minutes = minutes;
  }

  private void setSeconds(int seconds) {
    if (seconds < 0 || seconds > 59) {
      throw new IllegalArgumentException("seconds must be in the [0-59] range");
    }
    this.seconds = seconds;
  }

  private void setFrames(int frames) {
    if (frames < 0 || frames > 75) {
      throw new IllegalArgumentException("frames must be in the [0-74] range");
    }
    this.frames = frames;
  }


  public TimeCode withMinutes(int minutes) {
    return new TimeCode(minutes, this.seconds, this.frames);
  }

  public TimeCode withSeconds(int seconds) {
    return new TimeCode(this.minutes, seconds, this.frames);
  }

  public TimeCode withFrames(int frames) {
    return new TimeCode(this.minutes, this.seconds, frames);
  }

  public int toFrames() {
    return (minutes * SECONDS_PER_MINUTE + seconds) * FRAMES_PER_SECOND + frames;
  }

  public long toMillis() {
    return ((long) minutes * SECONDS_PER_MINUTE + seconds) * MILLIS_PER_SECOND
        + (((long) frames * MILLIS_PER_SECOND + FRAMES_PER_SECOND - 1) / FRAMES_PER_SECOND); // upper rounding
  }

  public static TimeCode parse(String timeCode) {
    String[] parts = timeCode.split(":");
    return new TimeCode(parseInt(parts[0]),
                        parseInt(parts[1]),
                        parseInt(parts[2]));
  }

  public Duration until(TimeCode other) {
    return other.minus(this);
  }

  public Duration minus(TimeCode other) {
    return Duration.ofMillis(toMillis() - other.toMillis());
  }

  public TimeCode minus(Duration other) {
    return minus(other.toMillis());
  }

  public TimeCode minus(long otherMillis) {
    return new TimeCode(toMillis() - otherMillis);
  }

  public TimeCode plus(Duration other) {
    return plus(other.toMillis());
  }

  public TimeCode plus(long otherMillis) {
    return new TimeCode(toMillis() + otherMillis);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }

    return getFrames() == ((TimeCode)other).getFrames();
  }

  @Override
  public int hashCode() {
    return getFrames();
  }

  @Override
  public String toString() {
    return String.format("%02d:%02d:%02d", minutes, seconds, frames);
  }
}
