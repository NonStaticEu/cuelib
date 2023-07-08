/**
 * Cuelib
 * Copyright (C) 2022 NonStatic
 *
 * This file is part of cuelib.
 * cuelib is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with . If not, see <https://www.gnu.org/licenses/>.
 */
package eu.nonstatic.audio;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;

/**
 * Reminder:
 * x86 is little endian (LE)
 * 68k is big endian (BE)
 */
public class AudioInputStream extends BufferedInputStream {
  @Getter
  protected final String name;
  private long location;
  private long markedAt;


  public AudioInputStream(File file) throws IOException {
    this(file.toPath());
  }

  public AudioInputStream(Path file) throws IOException {
    this(Files.newInputStream(file), file.toString());
  }

  public AudioInputStream(InputStream is, String name) {
    super(is);
    this.name = name;
  }

  public short read16bitLE() throws IOException {
    return ByteBuffer.wrap(readNBytes(2)).order(LITTLE_ENDIAN).getShort();
  }

  public int read32bitLE() throws IOException {
    return ByteBuffer.wrap(readNBytes(4)).order(LITTLE_ENDIAN).getInt();
  }

  public short read16bitBE() throws IOException {
    return ByteBuffer.wrap(readNBytes(2)).order(BIG_ENDIAN).getShort();
  }

  public int read24bitBE() throws IOException {
    return ByteBuffer.allocate(4).order(BIG_ENDIAN)
        .put((byte) 0).put(readNBytes(3))
        .flip()
        .getInt();
  }

  public int read32bitBE() throws IOException {
    return ByteBuffer.wrap(readNBytes(4)).order(BIG_ENDIAN).getInt();
  }

  public long read64bitBE() throws IOException {
    return ByteBuffer.wrap(readNBytes(8)).order(BIG_ENDIAN).getLong();
  }

  /**
   * Very bad conversion for 80 bit IEEE Standard 754 floating point number (Standard Apple Numeric Environment [SANE] data type Extended) taken from
   * https://stackoverflow.com/a/35670539/6693204 but that will do!
   * https://en.wikipedia.org/wiki/Extended_precision#x86_Extended_Precision_Format
   * @return the converted float
   */
  public double readExtendedFloatBE() throws IOException {
    ByteBuffer bb = ByteBuffer.wrap(readNBytes(10)).order(BIG_ENDIAN);
    short high = bb.getShort();
    long low = bb.getLong();

    long e = (((high & 0x7FFFL) - 0x3FFFL) + 0x3FFL) & 0x7FFL;
    long ld = ((high & 0x8000L) << 48)
        | (e << 52)
        | ((low >>> 11) & 0xF_FFFF_FFFF_FFFFL);
    return Double.longBitsToDouble(ld);
  }

  public String readString(int len) throws IOException {
    return new String(readNBytes(len), StandardCharsets.US_ASCII); // BIG ENDIAN
  }

  void skipNBytesBeforeJava12(long n) throws IOException {
    while (n > 0) {
      long ns = skip(n);
      if (ns > 0 && ns <= n) {
        // adjust number to skip
        n -= ns;
      } else if (ns == 0) { // no bytes skipped
        // read one byte to check for EOS
        if (read() == -1) {
          throw new EOFException();
        }
        // one byte read so decrement number to skip
        n--;
      } else { // skipped negative or too many bytes
        throw new IOException("Unable to skip exactly");
      }
    }
  }

  public long location() {
    return location;
  }

  // Those delegates just to update the location

  @Override
  public synchronized int read() throws IOException {
    int read = super.read();
    this.location++;
    return read;
  }

  @Override
  public synchronized int read(byte[] b, int off, int len) throws IOException {
    int read = super.read(b, off, len);
    this.location += read;
    return read;
  }

  @Override
  public synchronized long skip(long n) throws IOException {
    long skipped = super.skip(n);
    this.location += skipped;
    return skipped;
  }

  @Override
  public synchronized void mark(int readlimit) {
    super.mark(readlimit);
    this.markedAt = location();
  }

  @Override
  public synchronized void reset() throws IOException {
    super.reset();
    this.location = markedAt;
    this.markedAt = -1;
  }
}
