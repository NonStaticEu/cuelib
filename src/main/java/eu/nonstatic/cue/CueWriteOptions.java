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
public final class CueWriteOptions {

  /**
   * Overwrites the existing target file if it exists
   */
  private final boolean overwrite;

  /**
   * Allows to have a cuesheet written with no track. But one can still have files with zero tracks.
   */
  private final boolean noTrackAllowed;

  /**
   * Should we write the complete file path or just its name
   */
  private final boolean fullPaths;

  /**
   * In theory it should be 4 seconds, that is 2 seconds of theoretical pregap + 2 seconds of audio
   */
  private final Duration minTrackDuration;

  /**
   * If set to false and your cuesheet actually ends up having tracks with a negative time,
   * - a player might decide to play such a track until the end of the file, or not play it at all,
   * - a burning software might just halt, or end up burning a zero duration track.
   */
  private final boolean orderedTimeCodes;

  /**
   * Max CD size in bytes
   * Is like orderedTimeCodes + check the overall size/duration is < limit
   * if burningCompliant is set, then burningLimit must be defined.
   */
  private final Long burningLimit;


  private CueWriteOptions(boolean overwrite, boolean noTrackAllowed, boolean fullPaths, Duration minTrackDuration, boolean orderedTimeCodes, Long burningLimit) {
    this.overwrite = overwrite;
    this.noTrackAllowed = noTrackAllowed;
    this.fullPaths = fullPaths;
    this.minTrackDuration = minTrackDuration;
    this.orderedTimeCodes = orderedTimeCodes;
    this.burningLimit = burningLimit;
  }

  /**
   * @return a new CueSheetOptions with overwrite=true, orderedTimeCodes=true, allowNoTrack=false
   */
  public static CueWriteOptions defaults() {
    return CueWriteOptions.builder()
        .overwrite(false)
        .noTrackAllowed(false)
        .fullPaths(false)
        .burningCompliant(CueDisc.DURATION_80_MIN) // comprises minTrackDuration
        .build();
  }

  public static Builder builder() {
    return new Builder();
  }



  public static class Builder {
    private boolean overwrite;
    private boolean noTrackAllowed;
    private boolean fullPaths;
    private Duration minTrackDuration;
    private boolean orderedTimeCodes;
    private Long burningLimit;


    public Builder overwrite(boolean overwrite) {
      this.overwrite = overwrite;
      return this;
    }

    public Builder noTrackAllowed(boolean noTrackAllowed) {
      this.noTrackAllowed = noTrackAllowed;
      return this;
    }

    public Builder fullPaths(boolean fullPaths) {
      this.fullPaths = fullPaths;
      return this;
    }

    public Builder minTrackDuration(Duration minTrackDuration) {
      this.minTrackDuration = minTrackDuration;
      return this;
    }

    public Builder orderedTimeCodes(boolean orderedTimeCodes) {
      this.orderedTimeCodes = orderedTimeCodes;
      return this;
    }

    public Builder burningLimit(Duration burningLimit) {
      Long maxSize = (burningLimit != null) ? SizeAndDuration.getCompactDiscBytesFrom(burningLimit, TimeCodeRounding.DOWN) : null;
      return burningLimit(maxSize);
    }

    public Builder burningLimit(Long burningLimit) {
      this.burningLimit = burningLimit;
      return this;
    }

    /**
     * orderedTimeCodes + burningLimit
     * not keeping minTrackDuration in 202x
     */
    public Builder burningCompliant(Duration burningLimit) {
      return orderedTimeCodes(true)
            .burningLimit(burningLimit);
    }

    public CueWriteOptions build() {
      return new CueWriteOptions(overwrite, noTrackAllowed, fullPaths, minTrackDuration, orderedTimeCodes, burningLimit);
    }
  }
}
