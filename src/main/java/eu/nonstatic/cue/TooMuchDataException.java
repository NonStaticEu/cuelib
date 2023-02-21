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
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TooMuchDataException extends RuntimeException {

  private final long maxSize;
  private final long actualSize;

  public Duration getMaxDuration() {
    return SizeAndDuration.getDurationForCompactDiscBytes(maxSize);
  }

  @Override
  public String getMessage() {
    return "Max duration: " + getMaxDuration() + ", max size: " + maxSize + " > Actual size: " + actualSize;
  }
}
