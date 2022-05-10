package eu.nonstatic.cue;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class CueTools {

  public static final String CUE_EXTENSION = "cue";

  public static final Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;
  public static final int DEFAULT_CONFIDENCE = 30;

  private final int confidence;
  private final Charset fallbackCharset;

  public CueTools() {
    this(DEFAULT_CONFIDENCE, DEFAULT_CHARSET);
  }

  public CueTools(int confidence, Charset fallbackCharset) {
    this.confidence = confidence;
    this.fallbackCharset = fallbackCharset;
  }

  public CueContext detectEncoding(File file) {
    return detectEncoding(file.toPath());
  }

  public CueContext detectEncoding(Path file) {
    String name = file.toString();
    try (BufferedInputStream is = new BufferedInputStream(Files.newInputStream(file))) {
      return detectEncoding(name, is);
    } catch (IOException e) {
      log.debug("Fallback to {} for {}: {}", fallbackCharset, file.toAbsolutePath(), e.getMessage(), e);
      return new CueContext(name, fallbackCharset);
    }
  }

  public CueContext detectEncoding(String name, InputStream is) throws IOException {
    try (BufferedInputStream bis = new BufferedInputStream(is)) {
      return detectEncoding(name, bis);
    }
  }

  /**
   * icu calls reset() on the stream, so it needs to support mark()
   */
  CueContext detectEncoding(String name, BufferedInputStream bis) throws IOException {
    Charset charset;

    CharsetDetector cd = new CharsetDetector();
    cd.setText(bis);
    CharsetMatch cm = cd.detect(); // calls reset() !
    if (cm != null && cm.getConfidence() > confidence) {
      charset = Charset.forName(cm.getName());
    } else {
      log.debug("Confidence low. Fallback to {} for {}", fallbackCharset, name);
      charset = fallbackCharset;
    }

    return new CueContext(name, charset);
  }


  public static String unquote(String s) {
    if (s != null) {
      int start = s.startsWith("\"") ? 1 : 0;
      int end = s.length() - (s.endsWith("\"") ? 1 : 0);
      s = s.substring(start, end);
    }
    return s;
  }

  public static boolean isCueFile(Path file) {
    return Files.isRegularFile(file) && CUE_EXTENSION.equalsIgnoreCase(getExt(file));
  }

  static String getExt(Path file) {
    String ext = null;

    String fileName = file.getFileName().toString();
    int dot = fileName.lastIndexOf('.');
    if (dot >= 0) {
      ext = fileName.substring(dot + 1);
    }
    return ext;
  }
}
