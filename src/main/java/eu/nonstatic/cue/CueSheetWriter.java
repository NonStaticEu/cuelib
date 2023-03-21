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

import lombok.NonNull;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Objects;

import static eu.nonstatic.cue.CueTools.quote;
import static java.util.stream.Collectors.joining;

public final class CueSheetWriter {

  private static final int INDENTATION_ROOT = 0;
  private static final int INDENTATION_FILE = INDENTATION_ROOT;
  private static final int INDENTATION_TRACK = 2;
  private static final int INDENTATION_INDEX = 4;
  private static final int INDENTATION_TRACK_PROPS = INDENTATION_INDEX;

  private static final OpenOption[] OPTIONS_STANDARD = { StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE };
  private static final OpenOption[] OPTIONS_OVERWRITE = { StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE };


  private CueSheetWriter() {}

  public static void writeCueSheet(CueDisc cueDisc, File cueFile, CueWriteOptions options) throws IOException {
    writeCueSheet(cueDisc, cueFile.toPath(), options);
  }

  public static void writeCueSheet(@NonNull CueDisc cueDisc, @NonNull Path cueFile, CueWriteOptions options) throws IOException {
    try (OutputStream os = Files.newOutputStream(cueFile, options.isOverwrite() ? OPTIONS_OVERWRITE : OPTIONS_STANDARD)) {
      writeCueSheet(cueDisc, os, options);
    }
  }

  public static void writeCueSheet(CueDisc cueDisc, OutputStream os, CueWriteOptions options) throws IOException {
    Charset charset = Objects.requireNonNullElse(cueDisc.getCharset(), CueDisc.DEFAULT_CHARSET);
    try (Writer writer = new OutputStreamWriter(os, charset)) {
      writeCueSheet(cueDisc, writer, options);
    }
  }

  public static void writeCueSheet(CueDisc cueDisc, Writer writer, CueWriteOptions options) {
    try (PrintWriter pw = new PrintWriter(writer)) {
      writeCueSheet(cueDisc, pw, options);
    }
  }

  public static void writeCueSheet(CueDisc cueDisc, PrintWriter pw, CueWriteOptions options) {
    CueIssues issues = cueDisc.checkConsistency(options);
    if(!issues.isEmpty()) {
      throw issues.toException();
    }

    cueDisc.getRemarks().forEach(remark -> printlnRaw(pw, INDENTATION_ROOT, remark.toSheetLine(options)));

    printlnQuoted(pw, INDENTATION_ROOT, CueWords.PERFORMER, cueDisc.getPerformer());
    printlnQuoted(pw, INDENTATION_ROOT, CueWords.TITLE, cueDisc.getTitle());
    printlnQuoted(pw, INDENTATION_ROOT, CueWords.SONGWRITER, cueDisc.getSongwriter());
    printlnQuoted(pw, INDENTATION_ROOT, CueWords.CATALOG, cueDisc.getCatalog());
    printlnQuoted(pw, INDENTATION_ROOT, CueWords.CDTEXTFILE, cueDisc.getCdTextFile());
    cueDisc.getOthers().forEach(other -> printlnRaw(pw, INDENTATION_ROOT, other.toSheetLine(options)));

    cueDisc.getFiles().forEach(file -> writeFile(file, pw, options));

    pw.flush();
  }


  public static void writeFile(CueFile file, PrintWriter pw, CueWriteOptions options) {
    if(file.getTrackCount() > 0) {
      printlnRaw(pw, INDENTATION_FILE, file.toSheetLine(options));

      file.getTracks().forEach(track -> writeTrack(track, pw, options));
    }
  }

  public static void writeTrack(CueTrack track, PrintWriter pw, CueWriteOptions options) {
    if(track.getIndexCount() > 0) {
      printlnRaw(pw, INDENTATION_TRACK, track.toSheetLine(options));

      track.getRemarks().forEach(remark -> printlnRaw(pw, INDENTATION_TRACK_PROPS, remark.toSheetLine(options)));
      printlnQuoted(pw, INDENTATION_TRACK_PROPS, CueWords.TITLE, track.getTitle());
      printlnQuoted(pw, INDENTATION_TRACK_PROPS, CueWords.PERFORMER, track.getPerformer());
      printlnQuoted(pw, INDENTATION_TRACK_PROPS, CueWords.SONGWRITER, track.getSongwriter());
      printlnQuoted(pw, INDENTATION_TRACK_PROPS, CueWords.ISRC, track.getIsrc());
      writeFlags(track.getFlags(), pw);
      track.getOthers().forEach(other -> printlnRaw(pw, INDENTATION_TRACK_PROPS, other.toSheetLine(options)));
      printlnRaw(pw, INDENTATION_TRACK_PROPS, CueWords.PREGAP, track.getPreGap());
      printlnRaw(pw, INDENTATION_TRACK_PROPS, CueWords.POSTGAP, track.getPostGap());

      track.getIndexes().forEach(index -> writeIndex(index, pw, options));
    }
  }

  public static void writeFlags(Collection<CueFlag> flags, PrintWriter pw) {
    if (!flags.isEmpty()) {
      String flagsString = flags.stream()
          .filter(Objects::nonNull)
          .map(CueFlag::getFlag)
          .collect(joining(" "));
      printIndent(pw, INDENTATION_TRACK_PROPS);
      pw.append(CueWords.FLAGS).append(' ').println(flagsString);
    }
  }

  public static void writeIndex(CueIndex index, PrintWriter pw, CueWriteOptions options) {
    printlnRaw(pw, INDENTATION_INDEX, index.toSheetLine(options));
  }


  private static void printlnQuoted(PrintWriter pw, int indent, String keyword, Object obj) {
    if (obj != null) {
      printlnRaw(pw, indent, keyword, quote(obj.toString()));
    }
  }

  private static void printlnRaw(PrintWriter pw, int indent, String keyword, Object obj) {
    if (obj != null) {
      printlnRaw(pw, indent, keyword + ' ' + obj);
    }
  }

  private static void printlnRaw(PrintWriter pw, int indent, Object obj) {
    if (obj != null) {
      printIndent(pw, indent);
      pw.println(obj);
    }
  }

  private static void printIndent(PrintWriter pw, int indent) {
    pw.append(" ".repeat(indent));
  }
}
