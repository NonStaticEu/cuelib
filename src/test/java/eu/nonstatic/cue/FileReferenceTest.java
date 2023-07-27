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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.nonstatic.cue.FileType.Audio;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class FileReferenceTest extends CueTestBase {

  static final String tempDir = System.getProperty("java.io.tmpdir");
  static final CueSheetContext context;

  static {
    CueOptions options = new CueOptions(StandardCharsets.UTF_8);
    context = new CueSheetContext(Paths.get(tempDir).resolve("testcue.cue"), options);
  }

  @Test
  void should_parse_compliant_quoted() throws IOException {
    Path audioFile = Files.createTempFile("my file", ".wav");
    String audioFileName = copyFileContents(WAVE_URL, audioFile).getFileName().toString();

    FileReference fileReference = CueFile.parse(String.format("\"%s\" WAVE", audioFileName), context);
    Files.delete(audioFile);

    assertEquals(audioFile.toString(), fileReference.file);
    assertEquals(FileType.Audio.WAVE, fileReference.type);
    assertTrue(fileReference.isSizeAndDurationSet());
  }

  @Test
  void should_parse_compliant_unquoted() throws IOException {
    Path audioFile = Files.createTempFile("my file", ".aiff");
    String audioFileName = copyFileContents(AIFF_URL, audioFile).getFileName().toString();

    FileReference fileReference = CueFile.parse(String.format("%s AIFF", audioFileName), context);
    Files.delete(audioFile);

    assertEquals(audioFile.toString(), fileReference.file);
    assertEquals(FileType.Audio.AIFF, fileReference.type);
    assertTrue(fileReference.isSizeAndDurationSet());
  }

  @Test
  void should_parse_full_path() throws IOException {
    Path audioFile = Files.createTempFile("my file", ".aiff");
    String audioFilePath = copyFileContents(AIFF_URL, audioFile).toString();

    FileReference fileReference = CueFile.parse(String.format("%s AIFF", audioFilePath), context);
    Files.delete(audioFile);

    assertEquals(audioFile.toString(), fileReference.file);
    assertEquals(FileType.Audio.AIFF, fileReference.type);
    assertTrue(fileReference.isSizeAndDurationSet());
  }

  @Test
  void should_parse_not_compliant() {
    FileReference fileReference = CueFile.parse("\"my file.mp3\"", new CueSheetContext("my file.cue", null));
    assertEquals("my file.mp3", fileReference.file);
    assertEquals(Audio.MP3, fileReference.type);
  }

  @Test
  void should_detect_audio() {
    FileType fileType = FileReference.getFileTypeByFileName("my file.mp3");
    assertEquals(FileType.Audio.MP3, fileType);
  }

  @Test
  void should_detect_other() {
    FileType fileType = FileReference.getFileTypeByFileName("my file.xyz");
    assertEquals(FileType.Data.BINARY, fileType);
  }

  /*
  @Test
  void should_get_from_audio_file() throws IOException {
    Path audioFile = Files.createTempFile("my file", ".flac");
    copyFileContents(FLAC_URL, audioFile);
    SizeAndDuration sd = new SizeAndDuration(audioFile.toFile(), TimeCodeRounding.DOWN);
    Files.delete(audioFile);

    assertEquals(649152L, sd.size);
    assertEquals(Duration.ofMillis(3692L), sd.duration);
  }

  @Test
  void should_get_from_audio_path() throws IOException {
    Path audioFile = Files.createTempFile("my file", ".flac");
    copyFileContents(FLAC_URL, audioFile);
    SizeAndDuration sd = new SizeAndDuration(audioFile, TimeCodeRounding.DOWN);
    Files.delete(audioFile);

    assertEquals(649152L, sd.size);
    assertEquals(Duration.ofMillis(3692L), sd.duration);
  }

  @Test
  void should_get_from_binary_path() throws IOException {
    Path binFile = Files.createTempFile("my file", ".bin");
    try(BufferedWriter bw = Files.newBufferedWriter(binFile)) {
      bw.append("You're never gonna figure out who's on first base, because Who is on first base.");
    }
    SizeAndDuration sd = new SizeAndDuration(binFile, TimeCodeRounding.DOWN);
    Files.delete(binFile);

    assertEquals(80L, sd.size);
    assertNull(sd.duration);
  }

  @Test
  void should_force_binary() throws IOException {
    Path audioFile = Files.createTempFile("my file", ".flac");
    copyFileContents(FLAC_URL, audioFile);
    SizeAndDuration sd = new SizeAndDuration(audioFile, TimeCodeRounding.DOWN, true);
    Files.delete(audioFile);

    assertEquals(484667L, sd.size);
    assertNull(sd.duration);
  }
   */
}
