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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.nonstatic.audio.AudioFormatException;
import eu.nonstatic.cue.FileType.Audio;
import eu.nonstatic.cue.FileType.Data;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class FileReferenceTest extends CueTestBase {

  static final String tempDir = System.getProperty("java.io.tmpdir");
  static final CueOptions options = new CueOptions(StandardCharsets.UTF_8);

  private static CueSheetContext newCueSheetContext() {
    return new CueSheetContext(Paths.get(tempDir).resolve("testcue.cue"), options);
  }

  @Test
  void should_parse_compliant_quoted() throws IOException {

    Path audioFile = Files.createTempFile("my file", ".wav");
    String audioFileName = copyFileContents(WAVE_URL, audioFile).getFileName().toString();

    FileReference fileReference = CueFile.parse(String.format("\"%s\" WAVE", audioFileName), newCueSheetContext());
    Files.delete(audioFile);

    assertEquals(audioFile.toString(), fileReference.file);
    assertEquals(FileType.Audio.WAVE, fileReference.type);
    assertTrue(fileReference.isSizeAndDurationSet());
  }

  @Test
  void should_parse_compliant_unquoted() throws IOException {
    Path audioFile = Files.createTempFile("my file", ".aiff");
    String audioFileName = copyFileContents(AIFF_URL, audioFile).getFileName().toString();

    FileReference fileReference = CueFile.parse(String.format("%s AIFF", audioFileName), newCueSheetContext());
    Files.delete(audioFile);

    assertEquals(audioFile.toString(), fileReference.file);
    assertEquals(FileType.Audio.AIFF, fileReference.type);
    assertTrue(fileReference.isSizeAndDurationSet());
  }

  @Test
  void should_parse_full_path() throws IOException {
    Path audioFile = Files.createTempFile("my file", ".aiff");
    String audioFilePath = copyFileContents(AIFF_URL, audioFile).toString();

    FileReference fileReference = CueFile.parse(String.format("%s AIFF", audioFilePath), newCueSheetContext());
    Files.delete(audioFile);

    assertEquals(audioFile.toString(), fileReference.file);
    assertEquals(FileType.Audio.AIFF, fileReference.type);
    assertTrue(fileReference.isSizeAndDurationSet());
  }

  @Test
  void should_parse_not_compliant() throws IOException {
    FileReference fileReference = CueFile.parse("\"my file.mp3\"", new CueSheetContext("my file.cue", null));
    assertEquals("my file.mp3", fileReference.file);
    assertEquals(Audio.MP3, fileReference.type);
  }

  @Test
  void should_detect_audio() {
    FileType fileType = FileReference.getTypeByFileName("my file.mp3");
    assertEquals(FileType.Audio.MP3, fileType);
  }

  @Test
  void should_detect_other() {
    FileType fileType = FileReference.getTypeByFileName("my file.xyz");
    assertEquals(FileType.Data.BINARY, fileType);
  }

  @Test
  void should_get_from_audio_file() throws IOException {
    Path audioFile = Files.createTempFile("my file", ".flac");
    copyFileContents(FLAC_URL, audioFile);

    SizeAndDuration sd = new FileReference(audioFile.toFile(), newCueSheetContext()).sizeAndDuration;
    Files.delete(audioFile);

    assertEquals(649152L, sd.size);
    assertEquals(Duration.ofMillis(3692L), sd.duration);
  }

  @Test
  void should_get_from_audio_path() throws IOException {
    Path audioFile = Files.createTempFile("my file", ".flac");
    copyFileContents(FLAC_URL, audioFile);

    SizeAndDuration sd = new FileReference(audioFile, newCueSheetContext()).sizeAndDuration;
    Files.delete(audioFile);

    assertEquals(649152L, sd.size);
    assertEquals(Duration.ofMillis(3692L), sd.duration);
  }

  @Test
  void should_handle_bad_file_argument() throws IOException {
    Path audioFile = Files.createTempFile("my file", ".mp3");
    try(BufferedWriter bw = Files.newBufferedWriter(audioFile)) {
      bw.append("garbage");
    }

    CueSheetContext context = newCueSheetContext();
    FileReference fileReference = new FileReference(audioFile, context);
    Files.delete(audioFile);

    assertNull(fileReference.sizeAndDuration);
    List<CueSheetIssue> issues = context.getIssues();
    assertEquals(1, issues.size());
    assertEquals(AudioFormatException.class, issues.get(0).getCause().getClass());
    assertEquals("Could not find a single frame at 0: " + audioFile, issues.get(0).getCause().getMessage());
  }

  @Test
  void should_handle_audio_issues() throws IOException {
    Path audioFile = Files.createTempFile("my file", ".aiff");
    try(DataOutputStream os = new DataOutputStream(Files.newOutputStream(audioFile))) {
      os.write("FORM".getBytes());
      os.write(ByteBuffer.allocate(4).putInt(1234).array());
      os.write("AIFF".getBytes());
      os.write("COMM".getBytes());
      os.writeInt(18);
      os.writeShort(2);
      // incomplete !
    }

    CueSheetContext context = newCueSheetContext();
    FileReference fileReference = new FileReference(audioFile, context);
    Files.delete(audioFile);

    assertNull(fileReference.sizeAndDuration);
    List<CueSheetIssue> issues = context.getIssues();
    assertEquals(1, issues.size());
    assertEquals("AudioIssue EOF at 21", issues.get(0).getMessage());
    assertEquals(EOFException.class, issues.get(0).getCause().getClass());
  }

  @Test
  void should_get_from_binary_path() throws IOException {
    Path binFile = Files.createTempFile("my file", ".bin");
    try(BufferedWriter bw = Files.newBufferedWriter(binFile)) {
      bw.append("You're never gonna figure out who's on first base, because Who is on first base.");
    }

    SizeAndDuration sd = new FileReference(binFile, newCueSheetContext()).sizeAndDuration;
    Files.delete(binFile);

    assertEquals(80L, sd.size);
    assertNull(sd.duration);
  }

  @Test
  void should_force_binary() throws IOException {
    Path audioFile = Files.createTempFile("my file", ".flac");
    copyFileContents(FLAC_URL, audioFile);

    SizeAndDuration sd = new FileReference(audioFile, Data.BINARY, newCueSheetContext()).sizeAndDuration;
    Files.delete(audioFile);

    assertEquals(484667L, sd.size);
    assertNull(sd.duration);
  }

  @Test
  void should_handle_audio_file_not_found() throws IOException {
    Path file = Files.createTempFile("my file", ".flac");
    Files.delete(file);

    CueSheetContext context = new CueSheetContext("whatever", CueOptions.builder().fileLeniency(false).build());
    assertThrows(NoSuchFileException.class, () -> new FileReference(file, context));
    assertFalse(context.isIssues());


    CueSheetContext lenientContext = new CueSheetContext("whatever", CueOptions.builder().fileLeniency(true).build());
    FileReference fileReference = new FileReference(file, lenientContext);
    assertNull(fileReference.sizeAndDuration);

    List<CueSheetIssue> issues = lenientContext.getIssues();
    assertEquals(1, issues.size());
    assertTrue(issues.get(0).getCause() instanceof NoSuchFileException);
    assertEquals(NoSuchFileException.class.getName() + ": " + file, issues.get(0).getMessage());
  }

  @Test
  void should_handle_binary_file_not_found() throws IOException {
    Path file = Files.createTempFile("my file", ".xyz");
    Files.delete(file);

    CueSheetContext context = new CueSheetContext("whatever", CueOptions.builder().fileLeniency(false).build());
    assertThrows(NoSuchFileException.class, () -> new FileReference(file, context));
    assertFalse(context.isIssues());


    CueSheetContext lenientContext = new CueSheetContext("whatever", CueOptions.builder().fileLeniency(true).build());
    FileReference fileReference = new FileReference(file, lenientContext);
    assertNull(fileReference.sizeAndDuration);

    List<CueSheetIssue> issues = lenientContext.getIssues();
    assertEquals(1, issues.size());
    assertTrue(issues.get(0).getCause() instanceof NoSuchFileException);
    assertEquals(NoSuchFileException.class.getName() + ": " + file, issues.get(0).getMessage());
  }

  @Test
  void should_refuse_incomplete_size_and_duration() {
    SizeAndDuration audioSD = new SizeAndDuration(123, Duration.ofMinutes(1));
    SizeAndDuration dataSD = new SizeAndDuration(123);

    FileReference audioFR = new FileReference("name", Audio.MP3);
    FileReference dataFR = new FileReference("name", Data.BINARY);

    assertThrows(IllegalArgumentException.class, () -> audioFR.setSizeAndDuration(dataSD));
    assertDoesNotThrow(() -> audioFR.setSizeAndDuration(audioSD));

    assertDoesNotThrow(() -> dataFR.setSizeAndDuration(dataSD));
    assertDoesNotThrow(() -> dataFR.setSizeAndDuration(null));
    assertDoesNotThrow(() -> dataFR.setSizeAndDuration(audioSD));

  }
}
