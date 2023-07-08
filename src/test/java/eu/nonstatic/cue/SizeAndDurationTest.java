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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class SizeAndDurationTest {

    @Test
    void should_build_for_size() {
        SizeAndDuration sd = new SizeAndDuration(123L);
        assertEquals(123L, sd.size);
        assertNull(sd.duration);
    }

    @Test
    void should_build_for_duration() {
        SizeAndDuration sd = new SizeAndDuration(Duration.ofMinutes(2), TimeCodeRounding.DOWN);
        assertEquals(2*60*44100*4, sd.size);
        assertEquals(Duration.ofMinutes(2), sd.duration);
    }
}
