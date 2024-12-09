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

import static eu.nonstatic.cue.CueTools.quote;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@EqualsAndHashCode
public class CueRemark implements CueEntity {

  public static final String KEYWORD = CueWords.REMARK;
  // Not extensive tag list
  public static final String TAG_GENRE = "GENRE";
  public static final String TAG_DATE = "DATE";
  public static final String TAG_COMPOSER = "COMPOSER";
  public static final String TAG_UPC = "UPC";
  public static final String TAG_DISCID = "DISCID";
  public static final String TAG_COMMENT = "COMMENT";

  private static final String TAG_PREFIX = "TAG_";
  protected static final List<String> TAGS = listTags();

  private final String tag;
  private final String value;

  private static List<String> listTags() {
    return Arrays.stream(CueRemark.class.getDeclaredFields())
        .filter(field -> String.class == field.getType() && field.getName().startsWith(TAG_PREFIX))
        .map(field -> {
          try {
            return (String) field.get(null);
          } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
          }
        })
        .collect(Collectors.toList());
  }

  public CueRemark(String value) {
    this(null, value);
  }

  public static CueRemark commentOf(String comment) {
    return new CueRemark(TAG_COMMENT, comment);
  }

  public boolean isComment() {
    return TAG_COMMENT.equalsIgnoreCase(tag);
  }

  public boolean isSeveralWords() {
    return value != null && value.split("\\s+").length > 1;
  }

  private boolean requiresQuotes() {
    return isComment() || isSeveralWords() || value.isEmpty();
  }

  @Override
  public String toSheetLine(CueWriteOptions options) {
    StringBuilder sb = new StringBuilder(KEYWORD);
    if (tag != null) {
      sb.append(' ').append(tag);
    }
    if (value != null) {
      sb.append(' ').append(requiresQuotes() ? quote(value) : value);
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return toSheetLine(null);
  }
}
