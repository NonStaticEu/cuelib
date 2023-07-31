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

import java.time.Duration;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class SizeAndDuration {

  private static final long BITS_PER_BYTE = 8;
  private static final long NANOS_PER_SECOND = 1_000_000_000L;

  public static final int CD_FREQUENCY = 44100;
  public static final int CD_BITS_PER_SAMPLE = 16;
  public static final int CD_CHANNELS = 2;
  public static final int CD_FRAMES_PER_SECOND = 75;

  public static final long CD_BYTES_PER_SAMPLE = CD_BITS_PER_SAMPLE/BITS_PER_BYTE; // 2 bytes / channel
  public static final long CD_BYTES_PER_SECOND = (CD_FREQUENCY * CD_BITS_PER_SAMPLE * CD_CHANNELS) / BITS_PER_BYTE; // 176400 bytes / sec
  public static final long CD_BYTES_PER_FRAME = CD_BYTES_PER_SECOND / CD_FRAMES_PER_SECOND; // 2352 bytes


  protected long size; // (projected) file size *on the CD* to check if the cuesheet fits on a CD(R)
  protected Duration duration; // only for an audio file, so we can calculate the last track's duration


  public SizeAndDuration(long size) {
    this(size, null);
  }

  /**
   * protected: don't want anyone to mess with unrelated values
   */
  protected SizeAndDuration(long size, Duration duration) {
    this.size = size;
    this.duration = duration;
  }

  public SizeAndDuration(@NonNull Duration duration, TimeCodeRounding rounding) {
    this(getCompactDiscBytesFrom(duration, rounding), duration);
  }


  /**
   * Transforms an (audio) duration into frame-aligned bytes on a CDR (each frame being 1/75th of a second, that is 2352 bytes)
   * @param duration audio duration
   * @return bytes on disc
   */
  public static long getCompactDiscBytesFrom(Duration duration, TimeCodeRounding rounding) {
    return getCompactDiscBytesFrom(new TimeCode(duration, rounding));
  }

  public static long getCompactDiscBytesFrom(TimeCode timeCode) {
    return timeCode.toFrameCount() * CD_BYTES_PER_FRAME;
  }

  public static Duration getDurationFromCompactDiscBytes(long bytes) {
    return Duration.ofNanos((bytes * NANOS_PER_SECOND) / CD_BYTES_PER_SECOND);
  }
}
