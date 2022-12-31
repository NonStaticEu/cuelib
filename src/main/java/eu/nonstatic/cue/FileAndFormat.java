package eu.nonstatic.cue;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import static eu.nonstatic.cue.CueTools.unquote;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
class FileAndFormat {

  protected String file;
  protected String format; // MP3, AIFF, WAVE, FLAC, BIN


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