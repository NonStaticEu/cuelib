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
import static eu.nonstatic.cue.CueTools.isCaseInsensitiveFileSystem;
import static eu.nonstatic.cue.SizeAndDuration.getCompactDiscBytesFrom;

import eu.nonstatic.audio.AudioInfo;
import eu.nonstatic.audio.AudioInfoException;
import eu.nonstatic.audio.AudioInfoSupplier;
import eu.nonstatic.audio.AudioInfoSuppliers;
import eu.nonstatic.audio.AudioIssue;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is meant to be immutable and shareable between copies of a CueFile.
 * In other words: a track-free CueFile
 */
@Slf4j
@Getter
@EqualsAndHashCode(exclude = "sizeAndDuration") // null-safe
@ToString
class FileReference implements FileReferable {

  private static final Map<String, FileType> AUDIO_TYPES = new HashMap<>(); // allows get(null)

  static {
    AUDIO_TYPES.put("aif",  FileType.Audio.AIFF);
    AUDIO_TYPES.put("aiff", FileType.Audio.AIFF);
    AUDIO_TYPES.put("wav",  FileType.Audio.WAVE);
    AUDIO_TYPES.put("wave", FileType.Audio.WAVE);
    AUDIO_TYPES.put("mp3",  FileType.Audio.MP3);
    AUDIO_TYPES.put("flac", FileType.Audio.FLAC);
  }

  protected final String file; // might be a filename, if the file in the same dir as the cuesheet, or may be a path
  protected final FileType type; // MP3, AIFF, WAVE, BIN, MOTOROLA
  protected SizeAndDuration sizeAndDuration;

  /**
   * Simple ctor when we know only the bare minimum
   */
  public FileReference(String fileNameOrFilePath, FileType type) {
    this(fileNameOrFilePath, type, null);
  }

  public FileReference(@NonNull String fileNameOrFilePath, @NonNull FileType type, SizeAndDuration sizeAndDuration) {
    this.file = fileNameOrFilePath;
    this.type = type;
    this.sizeAndDuration = sizeAndDuration;
  }

  public FileReference(File file, CueSheetContext context) throws IOException {
    this(file.toPath(), context);
  }

  public FileReference(File file, FileType type, CueSheetContext context) throws IOException {
    this(file.toPath(), type, context);
  }

  public FileReference(Path file, CueSheetContext context) throws IOException {
    this(file, getTypeByFileName(file.getFileName().toString()), context);
  }

  public FileReference(Path file, FileType type, CueSheetContext context) throws IOException {
    this(file.toString(), type, sizeAndDurationOf(file, type, context));
  }

  public void setSizeAndDuration(SizeAndDuration sizeAndDuration) {
    if(sizeAndDuration != null && sizeAndDuration.duration == null && type.isAudio()) {
      throw new IllegalArgumentException("Duration must be provided for audio types");
    } else {
      this.sizeAndDuration = sizeAndDuration;
    }
  }


  public static FileType getTypeByFileName(String fileName) {
    String ext = getExt(fileName);
    return getTypeByExtension(ext);
  }

  public static FileType getTypeByExtension(String ext) {
    return Optional.of(ext)
        .map(e -> e.toLowerCase(Locale.ROOT))
        .map(AUDIO_TYPES::get)
        .orElse(FileType.Data.BINARY);
  }


  interface SizeAndDurationSupplier {
    SizeAndDuration get() throws IllegalArgumentException, IOException, AudioInfoException;
  }

  public static SizeAndDuration sizeAndDurationOf(Path file, FileType type, CueSheetContext context) throws IOException {
    CueOptions options = context.getOptions();

    AudioInfoSupplier<?> audioInfoSupplier;
    try {
      audioInfoSupplier = type.isAudio() ? AudioInfoSuppliers.getByFileName(file.getFileName().toString()) : null;
    } catch(IllegalArgumentException e) { // file extension cannot be matched to a recognized audio format
      addIssue(AudioIssue.other(0, e), context);
      return null;
    }

    if(!checkFileExists(file, context)) {
      return null;
    }

    SizeAndDurationSupplier sizeAndDurationSupplier;
    if(audioInfoSupplier != null) {  // it's audio
      sizeAndDurationSupplier = () -> {
        AudioInfo audioInfos = audioInfoSupplier.getInfos(file);
        addIssues(audioInfos.getIssues(), context);
        Duration duration = audioInfos.getDuration();
        long size = getCompactDiscBytesFrom(duration, options.getRounding());
        return new SizeAndDuration(size, duration);
      };
    } else { // it's data
      sizeAndDurationSupplier = () -> new SizeAndDuration(Files.size(file));
    }

    try {
      return sizeAndDurationSupplier.get();
    } catch(IllegalArgumentException e) { // eg: an audio file is not what its extension claims it is
      context.addIssue(e);
    } catch (AudioInfoException e) {
      addIssues(e, context);
    }
    // NoSuchFile was already handled. Let other IOException types be thrown
    return null;
  }

  private static boolean checkFileExists(Path file, CueSheetContext context) throws IOException {
    CueOptions options = context.getOptions();
    // Case where the cue sheet references a file that has a different case on the file system. Windows will find it, not a Posix OS.
    if(isCaseInsensitiveFileSystem(file.getFileSystem())) {
      try {
        if(!file.toRealPath().getFileName().toString().equals(file.getFileName().toString())) { // file exists but has a different case on the file system
          String message = String.format("File exists, with a different case: %s", file);
          if(options.isFileLeniency()) {
            context.addIssue(message);
          } else {
            throw new IOException(message);
          }
        }
      } catch (IOException e) { // File not found obviously
        if(options.isFileLeniency()) {
          context.addIssue(e);
          return false;
        } else {
          throw e;
        }
      }
    } else if(!Files.exists(file)) {
      NoSuchFileException e = new NoSuchFileException(file.toString());
      if(options.isFileLeniency()) {
        context.addIssue(e);
      } else {
        throw e;
      }
      return false;
    }
    return true;
  }

  private static void addIssues(AudioInfoException e, CueSheetContext context) {
    List<AudioIssue> issues = e.getIssues();
    addIssues(issues, context);

    Throwable cause = e.getCause();
    if(issues.isEmpty() && cause != null) {
      context.addIssue(cause);
    }
  }

  private static void addIssues(List<AudioIssue> issues, CueSheetContext context) {
    for (AudioIssue issue : issues) {
      addIssue(issue, context);
    }
  }

  private static void addIssue(AudioIssue issue, CueSheetContext context) {
    CueSheetIssue cueSheetIssue = new CueSheetIssue(issue.toString(), issue.getCause());
    context.addIssue(cueSheetIssue);
  }
}
