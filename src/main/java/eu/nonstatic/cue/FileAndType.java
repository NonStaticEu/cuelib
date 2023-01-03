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

import static eu.nonstatic.cue.CueTools.getExt;
import static eu.nonstatic.cue.CueTools.unquote;

import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * This class is meant to be immutable
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode // null-safe
@ToString
class FileAndType {

  static final Map<String, String> AUDIO_TYPES = Map.of(
    "aiff", FileType.AIFF,
    "wav", FileType.WAVE,
    "mp3", FileType.MP3,
    "flac", FileType.FLAC
  );

  protected final String file;
  protected final String type; // MP3, AIFF, WAVE, BIN, MOTOROLA

  /**
   *
   * @param fileAndFormat "\"my file.wav\" WAVE" if the cue sheet contains "FILE \"my file.wav\" WAVE"
   * @return
   */
  static FileAndType parse(@NonNull String fileAndFormat) {
    String file;
    String format;
    int sep = fileAndFormat.lastIndexOf(' ');
    if (sep >= 0 && fileAndFormat.indexOf('"', sep) < 0) { // checking we aren't going to cut a part of the file name
      file = fileAndFormat.substring(0, sep).trim();
      format = fileAndFormat.substring(sep + 1);
    } else {
      file = fileAndFormat;
      format = null;
    }
    return new FileAndType(unquote(file), format);
  }

  public static FileAndType detect(String file) {
    String ext = getExt(file);
    String type = Objects.requireNonNullElse(AUDIO_TYPES.get(ext), FileType.BINARY);
    return new FileAndType(file, type);
  }
}
