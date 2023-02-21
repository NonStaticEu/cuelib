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

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * For non-standard lines (eg ARTIST or REM-less GENRE)
 */
@Getter @Setter
@AllArgsConstructor
@EqualsAndHashCode
public class CueOther implements CueEntity {

  @NonNull
  private final String keyword;
  private final String value;

  @Override
  public String toSheetLine(CueSheetOptions options) {
    StringBuilder sb = new StringBuilder(keyword);
    if (value != null) {
      sb.append(' ').append(quote(value));
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return toSheetLine(null);
  }
}
