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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CueSheetReader {

  public static final String CUE_EXTENSION = "cue";
  public static final int DEFAULT_CONFIDENCE = 30;
  public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private static final String MESSAGE_NO_KEYWORD = "%s#%s: No keyword on line: %s";
  static final String MESSAGE_NOT_CUE = "Not a cue file: ";


  private final int confidence;
  private final Charset fallbackCharset;

  public CueSheetReader() {
    this(DEFAULT_CONFIDENCE, DEFAULT_CHARSET);
  }

  public CueSheetReader(Charset fallbackCharset) {
    this(DEFAULT_CONFIDENCE, fallbackCharset);
  }

  public CueSheetReader(int confidence, Charset fallbackCharset) {
    this.confidence = confidence;
    this.fallbackCharset = fallbackCharset;
  }


  public static boolean isCueFile(@NonNull Path file) {
    return Files.isRegularFile(file) && CueTools.isExt(file.getFileName().toString(), CUE_EXTENSION);
  }

  public CueSheetReadout readCueSheet(File cueFile) throws IOException, BadCharsetException {
    return readCueSheet(cueFile.toPath());
  }

  public CueSheetReadout readCueSheet(File cueFile, Charset charset) throws IOException, BadCharsetException {
    return readCueSheet(cueFile.toPath(), charset);
  }

  public CueSheetReadout readCueSheet(File cueFile, CueOptions options) throws IOException, BadCharsetException {
    return readCueSheet(cueFile.toPath(), options);
  }

  public CueSheetReadout readCueSheet(Path cueFile) throws IOException, BadCharsetException {
    return readCueSheet(cueFile, (Charset) null);
  }

  public CueSheetReadout readCueSheet(Path cueFile, Charset charset) throws IOException, BadCharsetException {
    return readCueSheet(cueFile, new CueOptions(charset));
  }

  public CueSheetReadout readCueSheet(Path cueFile, CueOptions options) throws IOException, BadCharsetException {
    if (!isCueFile(cueFile)) {
      throw new IllegalArgumentException(MESSAGE_NOT_CUE + cueFile);
    }

    CueSheetContext context = new CueSheetContext(cueFile, options);

    Charset charset;
    try(InputStream is = new BufferedInputStream(Files.newInputStream(cueFile))) {
      charset = handleBomAndCharset(is, context);
      CharsetDecoder cd = charset.newDecoder();
      // Not passing the charset because it's not as charset sensitive as the decoder
      // and would allow to read eg: iso-8859-1 files using an utf8 charset.
      BufferedReader reader = new BufferedReader(new InputStreamReader(is, cd));
      CueDisc disc = readCueSheet(reader, context);
      return new CueSheetReadout(disc, context);
    }
  }

  public CueSheetReadout readCueSheet(URL cueFile) throws IOException, BadCharsetException {
    return readCueSheet(cueFile, (Charset) null);
  }

  public CueSheetReadout readCueSheet(URL cueFile, Charset charset) throws IOException, BadCharsetException {
    return readCueSheet(cueFile, new CueOptions(charset));
  }

  public CueSheetReadout readCueSheet(URL cueFile, CueOptions options) throws IOException, BadCharsetException {
    try (InputStream is = cueFile.openStream()) {
      CueSheetContext context = new CueSheetContext(cueFile.toExternalForm(), options);
      CueDisc disc = readCueSheet(is, context);
      return new CueSheetReadout(disc, context);
    }
  }

  public CueDisc readCueSheet(InputStream is, CueSheetContext context) throws IOException, BadCharsetException {
    if(!is.markSupported()) {
      is = new BufferedInputStream(is);
    }

    Charset charset = handleBomAndCharset(is, context);

    try (InputStreamReader reader = new InputStreamReader(is, charset)) {
      return readCueSheet(reader, context);
    }
  }

  /**
   * Will NOT try to skip any BOM since a reader implies we already know the charset (and hence may have done the necessary skipping)
   */
  public CueDisc readCueSheet(Reader reader, CueSheetContext context) throws IOException, BadCharsetException {
    return readCueSheet(new CueLineReader(reader), context);
  }

  public CueDisc readCueSheet(CharSequence[] lines, CueSheetContext context) throws IOException, BadCharsetException {
    byte[] bytes = String.join("\n", lines)
        .getBytes(context.getOptions().getCharset());
    return readCueSheet(bytes, context);
  }

  public CueDisc readCueSheet(Iterable<? extends CharSequence> lines, CueSheetContext context) throws IOException, BadCharsetException {
    Charset charset = context.getOptions().getCharset();
    byte[] bytes = String.join("\n", lines)
                         .getBytes(charset);
    return readCueSheet(bytes, context);
  }

  public CueDisc readCueSheet(byte[] bytes, CueSheetContext context) throws IOException, BadCharsetException {
    return readCueSheet(new ByteArrayInputStream(bytes), context);
  }

  public static CueDisc readCueSheet(CueLineReader cueLineReader, CueSheetContext context) throws IOException, BadCharsetException {
    CueOptions options = context.getOptions();
    CueDisc disc = new CueDisc(context.getPath(), options.getCharset());
    int previousTrackNum = 0;

    CueLine line;
    while((line = cueLineReader.readLine()) != null) {
      if (!line.isSkippable()) {
        previousTrackNum = readCueSheetLine(cueLineReader, line, previousTrackNum, disc, context);
      }
    }

    // if some timecodes were straightened, the odds are that all the timecodes' frames of the sheet were in hundredths of a second
    if(options.isTimeCodeLeniency()
        && disc.getIndexes().stream().anyMatch(index -> index.getTimeCode().isScaled100to75())) {
      disc.getIndexes().stream()
          .filter(index -> !index.getTimeCode().isScaled100to75())
          .forEach(cueIndex -> cueIndex.setTimeCode(cueIndex.getTimeCode().scale100to75()));
    }

    return disc;
  }

  /**
   * @param is must support marking
   * @param context; its options charset may be altered if the bom or the charset detection decides
   * @return detected charset, else fallback
   * @throws IOException when issue reading the stream
   */
  private Charset handleBomAndCharset(InputStream is, CueSheetContext context) throws IOException, BadCharsetException {
    CueOptions options = context.getOptions();
    try {
      return handleBomAndCharset(is, options);
    } catch (BadCharsetException e) {
      Charset fallback = options.getCharset();
      if(fallback != null) {
        String message = String.format("Fallback to %s for %s: %s", this.fallbackCharset, context.getPath(), e.getMessage());
        log.warn(message, e);
        context.addIssue(message);
        return fallback;
      } else {
        throw e;
      }
    }
  }

  /**
   * @param is must support marking
   * @param options to be altered if the bom or the charset detection decides
   * @return detected charset, else fallback
   * @throws IOException when issue reading the stream
   */
  private Charset handleBomAndCharset(InputStream is, CueOptions options) throws IOException, BadCharsetException {
    is.mark(Bom.MAX_LENGTH_BYTES);
    Bom bom = Bom.read(is);
    is.reset();

    Charset actualCharset = options.getCharset();
    if (bom != null) {
      int skipped = 0;
      while(skipped < bom.length()) {
        skipped += (int) is.skip(bom.length());
      }
      // and forcing charset to the one we're now sure of.
      actualCharset = bom.getCharset();
    } else if(options.getCharset() == null) {
      try {
        actualCharset = detectEncoding(is);
      } catch(BadCharsetException e) {
        options.setCharset(fallbackCharset); // may be null
        throw e;
      }
    }
    options.setCharset(actualCharset);
    return actualCharset;
  }

  public Charset detectEncoding(File file) throws IOException, BadCharsetException {
    return detectEncoding(file.toPath());
  }

  public Charset detectEncoding(Path file) throws IOException, BadCharsetException {
    try (InputStream is = Files.newInputStream(file)) {
      return detectEncoding(is);
    }
  }

  public Charset detectEncoding(URL url) throws IOException, BadCharsetException {
    try (InputStream is = url.openStream()) {
      return detectEncoding(is);
    }
  }

  public Charset detectEncoding(InputStream is) throws BadCharsetException {
    if(!is.markSupported()) { // icu calls reset() on the stream, so it needs to support mark()
      is = new BufferedInputStream(is); // no try,
    }

    CharsetDetector cd = new CharsetDetector();
    try {
      cd.setText(is);
    } catch (IOException e) {
      throw new BadCharsetException(e);
    }
    CharsetMatch cm = cd.detect(); // calls reset() !
    if (cm != null && cm.getConfidence() >= confidence) {
      return Charset.forName(cm.getName());
    } else {
      throw new BadCharsetException("Confidence low. Cannot detect charset");
    }
  }

  private static int readCueSheetLine(CueLineReader cueLineReader, CueLine line, int previousTrackNum, CueDisc disc, CueSheetContext context) throws IOException {
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
          FileReference fileReference = CueFile.parse(tail, context);
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
          context.addIssue("%s#%s: Unknown disc line: %s", context.getPath(), line.getLineNumber(), line.getRaw());
          disc.addOther(readOther(line));
      }
    } else {
      context.addIssue(MESSAGE_NO_KEYWORD, context.getPath(), line.getLineNumber(), line.getRaw());
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

  private static CueFile readFile(FileReference fileReference, int previousTrackNum, CueLineReader reader, CueSheetContext context) throws IOException, BadCharsetException {
    reader.mark(); // to avoid infinite loop on FILE followed by FILE
    CueFile file = new CueFile(fileReference);

    CueLine line;
    while ((line = reader.readLine()) != null) {
      if (!line.isSkippable()) {
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
              context.addIssue("%S: Unknown file line: %S", context.getPath(), line.getRaw());
              // maybe belongs to the upper level
              reader.reset();
              return file;
          }
        } else {
          context.addIssue(MESSAGE_NO_KEYWORD, context.getPath(), line.getLineNumber(), line.getRaw());
        }
      }
      reader.mark();
    }

    return file; // for the last file
  }


  private static CueTrack readTrack(int trackNumber, String type, CueLineReader reader, CueSheetContext context) throws IOException, BadCharsetException {
    reader.mark();
    CueTrack track = new CueTrack(trackNumber, type);

    CueLine line;
    while ((line = reader.readLine()) != null) {
      if (!line.isSkippable()) {
        String keyword = line.getKeyword();
        if (keyword != null) {
          String tail = line.getTail();

          switch (keyword) {
            case CueFile.KEYWORD: // found new file
            case CueTrack.KEYWORD: // found new track
              reader.reset();
              return track;
            case CueIndex.KEYWORD:
              track.addIndex(readIndex(line, context));
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
              setIsrc(track, isrc, context);
              break;
            case PREGAP:
              track.setPreGap(readTimeCode(tail, context));
              break;
            case POSTGAP:
              track.setPostGap(readTimeCode(tail, context));
              break;
            case FLAGS:
              track.setFlags(readFlags(line));
              break;
            case CueRemark.KEYWORD:
              track.addRemark(readRemark(line));
              break;
            default:
              context.addIssue("%s#%S: Unknown track line: %s", context.getPath(), line.getLineNumber(), line.getRaw());
              track.addOther(readOther(line));
          }
        } else {
          context.addIssue(MESSAGE_NO_KEYWORD, context.getPath(), line.getLineNumber(), line.getRaw());
        }
      }
      reader.mark();
    }

    return track; // for the last track
  }

  private static void setIsrc(CueTrack track, String isrc, CueSheetContext context) {
    CueOptions options = context.getOptions();
    if(track.setIsrc(isrc, options.isIsrcLeniency())) {
      context.addIssue(CueTrack.MESSAGE_BAD_ISRC, isrc);
    }
  }

  private static List<CueFlag> readFlags(CueLine line) {
    return line.getTailParts().stream().map(CueFlag::flagOf).collect(toList());
  }

  private static CueIndex readIndex(CueLine cueLine, CueSheetContext context) {
    int number = Integer.parseInt(cueLine.getTailWord(0));
    String timeCode = cueLine.getTailWord(1);
    return new CueIndex(number, readTimeCode(timeCode, context));
  }

  private static TimeCode readTimeCode(String timeCodeString, CueSheetContext context) {
    TimeCode timeCode = TimeCode.parse(timeCodeString, context.getOptions().isTimeCodeLeniency());
    if(timeCode.isScaled100to75()) {
      context.addIssue("Scaled %s to %s", timeCode.toStringRaw(), timeCode);
    }
    return timeCode;
  }
}
