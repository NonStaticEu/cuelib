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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import lombok.Getter;

/**
 * see http://www.faqs.org/rfcs/rfc3629.html see http://www.unicode.org/unicode/faq/utf_bom.html
 */
@Getter
public enum Bom {

  BOM_UTF_8(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, StandardCharsets.UTF_8),
  BOM_UTF_16_LE(new byte[]{(byte) 0xFF, (byte) 0xFE}, StandardCharsets.UTF_16LE),
  BOM_UTF_16_BE(new byte[]{(byte) 0xFE, (byte) 0xFF}, StandardCharsets.UTF_16BE),
  BOM_UTF_32_LE(new byte[]{(byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x00}, Charset.forName("UTF-32LE")),
  BOM_UTF_32_BE(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0xFE, (byte) 0xFF}, Charset.forName("UTF-32BE"));

  private final byte[] bytes;
  private final Charset charset;

  static final int MAX_LENGTH_BYTES = 4;

  Bom(byte[] bytes, Charset charset) {
    this.bytes = bytes;
    this.charset = charset;
  }

  public int length() {
    return bytes.length;
  }

  /**
   * bytes may be longer than the Bom it is tested against
   */
  private boolean matches(byte[] bytes) {
    boolean result = true;
    for (int i = 0; i < length() && result; i++) {
      result &= (bytes[i] == this.bytes[i]);
    }
    return result;
  }


  public static Bom read(InputStream is) throws IOException {
    byte[] bytes = new byte[MAX_LENGTH_BYTES];
    int read;
    int off;
    for (off = 0; off != MAX_LENGTH_BYTES && (read = is.read(bytes, off, MAX_LENGTH_BYTES - off)) >= 0; off += read)
      ;

    return identify(bytes, off);
  }

  public static Bom identify(byte[] bytes) {
    return identify(bytes, bytes.length);
  }

  public static Bom identify(byte[] bytes, int read) {
    Bom bom = null;
    switch (read) {
      case 4:
        if (BOM_UTF_32_LE.matches(bytes)) {
          bom = BOM_UTF_32_LE;
          break;
        } else if (BOM_UTF_32_BE.matches(bytes)) {
          bom = BOM_UTF_32_BE;
          break;
        }
        // else continue to case 3
      case 3:
        if (BOM_UTF_8.matches(bytes)) {
          bom = BOM_UTF_8;
          break;
        }
        // else continue to case 2
      case 2:
        if (BOM_UTF_16_LE.matches(bytes)) {
          bom = BOM_UTF_16_LE;
          break;
        } else if (BOM_UTF_16_BE.matches(bytes)) {
          bom = BOM_UTF_16_BE;
          break;
        }
      default:
    }
    return bom;
  }
}
