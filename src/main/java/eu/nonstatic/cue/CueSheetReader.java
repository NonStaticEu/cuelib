/**
 * Cuelib
 * Copyright (C) 2022 NonStatic
 *
 * This file is part of cuelib.
 * cuelib is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with . If not, see <https://www.gnu.org/licenses/>.
 */
package eu.nonstatic.cue;

import static eu.nonstatic.cue.CueTools.unquote;
import static eu.nonstatic.cue.CueWords.CATALOG;
import static eu.nonstatic.cue.CueWords.CDTEXTFILE;
import static eu.nonstatic.cue.CueWords.FLAGS;
import static eu.nonstatic.cue.CueWords.ISRC;
import static eu.nonstatic.cue.CueWords.PERFORMER;
import static eu.nonstatic.cue.CueWords.POSTGAP;
import static eu.nonstatic.cue.CueWords.PREGAP;
import static eu.nonstatic.cue.CueWords.SONGWRITER;
import static eu.nonstatic.cue.CueWords.TITLE;
import static java.util.stream.Collectors.toList;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CueSheetReader {

  public static final String CUE_EXTENSION = "cue";
  public static final Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;
  private static final int DEFAULT_CONFIDENCE = 30;

  private static final String MESSAGE_NO_KEYWORD = "{}#{}: No keyword on line: {}";
  protected static final String MESSAGE_NOT_CUE = "Not a cue file: ";

  private final int confidence;
  private final Charset fallbackCharset;

  public CueSheetReader() {
    this(DEFAULT_CONFIDENCE, DEFAULT_CHARSET);
  }

  public CueSheetReader(int confidence, Charset fallbackCharset) {
    this.confidence = confidence;
    this.fallbackCharset = fallbackCharset;
  }

  public CueContext detectEncoding(File file) {
    return detectEncoding(file.toPath());
  }

  public CueContext detectEncoding(Path file) {
    try (InputStream is = Files.newInputStream(file)) {
      Charset charset = detectEncoding(is);
      return new CueContext(file, charset);
    } catch (IOException e) {
      log.debug("Fallback to {} for {}: {}", fallbackCharset, file.toAbsolutePath(), e.getMessage(), e);
      return new CueContext(file, fallbackCharset);
    }
  }

  public Charset detectEncoding(InputStream is) throws IOException {
    Charset charset;

    if(!is.markSupported()) { // icu calls reset() on the stream, so it needs to support mark()
      is = new BufferedInputStream(is); // no try,
    }

    CharsetDetector cd = new CharsetDetector();
    cd.setText(is);
    CharsetMatch cm = cd.detect(); // calls reset() !
    if (cm != null && cm.getConfidence() > confidence) {
      charset = Charset.forName(cm.getName());
    } else {
      log.debug("Confidence low. Fallback to {}", fallbackCharset);
      charset = fallbackCharset;
    }
    return charset;
  }


  public CueDisc readCueSheet(File cueFile) throws IOException {
    return readCueSheet(cueFile.toPath());
  }

  public static CueDisc readCueSheet(File cueFile, Charset charset) throws IOException {
    return readCueSheet(cueFile.toPath(), charset);
  }

  public CueDisc readCueSheet(Path cueFile) throws IOException {
    CueContext context = detectEncoding(cueFile);
    return readCueSheet(cueFile, context.getCharset());
  }

  public static CueDisc readCueSheet(Path cueFile, Charset charset) throws IOException {
    if (!isCueFile(cueFile)) {
      throw new IllegalArgumentException(MESSAGE_NOT_CUE + cueFile);
    }
    try (InputStream inputStream = Files.newInputStream(cueFile)) {
      return readCueSheet(inputStream, new CueContext(cueFile, charset));
    }
  }

  public static boolean isCueFile(@NonNull Path file) {
    return Files.isRegularFile(file) && CueTools.isExt(file.getFileName().toString(), CUE_EXTENSION);
  }

  public static CueDisc readCueSheet(URL cueFile,  Charset charset) throws IOException {
    try (InputStream is = cueFile.openStream()) {
      return readCueSheet(is, new CueContext(cueFile.toExternalForm(), charset));
    }
  }

  public static CueDisc readCueSheet(InputStream inputStream, CueContext context) throws IOException {
    try (CueLineReader cueLineReader = new CueLineReader(inputStream, context.getCharset())) {
      return readCueSheet(cueLineReader, context);
    }
  }

  public static CueDisc readCueSheet(Reader reader, CueContext context) throws IOException {
    try (CueLineReader cueLineReader = new CueLineReader(reader, context.getCharset())) {
      return readCueSheet(cueLineReader, context);
    }
  }

  public static CueDisc readCueSheet(CharSequence[] lines, CueContext context) throws IOException {
    Charset charset = context.getCharset();
    byte[] bytes = String.join("\n", lines)
        .getBytes(charset);
    return readCueSheet(bytes, context);
  }

  public static CueDisc readCueSheet(Iterable<? extends CharSequence> lines, CueContext context) throws IOException {
    Charset charset = context.getCharset();
    byte[] bytes = String.join("\n", lines)
        .getBytes(charset);
    return readCueSheet(bytes, context);
  }

  public static CueDisc readCueSheet(byte[] bytes, CueContext context) throws IOException {
    try (CueLineReader cueLineReader = new CueLineReader(new ByteArrayInputStream(bytes), context.getCharset())) {
      return readCueSheet(cueLineReader, context);
    }
  }

  public static CueDisc readCueSheet(CueLineReader cueLineReader, CueContext context) throws IOException {
    CueDisc disc = new CueDisc(context.getPath(), context.getCharset());
    int previousTrackNum = 0;

    CueLine line;
    while((line = cueLineReader.readLine()) != null) {
      if (!line.isEmpty()) {
        previousTrackNum = readCueSheetLine(cueLineReader, line, previousTrackNum, disc, context);
      }
    }
    return disc;
  }

  private static int readCueSheetLine(CueLineReader cueLineReader, CueLine line, int previousTrackNum, CueDisc disc, CueContext context) throws IOException {
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
          FileReference fileReference = FileReference.parse(tail, context);
          CueFile file = readFile(fileReference, previousTrackNum, cueLineReader, context);
          disc.addFileUnsafe(file);
          CueTrack latestTrack = file.getLastTrack();
          if(latestTrack != null) { // a file without track may be possible on a peculiar cue sheet
            previousTrackNum = latestTrack.number;
          }
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
      log.warn(MESSAGE_NO_KEYWORD, context.getPath(), line.getLineNumber(), line.getRaw());
    }
    return previousTrackNum;
  }


  /**
   * REM DISCID 750FF008
   * REM COMMENT "ExactAudioCopy v1.0b3"
   */
  private static CueRemark readRemark(CueLine line) {
    String tail = line.getTail();
    if (tail != null) {
      int sep = tail.indexOf(' ');
      if (sep >= 0) {
        String type = tail.substring(0, sep);
        if(CueRemark.TAGS.contains(type)) {
          String content = tail.substring(sep + 1).trim();
          return new CueRemark(type, unquote(content));
        } else {
          return new CueRemark(null, unquote(tail));
        }
      }
    }

    return new CueRemark(null, unquote(tail));
  }

  private static CueOther readOther(CueLine line) {
    return new CueOther(line.getKeyword(), unquote(line.getTail()));
  }

  private static CueFile readFile(FileReference fileReference, int previousTrackNum, CueLineReader reader, CueContext context) throws IOException {
    reader.mark(); // to avoid infinite loop on FILE followed by FILE
    CueFile file = new CueFile(fileReference);

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
              file.addTrackUnsafe(readTrack(number, type, reader, context)); // cannot control track numbers' consistency/chaining
              file.renumberingNecessary = file.renumberingNecessary || (number != ++previousTrackNum);
              break;
            default:
              log.warn("{}: Unknown file line: {}", context.getPath(), line.getRaw());
              // maybe belongs to the upper level
              reader.reset();
              return file;
          }
        } else {
          log.warn(MESSAGE_NO_KEYWORD, context.getPath(), line.getLineNumber(), line.getRaw());
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
              String isrc = unquote(tail);
              if(!CueTrack.ISRC_ZERO.equals(isrc)) {
                track.setIsrc(isrc);
              }
              break;
            case PREGAP:
              track.setPreGap(TimeCode.parse(tail));
              break;
            case POSTGAP:
              track.setPostGap(TimeCode.parse(tail));
              break;
            case FLAGS:
              track.setFlags(readFlags(line));
              break;
            case CueRemark.KEYWORD:
              track.addRemark(readRemark(line));
              break;
            default:
              log.warn("{}#{}: Unknown track line: {}", context.getPath(), line.getLineNumber(), line.getRaw());
              track.addOther(readOther(line));
          }
        } else {
          log.warn(MESSAGE_NO_KEYWORD, context.getPath(), line.getLineNumber(), line.getRaw());
        }
      }
      reader.mark();
    }

    return track; // for the last track
  }

  private static List<CueFlag> readFlags(CueLine line) {
    return line.getTailParts().stream().map(CueFlag::flagOf).collect(toList());
  }

  private static CueIndex readIndex(CueLine cueLine) {
    int number = Integer.parseInt(cueLine.getTailWord(0));
    String timeCode = cueLine.getTailWord(1);
    return new CueIndex(number, TimeCode.parse(timeCode));
  }
}
