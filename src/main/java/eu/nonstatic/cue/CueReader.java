package eu.nonstatic.cue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static eu.nonstatic.cue.CueTools.isCueFile;
import static eu.nonstatic.cue.CueTools.unquote;

@Slf4j
public class CueReader implements CueWords {

  public static CueDisc readCue(File cueFile) throws IOException {
    return readCue(cueFile.toPath());
  }

  public static CueDisc readCue(File cueFile, Charset charset) throws IOException {
    return readCue(cueFile.toPath(), charset);
  }

  public static CueDisc readCue(Path cueFile) throws IOException {
    checkCueExtension(cueFile);
    try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(cueFile))) {
      CueContext context = new CueTools().detectEncoding(cueFile.toString(), bis); // the stream is reset after detection
      return readCue(bis, context);
    }
  }

  public static CueDisc readCue(Path cueFile, Charset charset) throws IOException {
    checkCueExtension(cueFile);
    try (InputStream inputStream = Files.newInputStream(cueFile)) {
      return readCue(inputStream, new CueContext(cueFile, charset));
    }
  }

  private static void checkCueExtension(Path cueFile) throws IllegalArgumentException {
    if (!isCueFile(cueFile)) {
      throw new IllegalArgumentException("Not a cue file: " + cueFile);
    }
  }

  public static CueDisc readCue(InputStream inputStream, CueContext context) throws IOException {
    try (CueLineReader cueLineReader = new CueLineReader(inputStream, context.getCharset())) {
      return readCue(cueLineReader, context);
    }
  }

  public static CueDisc readCue(Reader reader, CueContext context) throws IOException {
    try (CueLineReader cueLineReader = new CueLineReader(reader, context.getCharset())) {
      return readCue(cueLineReader, context);
    }
  }

  public static CueDisc readCue(CharSequence[] lines, CueContext context) throws IOException {
    Charset charset = context.getCharset();
    byte[] bytes = String.join("\n", lines)
        .getBytes(charset);
    return readCue(bytes, context);
  }

  public static CueDisc readCue(Iterable<? extends CharSequence> lines, CueContext context) throws IOException {
    Charset charset = context.getCharset();
    byte[] bytes = String.join("\n", lines)
        .getBytes(charset);
    return readCue(bytes, context);
  }

  private static CueDisc readCue(byte[] bytes, CueContext context) throws IOException {
    try (CueLineReader cueLineReader = new CueLineReader(new ByteArrayInputStream(bytes), context.getCharset())) {
      return readCue(cueLineReader, context);
    }
  }

  private static CueDisc readCue(CueLineReader cueLineReader, CueContext context) throws IOException {
    CueDisc disc = new CueDisc(context.getPath(), context.getCharset());

    CueLine line;
    while ((line = cueLineReader.readLine()) != null) {
      if (!line.isEmpty()) {
        String keyword = line.getKeyword();
        if (keyword != null) {
          String tail = line.getTail();

          switch (keyword) {
            case TITLE:
              disc.setTitle(unquote(tail));
              break;
            case PERFORMER:
              disc.setPerformer(unquote(tail));
              break;
            case SONGWRITER:
              disc.setSongwriter(unquote(tail));
              break;
            case CATALOG:
              disc.setCatalog(unquote(tail));
              break;
            case CueFile.KEYWORD:
              FileAndFormat ff = FileAndFormat.of(tail);
              disc.addFile(readFile(ff.file, ff.format, cueLineReader, context));
              break;
            case CDTEXTFILE:
              disc.setCdTextFile(unquote(tail));
              break;
            case CueRemark.KEYWORD:
              disc.addRemark(readRemark(line));
              break;
            default:
              log.warn("{}#{}: Unknown disc line: {}", context.getPath(), line.getLineNumber(), line.getRaw());
              disc.addOther(readOther(line));
          }
        } else {
          log.warn("{}#{}: No keyword on line: {}", context.getPath(), line.getLineNumber(), line.getRaw());
        }
      }
    }
    return disc;
  }


  /**
   * REM DISCID 750FF008 REM COMMENT "ExactAudioCopy v1.0b3"
   */
  private static CueRemark readRemark(CueLine line) {
    String tail = line.getTail();
    if (tail != null) {
      int sep = tail.indexOf(' ');
      if (sep >= 0) {
        String type = tail.substring(0, sep);
        String content = tail.substring(sep + 1).trim();
        return new CueRemark(type, unquote(content));
      }
    }

    return new CueRemark(null, unquote(tail));
  }

  private static CueOther readOther(CueLine line) {
    return new CueOther(line.getKeyword(), unquote(line.getTail()));
  }

  private static CueFile readFile(String fileName, String fileFormat, CueLineReader reader, CueContext context) throws IOException {
    reader.mark(); // to avoid infinite loop on FILE followed by FILE
    CueFile file = new CueFile(fileName, fileFormat);

    CueLine line;
    while ((line = reader.readLine()) != null) {
      if (!line.isEmpty()) {
        String keyword = line.getKeyword();
        if (keyword != null) {
          switch (keyword) {
            case CueFile.KEYWORD: // found new file
              reader.reset();
              return file;
            case CueTrack.KEYWORD:
              int number = Integer.parseInt(line.getTailWord(0));
              String type = line.getTailWord(1);
              file.addTrack(readTrack(number, type, reader, context));
              break;
            default:
              log.warn("{}: Unknown file line: {}", context.getPath(), line.getRaw());
              // maybe belongs to the upper level
              reader.reset();
              return file;
          }
        } else {
          log.warn("{}#{}: No keyword on line: {}", context.getPath(), line.getLineNumber(), line.getRaw());
        }
      }
      reader.mark();
    }

    return file; // for the last file
  }


  private static CueTrack readTrack(int trackNumber, String type, CueLineReader reader, CueContext context) throws IOException {
    reader.mark();
    CueTrack track = new CueTrack(trackNumber, type);

    CueLine line;
    while ((line = reader.readLine()) != null) {
      if (!line.isEmpty()) {
        String keyword = line.getKeyword();
        if (keyword != null) {
          String tail = line.getTail();

          switch (keyword) {
            case CueFile.KEYWORD: // found new file
            case CueTrack.KEYWORD: // found new track
              reader.reset();
              return track;
            case CueIndex.KEYWORD:
              track.addIndex(readIndex(line));
              break;
            case TITLE:
              track.setTitle(unquote(tail));
              break;
            case PERFORMER:
              track.setPerformer(unquote(tail));
              break;
            case SONGWRITER:
              track.setSongwriter(unquote(tail));
              break;
            case ISRC:
              track.setIsrc(unquote(tail));
              break;
            case PREGAP:
              track.setPregap(tail);
              break;
            case POSTGAP:
              track.setPostgap(tail);
              break;
            case FLAGS:
              track.setFlags(line.getTailParts());
              break;
            case CueRemark.KEYWORD:
              track.addRemark(readRemark(line));
              break;
            default:
              log.warn("{}#{}: Unknown track line: {}", context.getPath(), line.getLineNumber(), line.getRaw());
              track.addOther(readOther(line));
          }
        } else {
          log.warn("{}#{}: No keyword on line: {}", context.getPath(), line.getLineNumber(), line.getRaw());
        }
      }
      reader.mark();
    }

    return track; // for the last track
  }


  private static CueIndex readIndex(CueLine cueLine) {
    int number = Integer.parseInt(cueLine.getTailWord(0));
    String timeCode = cueLine.getTailWord(1);
    return new CueIndex(number, timeCode);
  }

  @Getter
  @AllArgsConstructor
  static class FileAndFormat {

    private String file;
    private String format;

    static FileAndFormat of(@NonNull String tail) {
      String file, format;
      int sep = tail.lastIndexOf(' ');
      if (sep >= 0) {
        file = tail.substring(0, sep).trim();
        format = tail.substring(sep + 1);
      } else {
        file = tail;
        format = null;
      }
      return new FileAndFormat(file, format);
    }
  }
}
