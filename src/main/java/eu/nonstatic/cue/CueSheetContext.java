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
import lombok.NonNull;

@Getter
public class CueSheetContext {

  private final String path;
  private final Path parent;
  private final String name;

  private final CueOptions options;
  private final List<CueSheetIssue> issues = new ArrayList<>();

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

  public boolean isIssues() {
    return !issues.isEmpty();
  }

  public List<CueSheetIssue> getIssues() {
    return Collections.unmodifiableList(issues);
  }

  public void addIssue(String format, Object... args) {
    addIssue(new CueSheetIssue(String.format(format, args)));
  }

  public void addIssue(Throwable throwable) {
    addIssue(new CueSheetIssue(throwable));
  }

  public void addIssue(@NonNull CueSheetIssue issue) {
    issues.add(issue);
  }
}
