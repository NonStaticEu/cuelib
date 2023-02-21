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

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

/**²
 * see https://www.gnu.org/software/ccd2cue/manual/html_node/FILE-_0028CUE-Command_0029.html#FILE-_0028CUE-Command_0029
 */
public interface FileType {

  Map<String, ? extends FileType> ALL = Stream.of(FileType.Data.BINARY, FileType.Data.MOTOROLA, FileType.Audio.AIFF, FileType.Audio.WAVE, FileType.Audio.MP3, FileType.Audio.FLAC)
                .collect(Collectors.toUnmodifiableMap(FileType::getValue, Function.identity()));

  String getValue();
  boolean isData();
  boolean isAudio();


  static FileType of(String type) {
    if(type == null) {
      return Data.BINARY;
    }

    FileType fileType = ALL.get(type);
    if(fileType != null) {
      return fileType;
    } else {
      throw new IllegalArgumentException("Unsupported fileType: " + type);
    }
  }


  final class Data implements FileType {
    public static final Data BINARY = new Data("BINARY"); // raw little endian
    public static final Data MOTOROLA = new Data("MOTOROLA"); // raw big endian

    @Getter
    private final String value;

    public Data(String value) {
      this.value = value;
    }

    @Override
    public boolean isData() {
      return true;
    }

    @Override
    public boolean isAudio() {
      return false;
    }

    @Override
    public String toString() {
      return value;
    }
  }


  final class Audio implements FileType {
    public static final Audio AIFF = new Audio("AIFF");
    public static final Audio WAVE = new Audio("WAVE");
    public static final Audio MP3 = new Audio("MP3");
    public static final Audio FLAC = new Audio("FLAC"); // other code I read doesn't list it, but I don't see why not

    @Getter
    private final String value;

    public Audio(String value) {
      this.value = value;
    }

    @Override
    public boolean isData() {
      return false;
    }

    @Override
    public boolean isAudio() {
      return true;
    }

    @Override
    public String toString() {
      return value;
    }
  }
}
