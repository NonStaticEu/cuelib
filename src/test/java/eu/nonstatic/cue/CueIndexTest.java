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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CueIndexTest {

  @Test
  void should_initialize_with_empty_index() {
    assertThrows(NullPointerException.class, () -> new CueIndex(null));

    CueIndex index = new CueIndex(12, 20, 30);
    assertNull(index.getNumber());
  }

  @Test
  void should_reset_timecode() {
    CueIndex index = new CueIndex(1, TimeCode.ZERO_SECOND);

    index.setFrames(36);
    assertEquals(new TimeCode(0, 0, 36), index.getTimeCode());

    index.setSeconds(17);
    assertEquals(new TimeCode(0, 17, 36), index.getTimeCode());

    index.roundSecond();
    assertEquals(new TimeCode(0, 17, 0), index.getTimeCode());

    index.setFrames(37);
    index.roundSecond();
    assertEquals(new TimeCode(0, 18, 0), index.getTimeCode());


    index.setMinutes(59);
    assertEquals(new TimeCode(59, 18, 0), index.getTimeCode());

    index.setTime(11, 22, 38);
    assertEquals(new TimeCode(11, 22, 38), index.getTimeCode());

    index.ceilSecond();
    assertEquals(new TimeCode(11, 23, 0), index.getTimeCode());
    index.ceilSecond();
    assertEquals(new TimeCode(11, 23, 0), index.getTimeCode());
    index.setTime(11, 59, 70);
    index.ceilSecond();
    assertEquals(new TimeCode(12, 0, 0), index.getTimeCode());

    index.setTimeCode("54:32:10");
    assertEquals(new TimeCode(54, 32, 10), index.getTimeCode());

    index.floorSecond();
    assertEquals(new TimeCode(54, 32, 0), index.getTimeCode());

    index.setTimeMillis(2547861, TimeCodeRounding.CLOSEST);
    assertEquals(new TimeCode(42, 27, 65), index.getTimeCode());
  }

  @Test
  void should_be_pregap_start() {
    assertThrows(IllegalArgumentException.class, () -> CueIndex.isPreGapOrStart(CueIndex.INDEX_PRE_GAP-1));
    assertTrue(CueIndex.isPreGapOrStart(CueIndex.INDEX_PRE_GAP));
    assertTrue(CueIndex.isPreGapOrStart(CueIndex.INDEX_TRACK_START));
    assertFalse(CueIndex.isPreGapOrStart(2));
    assertThrows(IllegalArgumentException.class, () -> CueIndex.isPreGapOrStart(CueIndex.INDEX_MAX+1));
  }

  @Test
  void should_set_number() {
    CueIndex index = new CueIndex(null, TimeCode.ZERO_SECOND);
    assertThrows(IllegalArgumentException.class, () -> index.setNumberOnce(CueIndex.INDEX_PRE_GAP-1));
    assertThrows(IllegalArgumentException.class, () -> index.setNumberOnce(CueIndex.INDEX_MAX+1));
    index.setNumberOnce(2);
    assertEquals(2, index.getNumber());
    index.setNumberOnce(2); // same value, won't complain
    assertEquals(2, index.getNumber());
    assertThrows(IllegalStateException.class, () -> index.setNumberOnce(3));
  }

  @Test
  void should_give_tostring() {
    CueIndex index = new CueIndex(3, new TimeCode(33, 42, 69));
    assertEquals("INDEX 03 33:42:69", index.toString());
  }

  @Test
  void should_compare() {
    CueIndex index1a = new CueIndex(2, TimeCode.ZERO_SECOND);
    CueIndex index1b = new CueIndex(2, TimeCode.ZERO_SECOND);
    CueIndex index2 = new CueIndex(3, TimeCode.ZERO_SECOND);
    CueIndex index3 = new CueIndex(null, TimeCode.ZERO_SECOND);
    assertEquals(0, index1a.compareTo(index1b));
    assertTrue(index1a.compareTo(index2) < 0);
    assertTrue(index2.compareTo(index1a) > 0);
    assertTrue(index1a.compareTo(index3) < 0);
    assertTrue(index3.compareTo(index2) > 0);
  }
}
