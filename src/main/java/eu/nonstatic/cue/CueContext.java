package eu.nonstatic.cue;

import java.nio.charset.Charset;
import java.nio.file.Path;
import lombok.Getter;

@Getter
public class CueContext {

  private String path;
  private String parent;
  private String name;

  private Charset charset;

  public CueContext(String name, Charset charset) {
    this(name, null, name, charset);
  }

  public CueContext(Path cueFile, Charset charset) {
    this(cueFile.toString(), cueFile.getParent().toString(), cueFile.getFileName().toString(), charset);
  }

  private CueContext(String path, String parent, String name, Charset charset) {
    this.path = path;
    this.parent = parent;
    this.name = name;
    this.charset = charset;
  }
}
