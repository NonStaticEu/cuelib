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

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
class CueLine {

  private final int lineNumber;
  private final String raw;
  private final String keyword;
  private final String tail;
  private final List<String> tailParts;

  private static final Pattern SPACING_PATTERN = Pattern.compile("\\s+");

  CueLine(int lineNumber, String line) {
    this.lineNumber = lineNumber;
    this.raw = line.trim();

    int sep = raw.indexOf(' ');
    if (sep >= 0) {
      this.keyword = this.raw.substring(0, sep).toUpperCase(Locale.ROOT);
      this.tail = this.raw.substring(sep + 1).trim();
      this.tailParts = SPACING_PATTERN.splitAsStream(this.tail).collect(Collectors.toList());
    } else {
      this.keyword = this.raw;
      this.tail = null;
      this.tailParts = List.of();
    }
  }

  public String getTailWord(int i) {
    return this.tailParts.get(i);
  }

  public int words() {
    return 1 + tailParts.size();
  }

  public int length() {
    return raw.length();
  }

  public boolean isEmpty() {
    return raw.isEmpty();
  }

  private boolean isComment() {
    return raw.startsWith("#")
        || raw.startsWith("//")
        || raw.startsWith(";");
  }

  public boolean isSkippable() {
    return isEmpty() || isComment();
  }
}
