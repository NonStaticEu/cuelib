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

@AllArgsConstructor
public class CueHiddenTrack {

  private FileAndTrack fileAndTrack;
  @Getter
  private Duration duration; //from index 0 till index 1 on track 1

  public FileReference getFileAndFormat() {
    return fileAndTrack.fileReference;
  }

  public CueTrack getTrack() {
    return fileAndTrack.track;
  }

  public CueIndex getPreGapIndex() {
    return getTrack().getPreGapIndex();
  }

  public CueIndex getStartIndex() {
    return getTrack().getStartIndex();
  }
}
