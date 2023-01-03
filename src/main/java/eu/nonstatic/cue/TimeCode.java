/**
 * Cuelib
 * Copyright (C) 2022 NonStatic
 *
 * This file is part of cuelib.
 * cuelib is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with . If not, see <https://www.gnu.org/licenses/>.
 */
package eu.nonstatic.cue;

import static java.lang.Integer.min;
import static java.lang.Integer.parseInt;

import java.io.Serializable;
import java.time.Duration;
import java.util.Comparator;
import lombok.Getter;

/**
 * Immutable class
 */
@Getter
public final class TimeCode implements Comparable<TimeCode>, Serializable {

  public static final int FRAMES_PER_SECOND = 75;
  public static final int MILLIS_PER_SECOND = 1000;
  public static final int SECONDS_PER_MINUTE = 60;
  public static final int MAX_CDR_MINUTES = 100; // yes I know about the red book, but there's this too: https://www.amazon.com/dp/B000R4LZ3A
  public static final Comparator<TimeCode> COMPARATOR = Comparator.comparing(TimeCode::toFrameCount);

  public static final TimeCode ZERO_SECOND = new TimeCode(0, 0, 0);
  public static final TimeCode ONE_SECOND = new TimeCode(0, 1, 0);
  public static final TimeCode TWO_SECONDS = new TimeCode(0, 2, 0);

  private final int minutes;
  private final int seconds;
  private final int frames;


  public TimeCode(int minutes, int seconds, int frames) {
    validate(minutes, seconds, frames);
    this.minutes = minutes;
    this.seconds = seconds;
    this.frames = frames;
  }

  public TimeCode(long millis) {
    this((int) (millis / (SECONDS_PER_MINUTE * MILLIS_PER_SECOND)),
      (int) ((millis / MILLIS_PER_SECOND) % SECONDS_PER_MINUTE),
      Math.round((float) (FRAMES_PER_SECOND * (millis % MILLIS_PER_SECOND)) / MILLIS_PER_SECOND));
  }

  public TimeCode(Duration duration) {
    this(duration.toMillis());
  }

  public TimeCode(TimeCode timeCode) {
    this(timeCode.minutes, timeCode.seconds, timeCode.frames);
  }

  public static void validate(int minutes, int seconds, int frames) {
    if (minutes < 0 || minutes >= MAX_CDR_MINUTES) {
      throw new IllegalArgumentException("minutes must be in the [0-99] range");
    }

    if (seconds < 0 || seconds >= SECONDS_PER_MINUTE) {
      throw new IllegalArgumentException("seconds must be in the [0-59] range");
    }

    if (frames < 0 || frames >= FRAMES_PER_SECOND) {
      throw new IllegalArgumentException("frames must be in the [0-74] range");
    }
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

  public int toFrameCount() {
    return (minutes * SECONDS_PER_MINUTE + seconds) * FRAMES_PER_SECOND + frames;
  }

  public long toMillis() {
    return ((long) minutes * SECONDS_PER_MINUTE + seconds) * MILLIS_PER_SECOND
        + (((long) frames * MILLIS_PER_SECOND + FRAMES_PER_SECOND - 1) / FRAMES_PER_SECOND); // upper rounding
  }

  public Duration toDuration() {
    return Duration.ofMillis(toMillis());
  }

  /**
   * @param timeCode mm:ss:ff (minutes:seconds:frames) format
   * @return
   */
  public static TimeCode parse(String timeCode) {
    return parse(timeCode, false);
  }

  /**
   * @param timeCode mm:ss:ff (minutes:seconds:frames) format
   * @param lenient allows 75 as a frame value
   * @return
   */
  public static TimeCode parse(String timeCode, boolean lenient) {
    String[] parts = timeCode.split(":");
    int minutes = parseInt(parts[0]); // figures may not be 0-padded
    int seconds = parseInt(parts[1]);
    int frames = parseInt(parts[2]);

    if(lenient && frames == FRAMES_PER_SECOND) {
      frames = 0;
      if(++seconds >= SECONDS_PER_MINUTE) {
        seconds = 0;
        ++minutes;
      }
    }
    return new TimeCode(minutes, seconds, frames);
  }

  public Duration until(TimeCode other) {
    return other.minus(this);
  }

  public Duration minus(TimeCode other) {
    int framesDiff = toFrameCount() - other.toFrameCount();
    long millisDiff = Math.round(((double) (MILLIS_PER_SECOND * framesDiff)) / FRAMES_PER_SECOND); // Converting frames diff instead of millis diff to have a result in 75th's of a second
    return Duration.ofMillis(millisDiff);
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
  public int compareTo(TimeCode other) {
    return COMPARATOR.compare(this, other);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }

    return toFrameCount() == ((TimeCode)other).toFrameCount();
  }

  @Override
  public int hashCode() {
    return toFrameCount();
  }

  @Override
  public String toString() {
    return String.format("%02d:%02d:%02d", minutes, seconds, frames);
  }
}
