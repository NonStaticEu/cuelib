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
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AiffInfoSupplier implements AudioInfoSupplier<AiffInfo> {

  /**
   * https://www.mmsp.ece.mcgill.ca/Documents/AudioFormats/AIFF/Docs/AIFF-1.3.pdf
   */
  public AiffInfo getInfos(InputStream is, String name) throws AudioInfoException {
    try (AudioInputStream ais = new AudioInputStream(is, name)) {
      checkHeader(ais);
      return readInfos(ais);
    } catch (IOException e) {
      throw new AudioInfoException(name, e);
    }
  }

  private void checkHeader(AudioInputStream ais) throws IOException, IllegalArgumentException {
    if (!"FORM".equals(ais.readString(4))) {
      throw new IllegalArgumentException("Not an AIFF file: " + ais.name);
    }
    ais.read32bitBE(); // total size
    if (!"AIFF".equals(ais.readString(4))) {
      throw new IllegalArgumentException("No AIFF id in: " + ais.name);
    }
  }

  private AiffInfo readInfos(AudioInputStream ais) throws IOException {
    findChunk(ais, "COMM");
    return AiffInfo.builder()
        .numChannels(ais.read16bitBE())
        .numFrames(ais.read32bitBE())
        .frameSize(ais.read16bitBE())
        .frameRate(ais.readExtendedFloatBE())
        .build();
  }

  private void findChunk(AudioInputStream ais, String name) throws IOException {
    while (true) {
      String ckName = ais.readString(4);
      int ckSize = ais.read32bitBE();
      if (name.equals(ckName)) {
        break;
      } else {
        ais.skipNBytesBeforeJava12(ckSize);
      }
    }
  }

  @Getter @Builder
  public static class AiffInfo implements AudioInfo {
    private short numChannels;
    private double frameRate;
    private int frameSize; // bits
    private int numFrames;

    @Override
    public Duration getDuration() {
      return Duration.ofMillis(Math.round((numFrames * 1000.0) / frameRate));
    }

    @Override
    public List<AudioIssue> getIssues() {
      return List.of();
    }
  }
}
