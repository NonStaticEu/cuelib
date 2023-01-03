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

import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class IndexNotFoundException extends RuntimeException {

  private final int[] indexes;

  public IndexNotFoundException(int... indexes) {
    this("No index " + Arrays.stream(indexes).mapToObj(Integer::toString).collect(Collectors.joining(",")) + " on track", indexes);
  }

  public IndexNotFoundException(String message, int... indexes) {
    super(message);
    this.indexes = indexes;
  }
}
