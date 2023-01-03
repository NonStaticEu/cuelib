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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

abstract class CueTestBase {

  static URL myTestUrl = CueDiscTest.class.getResource("/My Test.cue");

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

  public static void writeLines(Path file, List<String> lines, Charset cs) throws IOException {
    try(PrintWriter pw = new PrintWriter(Files.newBufferedWriter(file, cs, StandardOpenOption.TRUNCATE_EXISTING))) {
      for (String line : lines) {
        pw.println(line);
      }
    }
  }

}
