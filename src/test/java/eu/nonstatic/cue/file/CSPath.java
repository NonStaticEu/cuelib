package eu.nonstatic.cue.file;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;

public class CSPath implements Path {

  static final String SCHEME = "cs";
  protected final FileSystem fs;
  protected final String path;

  CSPath(FileSystem fs, String path) {
    this.fs = fs;
    this.path = path;
  }

  CSPath spawn(String path) {
    return new CSPath(fs, path);
  }

  @Override
  public FileSystem getFileSystem() {
    return fs;
  }

  @Override
  public boolean isAbsolute() {
    return true;
  }

  @Override
  public Path getRoot() {
    return null;
  }

  @Override
  public Path getFileName() {
    int idx = path.lastIndexOf(fs.getSeparator());
    if (idx < 0) {
      return this;
    } else {
      return spawn(path.substring(idx + 1));
    }
  }

  @Override
  public Path getParent() {
    int idx = path.lastIndexOf(fs.getSeparator());
    if (idx < 0) {
      return getRoot();
    } else {
      return spawn(path.substring(0, idx));
    }
  }

  @Override
  public int getNameCount() {
    return split().length;
  }

  @Override
  public Path getName(int index) {
    return spawn(split()[index]);
  }

  private String[] split() {
    return path.split(fs.getSeparator());
  }

  @Override
  public Path subpath(int beginIndex, int endIndex) {
    String subpath = String.join(fs.getSeparator(), Arrays.copyOfRange(split(), beginIndex, endIndex));
    return spawn(subpath);
  }

  @Override
  public boolean startsWith(Path other) {
    return path.startsWith(((CSPath)other).path);
  }

  @Override
  public boolean endsWith(Path other) {
    return path.endsWith(((CSPath)other).path);
  }

  @Override
  public Path normalize() {
    return this; // Don't use /../ or /./
  }

  @Override
  public Path resolve(Path other) {
    return spawn(path + fs.getSeparator() + ((CSPath)other).path);
  }

  // TODO p.relativize(p .resolve(q)).equals(q)
  @Override
  public Path relativize(Path other) {
    String otherPath = ((CSPath) other).path;
    int sepStart = path.length(), sepEnd = sepStart + fs.getSeparator().length();
    if(otherPath.startsWith(path) && fs.getSeparator().equals(otherPath.substring(sepStart, sepEnd))) {
      return spawn(otherPath.substring(sepEnd));
    } else {
      throw new IllegalArgumentException();
    }
  }

  @Override
  public URI toUri() {
    return URI.create(scheme() + path);
  }

  String scheme() {
    return SCHEME;
  }

  @Override
  public Path toAbsolutePath() {
    return this;
  }

  @Override
  public Path toRealPath(LinkOption... options) throws IOException {
    return this;
  }

  @Override
  public WatchKey register(WatchService watcher, Kind<?>[] events,
      Modifier... modifiers) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public int compareTo(Path other) {
    return path.compareTo(((CSPath)other).path);
  }

  @Override
  public boolean equals(Object obj) {
    return compareTo((CSPath)obj) == 0;
  }

  @Override
  public String toString() {
    return path;
  }
}
