package eu.nonstatic.cue;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.LineNumberReader;

public class CueLineReader implements Closeable {

  private final LineNumberReader reader;

  public CueLineReader(BufferedReader reader) {
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
