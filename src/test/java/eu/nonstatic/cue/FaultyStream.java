package eu.nonstatic.cue;

import java.io.IOException;
import java.io.InputStream;

public class FaultyStream extends InputStream {

  final InputStream is;
  final int afterReads;
  int readCount;

  public FaultyStream() {
    this(null, 0);
  }

  public FaultyStream(InputStream is, int afterReads) {
    this.is = is;
    this.afterReads = afterReads;
  }

  @Override
  public int read() throws IOException {
    if (readCount >= afterReads) {
      throw new IOException("reads: " + readCount);
    } else {
      int read = is.read();
      readCount++;
      return read;
    }
  }

  @Override
  public void mark(int readlimit) {
    // nope
  }

  @Override
  public void reset() throws IOException {
    // nope
  }

  @Override
  public boolean markSupported() {
    return true; // to avoid wrapping in a BufferedInputStream
  }
}
