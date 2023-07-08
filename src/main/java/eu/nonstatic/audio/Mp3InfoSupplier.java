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

import static java.util.Map.entry;

import eu.nonstatic.audio.AudioIssue.Type;
import eu.nonstatic.audio.Mp3InfoSupplier.Mp3Info;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Mp3InfoSupplier implements AudioInfoSupplier<Mp3Info> {

  private static final int MP3_VERSION_2_5 = 0;
  private static final int MP3_VERSION_2 = 2;
  private static final int MP3_VERSION_1 = 3;

  private static final int MP3_LAYER_I = 3;
  private static final int MP3_LAYER_II = 2;
  private static final int MP3_LAYER_III = 1;

  private static final int MODE_STEREO = 0;
  private static final int MODE_JOINT_STEREO = 1;
  private static final int MODE_DUAL_CHANNEL = 2;
  private static final int MODE_MONO = 3;

  // pretty much the same as https://bitbucket.org/ijabz/jaudiotagger/src/master/src/org/jaudiotagger/audio/mp3/MPEGFrameHeader.java
  private static final Map<Integer, Integer> MP3_BIT_RATE_MAP = Map.ofEntries(
      // MPEG-1, Layer I (E)
      entry(0x1E, 32),
      entry(0x2E, 64),
      entry(0x3E, 96),
      entry(0x4E, 128),
      entry(0x5E, 160),
      entry(0x6E, 192),
      entry(0x7E, 224),
      entry(0x8E, 256),
      entry(0x9E, 288),
      entry(0xAE, 320),
      entry(0xBE, 352),
      entry(0xCE, 384),
      entry(0xDE, 416),
      entry(0xEE, 448),
      // MPEG-1, Layer II (C)
      entry(0x1C, 32),
      entry(0x2C, 48),
      entry(0x3C, 56),
      entry(0x4C, 64),
      entry(0x5C, 80),
      entry(0x6C, 96),
      entry(0x7C, 112),
      entry(0x8C, 128),
      entry(0x9C, 160),
      entry(0xAC, 192),
      entry(0xBC, 224),
      entry(0xCC, 256),
      entry(0xDC, 320),
      entry(0xEC, 384),
      // MPEG-1, Layer III (A)
      entry(0x1A, 32),
      entry(0x2A, 40),
      entry(0x3A, 48),
      entry(0x4A, 56),
      entry(0x5A, 64),
      entry(0x6A, 80),
      entry(0x7A, 96),
      entry(0x8A, 112),
      entry(0x9A, 128),
      entry(0xAA, 160),
      entry(0xBA, 192),
      entry(0xCA, 224),
      entry(0xDA, 256),
      entry(0xEA, 320),
      // MPEG-2, Layer I (6)
      entry(0x16, 32),
      entry(0x26, 48),
      entry(0x36, 56),
      entry(0x46, 64),
      entry(0x56, 80),
      entry(0x66, 96),
      entry(0x76, 112),
      entry(0x86, 128),
      entry(0x96, 144),
      entry(0xA6, 160),
      entry(0xB6, 176),
      entry(0xC6, 192),
      entry(0xD6, 224),
      entry(0xE6, 256),
      // MPEG-2, Layer II (4)
      entry(0x14, 8),
      entry(0x24, 16),
      entry(0x34, 24),
      entry(0x44, 32),
      entry(0x54, 40),
      entry(0x64, 48),
      entry(0x74, 56),
      entry(0x84, 64),
      entry(0x94, 80),
      entry(0xA4, 96),
      entry(0xB4, 112),
      entry(0xC4, 128),
      entry(0xD4, 144),
      entry(0xE4, 160),
      // MPEG-2, Layer III (2)
      entry(0x12, 8),
      entry(0x22, 16),
      entry(0x32, 24),
      entry(0x42, 32),
      entry(0x52, 40),
      entry(0x62, 48),
      entry(0x72, 56),
      entry(0x82, 64),
      entry(0x92, 80),
      entry(0xA2, 96),
      entry(0xB2, 112),
      entry(0xC2, 128),
      entry(0xD2, 144),
      entry(0xE2, 160)
  );

  private static final Map<Integer, Integer> MP3_SAMPLING_V1_MAP = Map.of(
      0, 44100,
      1, 48000,
      2, 32000
  );
  private static final Map<Integer, Integer> MP3_SAMPLING_V2_MAP = Map.of(
      0, 22050,
      1, 24000,
      2, 16000
  );
  private static final Map<Integer, Integer> MP3_SAMPLING_V25_MAP = Map.of(
      0, 11025,
      1, 12000,
      2, 8000
  );

  private static final Map<Integer, Map<Integer, Integer>> MP3_SAMPLING_RATE_MAP = Map.of(
      MP3_VERSION_1, MP3_SAMPLING_V1_MAP,
      MP3_VERSION_2, MP3_SAMPLING_V2_MAP,
      MP3_VERSION_2_5, MP3_SAMPLING_V25_MAP
  );

  private static final Map<Integer, Integer> MP3_MODE_CHANNEL_MAP = Map.of(
      MODE_STEREO, 2,
      MODE_JOINT_STEREO, 2,
      MODE_DUAL_CHANNEL, 2,
      MODE_MONO, 1
  );

  private static final int LAYER_I_SAMPLES_PER_FRAME = 384;
  private static final int LAYER_II_OR_III_SAMPLES_PER_FRAME = 1152;


  /**
   * https://mutagen-specs.readthedocs.io/en/latest/id3/id3v2.4.0-structure.html
   * http://www.datavoyage.com/mpgscript/mpeghdr.htm
   * https://phoxis.org/2010/05/08/synch-safe/
   * Lyrics custom tag: found some intel in the code: https://sourceforge.net/projects/mp3diags/
   * Example: LYRICSBEGININD0000200ETT00040Jam & Spoon - Tripomatic Fairytales 2002CRC0000835FAE1F2000085LYRICS200
   * https://en.wikipedia.org/wiki/APE_tag
   * We're assuming there is no weird sync/alignment issue
   */
  public Mp3Info getInfos(InputStream is, String name) throws AudioInfoException {
    Mp3Info infos = new Mp3Info();
    AudioInputStream ais = null;
    try {
      ais = new AudioInputStream(is, name);
      findData(ais);
      skipID3v2(ais);

      while(!readFramesWithResync(ais, infos));

      if (infos.isEmpty()) {
        throw new IllegalArgumentException("Could not find a single MP3 frame: " + name);
      }
    } catch (EOFException e) {
      log.warn("End of file reached, incomplete frame: {}", name);
      infos.incomplete = true;
      infos.addIssue(new AudioIssue(Type.EOF, ais.location()));
    } catch (IOException e) {
      throw new AudioInfoException(name, infos.getIssues(), e);
    }
    return infos;
  }

  private void findData(AudioInputStream ais) throws IOException {
    do {
      ais.mark(1);
    } while(ais.read() == 0x00);
    ais.reset();
  }


  private void skipID3v2(AudioInputStream ais) throws IOException {
    ais.mark(3);
    String tag2 = ais.readString(3);
    if ("ID3".equals(tag2)) {
      int minorVersion = ais.read();
      int revVersion = ais.read();
      int flags = ais.read();
      // byte length of the extended header, the padding and the frames after desynchronisation.
      // If a footer is present this equals to (‘total size’ - 20) bytes, otherwise (‘total size’ - 10) bytes.
      int size = read32bitSynchSafe(ais);
      boolean extended = (flags & 0x2) != 0;
      boolean footer = (flags & 0x8) != 0;
      ais.skipNBytesBeforeJava12((long)size + (footer ? 10 : 0));
    } else { // no ID3v2
      ais.reset();
    }
  }

  private boolean readFramesWithResync(AudioInputStream ais, Mp3Info details) throws IOException {
    boolean stop = false;

    try {
      readFrames(ais, details);

      if (isEndOfFileAhead(ais)) {
        stop = true;
      } else {
        long locationBeforeResync = ais.location();
        int skipped = resync(ais);
        if(skipped >= 0) {
          details.addIssue(new AudioIssue(Type.SYNC, locationBeforeResync, skipped));
          log.info("Managed to resync after skipping {} bytes", skipped);
        } else {
          stop = true;
        }
      }
    } catch(BufferUnderflowException e) {
      stop = true;
    }

    return stop;
  }

  private void readFrames(AudioInputStream ais, Mp3Info details) throws IOException {
    try {
      FrameDetails frameDetails;
      while ((frameDetails = readFrame(ais)) != null) {
        details.appendFrame(frameDetails);
      }
    } catch(MalformedFrameException e) {
      log.warn("Frame seems malformed, will try to find another one");
    }
  }

  /**
   * @param ais
   * @return the frame details or null if what's read looks like no frame
   * @throws MalformedFrameException
   * @throws IOException
   * @throws BufferUnderflowException read32bitBE uses readNBytes
   */
  private FrameDetails readFrame(AudioInputStream ais) throws MalformedFrameException, IOException, BufferUnderflowException {
    ais.mark(4);
    int header = ais.read32bitBE();
    if (!isMp3Frame(header)) {
      ais.reset();
      return null;
    }

    FrameDetails details = new FrameDetails();

    details.version = (header >> 19) & 0x3; // 00: MPEG Version 2.5, 01: reserved, 10: MPEG Version 2 (ISO/IEC 13818-3), 11: MPEG Version 1 (ISO/IEC 11172-3)
    details.layer = (header >> 17) & 0x3; // 00: reserved, 01: Layer III, 10: Layer II, 11: Layer I
    boolean protection = ((header >> 16) & 0x1) == 0;
    int samplingIndex = ((header >> 10) & 0x3);
    int padding = ((header >> 9) & 0x1);
    int channelIndex = ((header >> 6) & 0x3); // 00: Stereo, 01: Joint stereo (Stereo), 10: Dual channel (Stereo), 11: Single channel (Mono)
    details.numChannels = Optional.ofNullable(MP3_MODE_CHANNEL_MAP.get(channelIndex))
        .orElseThrow(() -> new MalformedFrameException("Cannot compute channel number"));

    int bitRateKey = ((header >> 16) & 0x0E) | ((header >> 8) & 0xF0);
    Integer bitRate = MP3_BIT_RATE_MAP.get(bitRateKey);
    if (bitRate == null) { // free
      int bitRateIndex = ((header >> 12) & 0xF);
      throw new MalformedFrameException("Cannot handle bitrate for index " + Integer.toHexString(bitRateIndex));
    }
    details.bitRate = bitRate;

    Integer sampleRate = Optional.ofNullable(MP3_SAMPLING_RATE_MAP.get(details.version))
        .map(map -> map.get(samplingIndex))
        .orElseThrow(() -> new MalformedFrameException("Cannot compute sampling rate"));
    if(sampleRate == null) {
      throw new MalformedFrameException("Cannot handle sampling for index " + Integer.toHexString(samplingIndex));
    }
    details.sampleRate = sampleRate;

    int frameLength;
    switch (details.layer) {
      case MP3_LAYER_I:
        frameLength = ((12 * bitRate * 1000) / sampleRate + padding) * 4;
        details.sampleCount = LAYER_I_SAMPLES_PER_FRAME;
        break;
      case MP3_LAYER_II:
      case MP3_LAYER_III:
        frameLength = (144 * bitRate * 1000) / sampleRate + padding;
        details.sampleCount = LAYER_II_OR_III_SAMPLES_PER_FRAME;
        break;
      default:
        throw new MalformedFrameException("Layer 0x00");
    }

    //No, we're not going to retry and get as much data as possible in case of an EOF
    ais.skipNBytesBeforeJava12((long)frameLength - 4); // 4 is the header we've already read
    return details;
  }

  private int resync(AudioInputStream ais) throws IOException {
    for(int skipped = 0; ; skipped++) {
      ais.mark(4);
      int header = ais.read32bitBE();
      ais.reset();
      if (isMp3Frame(header)) {
        return skipped;
      } else {
        ais.skipNBytesBeforeJava12(1);
      }
    }
  }


  /**
   * Checks if the bits 21-31 are set
   */
  private static boolean isMp3Frame(int header) {
    return (header & 0xFFE00000) == 0xFFE00000;
  }

  private static boolean isEndOfFileAhead(AudioInputStream ais) throws IOException {
    if (ais.available() >= 8) {
      String tag = ais.readString(8);
      return tag.startsWith("TAG") // ID3v1 tag
          || tag.startsWith("LYRICSBE") // seems to be a LYRIGSBEGIN sequence with 3-char tags followed by 5-char sizes (in ascii!) and finishing in LYRICS200
          || tag.startsWith("APETAGEX");
    } else {
      return true;
    }
  }

  private int read32bitSynchSafe(AudioInputStream ais) throws IOException {
    return read32bitSynchSafeInt(ais.readNBytes(4));
  }

  private static int read32bitSynchSafeInt(byte[] bytes) {
    return (((bytes[0] << 7 | bytes[1]) << 7) | bytes[2]) << 7 | bytes[3];
  }

  protected static byte[] toSynchSafeBytes(int i) {
    byte b0 = (byte)(i >> 21);
    byte b1 = (byte)(i >> 14 & 0xff);
    byte b2 = (byte)(i >> 7 & 0xff);
    byte b3 = (byte)(i & 0xff);
    return new byte[]{b0, b1, b2, b3};
  }

  private static final class FrameDetails {
    private int version;
    private int layer;
    private int numChannels;
    private int bitRate;
    private int sampleRate;
    private int sampleCount;
  }

  public static final class Mp3Info implements AudioInfo {

    private final Map<Integer, Long> sampleCounts = new HashMap<>(); // samplingRate => samples
    private final List<AudioIssue> audioIssues = new ArrayList<>(); // location => bytes skipped
    private boolean incomplete;

    /**
     * Sync errors don't have any effect on this flag
     * @return true if the file unexpectedly reached EOF
     */
    public boolean isIncomplete() {
      return incomplete;
    }

    void appendFrame(FrameDetails details) {
      sampleCounts.compute(details.sampleRate, (sampleRate, sampleCount) -> (sampleCount == null ? 0 : sampleCount) + details.sampleCount);
    }

    public boolean isEmpty() {
      return sampleCounts.isEmpty();
    }

    @Override
    public Duration getDuration() {
      double duration = 0.0;
      for (Entry<Integer, Long> entry : sampleCounts.entrySet()) {
        duration += entry.getValue() / (double) entry.getKey();
      }
      return Duration.ofNanos(Math.round(duration * 1_000_000_000.0));
    }

    public List<AudioIssue> getIssues() {
      return Collections.unmodifiableList(audioIssues);
    }

    public void addIssue(@NonNull AudioIssue issue) {
      audioIssues.add(issue);
    }
  }

  public static final class MalformedFrameException extends Exception {
    public MalformedFrameException(String message) {
      super(message);
    }
  }
}
