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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.nonstatic.audio.AiffInfoSupplier.AiffInfo;
import eu.nonstatic.cue.FaultyStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class AiffInfoSupplierTest implements AudioTestBase {

  @Test
  void should_give_aiff_infos() throws IOException, AudioInfoException {
    AiffInfo aiffInfo = new AiffInfoSupplier().getInfos(AIFF_URL.openStream(), AIFF_NAME);
    assertEquals(Duration.ofMillis(30407L), aiffInfo.getDuration());
  }

  @Test
  void should_throw_read_issue() {
    AiffInfoSupplier infoSupplier = new AiffInfoSupplier();
    AudioInfoException aie = assertThrows(AudioInfoException.class, () -> infoSupplier.getInfos(new FaultyStream(), AIFF_NAME));
    assertTrue(aie.getIssues().isEmpty());
    assertEquals(AIFF_NAME + ": " + IOException.class.getName() + ": reads: 0", aie.getMessage());
  }

  @Test
  void should_throw_on_bad_form_header() throws AudioInfoException {
    ByteBuffer bb = ByteBuffer.allocate(12);
    bb.put("XXXX".getBytes());
    bb.putInt(1234);
    bb.put("AIFF".getBytes());

    AiffInfoSupplier infoSupplier = new AiffInfoSupplier();
    ByteArrayInputStream bais = new ByteArrayInputStream(bb.array());
    IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> infoSupplier.getInfos(bais, AIFF_NAME));
    assertEquals("Not an AIFF file: /audio/Arpeggio.aiff", iae.getMessage());
  }

  @Test
  void should_throw_on_bad_aiff_id_header() throws AudioInfoException {
    ByteBuffer bb = ByteBuffer.allocate(12);
    bb.put("FORM".getBytes());
    bb.putInt(1234);
    bb.put("XXXX".getBytes());

    AiffInfoSupplier infoSupplier = new AiffInfoSupplier();
    ByteArrayInputStream bais = new ByteArrayInputStream(bb.array());
    IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> infoSupplier.getInfos(bais, AIFF_NAME));
    assertEquals("No AIFF id in: /audio/Arpeggio.aiff", iae.getMessage());
  }

  @Test
  void should_search_chunk() throws AudioInfoException {
    ByteBuffer bb = ByteBuffer.allocate(52); // BIG_ENDIAN by default
    bb.put("FORM".getBytes());
    bb.putInt(1234);
    bb.put("AIFF".getBytes());

    bb.put("FOOO".getBytes());
    bb.putInt(6);
    bb.put(new byte[]{1, 2, 3, 4, 5, 6});

    bb.put("COMM".getBytes());
    bb.putInt(18);
    bb.putShort((short) 2);
    bb.putInt(0);
    bb.putShort((short) 0);
    bb.putInt(0);

    AiffInfo infos = new AiffInfoSupplier().getInfos(new ByteArrayInputStream(bb.array()), AIFF_NAME);
    assertEquals(2, infos.getNumChannels());
  }
}
