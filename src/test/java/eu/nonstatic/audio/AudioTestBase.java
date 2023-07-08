/**
 * Cuelib
 * Copyright (C) 2022 NonStatic
 *
 * This file is part of cuelib.
 * cuelib is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with . If not, see <https://www.gnu.org/licenses/>.
 */
package eu.nonstatic.audio;

import java.net.URL;

/**
 * Those samples are taken from here:
 * https://en.wikipedia.org/wiki/Synthesizer
 * https://commons.wikimedia.org/wiki/File:Amplitudenmodulation.ogg
 */
public interface AudioTestBase {

  String AIFF_NAME = "/audio/Arpeggio.aiff";
  String WAVE_NAME = "/audio/Amplitudenmodulation.wav";
  String MP3_NAME = "/audio/Moog-juno-303-example.mp3";
  String FLAC_NAME = "/audio/Filtered_envelope_sawtooth_moog.flac";

  URL AIFF_URL = AudioTestBase.class.getResource(AIFF_NAME);
  URL WAVE_URL = AudioTestBase.class.getResource(WAVE_NAME);
  URL MP3_URL = AudioTestBase.class.getResource(MP3_NAME);
  URL FLAC_URL = AudioTestBase.class.getResource(FLAC_NAME);
}
