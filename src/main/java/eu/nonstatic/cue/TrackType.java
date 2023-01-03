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

/**
 * Copied from https://www.gnu.org/software/ccd2cue/manual/html_node/MODE-_0028Compact-Disc-fields_0029.html#MODE-_0028Compact-Disc-fields_0029
 * The modes marked with ‘*’ are not defined in the original CUE sheet format specification.
 */
public class TrackType {

  public static final String AUDIO = "AUDIO";	// Audio/Music (2352 — 588 samples)
  public static final String CDG = "CDG";	// Karaoke CD+G (2448)
  public static final String MODE1_2048 = "MODE1/2048";	// CD-ROM Mode 1 Data (cooked)
  public static final String MODE1_2352 = "MODE1/2352";	// CD-ROM Mode 1 Data (raw)
  public static final String MODE2_2048 = "MODE2/2048";	// CD-ROM XA Mode 2 Data (form 1) *
  public static final String MODE2_2324 = "MODE2/2324";	// CD-ROM XA Mode 2 Data (form 2) *
  public static final String MODE2_2336 = "MODE2/2336";	// CD-ROM XA Mode 2 Data (form mix)
  public static final String MODE2_2352 = "MODE2/2352";	// CD-ROM XA Mode 2 Data (raw)
  public static final String CDI_2336 = "CDI/2336";	// CDI Mode 2 Data
  public static final String CDI_2352 = "CDI/2352";	// CDI Mode 2 Data

  private TrackType() {}
}
