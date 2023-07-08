/**
 * Cuelib
 * Copyright (C) 2022 NonStatic
 *
 * This file is part of cuelib.
 * cuelib is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with . If not, see <https://www.gnu.org/licenses/>.
 */
package eu.nonstatic.audio;

import java.util.Collections;
import java.util.List;
import lombok.Getter;

public class AudioInfoException extends Exception {

  @Getter
  private final String name;
  private final List<AudioIssue> issues;

  public AudioInfoException(String name, Throwable cause) {
    this(name, List.of(), cause);
  }

  public AudioInfoException(String name, List<AudioIssue> issues, Throwable cause) {
    super(cause);
    this.name = name;
    this.issues = issues;
  }

  public List<AudioIssue> getIssues() {
    return Collections.unmodifiableList(issues);
  }

  @Override
  public String getMessage() {
    return name + ": " + super.getMessage();
  }
}
