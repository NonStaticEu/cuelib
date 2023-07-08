/**
 * Cuelib
 * Copyright (C) 2022 NonStatic
 *
 * This file is part of cuelib.
 * cuelib is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with . If not, see <https://www.gnu.org/licenses/>.
 */
package eu.nonstatic.audio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.nonstatic.audio.AudioIssue.Type;
import eu.nonstatic.audio.Mp3InfoSupplier.Mp3Info;
import eu.nonstatic.cue.FaultyStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class Mp3InfoSupplierTest implements AudioTestBase {

  @Test
  void should_give_mp3_infos() throws IOException, AudioInfoException {
    Mp3Info mp3Info = new Mp3InfoSupplier().getInfos(MP3_URL.openStream(), MP3_NAME);
    assertFalse(mp3Info.isIncomplete());
    assertEquals(Duration.ofNanos(11154285714L), mp3Info.getDuration());
  }

  @Test
  void should_throw_empty_file() {
    Mp3InfoSupplier infoSupplier = new Mp3InfoSupplier();
    ByteArrayInputStream emptyStream = new ByteArrayInputStream(new byte[]{});
    IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> infoSupplier.getInfos(emptyStream, MP3_NAME));
    assertEquals("Could not find a single MP3 frame: /audio/Moog-juno-303-example.mp3", iae.getMessage());
  }

  @Test
  void should_throw_read_issue() {
    Mp3InfoSupplier infoSupplier = new Mp3InfoSupplier();
    AudioInfoException aie = assertThrows(AudioInfoException.class, () -> infoSupplier.getInfos(new FaultyStream(), MP3_NAME));
    assertTrue(aie.getIssues().isEmpty());
    assertEquals(MP3_NAME + ": " + IOException.class.getName() + ": reads: 0", aie.getMessage());
  }

  @Test
  void should_give_mp3_infos_on_incomplete_file() throws IOException, AudioInfoException {
    byte[] bytes;
    try(InputStream is = MP3_URL.openStream()) {
      bytes = is.readAllBytes();
    }

    Duration fullDuration = new Mp3InfoSupplier().getInfos(new ByteArrayInputStream(bytes), MP3_NAME + ":complete").getDuration();

    int incompleteLength = bytes.length - 50;
    try(ByteArrayInputStream incompleteStream = new ByteArrayInputStream(bytes, 0, incompleteLength)) {
      Mp3Info incompleteInfos = new Mp3InfoSupplier().getInfos(incompleteStream, MP3_NAME + ":incomplete");

      assertTrue(incompleteInfos.isIncomplete());
      List<AudioIssue> issues = incompleteInfos.getIssues();
      assertEquals(1, issues.size());
      AudioIssue issue = issues.get(0);
      assertEquals(Type.EOF, issue.getType());
      assertEquals(incompleteLength + 1, issue.getLocation());
      assertEquals(0, issue.getSkipped());
      assertEquals("AudioIssue EOF at 210420", issue.toString());

      Duration incompleteDuration = incompleteInfos.getDuration();
      assertEquals(Duration.ofNanos(11128163265L), incompleteDuration);
      // That's one Layer III frame less
      assertEquals(Math.round(1152*(1_000_000_000.0)/44100), fullDuration.minus(incompleteDuration).toNanos());
    }
  }

  @Test
  void should_give_mp3_infos_on_out_of_synch_file() throws IOException, AudioInfoException {
    byte[] bytes;
    try(InputStream is = MP3_URL.openStream()) {
      bytes = is.readAllBytes();
    }

    Duration fullDuration = new Mp3InfoSupplier().getInfos(new ByteArrayInputStream(bytes), MP3_NAME + ":complete").getDuration();

    int split = 1515, missing = 50;
    ByteBuffer bb = ByteBuffer.allocate(bytes.length - missing);
    bb.put(bytes, 0, split);
    bb.put(bytes, split+missing, bytes.length - missing - split);

    try(ByteArrayInputStream faultyStream = new ByteArrayInputStream(bb.array())) {
      Mp3Info incompleteInfos = new Mp3InfoSupplier().getInfos(faultyStream, MP3_NAME + ":outofsynch");

      assertFalse(incompleteInfos.isIncomplete());

      List<AudioIssue> issues = incompleteInfos.getIssues();
      assertEquals(1, issues.size());
      AudioIssue issue = issues.get(0);
      assertEquals(Type.SYNC, issue.getType());
      assertEquals(1930, issue.getLocation());
      assertEquals(359, issue.getSkipped());
      assertEquals("AudioIssue Resynchro at 1930, skipped 359", issue.toString());

      Duration incompleteDuration = incompleteInfos.getDuration();
      assertEquals(Duration.ofNanos(11128163265L), incompleteDuration);
      // That's one Layer III frame less
      assertEquals(Math.round(1152*(1_000_000_000.0)/44100), fullDuration.minus(incompleteDuration).toNanos());
    }
  }

  @Test
  void should_encode_synch_safe_bytes() {
    assertArrayEquals(new byte[]{9, -74, 100, 119}, Mp3InfoSupplier.toSynchSafeBytes(19772023));
  }
}
