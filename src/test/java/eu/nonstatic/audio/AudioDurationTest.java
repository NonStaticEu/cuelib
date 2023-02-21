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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.nonstatic.audio.AiffInfoSupplier.AiffInfo;
import eu.nonstatic.audio.FlacInfoSupplier.FlacInfo;
import eu.nonstatic.audio.Mp3InfoSupplier.Mp3Info;
import eu.nonstatic.audio.WaveInfoSupplier.WaveInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * Those samples are taken from here:
 * https://en.wikipedia.org/wiki/Synthesizer
 * https://commons.wikimedia.org/wiki/File:Amplitudenmodulation.ogg
 */
class AudioDurationTest implements AudioTestBase {

  @Test
  void should_give_aiff_infos() throws IOException {
    AiffInfo aiffInfo = new AiffInfoSupplier().getInfos(AIFF_URL.openStream(), AIFF_NAME);
    assertEquals(Duration.ofMillis(30407L), aiffInfo.getDuration());
  }

  @Test
  void should_give_wave_infos() throws IOException {
    WaveInfo waveInfo = new WaveInfoSupplier().getInfos(WAVE_URL.openStream(), WAVE_NAME);
    assertEquals(Duration.ofMillis(8011L), waveInfo.getDuration());
  }

  @Test
  void should_give_flac_infos() throws IOException {
    FlacInfo flacInfo = new FlacInfoSupplier().getInfos(FLAC_URL.openStream(), FLAC_NAME);
    assertEquals(Duration.ofMillis(3692L), flacInfo.getDuration());
  }

  @Test
  void should_give_mp3_infos() throws IOException {
    Mp3Info mp3Info = new Mp3InfoSupplier().getInfos(MP3_URL.openStream(), MP3_NAME);
    assertFalse(mp3Info.isIncomplete());
    assertEquals(Duration.ofNanos(11154285714L), mp3Info.getDuration());
  }

  @Test
  void should_give_mp3_infos_on_incomplete_file() throws IOException {
    byte[] bytes;
    try(InputStream is = MP3_URL.openStream()) {
      bytes = is.readAllBytes();
    }

    Duration fullDuration = new Mp3InfoSupplier().getInfos(new ByteArrayInputStream(bytes), MP3_NAME + ":complete").getDuration();

    try(ByteArrayInputStream incompleteStream = new ByteArrayInputStream(bytes, 0, bytes.length - 50)) {
      Mp3Info incompleteInfos = new Mp3InfoSupplier().getInfos(incompleteStream, MP3_NAME + ":incomplete");

      assertTrue(incompleteInfos.isIncomplete());
      Duration incompleteDuration = incompleteInfos.getDuration();
      assertEquals(Duration.ofNanos(11128163265L), incompleteDuration);
      // That's one Layer III frame less
      assertEquals(Math.round(1152*(1_000_000_000.0)/44100), fullDuration.minus(incompleteDuration).toNanos());
    }
  }
}
