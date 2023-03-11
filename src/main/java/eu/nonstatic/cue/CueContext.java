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
import java.nio.file.Path;
import lombok.Getter;
import lombok.Setter;

@Getter
public class CueContext {
  public static final TimeCodeRounding DEFAULT_ROUNDING = TimeCodeRounding.DOWN;

  private final String path;
  private final Path parent;
  private final String name;

  private final Charset charset;

  @Setter
  private TimeCodeRounding rounding;
  @Setter
  private boolean timeCodeLeniency;
  @Setter
  private boolean isrcLeniency;


  public CueContext(String name, Charset charset) {
    this(name, null, name, charset, DEFAULT_ROUNDING);
  }

  public CueContext(Path cueFile, Charset charset) {
    this(cueFile.toString(), cueFile.getParent(), cueFile.getFileName().toString(), charset, DEFAULT_ROUNDING);
  }

  private CueContext(String path, Path parent, String name, Charset charset, TimeCodeRounding rounding) {
    this.path = path;
    this.parent = parent;
    this.name = name;
    this.charset = charset;
    this.rounding = rounding;
  }
}
