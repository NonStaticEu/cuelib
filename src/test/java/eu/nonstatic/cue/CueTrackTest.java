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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.nonstatic.cue.CueIterable.CueIterator;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class CueTrackTest {

  @Test
  void should_initialize_with_empty_track() {
    assertThrows(NullPointerException.class, () -> new CueTrack(null));

    CueTrack track = new CueTrack(TrackType.AUDIO);
    assertNull(track.getNumber());
    assertNull(track.getTitle());
    assertNull(track.getPerformer());
    assertNull(track.getSongwriter());
    assertNull(track.getIsrc());

    assertEquals(0, track.getIndexCount());
    assertEquals(0, track.getIndexes().size());
    assertNull(track.getFirstIndex());
    assertNull(track.getLastIndex());
    assertEquals(1, track.getNextIndexNumber());
    assertNull(track.getPreGap());
    assertNull(track.getPreGapIndex());

    assertEquals(0, track.getFlags().size());
    assertEquals(0, track.getRemarks().size());
    assertEquals(0, track.getOthers().size());
  }

  @Test
  void should_validate_isrc() {
    CueTrack track = new CueTrack(TrackType.AUDIO);
    assertDoesNotThrow(() -> track.setIsrc("ITXXX0011111"));

    track.setIsrc("FR-XXX-42-12345");
    assertEquals("FRXXX4212345", track.getIsrc());

    assertThrows(IllegalArgumentException.class, () -> track.setIsrc("whatever"));
  }


  @Test
  void should_get_indexes() {
    CueTrack track = new CueTrack(TrackType.AUDIO);
    CueIndex index1 = track.addIndex(new CueIndex(1, new TimeCode(10, 20, 30)));
    CueIndex index2 = track.addIndex(new CueIndex(2, new TimeCode(20, 30, 40)));

    assertEquals(2, track.getIndexCount());
    assertEquals(List.of(index1, index2), track.getIndexes());

    assertSame(index1, track.getFirstIndex());
    assertSame(index2, track.getLastIndex());
  }

  @Test
  void should_deep_copy() {
    CueIndex index1 = new CueIndex(1, new TimeCode(10, 20, 30));
    CueIndex index2 = new CueIndex(2, new TimeCode(20, 30, 40));
    List<CueIndex> indexes = List.of(index1, index2);
    CueTrack track = new CueTrack(TrackType.AUDIO, indexes);

    CueTrack trackCopy = track.deepCopy();

    assertNotSame(trackCopy, track);
    assertEquals(track.getIndexCount(), trackCopy.getIndexCount());

    List<CueIndex> indexCopies = trackCopy.getIndexes();

    assertNotSame(indexes, indexCopies);
    assertEquals(indexes, indexCopies);

    CueIndex index1Copy = indexCopies.get(0);
    assertNotSame(index1, index1Copy);
    assertEquals(index1, index1Copy);

    CueIndex index2Copy = indexCopies.get(1);
    assertNotSame(index2, index2Copy);
    assertEquals(index2, index2Copy);
  }

  @Test
  void should_remove_index() {
    CueTrack track = new CueTrack(TrackType.AUDIO);
    assertThrows(IllegalArgumentException.class, () -> track.removeIndex(1));

    CueIndex index0 = track.addIndex(new CueIndex(CueIndex.INDEX_PRE_GAP, TimeCode.TWO_SECONDS));
    CueIndex index1 = track.addIndex(new CueIndex(1, new TimeCode(10, 20, 30)));
    CueIndex index2 = track.addIndex(new CueIndex(2, new TimeCode(20, 30, 40)));

    assertEquals(3, track.getIndexCount());


    CueIndex removedIndex1 = track.removeIndex(1);
    assertSame(index1, removedIndex1);

    assertEquals(2, track.getIndexCount());
    assertEquals(List.of(index0, index2), track.getIndexes());
    assertSame(index0, track.getFirstIndex());
    assertSame(index2, track.getLastIndex());


    CueIndex removedIndex0 = track.removeIndex(CueIndex.INDEX_PRE_GAP);
    assertSame(index0, removedIndex0);

    assertEquals(1, track.getIndexCount());
    assertEquals(List.of(index2), track.getIndexes());
    assertSame(index2, track.getFirstIndex());
    assertSame(index2, track.getLastIndex());
  }

  @Test
  void should_clear_indexes() {
    CueTrack track = new CueTrack(TrackType.AUDIO,
      new CueIndex(1, new TimeCode(10, 20, 30)),
      new CueIndex(2, new TimeCode(20, 30, 40))
    );

    assertEquals(2, track.getIndexCount());

    track.clearIndexes();
    assertEquals(0, track.getIndexCount());
  }

  @Test
  void should_not_add_twice_the_same_index() {
    CueIndex index1 = new CueIndex(1, new TimeCode(10, 20, 30));
    CueIndex index2 = new CueIndex(2, new TimeCode(20, 30, 40));
    CueTrack track = new CueTrack(TrackType.AUDIO, index1, index2);

    for (CueIndex index : track) {
      assertThrows(IllegalArgumentException.class, () -> track.addIndex(index));
    }
  }

  @Test
  void should_allow_pregap_with_existing_index_1() {
    CueIndex index0 = new CueIndex(0, new TimeCode(20, 30, 40));
    CueIndex index1 = new CueIndex(1, new TimeCode(10, 20, 30));
    CueTrack track = new CueTrack(TrackType.AUDIO, index1);
    assertEquals(1, track.getIndexCount());
    track.addIndex(index0);
    assertEquals(2, track.getIndexCount());
  }

  @Test
  void should_allow_index_1_with_existing_pregap() {
    CueIndex index0 = new CueIndex(0, new TimeCode(20, 30, 40));
    CueIndex index1 = new CueIndex(1, new TimeCode(10, 20, 30));
    CueTrack track = new CueTrack(TrackType.AUDIO, index0);
    assertEquals(1, track.getIndexCount());
    track.addIndex(index1);
    assertEquals(2, track.getIndexCount());
  }

  @Test
  void should_insert_track() {
    TimeCode timeCode1 = new TimeCode(10, 20, 30);
    TimeCode timeCode2 = new TimeCode(20, 30, 40);
    TimeCode timeCode3 = new TimeCode(30, 40, 50);

    CueIndex index1 = new CueIndex(1, timeCode1);
    CueIndex index2_to_be_3 = new CueIndex(2, timeCode3);
    CueTrack track = new CueTrack(TrackType.AUDIO, index1, index2_to_be_3);
    assertEquals(2, track.getIndexCount());

    CueIndex index2_to_insert_inbetween = new CueIndex(2, timeCode2); // index 2 as well
    track.addIndex(index2_to_insert_inbetween);


    assertEquals(3, track.getIndexCount());

    CueIndex finalIndex1 = track.getIndex(1);
    CueIndex finalIndex2 = track.getIndex(2);
    CueIndex finalIndex3 = track.getIndex(3);

    assertEquals(1, finalIndex1.getNumber());
    assertEquals(timeCode1, finalIndex1.getTimeCode());

    assertEquals(2, finalIndex2.getNumber());
    assertEquals(timeCode2, finalIndex2.getTimeCode());

    assertEquals(3, finalIndex3.getNumber());
    assertEquals(timeCode3, finalIndex3.getTimeCode());
  }

  @Test
  void should_not_add_index_out_of_range() {
    CueIndex index1 = new CueIndex(1, new TimeCode(10, 20, 30));
    CueIndex index2 = new CueIndex(2, new TimeCode(20, 30, 40));
    CueIndex index3 = new CueIndex(3, new TimeCode(30, 40, 50));
    CueTrack track = new CueTrack(TrackType.AUDIO);
    // adding index > 1 when indexes is empty
    assertThrows(IllegalArgumentException.class, () -> track.addIndex(index2));
    assertEquals(0, track.getIndexCount());

    assertEquals(0, track.getIndexCount());
    track.addIndex(index1);
    assertEquals(1, track.getIndexCount());

    //adding track out of range (1 cannot be followed by 3)
    assertThrows(IllegalArgumentException.class, () -> track.addIndex(index3));
  }

  @Test
  void should_give_iterator() {
    CueTrack track = new CueTrack(TrackType.AUDIO);
    CueIterator<CueIndex> it1 = track.iterator();
    assertFalse(it1.hasNext());

    CueIndex index = new CueIndex(1, new TimeCode(10, 20, 30));
    track.addIndex(index);

    CueIterator<CueIndex> it2 = track.iterator();
    assertTrue(it2.hasNext());
    assertEquals(index, it2.next());
  }

  @Test
  void should_stream() {
    CueTrack track = new CueTrack(TrackType.AUDIO);
    assertEquals(0, track.stream().count());

    track.addIndex(new CueIndex(new TimeCode(10, 20, 30)));
    track.addIndex(new CueIndex(new TimeCode(20, 30, 40)));
    assertEquals(2, track.stream().count());
  }

  @Test
  void should_add_remove_remark() {
    CueTrack track = new CueTrack(TrackType.CDI_2352);
    track.addRemark(new CueRemark("tag1", "value1"));
    track.addRemark(new CueRemark("tag2", "value2"));
    assertEquals(2, track.getRemarks().size());
    assertEquals(0, track.getOthers().size());
    track.clearRemarks();
    assertEquals(0, track.getRemarks().size());
    assertEquals(0, track.getOthers().size());
  }

  @Test
  void should_add_remove_other() {
    CueTrack track = new CueTrack(TrackType.AUDIO);
    track.addOther(new CueOther("tag1", "value1"));
    track.addOther(new CueOther("tag2", "value2"));
    assertEquals(2, track.getOthers().size());
    assertEquals(0, track.getRemarks().size());
    track.clearOthers();
    assertEquals(0, track.getOthers().size());
    assertEquals(0, track.getRemarks().size());
  }

  @Test
  void should_get_duration() throws IllegalTrackTypeException, IndexNotFoundException, NegativeDurationException {
    TimeCode timeCode1 = new TimeCode(10, 20, 30);
    TimeCode timeCode2 = new TimeCode(20, 30, 40);
    CueTrack track1 = new CueTrack(TrackType.AUDIO);
    CueTrack track2 = new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_TRACK_START, timeCode2));

    Duration fileDuration = Duration.ofMinutes(30);
    // missing index on this
    assertThrows(IndexNotFoundException.class, () -> track1.until(track2, fileDuration));

    // missing index on other
    CueTrack track3 = new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_TRACK_START, timeCode1));
    CueTrack track4 = new CueTrack(TrackType.AUDIO);
    assertThrows(IndexNotFoundException.class, () -> track3.until(track4, fileDuration));

    // bad types: one, other, both
    CueTrack track5 = new CueTrack(TrackType.MODE1_2352, new CueIndex(CueIndex.INDEX_TRACK_START, timeCode1));
    CueTrack track6 = new CueTrack(TrackType.MODE1_2352, new CueIndex(CueIndex.INDEX_TRACK_START, timeCode2));
    assertThrows(IllegalTrackTypeException.class, () -> track3.until(track6, fileDuration));
    assertThrows(IllegalTrackTypeException.class, () -> track5.until(track2, fileDuration));
    assertThrows(IllegalTrackTypeException.class, () -> track5.until(track6, fileDuration));

    // negative track duration
    CueTrack track7 = new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_TRACK_START, new TimeCode(30, 0, 1)));
    assertThrows(NegativeDurationException.class, () -> track7.until(null, fileDuration));

    // negative last track duration
    assertThrows(NegativeDurationException.class, () -> track7.until(null, fileDuration));

    // OK
    assertEquals(Duration.ofMillis(610133L), track3.until(track2, fileDuration)); // duration is calculated from the frames diff, hence the 0.133s instead of 0.134s calculated from millis.
    // OK last track
    assertEquals(Duration.ofMillis(1179600L), track3.until(null, fileDuration));
  }

  @Test
  void should_give_tostring() {
    CueTrack track = new CueTrack(7, TrackType.AUDIO);
    assertEquals("TRACK 07 AUDIO", track.toString());
  }
}
