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

import eu.nonstatic.audio.WaveInfoSupplier.WaveInfo;
import eu.nonstatic.cue.FaultyStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class WaveInfoSupplierTest implements AudioTestBase {

  @Test
  void should_give_wave_infos() throws IOException, AudioInfoException {
    WaveInfo waveInfo = new WaveInfoSupplier().getInfos(WAVE_URL.openStream(), WAVE_NAME);
    assertEquals(Duration.ofMillis(8011L), waveInfo.getDuration());
  }


  @Test
  void should_throw_read_issue() {
    WaveInfoSupplier infoSupplier = new WaveInfoSupplier();
    AudioInfoException aie = assertThrows(AudioInfoException.class, () -> infoSupplier.getInfos(new FaultyStream(), WAVE_NAME));
    assertTrue(aie.getIssues().isEmpty());
    assertEquals(WAVE_NAME + ": " + IOException.class.getName() + ": reads: 0", aie.getMessage());
  }

  @Test
  void should_throw_on_bad_riff_header() throws AudioInfoException {
    ByteBuffer bb = ByteBuffer.allocate(12);
    bb.put("XXXX".getBytes());
    bb.putInt(1234);
    bb.put("WAVE".getBytes());

    WaveInfoSupplier infoSupplier = new WaveInfoSupplier();
    ByteArrayInputStream bais = new ByteArrayInputStream(bb.array());
    IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> infoSupplier.getInfos(bais, AIFF_NAME));
    assertEquals("Not a WAVE file: /audio/Arpeggio.aiff", iae.getMessage());
  }

  @Test
  void should_throw_on_bad_wave_id_header() throws AudioInfoException {
    ByteBuffer bb = ByteBuffer.allocate(12);
    bb.put("RIFF".getBytes());
    bb.putInt(1234);
    bb.put("XXXX".getBytes());

    WaveInfoSupplier infoSupplier = new WaveInfoSupplier();
    ByteArrayInputStream bais = new ByteArrayInputStream(bb.array());
    IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> infoSupplier.getInfos(bais, AIFF_NAME));
    assertEquals("No WAVE id in: /audio/Arpeggio.aiff", iae.getMessage());
  }
}
