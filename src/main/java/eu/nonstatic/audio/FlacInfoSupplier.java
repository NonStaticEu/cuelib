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

import eu.nonstatic.audio.FlacInfoSupplier.FlacInfo;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FlacInfoSupplier implements AudioInfoSupplier<FlacInfo> {

  private static final int STREAMINFO_BLOCK_TYPE = 0;

  /**
   * https://xiph.org/flac/format.html#metadata_block_streaminfo
   */
  public FlacInfo getInfos(InputStream is, String name) throws IOException {
    try (AudioInputStream ais = new AudioInputStream(is, name)) {
      checkHeader(ais);
      return readInfos(ais);
    }
  }

  private void checkHeader(AudioInputStream ais) throws IOException, IllegalArgumentException {
    if (!"fLaC".equals(ais.readString(4))) {
      throw new IllegalArgumentException("Not a FLAC file: " + ais.name);
    }
  }

  private FlacInfo readInfos(AudioInputStream ais) throws IOException {
    int blockType = ais.read() & 0x7;
    if (blockType == STREAMINFO_BLOCK_TYPE) {
      ais.skipNBytesBeforeJava12(3); // length
      ais.skipNBytesBeforeJava12(10);
      long samplingInfo = ais.read64bitBE();

      int samplingRate = (int) (samplingInfo >> 44);
      int numChannels = (((int) (samplingInfo >> 41)) & 0x7) + 1;
      int bitsPerSample = (((int) (samplingInfo >> 36)) & 0x1F) + 1;
      long totalSamples = (samplingInfo & 0xFFFFFFFFFL);

      return FlacInfo.builder()
          .frameRate(samplingRate)
          .numChannels(numChannels)
          .frameSize(bitsPerSample)
          .numFrames(totalSamples)
          .build();
    } else {
      throw new IllegalArgumentException("STREAMINFO block not found: " + ais.name);
    }
  }




  @Builder
  public static class FlacInfo implements AudioInfo {
    private int numChannels;
    private int frameRate;
    private int frameSize; // bits
    private long numFrames;

    @Override
    public Duration getDuration() {
      return Duration.ofMillis(Math.round((numFrames * 1000.0) / frameRate));
    }
  }
}
