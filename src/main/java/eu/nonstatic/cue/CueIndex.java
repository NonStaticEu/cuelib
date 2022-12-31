package eu.nonstatic.cue;

import static eu.nonstatic.cue.TimeCode.FRAMES_PER_SECOND;
import static eu.nonstatic.cue.TimeCode.SECONDS_PER_MINUTE;

import java.time.Duration;
import java.util.Comparator;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class CueIndex implements CueEntity, Comparable<CueIndex> {

  public static final String KEYWORD = CueWords.INDEX;
  public static final int INDEX_PRE_GAP = 0; // also the hidden track index in track 1
  public static final int INDEX_TRACK_START = 1;
  public static final Comparator<Integer> COMPARATOR = Comparator.naturalOrder();

  protected Integer number; // 1 is the track start, 0 is the pregap.
  private TimeCode timeCode;

  public CueIndex(Integer number, int minutes, int seconds, int frames) {
    this(number, new TimeCode(minutes, seconds, frames));
  }

  public CueIndex(Integer number, TimeCode timeCode) {
    if (number != null) {
      setNumberOnce(number);
    }
    this.timeCode = timeCode;
  }

  public CueIndex(Integer number, String timeCode) {
    this(number, TimeCode.parse(timeCode));
  }

  public CueIndex deepCopy() {
    return deepCopy(number);
  }

  public CueIndex deepCopy(Integer number) {
    return new CueIndex(number, timeCode);
  }

  public boolean hasNumber() {
    return number != null;
  }

  public void setNumberOnce(int number) {
    if (number < 0) {
      throw new IllegalArgumentException("Index number must be zero or positive");
    } else if (this.number != null) {
      throw new IllegalStateException("Index number already set to " + this.number);
    } else {
      this.number = number;
    }
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
    timeCode = timeCode.withMinutes(minutes);
  }

  public void setSeconds(int seconds) {
    timeCode = timeCode.withSeconds(seconds);
  }

  public void setFrames(int frames) {
    timeCode = timeCode.withFrames(frames);
  }

  public void setTime(int minutes, int seconds, int frames) {
    timeCode = new TimeCode(minutes, seconds, frames);
  }


  public void roundSecond() {
    if (getFrames() >= FRAMES_PER_SECOND / 2) {
      ceilSecond();
    } else {
      floorSecond();
    }
  }

  public void floorSecond() {
    setFrames(0);
  }

  public void ceilSecond() {
    if (getFrames() > 0) {
      int seconds = getSeconds() + 1;

      int minutes = getMinutes();
      if (seconds == SECONDS_PER_MINUTE) {
        minutes++;
        seconds = 0;
      }
      setTime(minutes, seconds, 0);
    }
  }

  public long getTimeMillis() {
    return timeCode.toMillis();
  }

  public void setTimeMillis(long millis) {
    timeCode = new TimeCode(millis);
  }


  public String getTimeCode() {
    return timeCode.toString();
  }

  public void setTimeCode(String timeCode) {
    this.timeCode = TimeCode.parse(timeCode);
  }

  public TimeCode toTimeCode() {
    return timeCode;
  }

  public Duration until(CueIndex other) {
    return timeCode.until(other.timeCode);
  }

  /**
   * Not comparing times, just indexes
   */
  @Override
  public int compareTo(CueIndex otherIndex) {
    return COMPARATOR.compare(number, otherIndex.number);
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
