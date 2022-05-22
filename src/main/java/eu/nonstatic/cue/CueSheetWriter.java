package eu.nonstatic.cue;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;

import static eu.nonstatic.cue.CueTools.quote;
import static java.util.stream.Collectors.joining;

public class CueSheetWriter {

  public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
  public static final int INDENTATION_ROOT = 0;
  public static final int INDENTATION_FILE = INDENTATION_ROOT;
  public static final int INDENTATION_TRACK = 2;
  public static final int INDENTATION_INDEX = 4;
  public static final int INDENTATION_TRACK_PROPS = INDENTATION_INDEX;

  public static final OpenOption[] OPTIONS_STANDARD = {StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE};
  public static final OpenOption[] OPTIONS_OVERWRITE = {StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE};

  public static void writeCueSheet(CueDisc cueDisc, File cueFile) throws IOException {
    writeCueSheet(cueDisc, cueFile.toPath());
  }

  public static void writeCueSheet(CueDisc cueDisc, File cueFile, boolean overWrite) throws IOException {
    writeCueSheet(cueDisc, cueFile.toPath(), overWrite);
  }

  public static void writeCueSheet(CueDisc cueDisc, Path cueFile) throws IOException {
    writeCueSheet(cueDisc, cueFile, false);
  }

  public static void writeCueSheet(CueDisc cueDisc, Path cueFile, boolean overWrite) throws IOException {
    try (OutputStream os = Files.newOutputStream(cueFile, overWrite ? OPTIONS_OVERWRITE : OPTIONS_STANDARD)) {
      writeCueSheet(cueDisc, os);
    }
  }

  public static void writeCueSheet(CueDisc cueDisc, OutputStream os) throws IOException {
    Charset charset = Objects.requireNonNullElse(cueDisc.getCharset(), DEFAULT_CHARSET);
    try (Writer writer = new OutputStreamWriter(os, charset)) {
      writeCueSheet(cueDisc, writer);
    }
  }

  public static void writeCueSheet(CueDisc cueDisc, Writer writer) {
    try (PrintWriter pw = new PrintWriter(writer)) {
      writeCueSheet(cueDisc, pw);
    }
  }

  public static void writeCueSheet(CueDisc cueDisc, PrintWriter pw) {
    cueDisc.getRemarks().forEach(remark -> printlnRaw(pw, INDENTATION_ROOT, remark.toSheetLine()));

    printlnQuoted(pw, INDENTATION_ROOT, CueWords.PERFORMER, cueDisc.getPerformer());
    printlnQuoted(pw, INDENTATION_ROOT, CueWords.TITLE, cueDisc.getTitle());
    printlnQuoted(pw, INDENTATION_ROOT, CueWords.SONGWRITER, cueDisc.getSongwriter());
    printlnQuoted(pw, INDENTATION_ROOT, CueWords.CATALOG, cueDisc.getCatalog());
    printlnQuoted(pw, INDENTATION_ROOT, CueWords.CDTEXTFILE, cueDisc.getCdTextFile());
    cueDisc.getOthers().forEach(other -> printlnRaw(pw, INDENTATION_ROOT, other.toSheetLine()));

    cueDisc.getFiles().forEach(file -> writeFile(file, pw));

    pw.flush();
  }


  public static void writeFile(CueFile file, PrintWriter pw) {
    printlnRaw(pw, INDENTATION_FILE, file.toSheetLine());

    file.getTracks().forEach(track -> writeTrack(track, pw));
  }

  public static void writeTrack(CueTrack track, PrintWriter pw) {
    printlnRaw(pw, INDENTATION_TRACK, track.toSheetLine());

    track.getRemarks().forEach(remark -> printlnRaw(pw, INDENTATION_TRACK_PROPS, remark.toSheetLine()));
    printlnQuoted(pw, INDENTATION_TRACK_PROPS, CueWords.TITLE, track.getTitle());
    printlnQuoted(pw, INDENTATION_TRACK_PROPS, CueWords.PERFORMER, track.getPerformer());
    printlnQuoted(pw, INDENTATION_TRACK_PROPS, CueWords.SONGWRITER, track.getSongwriter());
    printlnQuoted(pw, INDENTATION_TRACK_PROPS, CueWords.ISRC, track.getIsrc());
    writeFlags(track.getFlags(), pw);
    track.getOthers().forEach(other -> printlnRaw(pw, INDENTATION_TRACK_PROPS, other.toSheetLine()));
    printlnQuoted(pw, INDENTATION_TRACK_PROPS, CueWords.PREGAP, track.getPregap());
    printlnQuoted(pw, INDENTATION_TRACK_PROPS, CueWords.POSTGAP, track.getPostgap());

    track.getIndexes().forEach(index -> writeIndex(index, pw));
  }

  public static void writeFlags(List<CueFlag> flags, PrintWriter pw) {
    if (!flags.isEmpty()) {
      String flagsString = flags.stream()
          .filter(Objects::nonNull)
          .map(CueFlag::getFlag)
          .collect(joining(" "));
      printIndent(pw, INDENTATION_TRACK_PROPS);
      pw.append(CueWords.FLAGS).append(' ').println(flagsString);
    }
  }

  public static void writeIndex(CueIndex index, PrintWriter pw) {
    printlnRaw(pw, INDENTATION_INDEX, index.toSheetLine());
  }


  private static void printlnQuoted(PrintWriter pw, int indent, String keyword, String str) {
    if (str != null) {
      printlnRaw(pw, indent, keyword + ' ' + quote(str));
    }
  }

  private static void printlnRaw(PrintWriter pw, int indent, String str) {
    if (str != null) {
      printIndent(pw, indent);
      pw.println(str);
    }
  }

  private static void printIndent(PrintWriter pw, int indent) {
    pw.append(" ".repeat(indent));
  }
}
