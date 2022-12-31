package eu.nonstatic.cue;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.nio.charset.Charset;

public class CueLineReader implements Closeable {

  private static final int BUFFER_SIZE = 8 * 1024;

  private final LineNumberReader reader;


  public CueLineReader(InputStream is, Charset charset) throws IOException {
    if (!is.markSupported()) {
      is = new BufferedInputStream(is);
    }
    is.mark(Bom.MAX_LENGTH_BYTES);

    byte[] bom = Bom.read(is);
    is.reset();
    if (bom != null) {
      is.skip(bom.length);
    }

    this.reader = new LineNumberReader(new InputStreamReader(is, charset));
  }

  public CueLineReader(Reader reader, Charset charset) throws IOException {
    if (!reader.markSupported()) {
      reader = new BufferedReader(reader, BUFFER_SIZE);
    }
    reader.mark(Bom.MAX_LENGTH_CHARS);

    byte[] bom = Bom.read(reader, charset);
    reader.reset();
    if (bom != null) {
      reader.skip(1); // In all likelihood, a BOM should be one char in the file's encoding
    }

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
