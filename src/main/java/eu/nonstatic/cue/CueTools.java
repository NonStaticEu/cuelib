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

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CueTools {

  public static final int MAX_CDTEXT_FIELD_LENGTH = 160; // CD-text block space is shared between tracks/fields, so there's no real limit, but let's say 160 is enough.

  private CueTools() {}

  public static String quote(String str) {
    return str != null ? '"' + str + '"' : "\"\"";
  }

  public static String unquote(String str) {
    if (str != null && str.length() >= 2) {
      int start = str.charAt(0) == '"' ? 1 : 0;
      int end = str.length() - (str.charAt(str.length()-1) == '"' ? 1 : 0);
      str = str.substring(start, end);
    }
    return str;
  }

  public static boolean isQuoted(String str) {
    return str != null && str.length() >= 2 && str.charAt(0) == '"' && str.charAt(str.length()-1) == '"';
  }

  static boolean isExt(@NonNull String file, String extension) {
    return equalsIgnoreCase(getExt(file), extension);
  }

  public static String getExt(@NonNull String fileName) {
    String ext = null;
    int dot = fileName.lastIndexOf('.');
    if (dot >= 0) {
      ext = fileName.substring(dot + 1);
    }
    return ext;
  }

  public static void validateTrackRange(String name, int trackNumber, int firstTrackNumber, int trackCount) {
    CueTools.validateRange(name, trackNumber, firstTrackNumber, firstTrackNumber+trackCount-1);
  }

  public static void validateIndexRange(String name, int indexNumber, int indexCount) {
    CueTools.validateRange(name, indexNumber, CueIndex.INDEX_PRE_GAP, indexCount);
  }

  public static void validateRange(String name, int val, int min, int max) {
    if(max-min < 0) {
      throw new IllegalArgumentException(name + ' ' + val + " is out of range []");
    }
    else if(val < min || val > max) {
      throw new IllegalArgumentException(name + ' ' + val + " is out of range [" + min + "," + max + "]");
    }
  }

  public static String validateLength(String name, String str, int minLength, int maxLength, boolean trim, boolean nullAllowed) {
    if(trim) {
      str = trimToNull(str);
    }

    if(str != null) {
      validateRange(name, str.length(), minLength, maxLength);
    } else if(!nullAllowed) {
      throw new NullPointerException(name + " is null");
    }
    return str;
  }

  public static String validateCdText(String name, String str) {
    return validateLength(name, str, 0, MAX_CDTEXT_FIELD_LENGTH, true, true);
  }

  public static String trimToNull(String str) {
    if(str != null) {
      str = str.trim();
      if(str.isEmpty()) {
        str = null;
      }
    }
    return str;
  }

  public static boolean equalsIgnoreCase(final String s1, final String s2) {
    if (s1 == s2) {
      return true;
    } else if (s1 == null || s2 == null) {
      return false;
    } else if (s1.length() != s2.length()) {
      return false;
    } else {
      return s1.equalsIgnoreCase(s2);
    }
  }
}
