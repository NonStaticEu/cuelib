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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Random;
import org.junit.jupiter.api.Test;

class BomTest {

  static final Random RANDOM = new Random();
  static URL url = CueDiscTest.class.getResource("/Bom Test.cue");

  @Test
  void should_read_bom() throws IOException {
    byte[] bom = Bom.read(url.openStream());
    assertEquals(Bom.BOM_UTF_8, bom);
  }

  @Test
  void should_not_have_bom() throws IOException {
    URL noBomUrl = CueDiscTest.class.getResource("/My Test.cue");
    byte[] bom = Bom.read(noBomUrl.openStream());
    assertNull(bom);
  }

  // Useless but coverage likes it
  @Test
  void should_identify_UTF_8() throws IOException {
    assertEquals(Bom.BOM_UTF_8, Bom.read(streamOf(Bom.BOM_UTF_8)));
  }

  @Test
  void should_identify_UTF_16_LE() throws IOException {
    assertEquals(Bom.BOM_UTF_16_LE, Bom.read(streamOf(Bom.BOM_UTF_16_LE)));
  }

  @Test
  void should_identify_UTF_16_BE() throws IOException {
    assertEquals(Bom.BOM_UTF_16_BE, Bom.read(streamOf(Bom.BOM_UTF_16_BE)));
  }

  @Test
  void should_identify_UTF_32_LE() throws IOException {
    assertEquals(Bom.BOM_UTF_32_LE, Bom.read(streamOf(Bom.BOM_UTF_32_LE)));
  }

  @Test
  void should_identify_UTF_32_BE() throws IOException {
    assertEquals(Bom.BOM_UTF_32_BE, Bom.read(streamOf(Bom.BOM_UTF_32_BE)));
  }


  private static InputStream streamOf(byte[] bytes) {
    byte[] random = new byte[5];
    RANDOM.nextBytes(random);

    ByteBuffer bb = ByteBuffer.allocate(bytes.length + 5);
    bb.put(bytes);
    bb.put(random);
    return new ByteArrayInputStream(bb.array());
  }
}
