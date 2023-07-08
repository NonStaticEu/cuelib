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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;

class AudioInputStreamTest {
  static final byte[] INTEGRAL_DATA = {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, (byte) 0xDE, (byte) 0xCA, (byte) 0xFA, (byte) 0xCE};
  static final byte[] FLOAT_DATA_44KHZ = new byte[]{64, 14, -84, 68, 0, 0, 0, 0, 0, 0};
  static final byte[] FLOAT_DATA_48KHZ = new byte[]{64, 14, -69, -128, 0, 0, 0, 0, 0, 0};

  @Test
  void should_open_on_file() throws IOException {
    File tempFile = File.createTempFile("foo", null);
    try(OutputStream os = new FileOutputStream(tempFile)) {
      os.write(INTEGRAL_DATA);
      os.flush();
    }
    AudioInputStream ais = new AudioInputStream(tempFile);
    assertArrayEquals(INTEGRAL_DATA, ais.readAllBytes());
    tempFile.delete();
  }

  @Test
  void should_read_16bitLE() throws IOException {
    try(AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(INTEGRAL_DATA), "data")) {
      assertEquals((short)0xFECA, ais.read16bitLE());
    }
  }

  @Test
  void should_read_16bitBE() throws IOException {
    try(AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(INTEGRAL_DATA), "data")) {
      assertEquals((short)0xCAFE, ais.read16bitBE());
    }
  }

  @Test
  void should_read_24bitBE() throws IOException {
    try(AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(INTEGRAL_DATA), "data")) {
      assertEquals((long)0xCAFEBA, ais.read24bitBE());
    }
  }

  @Test
  void should_read_32bitLE() throws IOException {
    try(AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(INTEGRAL_DATA), "data")) {
      assertEquals(0xBEBAFECA, ais.read32bitLE());
    }
  }

  @Test
  void should_read_32bitBE() throws IOException {
    try(AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(INTEGRAL_DATA), "data")) {
      assertEquals(0xCAFEBABE, ais.read32bitBE());
    }
  }

  @Test
  void should_read_64bitBE() throws IOException {
    try(AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(INTEGRAL_DATA), "data")) {
      assertEquals(0xCAFEBABEDECAFACEL, ais.read64bitBE());
    }
  }

  @Test
  void should_read_extended_float_BE() throws IOException {
    try(AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(FLOAT_DATA_44KHZ), "data")) {
      assertEquals(44100.0, ais.readExtendedFloatBE());
    }

    try(AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(FLOAT_DATA_48KHZ), "data")) {
      assertEquals(48000.0, ais.readExtendedFloatBE());
    }
  }

  @Test
  void should_skipNbytes() throws IOException {
    try(AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(INTEGRAL_DATA), "data")) {
      ais.skipNBytesBeforeJava12(4);
      assertEquals(0xDECAFACE, ais.read32bitBE());
    }
  }

  @Test
  void should_provide_location() throws IOException {
    try(AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(INTEGRAL_DATA), "data")) {
      // simple read
      ais.read();
      assertEquals(1, ais.location());

      // array read
      ais.mark(6);
      assertEquals(1, ais.location());
      ais.read(new byte[4]);
      assertEquals(5, ais.location());
      ais.reset();
      assertEquals(1, ais.location());

      // array N read
      ais.mark(2);
      ais.readNBytes(2);
      assertEquals(3, ais.location());
      ais.reset();
      assertEquals(1, ais.location());

      // array N read offset
      ais.mark(10);
      ais.readNBytes(new byte[10], 3, 7);
      assertEquals(8, ais.location()); // end of stream
      ais.reset();
      assertEquals(1, ais.location());

      // skip
      ais.skip(3);
      assertEquals(4, ais.location());

      // skip N
      ais.mark(5);
      ais.skipNBytesBeforeJava12(2);
      assertEquals(6, ais.location());
      ais.reset();
      assertEquals(4, ais.location());
    }
  }
}
