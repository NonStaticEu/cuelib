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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class FileAndTypeTest {

  @Test
  void should_parse_compliant_quoted() {
    FileAndType fileAndType = FileAndType.parse("\"my file.wav\" WAVE");
    assertEquals("my file.wav", fileAndType.file);
    assertEquals(FileType.WAVE, fileAndType.type);
  }

  @Test
  void should_parse_compliant_unquoted() {
    FileAndType fileAndType = FileAndType.parse("my_file.aiff AIFF");
    assertEquals("my_file.aiff", fileAndType.file);
    assertEquals(FileType.AIFF, fileAndType.type);
  }

  @Test
  void should_parse_not_compliant() {
    FileAndType fileAndType = FileAndType.parse("\"my file.mp3\"");
    assertEquals("my file.mp3", fileAndType.file);
    assertNull(fileAndType.type);
  }


  @Test
  void should_detect_audio() {
    FileAndType fileAndType = FileAndType.detect("my file.mp3");
    assertEquals("my file.mp3", fileAndType.file);
    assertEquals(FileType.MP3, fileAndType.type);
  }

  @Test
  void should_detect_other() {
    FileAndType fileAndType = FileAndType.detect("my file.xyz");
    assertEquals("my file.xyz", fileAndType.file);
    assertEquals(FileType.BINARY, fileAndType.type);
  }
}
