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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CueSheetOptions {

  private boolean overwrite;
  private boolean orderedTimeCodes;
  private boolean noTrackAllowed;


  public CueSheetOptions setOverwrite(boolean overwrite) {
    this.overwrite = overwrite;
    return this;
  }

  public CueSheetOptions setOrderedTimeCodes(boolean orderedTimeCodes) {
    this.orderedTimeCodes = orderedTimeCodes;
    return this;
  }

  public CueSheetOptions setNoTrackAllowed(boolean noTrackAllowed) {
    this.noTrackAllowed = noTrackAllowed;
    return this;
  }

  /**
   * @return a new CueSheetOptions with overwrite=true, orderedTimeCodes=true, allowNoTrack=false
   */
  public static CueSheetOptions defaults() {
    CueSheetOptions options = new CueSheetOptions();
    options.setOverwrite(false);
    options.setOrderedTimeCodes(true);
    options.setNoTrackAllowed(false);
    return options;
  }
}
