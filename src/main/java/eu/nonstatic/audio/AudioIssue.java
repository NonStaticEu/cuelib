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

import java.io.Serializable;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class AudioIssue implements Serializable {

  private final Type type;
  private final long location;
  private final long skipped; // skipped before sync recovery


  public AudioIssue(Type type, long location) {
    this(type, location, 0);
  }

  public AudioIssue(@NonNull Type type, long location, long skipped) {
    this.type = type;
    this.location = location;
    this.skipped = skipped;
  }

  @Override
  public String toString() {
    String message = null;
    if(type == Type.SYNC) {
      message = "Resynchro at " + location + ", skipped " + skipped;
    } else if(type == Type.EOF) {
      message = "EOF at " + location;
    }

    return getClass().getSimpleName() + ' ' + message;
  }

  public enum Type {
    SYNC, EOF
  }
}
