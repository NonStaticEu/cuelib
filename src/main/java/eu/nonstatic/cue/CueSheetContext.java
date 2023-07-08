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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
class CueSheetContext {

  private final String path;
  private final Path parent;
  private final String name;

  private final CueOptions options;
  private final List<String> errors = new ArrayList<>();

  public CueSheetContext(String name, CueOptions options) {
    this(name, null, name, options);
  }

  public CueSheetContext(Path cueFile, CueOptions options) {
    this(cueFile.toString(), cueFile.getParent(), cueFile.getFileName().toString(), options);
  }

  public CueSheetContext(File cueFile, CueOptions options) {
    this(cueFile.toPath(), options);
  }

  private CueSheetContext(String path, Path parent, String name, CueOptions options) {
    this.path = path;
    this.parent = parent;
    this.name = name;
    this.options = options;
  }

  public boolean isErrors() {
    return !errors.isEmpty();
  }

  public List<String> getErrors() {
    return Collections.unmodifiableList(errors);
  }

  public void addError(String message) {
    errors.add(message);
  }

  public void addError(String format, Object... args) {
    addError(String.format(format, args));
  }
}
