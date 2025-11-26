package eu.nonstatic.cue.file;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;

public class CIPath extends CSPath {

  static final String SCHEME = "ci";

  CIPath(FileSystem fs, String path) {
    super(fs, path);
  }

  @Override
  CSPath spawn(String path) {
    return new CIPath(fs, path);
  }

  @Override
  public Path toRealPath(LinkOption... options) throws IOException {
    return new CIPath(fs, lowerPath());
  }

  @Override
  public int compareTo(Path other) {
    try {
      return toRealPath().toString().compareTo(other.toRealPath().toString());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String lowerPath() {
    return path.toLowerCase(Locale.ROOT);
  }

  @Override
  String scheme() {
    return SCHEME;
  }
}
