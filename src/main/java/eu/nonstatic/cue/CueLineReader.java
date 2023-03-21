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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.nio.charset.Charset;

public class CueLineReader implements Closeable {

  private final LineNumberReader reader;

  public CueLineReader(InputStream is, Charset charset) {
    this(new InputStreamReader(is, charset));
  }

  public CueLineReader(Reader reader) {
    this.reader = new LineNumberReader(reader);
  }

  public int getLineNumber() {
    return reader.getLineNumber();
  }

  public CueLine readLine() throws IOException {
    int lineNumber = reader.getLineNumber();
    String line = reader.readLine();
    return (line != null) ? new CueLine(lineNumber, line) : null;
  }

  public int read() throws IOException {
    return reader.read();
  }

  public long skip(long n) throws IOException {
    return reader.skip(n);
  }

  public void mark() throws IOException {
    reader.mark(512);
  }

  public void reset() throws IOException {
    reader.reset();
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }
}
