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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TimeCodeTest {

  @Test
  void should_build_from_millis() {
    TimeCode timeCode = new TimeCode(620400L, TimeCode.DEFAULT_ROUNDING);
    assertEquals("10:20:30", timeCode.toString());
  }

  @Test
  void should_build_from_duration() {
    TimeCode timeCode = new TimeCode(Duration.ofMillis(620400L), TimeCode.DEFAULT_ROUNDING);
    assertEquals("10:20:30", timeCode.toString());
  }

  @Test
  void should_build_from_mmssff() {
    TimeCode timeCode = new TimeCode(10, 20, 30);
    assertEquals("10:20:30", timeCode.toString());
  }

  @Test
  void should_build_from_other() {
    TimeCode timeCode = new TimeCode(new TimeCode(10, 20, 30));
    assertEquals("10:20:30", timeCode.toString());
  }

  @Test
  void should_build_from_frames() {
    assertEquals("15:24:66", TimeCode.ofFrames(69366).toString());
  }

  @Test
  void should_build_zero() {
    TimeCode timeCode = TimeCode.ZERO_SECOND;
    assertEquals("00:00:00", timeCode.toString());
  }

  @Test
  void should_not_build() {
    assertThrows(IllegalArgumentException.class, () -> new TimeCode(-1, 0, 0));
    assertDoesNotThrow(() -> new TimeCode(100, 0, 0));

    assertThrows(IllegalArgumentException.class, () -> new TimeCode(0, -1, 0));
    assertThrows(IllegalArgumentException.class, () -> new TimeCode(0, 60, 0));

    assertThrows(IllegalArgumentException.class, () -> new TimeCode(0, 0, -1));
    assertThrows(IllegalArgumentException.class, () -> new TimeCode(0, 0, 75));
  }

  @Test
  void should_parse() {
    assertEquals("10:20:30", TimeCode.parse("10:20:30").toString());
    assertThrows(IllegalArgumentException.class, () -> TimeCode.parse("10:20:75"));

    assertEquals("10:20:30", TimeCode.parse("10:20:30", true).toString());

    // more than 99 minutes is allowed. And they're laid out on minutes only; 1:43:20:30 won't be accepted by players.
    assertEquals("103:20:30", TimeCode.parse("103:20:30", true).toString());
  }

  @Test
  void should_not_parse() {
    assertThrows(IllegalArgumentException.class, () -> TimeCode.parse("10:20:75"));
    assertThrows(IllegalArgumentException.class, () -> TimeCode.parse("10:20:75", false));
  }

  @Test
  void should_parse_lenient() {
    TimeCode timeCode = TimeCode.parse("10:20:75", true);
    assertEquals("10:20:56", timeCode.toString());
  }

  @Test
  void should_wither() {
    TimeCode timeCode = new TimeCode(10, 20, 30);
    assertEquals("11:20:30", timeCode.withMinutes(11).toString());
    assertEquals("10:21:30", timeCode.withSeconds(21).toString());
    assertEquals("10:20:31", timeCode.withFrames(31).toString());
  }

  @Test
  void should_convert() {
    TimeCode timeCode = new TimeCode(10, 20, 30);
    assertEquals(10, timeCode.getMinutes());
    assertEquals(20, timeCode.getSeconds());
    assertEquals(30, timeCode.getFrames());
    assertEquals(46530, timeCode.toFrameCount());
    assertEquals(620400L, timeCode.toMillis());
    assertEquals(Duration.ofMillis(620400L), timeCode.toDuration());
  }

  @Test
  void should_diff() {
    TimeCode timeCode = new TimeCode(10, 20, 30);
    assertEquals(Duration.ofMillis(60000+1000+13), timeCode.until(new TimeCode(11, 21, 31)));

    assertEquals(new TimeCode(9, 20, 30), timeCode.minus(Duration.ofMinutes(1)));
    assertEquals(Duration.ofMillis(374933L), timeCode.minus(new TimeCode(4, 5, 35)));
    assertEquals(new TimeCode(10, 15, 62), timeCode.minus(4576L));
    assertEquals(new TimeCode(10, 25, 30), timeCode.plus(Duration.ofSeconds(5)));
    assertEquals(new TimeCode(10, 24, 73), timeCode.plus(4576L));
  }

  @Test
  void should_equal() {
    TimeCode timeCode1 = new TimeCode(10, 20, 30);

    assertEquals(timeCode1, timeCode1);
    assertEquals(timeCode1, new TimeCode(620400L, TimeCode.DEFAULT_ROUNDING));
    assertNotEquals(timeCode1, new TimeCode(30, 20, 10));
    assertFalse(timeCode1.equals("whatever")); // using equals for coverage
    assertFalse(timeCode1.equals(null)); // using equals for coverage
  }

  @Test
  void should_not_equal() {
    TimeCode timeCode1 = new TimeCode(10, 20, 30);
    assertNotEquals(null, timeCode1);
    assertNotEquals("whatever", timeCode1);
  }

  @Test
  void should_hash() {
    assertEquals(46530, new TimeCode(10, 20, 30).hashCode());
  }
}
