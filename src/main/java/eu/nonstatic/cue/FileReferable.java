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

import java.nio.file.Paths;

public interface FileReferable {

    String getFile();

    default String getFileName() {
        return Paths.get(getFile())
                .getFileName()
                .toString();
    }

    FileType getType();

    default boolean isData() {
        FileType type = getType();
        return type != null && type.isData();
    }

    default boolean isAudio() {
        FileType type = getType();
        return type != null && type.isAudio();
    }


    default boolean isSizeAndDurationSet() {
        return getSizeDuration() != null;
    }
    SizeAndDuration getSizeDuration();
    void setSizeAndDuration(SizeAndDuration sizeAndDuration);
}
