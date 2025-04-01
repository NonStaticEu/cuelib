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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

abstract class CueTestBase {

  static URL myTestUrl = CueDiscTest.class.getResource("/My Test.cue");
  static URL sizeDurationTestUrl = CueDiscTest.class.getResource("/SD Test.cue");

  /*
   * Those samples are taken from here:
   * https://en.wikipedia.org/wiki/Synthesizer
   * https://commons.wikimedia.org/wiki/File:Amplitudenmodulation.ogg
   */
  static final String AIFF_NAME = "/audio/Arpeggio.aiff";
  static final String WAVE_NAME = "/audio/Amplitudenmodulation.wav";
  static final String MP3_NAME  = "/audio/Moog-juno-303-example.mp3";
  static final String FLAC_NAME = "/audio/Filtered_envelope_sawtooth_moog.flac";

  static final URL AIFF_URL = CueTestBase.class.getResource(AIFF_NAME);
  static final URL WAVE_URL = CueTestBase.class.getResource(WAVE_NAME);
  static final URL MP3_URL  = CueTestBase.class.getResource(MP3_NAME);
  static final URL FLAC_URL = CueTestBase.class.getResource(FLAC_NAME);


  public static List<String> readLines(URL url, Charset cs) throws IOException {
    List<String> lines = new ArrayList<>();
    try(BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), cs))) {
      String line;
      while((line = br.readLine()) != null) {
        lines.add(line);
      }
    }
    return lines;
  }

  public static void writeLines(File file, List<String> lines, Charset cs) throws IOException {
    writeLines(file.toPath(), lines, cs);
  }

  public static void writeLines(Path file, List<String> lines, Charset cs) throws IOException {
    try(PrintWriter pw = new PrintWriter(Files.newBufferedWriter(file, cs, StandardOpenOption.TRUNCATE_EXISTING))) {
      for (String line : lines) {
        pw.println(line);
      }
    }
  }

  public static Path copyFileContents(URL url, Path dir, String fileName) throws IOException {
    return copyFileContents(url, dir.resolve(fileName));
  }

  public static Path copyFileContents(URL url, Path file) throws IOException {
    try(InputStream is  = url.openStream()) {
      Files.copy(is, file, StandardCopyOption.REPLACE_EXISTING);
    }
    return file;
  }

  public static void deleteRecursive(Path dir) throws IOException {
    Files.walkFileTree(dir, new FileVisitor<>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
