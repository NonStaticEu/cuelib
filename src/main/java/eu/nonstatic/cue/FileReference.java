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
import static eu.nonstatic.cue.SizeAndDuration.getCompactDiscBytesFrom;

import eu.nonstatic.audio.AiffInfoSupplier;
import eu.nonstatic.audio.AudioInfo;
import eu.nonstatic.audio.AudioInfoException;
import eu.nonstatic.audio.AudioInfoSupplier;
import eu.nonstatic.audio.FlacInfoSupplier;
import eu.nonstatic.audio.Mp3InfoSupplier;
import eu.nonstatic.audio.WaveInfoSupplier;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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
 * In other words: a track-free CueFile
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

  private static final Map<String, ? extends AudioInfoSupplier<?>> AUDIO_INFO_SUPPLIERS = Map.of(
      "aif",  new AiffInfoSupplier(),
      "aiff", new AiffInfoSupplier(),
      "wav",  new WaveInfoSupplier(),
      "wave", new WaveInfoSupplier(),
      "mp3",  new Mp3InfoSupplier(),
      "mp2",  new Mp3InfoSupplier(),
      "flac", new FlacInfoSupplier()
  );



  protected final String file; // might be a filename, if the file in the same dir as the cuesheet, or may be a path
  protected final FileType type; // MP3, AIFF, WAVE, BIN, MOTOROLA
  protected SizeAndDuration sizeDuration;

  /**
   * Simple ctor when we know only the bare minimum
   */
  public FileReference(String fileNameOrFilePath, FileType type) {
    this(fileNameOrFilePath, type, null);
  }

  public FileReference(@NonNull String fileNameOrFilePath, @NonNull FileType type, SizeAndDuration sizeDuration) {
    this.file = fileNameOrFilePath;
    this.type = type;
    this.sizeDuration = sizeDuration;
  }

  public FileReference(File file, CueSheetContext context) {
    this(file.toPath(), context);
  }

  public FileReference(File file, FileType type, CueSheetContext context) {
    this(file.toPath(), type, context);
  }

  public FileReference(Path file, CueSheetContext context) {
    this(file, getFileTypeByFileName(file.getFileName().toString()), context);
  }

  public FileReference(Path file, FileType type, CueSheetContext context) {
    this(file.toString(), type, sizeAndDurationOf(file, type, context));
  }

  @Override
  public void setSizeAndDuration(SizeAndDuration sizeAndDuration) {
    this.sizeDuration = sizeAndDuration; // TODO check consistency with type
  }


  public static FileType getFileTypeByFileName(String fileName) {
    String ext = getExt(fileName);
    return getFileTypeByExtension(ext);
  }

  public static FileType getFileTypeByExtension(String ext) {
    return Objects.requireNonNullElse(AUDIO_TYPES.get(ext), FileType.Data.BINARY);
  }

  public static AudioInfoSupplier<?> getInfoSupplierByFileName(String fileName) {
    String ext = getExt(fileName);
    return getInfoSupplierByExtension(ext);
  }

  public static AudioInfoSupplier<?> getInfoSupplierByExtension(String ext) {
    return AUDIO_INFO_SUPPLIERS.get(ext);
  }


  @SneakyThrows
  public static SizeAndDuration sizeAndDurationOf(Path file, FileType type, CueSheetContext context) {
    CueOptions options = context.getOptions();
    SizeAndDuration sizeDuration = null;
    try {
      AudioInfoSupplier<?> audioInfoSupplier = getInfoSupplierByFileName(file.getFileName().toString());
      if(audioInfoSupplier != null && type.isAudio()) {
        AudioInfo audioInfos = audioInfoSupplier.getInfos(file);
        Duration duration = audioInfos.getDuration();
        long size = getCompactDiscBytesFrom(duration, options.getRounding());
        sizeDuration = new SizeAndDuration(size, duration);
        audioInfos.getIssues().forEach(issue -> context.addError(issue.toString()));
      } else {
        sizeDuration = new SizeAndDuration(Files.size(file));
      }
    /* TODO useful ?
    } catch (UnsupportedEncodingException e) {
    if(!options.isFileLeniency()) {
      throw e;
    }
    */
    } catch (AudioInfoException e) {
      e.getIssues().forEach(issue -> context.addError(issue.toString()));
    }
    return sizeDuration;
  }
}
