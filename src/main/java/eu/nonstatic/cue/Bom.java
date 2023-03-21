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

import java.io.IOException;
import java.io.InputStream;

public final class Bom {

  /**
   * see http://www.faqs.org/rfcs/rfc3629.html see http://www.unicode.org/unicode/faq/utf_bom.html
   */
  protected static final byte[] BOM_UTF_8 = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
  protected static final byte[] BOM_UTF_16_LE = new byte[]{(byte) 0xFF, (byte) 0xFE};
  protected static final byte[] BOM_UTF_16_BE = new byte[]{(byte) 0xFE, (byte) 0xFF};
  protected static final byte[] BOM_UTF_32_LE = new byte[]{(byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x00};
  protected static final byte[] BOM_UTF_32_BE = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0xFE, (byte) 0xFF};

  static final int MAX_LENGTH_BYTES = 4;
  static final int MAX_LENGTH_CHARS = 4; // we don't know how many bytes a char is here, let's assume the least: 1, so we read at least 4 bytes

  private Bom() {}

  public static byte[] read(InputStream is) throws IOException {
    byte[] bytes = new byte[MAX_LENGTH_BYTES];
    int read;
    int off;
    for (off = 0; off != MAX_LENGTH_BYTES && (read = is.read(bytes, off, MAX_LENGTH_BYTES - off)) >= 0; off += read)
      ;

    return identify(bytes, off);
  }

  private static byte[] identify(byte[] bytes, int read) {
    byte[] bom = null;
    switch (read) {
      case 4:
        if (equalsBom(bytes, MAX_LENGTH_BYTES, BOM_UTF_32_LE)) {
          bom = BOM_UTF_32_LE;
          break;
        } else if (equalsBom(bytes, MAX_LENGTH_BYTES, BOM_UTF_32_BE)) {
          bom = BOM_UTF_32_BE;
          break;
        }
        // else continue to case 3
      case 3:
        if (equalsBom(bytes, 3, BOM_UTF_8)) {
          bom = BOM_UTF_8;
          break;
        }
        // else continue to case 2
      case 2:
        if (equalsBom(bytes, 2, BOM_UTF_16_LE)) {
          bom = BOM_UTF_16_LE;
          break;
        } else if (equalsBom(bytes, 2, BOM_UTF_16_BE)) {
          bom = BOM_UTF_16_BE;
          break;
        }
      default:
    }
    return bom;
  }

  private static boolean equalsBom(byte[] bytes, int size, byte[] expectedBom) {
    if (size != expectedBom.length) {
      return false;
    } else {
      boolean result = true;
      for (int i = 0; i < size && result; i++) {
        result &= (bytes[i] == expectedBom[i]);
      }
      return result;
    }
  }
}
