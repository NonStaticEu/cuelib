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

import static java.lang.Integer.parseInt;
import static java.util.Objects.requireNonNullElse;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Comparator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Immutable class
 */
@Slf4j
@Getter
public final class TimeCode implements Comparable<TimeCode>, Serializable {

  public static final int FRAMES_PER_SECOND = 75;
  public static final int MILLIS_PER_SECOND = 1000;
  public static final BigDecimal MILLIS_PER_SECOND_BD = BigDecimal.valueOf(MILLIS_PER_SECOND);
  private static final int HUNDRED = 100;
  public static final int SECONDS_PER_MINUTE = 60;
  public static final int MAX_CDR_MINUTES = 100; // yes I know about the red book, but there's this too: https://www.amazon.com/dp/B000R4LZ3A
  public static final Comparator<TimeCode> COMPARATOR = Comparator.comparing(TimeCode::toFrameCount);

  public static final TimeCode ZERO_SECOND = new TimeCode(0, 0, 0);
  public static final TimeCode ONE_SECOND = new TimeCode(0, 1, 0);
  public static final TimeCode TWO_SECONDS = new TimeCode(0, 2, 0);
  static final TimeCodeRounding DEFAULT_ROUNDING = TimeCodeRounding.CLOSEST;


  private final int minutes;
  private final int seconds;
  private final int frames;
  @Getter(AccessLevel.NONE)
  private final int rawFrames; // == frames except when the sheet was mistakenly filled with 100th of a second instead of [0-74] frames, in which case the value is [0-99]
  private final TimeCodeRounding rounding;


  private TimeCode(int minutes, int seconds, int frames, TimeCodeRounding rounding) {
    this(minutes, seconds, frames, frames, rounding);
  }

  private TimeCode(int minutes, int seconds, int frames, int rawFrames, TimeCodeRounding rounding) {
    validate(minutes, seconds, frames);
    this.minutes = minutes;
    this.seconds = seconds;
    this.frames = frames;
    this.rawFrames = rawFrames;
    this.rounding = rounding;
  }

  private TimeCode(int minutes, int seconds, int frames, int rawFrames) {
    this(minutes, seconds, frames, rawFrames, DEFAULT_ROUNDING);
  }


  public TimeCode(int minutes, int seconds, int frames) {
    this(minutes, seconds, frames, frames, DEFAULT_ROUNDING);
  }

  /**
   * Caution:
   * rounding UP tends to create time ranges exceeding the original duration
   * rounding DOWN tends to create time ranges cropping the original duration
   */
  public TimeCode(long millis, TimeCodeRounding rounding) {
    this(
        (int) (millis / (SECONDS_PER_MINUTE * MILLIS_PER_SECOND)),
        (int) ((millis / MILLIS_PER_SECOND) % SECONDS_PER_MINUTE),
        BigDecimal.valueOf(FRAMES_PER_SECOND * (millis % MILLIS_PER_SECOND))
            .divide(MILLIS_PER_SECOND_BD, requireNonNullElse(rounding, DEFAULT_ROUNDING).roundingMode)
            .intValue(),
        rounding
    );
  }

  public TimeCode(Duration duration, TimeCodeRounding rounding) {
    this(duration.toMillis(), rounding);
  }

  public TimeCode(TimeCode timeCode) {
    this(timeCode.minutes, timeCode.seconds, timeCode.frames, timeCode.rawFrames, timeCode.rounding);
  }

  public static TimeCode ofFrames(int frames) {
    int ff = frames % FRAMES_PER_SECOND;
    int ts = frames / FRAMES_PER_SECOND;
    int mm  = ts / SECONDS_PER_MINUTE;
    int ss = ts % SECONDS_PER_MINUTE;
    return new TimeCode(mm, ss, ff);
  }

  public static void validate(int minutes, int seconds, int frames) {
    // Not controlling upper bound, you can make a cue longer than a CDR length without burning it.
    if (minutes < 0) {
      throw new IllegalArgumentException("Minutes must be in the [0-∞] range");
    }

    if (seconds < 0 || seconds >= SECONDS_PER_MINUTE) {
      throw new IllegalArgumentException("Seconds must be in the [0-59] range");
    }

    if (frames < 0 || frames >= FRAMES_PER_SECOND) {
      throw new IllegalArgumentException("Frames must be in the [0-74] range");
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
   * @param lenient interpolates hundredths of a second identified with frame values in range [75-99] to actual frame value
   * @return
   */
  public static TimeCode parse(String timeCode, boolean lenient) {
    String[] parts = timeCode.split(":");
    int rawMinutes = parseInt(parts[0]); // figures may not be 0-padded
    int rawSeconds = parseInt(parts[1]);
    int rawFrames = parseInt(parts[2]);

    if(lenient && rawFrames >= FRAMES_PER_SECOND) { // seems someone used hundredths of a second instead of frames!
      TimeCode result = new TimeCode(rawMinutes, rawSeconds, scale100to75(rawFrames), rawFrames);
      log.warn("Leniency over {} parsing, using {} as frame part", timeCode, result.frames);
      return result;
    } else {
      return new TimeCode(rawMinutes, rawSeconds, rawFrames);
    }
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
    return new TimeCode(toMillis() - otherMillis, rounding);
  }

  public TimeCode plus(Duration other) {
    return plus(other.toMillis());
  }

  public TimeCode plus(long otherMillis) {
    return new TimeCode(toMillis() + otherMillis, rounding);
  }

  public static int scale100to75(int hundredths) {
    if (hundredths < 0 || hundredths >= HUNDRED) {
      throw new IllegalArgumentException("Hundredths must be in the [0-99] range");
    }
    return (hundredths * FRAMES_PER_SECOND + FRAMES_PER_SECOND - 1) / HUNDRED;
  }

  TimeCode scale100to75() {
    if(isScaled100to75()) {
      return this;
    } else {
      return new TimeCode(minutes, seconds, scale100to75(rawFrames), rawFrames);
    }
  }

  boolean isScaled100to75() {
    return frames != rawFrames;
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
    return format(minutes, seconds, frames);
  }

  String toStringRaw() {
    return format(minutes, seconds, rawFrames);
  }
  private static String format(int minutes, int seconds, int frames) {
    return String.format("%02d:%02d:%02d", minutes, seconds, frames);
  }
}
