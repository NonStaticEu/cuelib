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

import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static eu.nonstatic.audio.AudioTestBase.FLAC_URL;
import static eu.nonstatic.cue.CueTestBase.copyFileContents;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Test
    void should_copy() {
        SizeAndDuration sd = new SizeAndDuration(Duration.ofMillis(123456L), TimeCodeRounding.DOWN);
        SizeAndDuration sdCOpy = new SizeAndDuration(sd);
        assertEquals(21777168L, sd.size);
        assertEquals(Duration.ofMillis(123456L), sd.duration);
    }

    @Test
    void should_get_from_audio_file() throws IOException {
        Path audioFile = Files.createTempFile("my file", ".flac");
        copyFileContents(FLAC_URL, audioFile);
        SizeAndDuration sd = new SizeAndDuration(audioFile.toFile(), TimeCodeRounding.DOWN);
        Files.delete(audioFile);

        assertEquals(649152L, sd.size);
        assertEquals(Duration.ofMillis(3692L), sd.duration);
    }

    @Test
    void should_get_from_audio_path() throws IOException {
        Path audioFile = Files.createTempFile("my file", ".flac");
        copyFileContents(FLAC_URL, audioFile);
        SizeAndDuration sd = new SizeAndDuration(audioFile, TimeCodeRounding.DOWN);
        Files.delete(audioFile);

        assertEquals(649152L, sd.size);
        assertEquals(Duration.ofMillis(3692L), sd.duration);
    }

    @Test
    void should_get_from_binary_path() throws IOException {
        Path binFile = Files.createTempFile("my file", ".bin");
        try(BufferedWriter bw = Files.newBufferedWriter(binFile)) {
            bw.append("You're never gonna figure out who's on first base, because Who is on first base.");
        }
        SizeAndDuration sd = new SizeAndDuration(binFile, TimeCodeRounding.DOWN);
        Files.delete(binFile);

        assertEquals(80L, sd.size);
        assertNull(sd.duration);
    }

    @Test
    void should_force_binary() throws IOException {
        Path audioFile = Files.createTempFile("my file", ".flac");
        copyFileContents(FLAC_URL, audioFile);
        SizeAndDuration sd = new SizeAndDuration(audioFile, TimeCodeRounding.DOWN, true);
        Files.delete(audioFile);

        assertEquals(484667L, sd.size);
        assertNull(sd.duration);
    }
}
