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

import lombok.Getter;

public enum CueFlag {

  DIGITAL_COPY_PERMITTED("DCP"), // Digital copy permitted.
  FOUR_CHANNEL_AUDIO("4CH"), // Four channel audio.
  PRE_EMPHASIS_ENABLED("PRE"), // Pre-emphasis enabled (audio tracks only).
  SERIAL_COPY_MANAGEMENT_SYSTEM("SCMS") // Serial Copy Management System (not supported by all recorders).
  // "DATA" is a flag that's only relevant in data tracks and needn't be specified
  ;

  @Getter
  private final String flag;

  CueFlag(String flag) {
    this.flag = flag;
  }


  public static CueFlag flagOf(String flag) {
    for (CueFlag value : values()) {
      if (value.flag.equals(flag)) {
        return value;
      }
    }
    throw new IllegalArgumentException("No enum constant " + CueFlag.class.getCanonicalName() + "(\"" + flag + "\")");
  }
}
