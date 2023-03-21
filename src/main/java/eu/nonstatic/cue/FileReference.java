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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is meant to be immutable and shareable between copies of a CueFile.
 * IOW a track-free CueFile
 */
@Slf4j
@Getter
@EqualsAndHashCode(exclude = "sizeDuration") // null-safe
@ToString
class FileReference implements FileReferable {

  static final Map<String, ? extends FileType> AUDIO_TYPES = Map.of(
      "aif",  FileType.Audio.AIFF,
      "aiff", FileType.Audio.AIFF,
      "wav",  FileType.Audio.WAVE,
      "wave", FileType.Audio.WAVE,
      "mp3",  FileType.Audio.MP3,
      "flac", FileType.Audio.FLAC
  );


  protected final String file; // might be a filename, if the file in the same dir as the cuesheet, or may be a path
  protected final FileType type; // MP3, AIFF, WAVE, BIN, MOTOROLA
  protected SizeAndDuration sizeDuration;

  /**
   * Simple ctor when we know only the bare minimum
   */
  public FileReference(String fileNameOrFilePath, FileType type) {
    this.file = fileNameOrFilePath;
    this.type = type;
  }

  public FileReference(File file, TimeCodeRounding rounding) {
    this(file.toPath(), rounding);
  }

  /**
   * @param file
   * @param type only Data subtype allowed for explicit assignment, else audio type will have to be detected
   */
  public FileReference(File file, FileType.Data type) {
    this(file.toPath(), type);
  }

  public FileReference(Path file, TimeCodeRounding rounding) {
    this(file, detectTypeByExtension(file.getFileName().toString()), rounding, false);
  }

  /**
   * @param file
   * @param type only Data subtype allowed for explicit assignment, else audio type will have to be detected
   */
  public FileReference(Path file, FileType.Data type) {
    this(file, type, null, false);
  }

  /**
   * Private because unsafe wrt type
   */
  @SneakyThrows
  private FileReference(Path file, FileType type, TimeCodeRounding rounding, boolean fileLeniency) {
    this(file.toString(), type);
    try {
      this.sizeDuration = new SizeAndDuration(file, rounding, type.isData());
    } catch (IOException e) {
      if(!fileLeniency) {
        throw e;
      }
    }
  }

  public static FileType detectTypeByExtension(String fileName) {
    String ext = getExt(fileName);
    return Objects.requireNonNullElse(AUDIO_TYPES.get(ext), FileType.Data.BINARY);
  }

  @Override
  public void setSizeAndDuration(SizeAndDuration sizeAndDuration) {
    this.sizeDuration = sizeAndDuration;
  }

  /**
   * @param fileAndFormat "\"my file.wav\" WAVE" if the cue sheet contains "FILE \"my file.wav\" WAVE"
   * @param context
   * @return
   */
  static FileReference parse(@NonNull String fileAndFormat, CueReadContext context) {
    String fileName;
    FileType fileType;
    int sep = fileAndFormat.lastIndexOf(' ');
    if (sep >= 0 && fileAndFormat.indexOf('"', sep) < 0) { // checking we aren't going to cut a part of the file name
      fileName = unquote(fileAndFormat.substring(0, sep).trim());
      String type = fileAndFormat.substring(sep + 1);
      fileType = FileType.of(type);
    } else {
      fileName = unquote(fileAndFormat.trim());
      fileType = detectTypeByExtension(fileName);
    }

    // parent may be null if we're loading the file from a stream or the network
    // fileName may be a filename or a complete path
    return fromParentDir(fileName, fileType, context);
  }

  @SneakyThrows
  private static FileReference fromParentDir(String fileOrFileName, FileType fileType, CueReadContext context) {
    Path dir = context.getParent();
    if(dir == null ) { // let's set what we can
      return new FileReference(fileOrFileName, fileType);
    }

    Path filePath = Paths.get(fileOrFileName);
    if(filePath.getNameCount() == 1) { // just a file name
      filePath = dir.resolve(fileOrFileName);
    }

    CueReadOptions options = context.getOptions();
    return new FileReference(filePath, fileType, options.getRounding(), options.isFileLeniency());
  }
}
