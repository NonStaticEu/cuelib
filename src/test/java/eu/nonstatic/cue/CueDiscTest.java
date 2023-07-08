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


import static eu.nonstatic.cue.CueFlag.DIGITAL_COPY_PERMITTED;
import static eu.nonstatic.cue.CueFlag.FOUR_CHANNEL_AUDIO;
import static eu.nonstatic.cue.CueFlag.SERIAL_COPY_MANAGEMENT_SYSTEM;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.nonstatic.cue.CueIterable.CueIterator;
import eu.nonstatic.cue.FileType.Audio;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CueDiscTest extends CueTestBase {

  @Test
  void should_initialize_with_empty_disc() throws IndexNotFoundException {
    CueDisc disc = new CueDisc();
    assertNull(disc.getPath());
    assertEquals(StandardCharsets.UTF_8, disc.getCharset());
    assertNull(disc.getTitle());
    assertNull(disc.getPerformer());
    assertNull(disc.getSongwriter());
    assertNull(disc.getCatalog());
    assertNull(disc.getCdTextFile());
    assertEquals(0, disc.getFileCount());
    assertEquals(0, disc.getFiles().size());
    assertNull(disc.getFirstFile());
    assertNull(disc.getLastFile());

    assertEquals(0, disc.getTrackCount());
    assertEquals(0, disc.getTracks().size());
    assertEquals(0, disc.getNumberedTracks().size());

    assertNull(disc.getFirstTrack());
    assertNull(disc.getLastTrack());
    assertEquals(CueTrack.TRACK_ONE, disc.getNextTrackNumber());
    assertNull(disc.getHiddenTrack());

    assertEquals(0, disc.getIndexCount());
    assertEquals(0, disc.getIndexes().size());

    assertEquals(0, disc.getRemarks().size());
    assertEquals(0, disc.getOthers().size());
  }

  @Test
  void should_build_with_files() {
    CueDisc disc = new CueDisc(null, StandardCharsets.UTF_8, new CueFile("file0", FileType.Data.BINARY), new CueFile("file1", FileType.Audio.AIFF));
    assertEquals(2, disc.getFileCount());

    List<CueFile> files = disc.getFiles();
    assertEquals("file0", files.get(0).getFile());
    assertEquals("file1", files.get(1).getFile());
  }

  private static CueDisc buildDiscWithTwoTracks() {
    CueDisc disc = new CueDisc();
    CueFile file0 = new CueFile("file_0", FileType.Audio.MP3);

    CueTrack track1 = new CueTrack(TrackType.AUDIO);
    track1.setTitle("title_1");
    track1.setPerformer("performer_1");
    track1.setSongwriter("songwriter_1");
    track1.setIsrc("ISRC09911111");
    track1.setFlags(List.of(FOUR_CHANNEL_AUDIO, DIGITAL_COPY_PERMITTED, SERIAL_COPY_MANAGEMENT_SYSTEM, DIGITAL_COPY_PERMITTED /*dupe on purpose*/));
    track1.addIndex(new CueIndex(new TimeCode(10, 20, 30)));
    track1.addIndex(new CueIndex(new TimeCode(15, 30, 45)));
    file0.addTrack(track1);

    CueTrack track2 = new CueTrack(99, TrackType.AUDIO);
    track2.setTitle("title_2");
    track2.setPerformer("performer_2");
    track2.setSongwriter("songwriter_2");
    track2.setIsrc("ISRC09922222");
    track2.setFlags(List.of(DIGITAL_COPY_PERMITTED));
    track2.addIndex(new CueIndex(new TimeCode(20, 40, 60)));
    file0.addTrack(track2);

    track2.setTitle("title_changed"); // won't be part of the disc cause addTrack copied the data

    disc.addFile(file0);

    disc.addRemark(CueRemark.commentOf("comment_0"));
    disc.addRemark(new CueRemark(CueRemark.TAG_UPC, "9876543210"));
    disc.addOther(new CueOther("keyword_0", "value_0"));
    return disc;
  }

  @Test
  void should_have_correct_disc_structure() {
    CueDisc disc = buildDiscWithTwoTracks();

    List<CueFile> files = checkDiscStructure(disc);

    CueFile file0 = files.get(0);
    assertSame(file0, disc.getFirstFile());
    assertSame(file0, disc.getLastFile());

    assertEquals("file_0", file0.getFile());
    assertEquals(FileType.Audio.MP3, file0.getType());

    assertEquals(2, file0.getTrackCount());
    List<CueTrack> tracks = file0.getTracks();
    assertEquals(2, tracks.size());
    CueTrack whateverTrack = new CueTrack("whatever");
    assertThrows(UnsupportedOperationException.class, () -> tracks.add(whateverTrack));

    CueTrack t1 = tracks.get(0);
    checkTrack01Structure(disc, t1);

    CueTrack t2 = tracks.get(1);
    checkTrack02Structure(disc, t2);

    // Adding files, tracks and indexes in different ways
    CueTrack t3 = file0.addTrack(new CueTrack(TrackType.AUDIO));
    CueTrack t4 = file0.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(0, 0, 0)));
    assertEquals(4, disc.getTrackCount());
    assertEquals(0, disc.getTrack(3).getIndexCount());
    assertEquals(1, disc.getTrack(4).getIndexCount());

    t2.addIndex(new CueIndex(44, 0, 2, 0), true);
    t2.addIndex(new CueIndex(3, 1, 0, 0));
    assertEquals(3, t2.getIndexCount());

    CueFile file1 = new CueFile("file_1", FileType.Data.BINARY, new CueTrack(5, TrackType.AUDIO));
    disc.addFile(file1);
    assertEquals(2, disc.getFileCount());

    disc.addFile(new CueFile("file_2", FileType.Data.BINARY));
    assertEquals(file1.getLastTrack(), disc.getLastTrack()); // despite a new trailing (empty) file
  }

  private static List<CueFile> checkDiscStructure(CueDisc disc) {
    List<CueRemark> remarks = disc.getRemarks();
    assertEquals(2, remarks.size());
    CueRemark whateverRemark = CueRemark.commentOf("whatever");
    assertThrows(UnsupportedOperationException.class, () -> remarks.add(whateverRemark));
    List<CueOther> others = disc.getOthers();
    assertEquals(1, others.size());
    CueOther fooBarOther = new CueOther("foo", "bar");
    assertThrows(UnsupportedOperationException.class, () -> others.add(fooBarOther));

    assertEquals(1, disc.getFileCount());
    List<CueFile> files = disc.getFiles();
    assertEquals(1, files.size());
    CueFile fooBarFile = new CueFile("foo", FileType.Data.BINARY);
    assertThrows(UnsupportedOperationException.class, () -> files.add(fooBarFile));
    return files;
  }

  private static void checkTrack01Structure(CueDisc disc, CueTrack t1) {
    assertSame(t1, disc.getTrack(1));
    assertSame(t1, disc.getTrackNumberOne());
    assertSame(t1, disc.getFirstTrack());
    assertEquals(1, t1.getNumber()); // renumbered on the fly
    assertEquals(TrackType.AUDIO, t1.getType());
    assertEquals("title_1", t1.getTitle());
    assertEquals("performer_1", t1.getPerformer());
    assertEquals("songwriter_1", t1.getSongwriter());
    assertEquals("ISRC09911111", t1.getIsrc());
    assertEquals(List.of(DIGITAL_COPY_PERMITTED, FOUR_CHANNEL_AUDIO, SERIAL_COPY_MANAGEMENT_SYSTEM),
      t1.getFlags().stream().sorted().collect(toList()));

    assertEquals(2, t1.getIndexCount());
    List<CueIndex> t1Indexes = t1.getIndexes();
    assertEquals(2, t1Indexes.size());
    CueIndex zeroIndex = new CueIndex(0, 0, 0);
    assertThrows(UnsupportedOperationException.class, () -> t1Indexes.add(zeroIndex));

    CueIndex t1i0 = t1Indexes.get(0);
    assertSame(t1i0, t1.getIndex(1));
    assertSame(t1i0, t1.getFirstIndex());

    CueIndex t1i1 = t1Indexes.get(1);
    assertSame(t1i1, t1.getIndex(2));
    assertSame(t1i1, t1.getLastIndex());
  }

  private static void checkTrack02Structure(CueDisc disc, CueTrack t2) {
    assertSame(t2, disc.getTrack(2));
    assertSame(t2, disc.getLastTrack());
    assertEquals(2, t2.getNumber()); // renumbred on the fly
    assertEquals(TrackType.AUDIO, t2.getType());
    assertEquals("title_2", t2.getTitle());
    assertEquals("performer_2", t2.getPerformer());
    assertEquals("songwriter_2", t2.getSongwriter());
    assertEquals("ISRC09922222", t2.getIsrc());
    assertEquals(Set.of(DIGITAL_COPY_PERMITTED), t2.getFlags());

    assertEquals(1, t2.getIndexCount());
    List<CueIndex> t2Indexes = t2.getIndexes();
    CueIndex t2i0 = t2Indexes.get(0);
    assertSame(t2i0, t2.getIndex(1));
    assertSame(t2i0, t2.getFirstIndex());
    assertSame(t2i0, t2.getLastIndex());
  }

  @Test
  void should_number_correctly_when_first_track_not_1() throws IOException {
    CueDisc disc = new CueDisc();
    disc.setFirstTrackNumber(42);
    CueFile file1 = disc.addFile(new CueFile("path1", FileType.Audio.WAVE));
    file1.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.ZERO_SECOND), new CueIndex(TimeCode.ONE_SECOND)));
    file1.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_PRE_GAP, TimeCode.ZERO_SECOND), new CueIndex(TimeCode.TWO_SECONDS)));
    CueFile file2 = disc.addFile(new CueFile("path2", Audio.AIFF));
    file2.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.ZERO_SECOND)));
    file2.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.TWO_SECONDS)));

    disc.renumberTracks();

    List<CueTrack> tracks = disc.getTracks();
    assertEquals(4, tracks.size());
    assertEquals(42, tracks.get(0).number);
    assertEquals(43, tracks.get(1).number);
    assertEquals(44, tracks.get(2).number);
    assertEquals(45, tracks.get(3).number);
  }

  @Test
  void should_set_catalog() {
    CueDisc disc = new CueDisc();
    assertThrows(IllegalArgumentException.class, () -> disc.setCatalog("whatever"));
    assertThrows(IllegalArgumentException.class, () -> disc.setCatalog("987654"));
    assertThrows(IllegalArgumentException.class, () -> disc.setCatalog("98765432109876"));

    disc.setCatalog("8 029368 237257");
    assertEquals("8029368237257", disc.getCatalog());

    disc.setCatalog("5060147129748");
    assertEquals("5060147129748", disc.getCatalog());

    disc.setCatalog("801199018377");
    assertEquals("0801199018377", disc.getCatalog());

    disc.setCatalog(null);
    assertNull(disc.getCatalog());
  }

  @Test
  void should_get_distinct_file_count() {
    CueDisc disc = new CueDisc();

    CueTrack track = new CueTrack(TrackType.AUDIO, "performer", "title");

    CueFile file1 = new CueFile("path1", FileType.Data.BINARY);
    disc.addFile(file1).addTrack(track);

    CueFile file2 = new CueFile("path2", FileType.Audio.MP3);
    file2.addTrack(track);
    disc.addFile(file2);

    file1.addTrack(track);
    disc.addFile(file1);

    assertEquals(2, disc.getDistinctFileCount());
  }

  @Test
  void should_clear_files() {
    CueDisc disc = new CueDisc();
    disc.addFile(new CueFile("whatever", FileType.Data.BINARY));
    assertEquals(1, disc.getFileCount());

    disc.clearFiles();
    assertEquals(0, disc.getFileCount());
  }

  @Test
  void should_clear_remarks() {
    CueDisc disc = new CueDisc();
    disc.addRemark(new CueRemark("tag1", "value1"));
    disc.addRemark(new CueRemark("tag2", "value2"));
    assertEquals(2, disc.getRemarks().size());
    disc.clearRemarks();
    assertEquals(0, disc.getRemarks().size());
  }

  @Test
  void should_clear_others() {
    CueDisc disc = new CueDisc();
    disc.addOther(new CueOther("key1", "value1"));
    disc.addOther(new CueOther("key2", "value2"));
    assertEquals(2, disc.getOthers().size());
    disc.clearOthers();
    assertEquals(0, disc.getOthers().size());
  }

  @Test
  void should_get_chunk() {
    CueDisc disc = new CueDisc();
    assertThrows(IllegalArgumentException.class, () -> disc.chunk(1));

    CueFile actualFile0 = disc.addFile(new CueFile("file0", FileType.Data.BINARY));
    assertThrows(IllegalArgumentException.class, () -> disc.chunk(1));

    CueTrack actualTrack1 = actualFile0.addTrack(new CueTrack(TrackType.MODE1_2352));

    CueFile actualFile1 = disc.addFile(new CueFile("file1", FileType.Audio.AIFF));
    CueTrack actualTrack2 = actualFile1.addTrack(new CueTrack(TrackType.MODE2_2324));

    FileAndTrack chunk = disc.chunk(2);
    assertEquals("file1", chunk.fileReference.file);
    assertEquals(FileType.Audio.AIFF, chunk.fileReference.type);
    assertSame(actualTrack2, chunk.track);
  }

  @Test
  void should_get_hidden_track() throws IndexNotFoundException {
    CueDisc disc = new CueDisc();
    assertFalse(disc.hasHiddenTrack());
    assertNull(disc.getHiddenTrack());

    CueFile file = new CueFile("file.aiff", FileType.Audio.AIFF);
    CueFile actualFile = disc.addFile(file);
    assertFalse(disc.hasHiddenTrack());
    assertNull(disc.getHiddenTrack());

    CueTrack track = new CueTrack(TrackType.MODE1_2352);
    CueTrack actualTrack = actualFile.addTrack(track);
    assertFalse(disc.hasHiddenTrack());
    assertNull(disc.getHiddenTrack());

    CueIndex startIndex = new CueIndex(CueIndex.INDEX_TRACK_START, new TimeCode(2, 4, 6));
    CueIndex actualIndex1 = actualTrack.addIndex(startIndex);
    assertFalse(disc.hasHiddenTrack());
    assertNull(disc.getHiddenTrack());

    CueIndex preGap = new CueIndex(CueIndex.INDEX_PRE_GAP, new TimeCode(0, 0, 1));
    CueIndex actualIndex0 = actualTrack.addIndex(preGap);

    assertTrue(disc.hasHiddenTrack());
    CueHiddenTrack hiddenTrack = disc.getHiddenTrack();
    assertEquals(new TimeCode(2, 4, 5).toDuration(), hiddenTrack.getDuration());

    FileReference fileReference = hiddenTrack.getFileAndFormat();
    assertEquals("file.aiff", fileReference.getFile());
    assertEquals(FileType.Audio.AIFF, FileReference.getFileTypeByFileName(fileReference.getFile()));
    assertEquals(actualTrack, hiddenTrack.getTrack());
    assertEquals(actualIndex0, hiddenTrack.getPreGapIndex());
    assertEquals(actualIndex1, disc.getHiddenTrack().getStartIndex());
  }

  @Test
  void should_complain_on_hidden_track_without_followup_index() {
    CueDisc disc = new CueDisc();
    disc.addFile(new CueFile("file", FileType.Audio.FLAC))
      .addTrack(new CueTrack(TrackType.AUDIO))
      .addIndex(new CueIndex(CueIndex.INDEX_PRE_GAP, TimeCode.ZERO_SECOND));

    assertTrue(disc.hasHiddenTrack());
    assertThrows(IndexNotFoundException.class, disc::getHiddenTrack);
  }

  @Test
  void should_give_iterator() {
    CueDisc disc = new CueDisc();
    CueIterator<CueFile> it1 = disc.iterator();
    assertFalse(it1.hasNext());

    CueFile file = new CueFile("file", FileType.Audio.WAVE);
    disc.addFile(file);
    CueIterator<CueFile> it2 = disc.iterator();
    assertTrue(it2.hasNext());
    assertEquals(file, it2.next());
  }

  @Test
  void should_stream() {
    CueDisc disc = new CueDisc();
    assertEquals(0, disc.stream().count());

    disc.addFile(new CueFile("file1", FileType.Audio.WAVE));
    disc.addFile(new CueFile("file2", FileType.Audio.AIFF));
    assertEquals(2, disc.stream().count());
  }

  @Test
  void should_not_remove_track() {
    CueDisc disc = new CueDisc();
    CueTrack track = new CueTrack("type");
    track.setTitle("title");
    disc.addFile(new CueFile("file", FileType.Audio.MP3)).addTrack(track);

    assertEquals(1, disc.getTrackCount());
    assertThrows(IllegalArgumentException.class, () -> disc.removeTrack(2));
  }

  @Test
  void should_remove_track() {
    CueDisc disc = new CueDisc();
    CueFile file1 = disc.addFile(new CueFile("file1", FileType.Audio.WAVE));
    for(int i = 0; i < 10; i++) {
      CueTrack track = new CueTrack("type1");
      track.setTitle(String.format("title%02d", i+1));
      file1.addTrack(track);
    }
    CueFile file2 = disc.addFile(new CueFile("file2", FileType.Audio.FLAC));
    for(int i = 10; i < 20; i++) {
      CueTrack track = new CueTrack("type2");
      track.setTitle(String.format("title%02d", i+1));
      file2.addTrack(track);
    }
    disc.removeTrack(13);
    assertEquals(19, disc.getTrackCount());
    assertEquals("title12", disc.getTrack(12).getTitle());
    assertEquals("title14", disc.getTrack(13).getTitle());
  }

  @Test
  void should_move_track_after() throws IOException {
    CueDisc disc = new CueSheetReader().readCueSheet(myTestUrl, StandardCharsets.UTF_8).getDisc();
    CueTrack track2 = disc.getTrack(2);
    CueTrack track5 = disc.getTrack(5);

    CueTrack movedTrack5 = disc.moveTrackAfter(5, 2); // 5 => 3
    assertSame(track5, movedTrack5);
    assertEquals(3, movedTrack5.getNumber());

    CueTrack movedTrack3 = disc.moveTrackAfter(3, 3); // 3 (ex-5) => 3
    assertSame(track5, movedTrack3);
    assertEquals(3, movedTrack3.getNumber());

    CueTrack movedTrack2 = disc.moveTrackAfter(2, 1); // 2 => 2
    assertSame(track2, movedTrack2);
    assertEquals(2, movedTrack2.getNumber());
  }

  @Test
  void should_move_track_before() throws IOException {
    CueDisc disc = new CueSheetReader().readCueSheet(myTestUrl, StandardCharsets.UTF_8).getDisc();
    CueTrack track1 = disc.getTrack(1);
    CueTrack track2 = disc.getTrack(2);
    CueTrack track4 = disc.getTrack(4);

    CueTrack movedTrack2 = disc.moveTrackBefore(2, 5); // 2 => 4
    assertSame(track2, movedTrack2);
    assertEquals(4, movedTrack2.getNumber());

    CueTrack movedTrack3 = disc.moveTrackBefore(3, 3); // 3 (ex-4) => 3
    assertSame(track4, movedTrack3);
    assertEquals(3, movedTrack3.getNumber());

    CueTrack movedTrack1 = disc.moveTrackBefore(1, 2); // 1 => 1
    assertSame(track1, movedTrack1);
    assertEquals(1, movedTrack1.getNumber());
  }
}
