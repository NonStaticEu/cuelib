package eu.nonstatic.cue;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class CueTools {

  public static final String CUE_EXTENSION = "cue";
  public static final Charset DEFAULT_CHARSET = Charset.forName("ISO-8859-1");

  public static Charset detectEncoding(Path file) {
    try (BufferedInputStream is = new BufferedInputStream(Files.newInputStream(file))) {
      CharsetDetector cd = new CharsetDetector();
      cd.setText(is);
      CharsetMatch cm = cd.detect();
      if (cm != null && cm.getConfidence() > 30) {
        return Charset.forName(cm.getName());
      } else {
        log.debug("Confidence low. Fallback to {} for {}", DEFAULT_CHARSET, file.toAbsolutePath());
        return DEFAULT_CHARSET;
      }
    } catch (IOException e) {
      log.debug("Fallback to {} for {}: {}", DEFAULT_CHARSET, file.toAbsolutePath(), e.getMessage(), e);
      return DEFAULT_CHARSET;
    }
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
    if (fileName != null) {
      int dotpos = fileName.lastIndexOf('.');
      if (dotpos >= 0) {
        ext = fileName.substring(dotpos + 1);
      }
    }
    return ext;
  }
}
