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

import eu.nonstatic.audio.AiffInfoSupplier.AiffInfo;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AiffInfoSupplier implements AudioInfoSupplier<AiffInfo> {

  /**
   * https://www.mmsp.ece.mcgill.ca/Documents/AudioFormats/AIFF/Docs/AIFF-1.3.pdf
   */
  public AiffInfo getInfos(InputStream is, String name) throws IOException {
    try (AudioInputStream wis = new AudioInputStream(is, name)) {
      checkHeader(wis);
      return readInfos(wis);
    }
  }

  private void checkHeader(AudioInputStream wis) throws IOException, IllegalArgumentException {
    if (!"FORM".equals(wis.readString(4))) {
      throw new IllegalArgumentException("Not an AIFF file: " + wis.name);
    }
    wis.read32bitBE(); // total size
    if (!"AIFF".equals(wis.readString(4))) {
      throw new IllegalArgumentException("No AIFF id in: " + wis.name);
    }
  }

  private AiffInfo readInfos(AudioInputStream wis) throws IOException {
    findChunk(wis, "COMM");
    return AiffInfo.builder()
        .numChannels(wis.read16bitBE())
        .numFrames(wis.read32bitBE())
        .frameSize(wis.read16bitBE())
        .frameRate(wis.readExtendedFloatBE())
        .build();
  }

  private void findChunk(AudioInputStream wis, String name) throws IOException {
    while (true) {
      String ckName = wis.readString(4);
      int ckSize = wis.read32bitBE();
      if (name.equals(ckName)) {
        break;
      } else {
        wis.skipNBytesBeforeJava12(ckSize);
      }
    }
  }

  @Builder
  public static class AiffInfo implements AudioInfo {
    private short numChannels;
    private double frameRate;
    private int frameSize; // bits
    private int numFrames;

    @Override
    public Duration getDuration() {
      return Duration.ofMillis(Math.round((numFrames * 1000.0) / frameRate));
    }
  }
}
