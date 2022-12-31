package eu.nonstatic.cue;

import static java.lang.Math.min;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;

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

  /**
   * Isn't it stupid to read a BOM (whose purpose is to identify encoding) knowing the charset beforehand ?
   */
  public static byte[] read(Reader br, Charset charset) throws IOException {
    char[] chars = new char[MAX_LENGTH_CHARS];
    int read;
    int off;
    for (off = 0; off != MAX_LENGTH_CHARS && (read = br.read(chars, off, MAX_LENGTH_CHARS - off)) >= 0; off += read)
      ;
    byte[] bytes = new String(chars, 0, off).getBytes(charset); // HEY, you wanted to use a reader!

    return identify(bytes, min(bytes.length, MAX_LENGTH_BYTES)); // yes, BYTES now.
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
