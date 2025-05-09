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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CueWriteOptionsTest {

    @Test
    void should_create_defaults() {
        CueWriteOptions defaults = CueWriteOptions.defaults();
        assertFalse(defaults.isOverwrite());
        assertFalse(defaults.isNoTrackAllowed());
        assertFalse(defaults.isFullPaths());
        assertTrue(defaults.isOrderedTimeCodes());
        assertNull(defaults.getMinTrackDuration());
        assertEquals(SizeAndDuration.getCompactDiscBytesFrom(CueDisc.DURATION_80_MIN, null), defaults.getBurningLimit());
    }
}
