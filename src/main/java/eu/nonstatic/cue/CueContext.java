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

@Getter
public class CueContext {

  private String path;
  private String parent;
  private String name;

  private Charset charset;

  public CueContext(String name, Charset charset) {
    this(name, null, name, charset);
  }

  public CueContext(Path cueFile, Charset charset) {
    this(cueFile.toString(), cueFile.getParent().toString(), cueFile.getFileName().toString(), charset);
  }

  private CueContext(String path, String parent, String name, Charset charset) {
    this.path = path;
    this.parent = parent;
    this.name = name;
    this.charset = charset;
  }
}
