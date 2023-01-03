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

import java.io.IOException;
import java.net.URL;
import org.junit.jupiter.api.Test;

class BomTest {

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

}
