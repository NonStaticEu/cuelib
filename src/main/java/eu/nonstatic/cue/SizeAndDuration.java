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

import eu.nonstatic.audio.*;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static eu.nonstatic.cue.CueTools.getExt;

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

  private static final Map<String, ? extends AudioInfoSupplier<?>> AUDIO_INFO_SUPPLIERS = Map.of(
      "aif",  new AiffInfoSupplier(),
      "aiff", new AiffInfoSupplier(),
      "wav",  new WaveInfoSupplier(),
      "wave", new WaveInfoSupplier(),
      "mp3",  new Mp3InfoSupplier(),
      "mp2",  new Mp3InfoSupplier(),
      "flac", new FlacInfoSupplier()
  );

  protected final Long size; // (projected) file size *on the CD* to check if the cuesheet fits on a CD(R)
  protected final Duration duration; // only for an audio file, so we can calculate the last track's duration


  public SizeAndDuration(long size) {
    this.size = size;
    this.duration = null;
  }

  public SizeAndDuration(Duration duration, TimeCodeRounding rounding) {
    this.size = getCompactDiscBytesFor(duration, rounding);
    this.duration = duration;
  }

  public SizeAndDuration(SizeAndDuration sizeAndDuration) {
    this.size = sizeAndDuration.size;
    this.duration = sizeAndDuration.duration;
  }

  public SizeAndDuration(File file, TimeCodeRounding rounding) throws IOException {
    this(file, rounding, false);
  }

  public SizeAndDuration(File file, TimeCodeRounding rounding, boolean forceBinary) throws IOException {
    this(file.toPath(), rounding, forceBinary);
  }

  public SizeAndDuration(Path file, TimeCodeRounding rounding) throws IOException {
    this(file, rounding, false);
  }

  public SizeAndDuration(Path file, TimeCodeRounding rounding, boolean forceBinary) throws IOException {
    String ext = getExt(file.getFileName().toString());
    AudioInfoSupplier<?> audioInfoSupplier = AUDIO_INFO_SUPPLIERS.get(ext);
    if(audioInfoSupplier != null && !forceBinary) {
      AudioInfo audioInfos = audioInfoSupplier.getInfos(file);
      this.duration = audioInfos.getDuration();
      this.size = getCompactDiscBytesFor(this.duration, rounding);
    } else {
      this.duration = null;
      this.size = Files.size(file);
    }
  }

  /**
   * Transforms an (audio) duration into frame-aligned bytes on a CDR (each frame being 1/75th of a second, that is 2352 bytes)
   * @param duration
   * @return bytes on disc
   */
  public static long getCompactDiscBytesFor(Duration duration, TimeCodeRounding rounding) {
    return getCompactDiscBytesFor(new TimeCode(duration, rounding));
  }

  public static long getCompactDiscBytesFor(TimeCode timeCode) {
    return timeCode.toFrameCount() * CD_BYTES_PER_FRAME;
  }

  public static Duration getDurationForCompactDiscBytes(long bytes) {
    return Duration.ofNanos((bytes * NANOS_PER_SECOND) / CD_BYTES_PER_SECOND);
  }
}
