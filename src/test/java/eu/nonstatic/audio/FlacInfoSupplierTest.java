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

import eu.nonstatic.audio.FlacInfoSupplier.FlacInfo;
import eu.nonstatic.cue.FaultyStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class FlacInfoSupplierTest implements AudioTestBase {

  @Test
  void should_give_flac_infos() throws IOException, AudioInfoException {
    FlacInfo flacInfo = new FlacInfoSupplier().getInfos(FLAC_URL.openStream(), FLAC_NAME);
    assertEquals(Duration.ofMillis(3692L), flacInfo.getDuration());
  }

  @Test
  void should_throw_read_issue() {
    FlacInfoSupplier infoSupplier = new FlacInfoSupplier();

    AudioInfoException aie = assertThrows(AudioInfoException.class, () -> infoSupplier.getInfos(new FaultyStream(), FLAC_NAME));
    assertTrue(aie.getIssues().isEmpty());
    assertEquals(FLAC_NAME + ": " + IOException.class.getName() + ": reads: 0", aie.getMessage());
  }

  @Test
  void should_throw_on_bad_flac_header() throws AudioInfoException {
    ByteBuffer bb = ByteBuffer.allocate(12);
    bb.put("NOPE".getBytes());
    bb.putInt(1234);

    FlacInfoSupplier infoSupplier = new FlacInfoSupplier();
    ByteArrayInputStream bais = new ByteArrayInputStream(bb.array());
    IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> infoSupplier.getInfos(bais, FLAC_NAME));
    assertEquals("Not a FLAC file: /audio/Filtered_envelope_sawtooth_moog.flac", iae.getMessage());
  }
}
