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

import java.nio.charset.Charset;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter @Builder
@AllArgsConstructor
public class CueReadOptions {
  public static final TimeCodeRounding DEFAULT_ROUNDING = TimeCodeRounding.DOWN;

  @Setter(AccessLevel.PACKAGE)
  private Charset charset;
  @NonNull @Builder.Default
  private TimeCodeRounding rounding = DEFAULT_ROUNDING;

  private boolean timeCodeLeniency;
  private boolean isrcLeniency;
  private boolean fileLeniency;

  public CueReadOptions(Charset charset) {
    this(charset, DEFAULT_ROUNDING);
  }

  public CueReadOptions(Charset charset, TimeCodeRounding rounding) {
    this.charset = charset;
    this.rounding = rounding;
  }
}
