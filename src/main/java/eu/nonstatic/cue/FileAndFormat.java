package eu.nonstatic.cue;

import static eu.nonstatic.cue.CueTools.unquote;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * This class is meant to be immutable
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode // null-safe
class FileAndFormat {

  protected final String file;
  protected final String format; // MP3, AIFF, WAVE, FLAC, BIN

  static FileAndFormat parse(@NonNull String fileAndFormat) {
    String file, format;
    int sep = fileAndFormat.lastIndexOf(' ');
    if (sep >= 0) {
      file = fileAndFormat.substring(0, sep).trim();
      format = fileAndFormat.substring(sep + 1);
    } else {
      file = fileAndFormat;
      format = null;
    }
    return new FileAndFormat(unquote(file), format);
  }
}