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

import static eu.nonstatic.cue.CueDisc.DURATION_LEAD_IN;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.nonstatic.cue.FileType.Audio;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

class CueSheetWriterTest extends CueTestBase {

  static URL myTestUrlExpected = CueDiscTest.class.getResource("/My Test expected.cue"); // UTF-8 encoded

  @Test
  void should_not_write_cuesheet() {
    Path anyPath = Paths.get("/tmp/whatever");
    CueWriteOptions options = CueWriteOptions.builder().build();
    assertThrows(NullPointerException.class, () -> CueSheetWriter.writeCueSheet(null, anyPath, options));
    CueDisc anyDisc = new CueDisc();
    assertThrows(NullPointerException.class, () -> CueSheetWriter.writeCueSheet(anyDisc, (Path)null, options));
  }

  @Test
  void should_write_cuesheet_to_file() throws IOException {
    Charset cs = StandardCharsets.UTF_8;
    CueDisc disc = new CueSheetReader().readCueSheet(myTestUrl, cs).getDisc();

    File tempFile = File.createTempFile("cue", null);
    tempFile.delete();

    CueSheetWriter.writeCueSheet(disc, tempFile, CueWriteOptions.builder().build());
    assertFileContents(myTestUrlExpected, tempFile, cs);
    tempFile.delete();
  }

  @Test
  void should_write_cuesheet_to_path() throws IOException {
    Charset cs = StandardCharsets.UTF_8;
    CueDisc disc = new CueSheetReader().readCueSheet(myTestUrl, cs).getDisc();

    Path tempFile = Files.createTempFile(null, null);
    Files.deleteIfExists(tempFile);

    CueSheetWriter.writeCueSheet(disc, tempFile, CueWriteOptions.builder().build());
    assertFileContents(myTestUrlExpected, tempFile.toFile(), cs);
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_write_cuesheet_to_file_other_charset() throws IOException {
    CueSheetReader cueSheetReader = new CueSheetReader();

    Charset inputCharset = StandardCharsets.UTF_8, outputCharset = StandardCharsets.ISO_8859_1;
    CueDisc disc = cueSheetReader.readCueSheet(myTestUrl, inputCharset).getDisc();

    disc.setCharset(outputCharset);
    File tempFile = File.createTempFile("cue", null);
    CueSheetWriter.writeCueSheet(disc, tempFile, CueWriteOptions.builder().overwrite(true).build());

    assertEquals(outputCharset, cueSheetReader.detectEncoding(tempFile));
    assertFileContents(myTestUrlExpected, tempFile, outputCharset);
    tempFile.delete();
  }

  @Test
  void should_write_cuesheet_to_file_overwrite() throws IOException {
    Charset cs = StandardCharsets.UTF_8;
    CueDisc disc = new CueSheetReader().readCueSheet(myTestUrl, cs).getDisc();
    File tempFile = File.createTempFile("cue", null);
    CueSheetWriter.writeCueSheet(disc, tempFile, CueWriteOptions.builder().overwrite(true).build());
    assertFileContents(myTestUrlExpected, tempFile, cs);
    tempFile.delete();
  }

  @Test
  void should_not_write_cuesheet_to_existing_file() throws IOException {
    Charset cs = StandardCharsets.UTF_8;

    CueDisc disc = new CueSheetReader().readCueSheet(myTestUrl, cs).getDisc();
    File tempFile = File.createTempFile("cue", null);

    CueWriteOptions options = CueWriteOptions.builder().overwrite(false).build();
    assertThrows(FileAlreadyExistsException.class, () -> CueSheetWriter.writeCueSheet(disc, tempFile, options));
    assertThrows(FileAlreadyExistsException.class, () -> CueSheetWriter.writeCueSheet(disc, tempFile.toPath(), options));
    tempFile.delete();
  }


  private static void assertFileContents(URL expectedFileUrl, File actualFile, Charset cs) throws IOException {
    List<String> writtenlines = Files.readAllLines(actualFile.toPath(), cs);

    List<String> expectedLines = readLines(expectedFileUrl, StandardCharsets.UTF_8);
    assertEquals(expectedLines.size(), writtenlines.size());
    Iterator<String> xit = expectedLines.iterator(), wit = writtenlines.iterator();
    while(xit.hasNext()) {
      assertEquals(xit.next(), wit.next());
    }
  }

  @Test
  void should_not_write_track_number_overflow_when__first_track_not_1() throws IOException {
    CueDisc disc = new CueDisc();
    CueFile file1 = disc.addFile(new CueFile("path1", FileType.Audio.WAVE));
    file1.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.ZERO_SECOND), new CueIndex(TimeCode.ONE_SECOND)));
    file1.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_PRE_GAP, TimeCode.ZERO_SECOND), new CueIndex(TimeCode.TWO_SECONDS)));
    CueFile file2 = disc.addFile(new CueFile("path2", Audio.AIFF));
    file2.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.ZERO_SECOND)));
    file2.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.TWO_SECONDS)));

    disc.setFirstTrackNumber(97);

    CueWriteOptions options = CueWriteOptions.builder().overwrite(true).build();
    Path tempFile = Files.createTempFile(null, null);
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CueSheetWriter.writeCueSheet(disc, tempFile, options));
    assertEquals("Track max 100 is out of range [1,99]", ex.getSuppressed()[0].getMessage());
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_write_cuesheet_no_file() throws IOException {
    CueDisc disc = new CueDisc();
    disc.addRemark(CueRemark.commentOf("hello world"));
    CueWriteOptions options = CueWriteOptions.builder().noTrackAllowed(true).overwrite(true).build();

    Path tempFile = Files.createTempFile(null, null);
    CueSheetWriter.writeCueSheet(disc, tempFile, options);

    assertEquals(1, Files.readAllLines(tempFile).size());
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_not_write_cuesheet_no_file() throws IOException {
    CueDisc disc = new CueDisc();
    disc.addRemark(CueRemark.commentOf("hello world"));
    CueWriteOptions options = CueWriteOptions.builder().noTrackAllowed(false).overwrite(true).build();

    Path tempFile = Files.createTempFile(null, null);
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CueSheetWriter.writeCueSheet(disc, tempFile, options));
    assertEquals("Tracks count 0 is out of range [1,99]", ex.getSuppressed()[0].getMessage());
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_write_cuesheet_no_track() throws IOException {
    CueDisc disc = new CueDisc();
    disc.addRemark(CueRemark.commentOf("hello world"));
    disc.addFile(new CueFile("path", FileType.Audio.WAVE));

    Path tempFile = Files.createTempFile(null, null);
    CueWriteOptions options = CueWriteOptions.builder().noTrackAllowed(true).overwrite(true).build();
    CueSheetWriter.writeCueSheet(disc, tempFile, options);

    assertEquals(1, Files.readAllLines(tempFile).size());
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_not_write_cuesheet_no_track() throws IOException {
    CueDisc disc = new CueDisc();
    disc.addFile(new CueFile("path", FileType.Audio.WAVE));

    Path tempFile = Files.createTempFile(null, null);
    CueWriteOptions options = CueWriteOptions.builder().overwrite(true).build();
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CueSheetWriter.writeCueSheet(disc, tempFile, options));
    assertEquals("Tracks count 0 is out of range [1,99]", ex.getSuppressed()[0].getMessage());
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_not_write_cuesheet_track_too_short() throws IOException {
    CueDisc disc = new CueDisc();
    CueFile file1 = disc.addFile(new CueFile("path1", Audio.WAVE));
    file1.setSizeAndDuration(new SizeAndDuration(Duration.ofSeconds(2+1+4), TimeCodeRounding.DOWN));
    CueTrack track1 = file1.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_PRE_GAP, TimeCode.ZERO_SECOND), new CueIndex(TimeCode.ONE_SECOND))); // 2s long, should work, with the mandatory 2s lead-in on track 1
    CueTrack track2 = file1.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.TWO_SECONDS))); // 1s long + 2s pregap, not enough
    CueTrack track3 = file1.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_PRE_GAP, new TimeCode(0, 3, 0)), new CueIndex(new TimeCode(0, 4, 0)))); // 4 seconds long, is enough

    CueFile file2 = disc.addFile(new CueFile("path2", Audio.MP3));
    file2.setSizeAndDuration(new SizeAndDuration(Duration.ofSeconds(1), TimeCodeRounding.DOWN));
    CueTrack track4 = file2.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.ZERO_SECOND))); // 1s+1s is too short
    track4.setPreGap(TimeCode.ONE_SECOND);

    Path tempFile = Files.createTempFile(null, null);
    CueWriteOptions options = CueWriteOptions.builder().overwrite(true).minTrackDuration(CueTrack.DURATION_MIN).build();
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CueSheetWriter.writeCueSheet(disc, tempFile, options));
    assertEquals(2, ex.getSuppressed().length);
    assertEquals("Track 2 duration PT1S is below PT4S", ex.getSuppressed()[0].getMessage());
    assertEquals("Track 4 duration PT2S is below PT4S", ex.getSuppressed()[1].getMessage());
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_not_write_cuesheet_when_two_pregaps() throws IOException {
    CueWriteOptions options = CueWriteOptions.builder().overwrite(true).build();

    Path tempFile = Files.createTempFile(null, null);

    CueDisc disc1 = new CueDisc();
    CueFile file1 = disc1.addFile(new CueFile("path1", FileType.Audio.WAVE));
    CueTrack track1 = file1.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_PRE_GAP, TimeCode.ZERO_SECOND), new CueIndex(TimeCode.ONE_SECOND)));
    track1.setPreGap(TimeCode.ONE_SECOND);

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CueSheetWriter.writeCueSheet(disc1, tempFile, options));
    assertEquals("Track 1 has both a pregap duration and a pregap index", ex.getSuppressed()[0].getMessage());
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_not_write_cuesheet_when_first_index_not_00_00_00() throws IOException {
    CueWriteOptions options = CueWriteOptions.builder().overwrite(true).build();
    Path tempFile = Files.createTempFile(null, null);

    CueDisc disc1 = new CueDisc();
    CueFile file1 = disc1.addFile(new CueFile("path1", FileType.Audio.WAVE));
    file1.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_PRE_GAP, TimeCode.ZERO_SECOND), new CueIndex(TimeCode.ONE_SECOND)));
    file1.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_PRE_GAP, TimeCode.ONE_SECOND), new CueIndex(TimeCode.TWO_SECONDS)));
    CueFile file2 = disc1.addFile(new CueFile("path2", Audio.AIFF));
    file2.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.TWO_SECONDS)));
    file2.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(new TimeCode(0, 5, 0))));
    CueFile file3 = disc1.addFile(new CueFile("path3", Audio.AIFF));
    file3.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_PRE_GAP, TimeCode.ONE_SECOND), new CueIndex(TimeCode.TWO_SECONDS)));

    assertEquals(5, disc1.getTrackCount());
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CueSheetWriter.writeCueSheet(disc1, tempFile, options));
    assertEquals(2, ex.getSuppressed().length);
    assertEquals("File path2 doesn't have 00:00:00 as its first track's first index", ex.getSuppressed()[0].getMessage());
    assertEquals("File path3 doesn't have 00:00:00 as its first track's first index", ex.getSuppressed()[1].getMessage());
  }

  @Test
  void should_not_write_cuesheet_when_more_than_99_tracks() throws IOException {
    CueWriteOptions options = CueWriteOptions.builder().overwrite(true).build();
    Path tempFile = Files.createTempFile(null, null);

    CueDisc disc = new CueDisc();
    CueFile file = disc.addFile(new CueFile("path1", FileType.Audio.WAVE));
    TimeCode timeCode = TimeCode.ZERO_SECOND;
    for (int i = 0; i < 100; i++) {
      file.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(timeCode)));
      timeCode = timeCode.plus(Duration.ofSeconds(55));
    }

    assertEquals(100, disc.getTrackCount());
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CueSheetWriter.writeCueSheet(disc, tempFile, options));
    assertEquals("Tracks count 100 is out of range [1,99]", ex.getSuppressed()[0].getMessage());
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_not_write_cuesheet_when_more_than_99_indexes() throws IOException {
    CueWriteOptions options = CueWriteOptions.builder().overwrite(true).build();
    Path tempFile = Files.createTempFile(null, null);

    CueDisc disc = new CueDisc();
    CueFile file = disc.addFile(new CueFile("path1", FileType.Audio.WAVE));
    CueTrack track = file.addTrack(new CueTrack(TrackType.AUDIO));

    TimeCode timeCode = TimeCode.ZERO_SECOND;
    for (int i = 1; i <= 100; i++) {
      CueIndex index = new CueIndex(timeCode);
      // very nasty to bypass all the checks
      index.number = i;
      track.addIndexUnsafe(index);
      timeCode = timeCode.plus(Duration.ofSeconds(10));
    }

    assertEquals(100, disc.getIndexCount());
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CueSheetWriter.writeCueSheet(disc, tempFile, options));
    assertEquals("Track 1 index count 100 is out of range [1,99]", ex.getSuppressed()[0].getMessage());
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_not_write_cuesheet_without_index_01() throws IOException {
    CueWriteOptions options = CueWriteOptions.builder().overwrite(true).build();
    Path tempFile = Files.createTempFile(null, null);

    CueDisc disc = new CueDisc();
    CueFile file = disc.addFile(new CueFile("path1", FileType.Audio.WAVE));
    CueTrack track1 = file.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_PRE_GAP, TimeCode.ZERO_SECOND)));

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CueSheetWriter.writeCueSheet(disc, tempFile, options));
    assertEquals("Track 1 doesn't have mandatory index 1", ex.getSuppressed()[0].getMessage());

    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_write_cuesheet_when_data_fits_no_gaps() throws IOException {
    Duration maxDuration = Duration.ofMinutes(74);
    CueWriteOptions options = CueWriteOptions.builder().overwrite(true).burningLimit(maxDuration).build();
    Path tempFile = Files.createTempFile(null, null);

    CueDisc disc = new CueDisc();
    CueFile file = disc.addFile(new CueFile("path1", FileType.Audio.WAVE));
    file.setSizeAndDuration(new SizeAndDuration(maxDuration.minus(DURATION_LEAD_IN), TimeCodeRounding.DOWN));
    CueTrack track1 = file.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.ZERO_SECOND)));
    track1.setPreGap(null, TimeCodeRounding.DOWN);  // coverage
    track1.setPostGap(null, TimeCodeRounding.DOWN); // coverage

    assertDoesNotThrow(() -> CueSheetWriter.writeCueSheet(disc, tempFile, options));
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_write_cuesheet_when_data_fits_even_with_gaps() throws IOException {
    Duration maxDuration = Duration.ofMinutes(74);
    CueWriteOptions options = CueWriteOptions.builder().overwrite(true).burningLimit(maxDuration).build();
    Path tempFile = Files.createTempFile(null, null);

    CueDisc disc = new CueDisc();
    CueFile file = disc.addFile(new CueFile("path1", FileType.Audio.WAVE));
    file.setSizeAndDuration(new SizeAndDuration(maxDuration.minusSeconds(5), TimeCodeRounding.DOWN)); // 2 mandatory + tk1 pregap 1s + tk2 pregap 2s
    CueTrack track1 = file.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.ZERO_SECOND)));
    track1.setPreGap(Duration.ofSeconds(1), TimeCodeRounding.DOWN);
    CueTrack track2 = file.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.ZERO_SECOND)));
    track2.setPostGap(Duration.ofSeconds(2), TimeCodeRounding.DOWN);

    assertFalse(track1.hasPreGapIndex());
    assertNotNull(track1.getPreGap());
    assertTrue(track1.hasPreGap());
    assertNull(track1.getPostGap());
    assertFalse(track1.hasPostGap());

    assertFalse(track2.hasPreGapIndex());
    assertNull(track2.getPreGap());
    assertFalse(track2.hasPreGap());
    assertNotNull(track2.getPostGap());
    assertTrue(track2.hasPostGap());

    assertDoesNotThrow(() -> CueSheetWriter.writeCueSheet(disc, tempFile, options));
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_not_write_cuesheet_when_data_fits_but_pre_post_gaps_make_it_exceed() throws IOException {
    Duration maxDuration = Duration.ofMinutes(74);
    CueWriteOptions options = CueWriteOptions.builder().overwrite(true).burningLimit(maxDuration).build();
    Path tempFile = Files.createTempFile(null, null);

    CueDisc disc = new CueDisc();
    CueFile file = disc.addFile(new CueFile("path1", FileType.Audio.WAVE));
    file.setSizeAndDuration(new SizeAndDuration(maxDuration, TimeCodeRounding.DOWN));
    CueTrack track1 = file.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.ZERO_SECOND)));
    track1.setPreGap(Duration.ofSeconds(1), TimeCodeRounding.DOWN);
    CueTrack track2 = file.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.ZERO_SECOND)));
    track2.setPostGap(Duration.ofSeconds(2), TimeCodeRounding.DOWN);

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CueSheetWriter.writeCueSheet(disc, tempFile, options));
    assertEquals("Max duration: PT1H14M, max size: 783216000 > Actual size: 784098000", ex.getSuppressed()[0].getMessage()); //74*60*44100*4 vs (74*60+3)*44100*4
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_not_write_cuesheet_when_too_much_data() throws IOException {
    CueWriteOptions options = CueWriteOptions.builder().overwrite(true).burningLimit(Duration.ofMinutes(74)).build();
    Path tempFile = Files.createTempFile(null, null);

    CueDisc disc = new CueDisc();
    CueFile file = disc.addFile(new CueFile("path1", FileType.Audio.WAVE));
    file.setSizeAndDuration(new SizeAndDuration(Duration.ofMinutes(74).plusMillis(14), TimeCodeRounding.CLOSEST));
    file.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.ZERO_SECOND)));

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CueSheetWriter.writeCueSheet(disc, tempFile, options));
    assertEquals("Max duration: PT1H14M, max size: 783216000 > Actual size: 783571152", ex.getSuppressed()[0].getMessage());
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_write_cuesheet_when_chaining() throws IOException {
    Path tempFile = Files.createTempFile(null, null);

    CueDisc disc = new CueDisc();
    CueFile file1 = disc.addFile(new CueFile("path1", FileType.Audio.WAVE));
    CueFile file2 = disc.addFile(new CueFile("path1", FileType.Audio.WAVE));
    CueTrack track1 = file1.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.ZERO_SECOND)));
    CueTrack track2 = file2.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.TWO_SECONDS)));
    CueTrack track3 = file2.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.ONE_SECOND)));

    // accepted when ignoring chaining
    assertEquals(3, disc.getTrackCount());
    CueWriteOptions optionsWithoutOrderedTimeCodes = CueWriteOptions.builder().orderedTimeCodes(false).overwrite(true).build();
    assertDoesNotThrow(() -> CueSheetWriter.writeCueSheet(disc, tempFile, optionsWithoutOrderedTimeCodes));

    // not accepted when enforcing chaining
    CueWriteOptions optionsWithOrderedTimeCodes = CueWriteOptions.builder().orderedTimeCodes(true).overwrite(true).build();
    IllegalStateException ex1 = assertThrows(IllegalStateException.class, () -> CueSheetWriter.writeCueSheet(disc, tempFile, optionsWithOrderedTimeCodes));
    assertEquals("Track 3 index 1 timecode 00:01:00 is before its predecessor 00:02:00", ex1.getSuppressed()[0].getMessage());


    // accepted as if f2-t2-i1 and f2-t3-i1 timecodes are ordered
    track2.getIndex(1).setTimeCode(TimeCode.ZERO_SECOND);
    track3.getIndex(1).setTimeCode(TimeCode.ONE_SECOND);
    assertDoesNotThrow(() -> CueSheetWriter.writeCueSheet(disc, tempFile, optionsWithOrderedTimeCodes));

    // not accepted any longer
    track2.addIndex(new CueIndex(TimeCode.TWO_SECONDS)); // so we have 0, 2, 1 seconds
    IllegalStateException ex2 = assertThrows(IllegalStateException.class, () -> CueSheetWriter.writeCueSheet(disc, tempFile, optionsWithOrderedTimeCodes));
    assertEquals("Track 3 index 1 timecode 00:01:00 is before its predecessor 00:02:00", ex2.getSuppressed()[0].getMessage());


    Files.deleteIfExists(tempFile);
  }
}
