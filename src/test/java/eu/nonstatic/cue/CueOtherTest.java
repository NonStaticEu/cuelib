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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CueOtherTest {

  @Test
  void should_give_tostring() {
    CueOther other = new CueOther("MY_KEY", "My Comment");
    assertEquals("MY_KEY \"My Comment\"", other.toString());

    CueOther other2 = new CueOther("MY_KEY", null);
    assertEquals("MY_KEY", other2.toString());

    assertThrows(NullPointerException.class, () -> new CueOther(null, "My Value"));
  }
}
