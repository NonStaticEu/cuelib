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

import static eu.nonstatic.audio.AudioTestBase.AIFF_URL;
import static eu.nonstatic.audio.AudioTestBase.WAVE_URL;
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
  static final CueContext context = new CueContext(Paths.get(tempDir).resolve("testcue.cue"), StandardCharsets.UTF_8);

  @Test
  void should_parse_compliant_quoted() throws IOException {
    Path audioFile = Files.createTempFile("my file", ".wav");
    String audioFileName = copyFileContents(WAVE_URL, audioFile).getFileName().toString();

    FileReference fileReference = FileReference.parse(String.format("\"%s\" WAVE", audioFileName), context);
    Files.delete(audioFile);

    assertEquals(audioFile.toString(), fileReference.file);
    assertEquals(FileType.Audio.WAVE, fileReference.type);
    assertTrue(fileReference.isSizeAndDurationSet());
  }

  @Test
  void should_parse_compliant_unquoted() throws IOException {
    Path audioFile = Files.createTempFile("my file", ".aiff");
    String audioFileName = copyFileContents(AIFF_URL, audioFile).getFileName().toString();

    FileReference fileReference = FileReference.parse(String.format("%s AIFF", audioFileName), context);
    Files.delete(audioFile);

    assertEquals(audioFile.toString(), fileReference.file);
    assertEquals(FileType.Audio.AIFF, fileReference.type);
    assertTrue(fileReference.isSizeAndDurationSet());
  }

  @Test
  void should_parse_full_path() throws IOException {
    Path audioFile = Files.createTempFile("my file", ".aiff");
    String audioFilePath = copyFileContents(AIFF_URL, audioFile).toString();

    FileReference fileReference = FileReference.parse(String.format("%s AIFF", audioFilePath), context);
    Files.delete(audioFile);

    assertEquals(audioFile.toString(), fileReference.file);
    assertEquals(FileType.Audio.AIFF, fileReference.type);
    assertTrue(fileReference.isSizeAndDurationSet());
  }

  @Test
  void should_parse_not_compliant() {
    FileReference fileReference = FileReference.parse("\"my file.mp3\"", new CueContext("my file.cue", null));
    assertEquals("my file.mp3", fileReference.file);
    assertEquals(Audio.MP3, fileReference.type);
  }

  @Test
  void should_detect_audio() {
    FileType fileType = FileReference.detectTypeByExtension("my file.mp3");
    assertEquals(FileType.Audio.MP3, fileType);
  }

  @Test
  void should_detect_other() {
    FileType fileType = FileReference.detectTypeByExtension("my file.xyz");
    assertEquals(FileType.Data.BINARY, fileType);
  }
}
