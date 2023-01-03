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
 * Mostly copied from https://www.gnu.org/software/ccd2cue/manual/html_node/FILE-_0028CUE-Command_0029.html#FILE-_0028CUE-Command_0029
 */
public final class FileType {
  public static final String BINARY = "BINARY"; // raw little endian
  public static final String MOTOROLA = "MOTROLA"; // raw big endian
  public static final String AIFF = "AIFF";
  public static final String WAVE = "WAVE";
  public static final String MP3 = "MP3";
  public static final String FLAC = "FLAC"; // other code I read doesn't list it, but I don't see why not

  private FileType() {}
}
