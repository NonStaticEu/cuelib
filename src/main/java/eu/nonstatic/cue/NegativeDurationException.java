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

import java.time.Duration;
import lombok.Getter;

@Getter
public class NegativeDurationException extends RuntimeException {

  private final TimeCode timeCode1;
  private final TimeCode timeCode2;

  public NegativeDurationException(TimeCode timeCode1, TimeCode timeCode2) {
    this("Difference between this track and other track is negative: " + timeCode1 + " > " + timeCode2, timeCode1, timeCode2);
  }

  public NegativeDurationException(TimeCode timeCode, Duration fileDuration) {
    this("Difference between this track and file duration is negative: " + timeCode + " > " + fileDuration, timeCode, new TimeCode(fileDuration, TimeCode.DEFAULT_ROUNDING));
  }

  private NegativeDurationException(String message, TimeCode timeCode1, TimeCode timeCode2) {
    super(message);
    this.timeCode1 = timeCode1;
    this.timeCode2 = timeCode2;
  }
}
