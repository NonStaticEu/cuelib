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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CueRemarkTest {

  @Test
  void should_test_several_words() {
    assertFalse(new CueRemark("tag", null).isSeveralWords());
    assertFalse(new CueRemark("tag", "nope").isSeveralWords());
    assertTrue(new CueRemark("tag", "right on").isSeveralWords());
  }

  @Test
  void should_give_tostring() {
    CueRemark remark1 = CueRemark.commentOf("My Comment");
    assertEquals("REM COMMENT \"My Comment\"", remark1.toString());

    CueRemark remark2 = new CueRemark("My Value");
    assertEquals("REM \"My Value\"", remark2.toString());

    CueRemark remark3 = new CueRemark(null, null);
    assertEquals("REM", remark3.toString());
  }
}
