package eu.nonstatic.cue.file;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestFileSystemProvider extends java.nio.file.spi.FileSystemProvider {

  private final TestFileSystem csFileSystem;
  private final TestFileSystem ciFileSystem;
  private final List<Path> createdFiles = new LinkedList<>();

  public TestFileSystemProvider() {
    csFileSystem = new TestFileSystem(this);
    ciFileSystem = new TestFileSystem(this) {
      @Override
      CSPath generator() {
        return new CIPath(this, null); // so we only spawn CI paths
      }
    };
  }

  @Override
  public String getScheme() {
    return "whatever";
  }

  @Override
  public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
    throw new FileSystemAlreadyExistsException();
  }

  @Override
  public FileSystem getFileSystem(URI uri) {
    String uristr = uri.toString();
    if(isScheme(uristr, CSPath.SCHEME)) {
      return csFileSystem;
    } else if(isScheme(uristr, CIPath.SCHEME)) {
      return ciFileSystem;
    } else {
      throw new FileSystemNotFoundException();
    }
  }

  private static boolean isScheme(String uri, String scheme) {
    scheme = scheme+"://";
    return uri.length() > scheme.length() && scheme.equals(uri.substring(0, scheme.length()));
  }

  @Override
  public Path getPath(URI uri) {
    return getFileSystem(uri).getPath(uri.toString().substring(5));
  }

  @Override
  public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
    createdFiles.add(path);
    return new SeekableByteChannel() {
      @Override
      public int read(ByteBuffer dst) throws IOException {
        return 0;
      }

      @Override
      public int write(ByteBuffer src) throws IOException {
        return 0;
      }

      @Override
      public long position() throws IOException {
        return 0;
      }

      @Override
      public SeekableByteChannel position(long newPosition) throws IOException {
        return null;
      }

      @Override
      public long size() throws IOException {
        return 0;
      }

      @Override
      public SeekableByteChannel truncate(long size) throws IOException {
        return null;
      }

      @Override
      public boolean isOpen() {
        return false;
      }

      @Override
      public void close() throws IOException {

      }
    };
  }

  @Override
  public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete(Path path) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void copy(Path source, Path target, CopyOption... options) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void move(Path source, Path target, CopyOption... options) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isSameFile(Path path, Path path2) throws IOException {
    return path.compareTo(path2) == 0;
  }

  @Override
  public boolean isHidden(Path path) throws IOException {
    return false;
  }

  @Override
  public FileStore getFileStore(Path path) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void checkAccess(Path path, AccessMode... modes) throws IOException {
    if(!createdFiles.contains(path)) {
      throw new NoSuchFileException(path.toString());
    }
  }

  @Override
  public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
    return null;
  }

  @Override
  public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
    return Map.of();
  }

  @Override
  public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
    // nothing
  }
}
