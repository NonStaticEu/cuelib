package eu.nonstatic.cue.file;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestFileSystem extends FileSystem {

  private static final String SEPARATOR = "/";
  protected final FileSystemProvider fsp;

  public TestFileSystem(FileSystemProvider fsp) {
    this.fsp = fsp;
  }

  CSPath generator() {
    return new CSPath(this, null);
  }

  @Override
  public FileSystemProvider provider() {
    return fsp;
  }

  @Override
  public void close() throws IOException {
    // nothing
  }

  @Override
  public boolean isOpen() {
    return true;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public String getSeparator() {
    return SEPARATOR;
  }

  @Override
  public Iterable<Path> getRootDirectories() {
    return Set.of(new CSPath(this, "$"));
  }

  @Override
  public Iterable<java.nio.file.FileStore> getFileStores() {
    return Set.of();
  }

  @Override
  public Set<String> supportedFileAttributeViews() {
    return Set.of();
  }

  @Override
  public Path getPath(String first, String... more) {
    return generator().spawn(Stream.concat(Stream.of(first), Stream.of(more)).collect(Collectors.joining(SEPARATOR)));
  }

  @Override
  public PathMatcher getPathMatcher(String syntaxAndPattern) {
    return path -> ((CSPath)path).path.matches(syntaxAndPattern);
  }

  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService() {
    return null;
  }

  @Override
  public WatchService newWatchService() throws IOException {
    return null;
  }
}
