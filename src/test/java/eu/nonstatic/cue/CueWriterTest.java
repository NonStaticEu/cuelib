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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CueWriterTest extends CueTestBase {

  static URL myTestUrlExpected = CueDiscTest.class.getResource("/My Test expected.cue"); // UTF-8 encoded

  @Test
  void should_not_write_cuesheet() {
    Path anyPath = Paths.get("/tmp/whatever");
    CueSheetOptions options = new CueSheetOptions();
    assertThrows(NullPointerException.class, () -> CueSheetWriter.writeCueSheet(null, anyPath, options));
    CueDisc anyDisc = new CueDisc();
    assertThrows(NullPointerException.class, () -> CueSheetWriter.writeCueSheet(anyDisc, (Path)null, options));
  }

  @Test
  void should_write_cuesheet_to_file() throws IOException {
    Charset cs = StandardCharsets.UTF_8;
    CueDisc disc = CueSheetReader.readCueSheet(myTestUrl, cs);

    File tempFile = File.createTempFile("cue", null);
    tempFile.delete();

    CueSheetWriter.writeCueSheet(disc, tempFile, new CueSheetOptions());
    assertFileContents(myTestUrlExpected, tempFile, cs);
    tempFile.delete();
  }

  @Test
  void should_write_cuesheet_to_path() throws IOException {
    Charset cs = StandardCharsets.UTF_8;
    CueDisc disc = CueSheetReader.readCueSheet(myTestUrl, cs);

    Path tempFile = Files.createTempFile(null, null);
    Files.deleteIfExists(tempFile);

    CueSheetWriter.writeCueSheet(disc, tempFile, new CueSheetOptions());
    assertFileContents(myTestUrlExpected, tempFile.toFile(), cs);
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_write_cuesheet_to_file_other_charset() throws IOException {
    Charset inputCharset = StandardCharsets.UTF_8, outputCharset = StandardCharsets.ISO_8859_1;
    CueDisc disc = CueSheetReader.readCueSheet(myTestUrl, inputCharset);

    disc.setCharset(outputCharset);
    File tempFile = File.createTempFile("cue", null);
    CueSheetWriter.writeCueSheet(disc, tempFile, new CueSheetOptions().setOverwrite(true));

    assertEquals(outputCharset, new CueSheetReader().detectEncoding(tempFile).getCharset());
    assertFileContents(myTestUrlExpected, tempFile, outputCharset);
    tempFile.delete();
  }

  @Test
  void should_write_cuesheet_to_file_overwrite() throws IOException {
    Charset cs = StandardCharsets.UTF_8;
    CueDisc disc = CueSheetReader.readCueSheet(myTestUrl, cs);
    File tempFile = File.createTempFile("cue", null);
    CueSheetWriter.writeCueSheet(disc, tempFile, new CueSheetOptions().setOverwrite(true));
    assertFileContents(myTestUrlExpected, tempFile, cs);
    tempFile.delete();
  }

  @Test
  void should_not_write_cuesheet_to_existing_file() throws IOException {
    Charset cs = StandardCharsets.UTF_8;

    CueDisc disc = CueSheetReader.readCueSheet(myTestUrl, cs);
    File tempFile = File.createTempFile("cue", null);

    CueSheetOptions options = new CueSheetOptions().setOverwrite(false);
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
  void should_write_cuesheet_no_file() throws IOException {
    CueDisc disc = new CueDisc();
    disc.addRemark(CueRemark.commentOf("hello world"));
    CueSheetOptions options = new CueSheetOptions().setNoTrackAllowed(true).setOverwrite(true);

    Path tempFile = Files.createTempFile(null, null);
    CueSheetWriter.writeCueSheet(disc, tempFile, options);

    assertEquals(1, Files.readAllLines(tempFile).size());
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_not_write_cuesheet_no_file() throws IOException {
    CueDisc disc = new CueDisc();
    disc.addRemark(CueRemark.commentOf("hello world"));
    CueSheetOptions options = new CueSheetOptions().setNoTrackAllowed(false).setOverwrite(true);

    Path tempFile = Files.createTempFile(null, null);
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CueSheetWriter.writeCueSheet(disc, tempFile, options));
    assertEquals("Tracks count 0 is out of range [1,99]", ex.getSuppressed()[0].getMessage());
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_write_cuesheet_no_track() throws IOException {
    CueDisc disc = new CueDisc();
    disc.addRemark(CueRemark.commentOf("hello world"));
    disc.addFile(new CueFile("path", FileType.WAVE));

    Path tempFile = Files.createTempFile(null, null);
    CueSheetOptions options = new CueSheetOptions().setNoTrackAllowed(true).setOverwrite(true);
    CueSheetWriter.writeCueSheet(disc, tempFile, options);

    assertEquals(1, Files.readAllLines(tempFile).size());
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_not_write_cuesheet_no_track() throws IOException {
    CueDisc disc = new CueDisc();
    disc.addFile(new CueFile("path", FileType.WAVE));

    Path tempFile = Files.createTempFile(null, null);
    CueSheetOptions options = CueSheetOptions.defaults().setOverwrite(true);
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CueSheetWriter.writeCueSheet(disc, tempFile, options));
    assertEquals("Tracks count 0 is out of range [1,99]", ex.getSuppressed()[0].getMessage());
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_write_cuesheet_when_pregap_on_track_one() throws IOException {
    CueSheetOptions options = CueSheetOptions.defaults().setOverwrite(true);

    Path tempFile = Files.createTempFile(null, null);

    CueDisc disc1 = new CueDisc();
    CueFile file1 = disc1.addFile(new CueFile("path1", FileType.WAVE));
    file1.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_PRE_GAP, TimeCode.ZERO_SECOND), new CueIndex(TimeCode.ONE_SECOND)));
    file1.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.TWO_SECONDS)));

    assertEquals(2, disc1.getTrackCount());
    assertDoesNotThrow(() -> CueSheetWriter.writeCueSheet(disc1, tempFile, options));


    CueDisc disc2 = new CueDisc();
    CueFile file2_1 = disc2.addFile(new CueFile("path2_1", FileType.WAVE));
    CueFile file2_2 = disc2.addFile(new CueFile("path2_2", FileType.WAVE));
    file2_2.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_PRE_GAP, TimeCode.ONE_SECOND))); // second file but still first track

    assertEquals(2, disc2.getFileCount());
    assertEquals(1, disc2.getTrackCount());
    assertDoesNotThrow(() -> CueSheetWriter.writeCueSheet(disc2, tempFile, options));

    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_not_write_cuesheet_when_pregap_not_only_on_track_one() throws IOException {
    CueSheetOptions options = CueSheetOptions.defaults().setOverwrite(true);
    Path tempFile = Files.createTempFile(null, null);

    CueDisc disc1 = new CueDisc();
    CueFile file1 = disc1.addFile(new CueFile("path1", FileType.WAVE));
    file1.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_PRE_GAP, TimeCode.ZERO_SECOND), new CueIndex(TimeCode.ONE_SECOND)));
    file1.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_PRE_GAP, TimeCode.ONE_SECOND), new CueIndex(TimeCode.TWO_SECONDS)));

    assertEquals(2, disc1.getTrackCount());
    IllegalStateException ex1 = assertThrows(IllegalStateException.class, () -> CueSheetWriter.writeCueSheet(disc1, tempFile, options));
    assertEquals("Track 2 has a pregap. Only track 1 may have one", ex1.getSuppressed()[0].getMessage());



    CueDisc disc2 = new CueDisc();
    CueFile file2_1 = disc2.addFile(new CueFile("path2_1", FileType.WAVE));
    CueFile file2_2 = disc2.addFile(new CueFile("path2_2", FileType.WAVE));
    CueFile file2_3 = disc2.addFile(new CueFile("path2_3", FileType.WAVE));
    file2_1.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.ONE_SECOND)));
    file2_3.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_PRE_GAP, TimeCode.ONE_SECOND), new CueIndex(TimeCode.TWO_SECONDS)));

    assertEquals(3, disc2.getFileCount());
    assertEquals(2, disc2.getTrackCount());

    IllegalStateException ex2 = assertThrows(IllegalStateException.class, () -> CueSheetWriter.writeCueSheet(disc2, tempFile, options));
    assertEquals("Track 2 has a pregap. Only track 1 may have one", ex2.getSuppressed()[0].getMessage());
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_not_write_cuesheet_when_more_than_99_tracks() throws IOException {
    CueSheetOptions options = CueSheetOptions.defaults().setOverwrite(true);
    Path tempFile = Files.createTempFile(null, null);

    CueDisc disc = new CueDisc();
    CueFile file = disc.addFile(new CueFile("path1", FileType.WAVE));
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
    CueSheetOptions options = CueSheetOptions.defaults().setOverwrite(true);
    Path tempFile = Files.createTempFile(null, null);

    CueDisc disc = new CueDisc();
    CueFile file = disc.addFile(new CueFile("path1", FileType.WAVE));
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
    assertEquals("Track 1 indexes 100 is out of range [0,99]", ex.getSuppressed()[0].getMessage());
    Files.deleteIfExists(tempFile);
  }

  @Test
  void should_write_cuesheet_when_chaining() throws IOException {
    CueSheetOptions options = CueSheetOptions.defaults().setOrderedTimeCodes(false).setOverwrite(true);
    Path tempFile = Files.createTempFile(null, null);

    CueDisc disc = new CueDisc();
    CueFile file1 = disc.addFile(new CueFile("path1", FileType.WAVE));
    CueFile file2 = disc.addFile(new CueFile("path1", FileType.WAVE));
    CueTrack track1 = file1.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.ZERO_SECOND)));
    CueTrack track2 = file2.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.TWO_SECONDS)));
    CueTrack track3 = file2.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.ONE_SECOND)));

    assertEquals(3, disc.getTrackCount());
    assertDoesNotThrow(() -> CueSheetWriter.writeCueSheet(disc, tempFile, options));



    options.setOrderedTimeCodes(true);
    IllegalStateException ex1 = assertThrows(IllegalStateException.class, () -> CueSheetWriter.writeCueSheet(disc, tempFile, options));
    assertEquals("Track 3 index 1 timecode 00:01:00 is before its predecessor 00:02:00", ex1.getSuppressed()[0].getMessage());



    track2.getIndex(1).setTimeCode(TimeCode.ZERO_SECOND);
    track3.getIndex(1).setTimeCode(TimeCode.ONE_SECOND);
    assertDoesNotThrow(() -> CueSheetWriter.writeCueSheet(disc, tempFile, options));


    track2.addIndex(new CueIndex(TimeCode.TWO_SECONDS)); // so we have 0, 2, 1 seconds
    IllegalStateException ex2 = assertThrows(IllegalStateException.class, () -> CueSheetWriter.writeCueSheet(disc, tempFile, options));
    assertEquals("Track 3 index 1 timecode 00:01:00 is before its predecessor 00:02:00", ex2.getSuppressed()[0].getMessage());


    Files.deleteIfExists(tempFile);
  }
}
