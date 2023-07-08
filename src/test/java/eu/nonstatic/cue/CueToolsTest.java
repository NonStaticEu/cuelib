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

import static eu.nonstatic.cue.CueTools.MAX_CDTEXT_FIELD_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * We don't need to test ICU4J here
 */
class CueToolsTest {

  @Test
  void should_quote() {
    assertEquals("\"\"", CueTools.quote(null));
    assertEquals("\"\"", CueTools.quote(""));
    assertEquals("\"\"\"", CueTools.quote("\""));
    assertEquals("\"\"\"\"", CueTools.quote("\"\""));

    assertEquals("\"blah\"", CueTools.quote("blah"));
    assertEquals("\"bl\"ah\"", CueTools.quote("bl\"ah"));
    assertEquals("\"bl ah\"", CueTools.quote("bl ah"));
    assertEquals("\"\"bl ah\"\"", CueTools.quote("\"bl ah\""));
  }

  @Test
  void should_unquote() {
    assertNull(CueTools.unquote(null));
    assertEquals("", CueTools.unquote(""));
    assertEquals("\"", CueTools.unquote("\""));
    assertEquals("", CueTools.unquote("\"\""));

    assertEquals("blah", CueTools.unquote("blah"));
    assertEquals("bl\"ah", CueTools.unquote("bl\"ah"));
    assertEquals("bl ah", CueTools.unquote("bl ah"));
    assertEquals("bl ah", CueTools.unquote("\"bl ah\""));
  }

  @Test
  void should_tell_quoted() {
    assertFalse(CueTools.isQuoted(null));
    assertFalse(CueTools.isQuoted(""));
    assertFalse(CueTools.isQuoted("\""));
    assertFalse(CueTools.isQuoted("a"));
    assertFalse(CueTools.isQuoted(" "));
    assertTrue(CueTools.isQuoted("\"\""));

    assertFalse(CueTools.isQuoted("blah"));
    assertFalse(CueTools.isQuoted("bl\"ah"));
    assertFalse(CueTools.isQuoted("bl ah"));
    assertFalse(CueTools.isQuoted("\"bl ah"));
    assertFalse(CueTools.isQuoted("bl ah\""));
    assertTrue(CueTools.isQuoted("\"bl ah\""));
  }

  @Test
  void should_match_ext() {
    assertThrows(NullPointerException.class, () -> CueTools.isExt(null, "xyz"));

    String path = "/tmp/file.xyz";
    assertFalse(CueTools.isExt(path, "abc"));
    assertTrue(CueTools.isExt(path, "xyz"));
    assertTrue(CueTools.isExt(path, "xYz"));
    assertTrue(CueTools.isExt(path, "XYZ"));
  }

  @Test
  void should_get_ext() {
    assertThrows(NullPointerException.class, () -> CueTools.getExt(null));
    assertNull(CueTools.getExt(""));
    assertNull(CueTools.getExt("/dev/null"));
    assertEquals("xyz", CueTools.getExt("/tmp/file.xYZ"));
  }

  @Test
  void should_validate_track_range() {
    Assertions.assertDoesNotThrow(() -> CueTools.validateTrackRange("track", 1, 1, 10));
    Assertions.assertDoesNotThrow(() -> CueTools.validateTrackRange("track", 8, 1, 10));
    Assertions.assertDoesNotThrow(() -> CueTools.validateTrackRange("track", 10, 1, 10));
    Assertions.assertDoesNotThrow(() -> CueTools.validateTrackRange("track", 2, 2, 10));
    Assertions.assertThrows(IllegalArgumentException.class, () -> CueTools.validateTrackRange("track", 0, 1, 10));
    Assertions.assertThrows(IllegalArgumentException.class, () -> CueTools.validateTrackRange("track", 11, 1, 10));
    Assertions.assertThrows(IllegalArgumentException.class, () -> CueTools.validateTrackRange("track", 3, 4, 10));
  }

  @Test
  void should_validate_cd_text() {
    assertEquals("blah", CueTools.validateCdText("field", "blah"));
    assertEquals("blah", CueTools.validateCdText("field", "  blah  "));

    assertNull(CueTools.validateCdText("field", "     "));
    assertNull(CueTools.validateCdText("field", ""));
    assertNull(CueTools.validateCdText("field", null));

    assertEquals(MAX_CDTEXT_FIELD_LENGTH, CueTools.validateCdText("field",
      "   " + makeString(MAX_CDTEXT_FIELD_LENGTH) + "   ").length());

    String field = makeString(MAX_CDTEXT_FIELD_LENGTH+1);
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CueTools.validateCdText("field", field));
    assertEquals("field 161 is out of range [0,160]", ex.getMessage());
  }

  @Test
  void should_validate_length() {
    String blah = "blah";
    int blahLength = blah.length();
    String blahspaced = "   blah  ";

    assertNull(CueTools.validateLength("field", "     ", 0, 100, true, true));
    assertThrows(NullPointerException.class, () -> CueTools.validateLength("field", "     ", 0, 100, true, false));
    assertEquals("     ", CueTools.validateLength("field", "     ", 0, 5, false, true));
    assertThrows(IllegalArgumentException.class, () -> CueTools.validateLength("field", "     ", 0, 4, false, false));

    assertThrows(IllegalArgumentException.class, () -> CueTools.validateLength("field", "     ", 6, 100, false, false));

    assertEquals(blah, CueTools.validateLength("field", blah, blahLength, 100, false, false));
    assertThrows(IllegalArgumentException.class, () -> CueTools.validateLength("field", blah, blahLength +1, 100, false, false));

    assertEquals(blahspaced, CueTools.validateLength("field", blahspaced, blahLength, 100, false, false));
    assertEquals(blahspaced, CueTools.validateLength("field", blahspaced, blahLength +1, 100, false, false));
    assertThrows(IllegalArgumentException.class, () -> CueTools.validateLength("field", blahspaced, blahLength +1, 100, true, false));
    assertEquals(blah, CueTools.validateLength("field", blahspaced, blahLength, 100, true, false));

    String maxLengthField = makeString(MAX_CDTEXT_FIELD_LENGTH);
    assertEquals(MAX_CDTEXT_FIELD_LENGTH, CueTools.validateLength("field", maxLengthField, 0, MAX_CDTEXT_FIELD_LENGTH, true, false).length());
    String moreThanMaxLengthField = makeString(MAX_CDTEXT_FIELD_LENGTH+1);
    assertThrows(IllegalArgumentException.class, () -> CueTools.validateLength("field", moreThanMaxLengthField, 0, MAX_CDTEXT_FIELD_LENGTH, false, false));

    String spacedMaxLengthField = "   " + maxLengthField + "   ";
    assertEquals(MAX_CDTEXT_FIELD_LENGTH, CueTools.validateLength("field", spacedMaxLengthField, 0, MAX_CDTEXT_FIELD_LENGTH, true, false).length());
    assertThrows(IllegalArgumentException.class, () -> CueTools.validateLength("field", spacedMaxLengthField, 0, MAX_CDTEXT_FIELD_LENGTH, false, false));
  }

  private static String makeString(int length) {
    return IntStream.range(0, length)
      .mapToObj(i -> Integer.toString((int) (Math.random() * 10)))
      .collect(Collectors.joining(""));
  }

  @Test
  void should_trim_to_null() {
    assertEquals("blah", CueTools.trimToNull("blah"));
    assertEquals("blah", CueTools.trimToNull("  blah"));
    assertEquals("blah", CueTools.trimToNull("blah  "));
    assertEquals("blah", CueTools.trimToNull("  blah  "));
    assertNull(CueTools.trimToNull("  "));
    assertNull(CueTools.trimToNull(""));
    assertNull(CueTools.trimToNull(null));
  }

  @Test
  void should_equals_ignore_case() {
    assertTrue(CueTools.equalsIgnoreCase(null, null));
    assertTrue(CueTools.equalsIgnoreCase("", new String()));
    assertTrue(CueTools.equalsIgnoreCase("\t", "\t"));

    assertFalse(CueTools.equalsIgnoreCase("ABC", null));
    assertFalse(CueTools.equalsIgnoreCase(null, "ABC"));

    assertTrue(CueTools.equalsIgnoreCase("abc", "abc"));
    assertTrue(CueTools.equalsIgnoreCase("ABC", "ABC"));

    assertTrue(CueTools.equalsIgnoreCase("ABC", "aBc"));
    assertTrue(CueTools.equalsIgnoreCase("aBc", "ABC"));

    assertTrue(CueTools.equalsIgnoreCase("ABC", "abc"));
    assertTrue(CueTools.equalsIgnoreCase("abc", "ABC"));

    assertFalse(CueTools.equalsIgnoreCase("ABC", "ABC "));
    assertFalse(CueTools.equalsIgnoreCase("ABC ", "ABC"));

    assertFalse(CueTools.equalsIgnoreCase("ABC", "ABCD"));
    assertFalse(CueTools.equalsIgnoreCase("abcd", "abc"));

    assertFalse(CueTools.equalsIgnoreCase("AbCdE", "zXyWvU"));
  }
}
