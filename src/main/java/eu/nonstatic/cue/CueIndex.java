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

import static eu.nonstatic.cue.TimeCode.FRAMES_PER_SECOND;
import static eu.nonstatic.cue.TimeCode.SECONDS_PER_MINUTE;

import java.time.Duration;
import java.util.Comparator;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class CueIndex implements CueEntity, Comparable<CueIndex> {

  public static final String KEYWORD = CueWords.INDEX;
  public static final int INDEX_PRE_GAP = 0; // also the hidden track index in track 1
  public static final int INDEX_TRACK_START = 1;
  public static final int INDEX_MAX = 99;
  public static final Comparator<Integer> COMPARATOR = Comparator.nullsLast(Comparator.naturalOrder());

  public static final CueIndex PREGAP_ZERO = new CueIndex(INDEX_PRE_GAP, TimeCode.ZERO_SECOND);

  protected Integer number; // 1 is the track start, 0 is the pregap.
  private TimeCode timeCode;

  public CueIndex(int minutes, int seconds, int frames) {
    this(new TimeCode(minutes, seconds, frames));
  }

  public CueIndex(TimeCode timeCode) {
    this(null, timeCode);
  }

  public CueIndex(Integer number, int minutes, int seconds, int frames) {
    this(number, new TimeCode(minutes, seconds, frames));
  }

  public CueIndex(Integer number, TimeCode timeCode) {
    if (number != null) {
      setNumberOnce(number);
    }
    setTimeCode(timeCode);
  }

  public CueIndex deepCopy() {
    return deepCopy(number);
  }

  public CueIndex deepCopy(Integer number) {
    return new CueIndex(number, timeCode);
  }

  protected void setNumberOnce(int number) {
    CueTools.validateIndexRange("Index", number, CueIndex.INDEX_MAX);
    if (this.number != null && this.number != number) {
      throw new IllegalStateException("Index number already set to " + this.number);
    } else {
      this.number = number;
    }
  }

  public static boolean isPreGapOrStart(int number) {
    CueTools.validateIndexRange("Index", number, CueIndex.INDEX_MAX);
    return number == INDEX_PRE_GAP || number == INDEX_TRACK_START;
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

  public void setTimeMillis(long millis, TimeCodeRounding rounding) {
    timeCode = new TimeCode(millis, rounding);
  }

  public String toTimeCode() {
    return timeCode.toString();
  }

  public void setTimeCode(TimeCode timeCode) {
    this.timeCode = Objects.requireNonNull(timeCode, "Index timecode must be provided");
  }

  public void setTimeCode(String timeCode) {
    setTimeCode(TimeCode.parse(timeCode));
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
  public String toSheetLine(CueWriteOptions options) {
    return String.format("%s %02d %s", KEYWORD, number, timeCode.toString());
  }

  @Override
  public String toString() {
    return toSheetLine(null);
  }
}
