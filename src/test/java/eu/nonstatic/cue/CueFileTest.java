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

import static eu.nonstatic.cue.CueTestBase.MP3_URL;
import static eu.nonstatic.cue.CueTestBase.copyFileContents;
import static eu.nonstatic.cue.CueTestBase.deleteRecursive;
import static eu.nonstatic.cue.SizeAndDuration.CD_BYTES_PER_FRAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.nonstatic.cue.CueIterable.CueIterator;
import eu.nonstatic.cue.FileType.Data;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.jupiter.api.Test;

class CueFileTest {

  @Test
  void should_construct_cuefile() throws IOException {
    Path tempDir = Files.createTempDirectory("should_construct_cuefile");
    Path path = copyFileContents(MP3_URL, tempDir, "file.mp3");

    TimeCode indexTimeCode = TimeCode.TWO_SECONDS;
    Duration mp3Duration = Duration.ofNanos(11154285714L);

    CueSheetContext context = new CueSheetContext(path, new CueOptions(null));

    CueFile cueFile1 = new CueFile(path.toFile(), context);
    cueFile1.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(indexTimeCode)));
    assertTrue(cueFile1.isAudio());
    assertTrue(cueFile1.isSizeAndDurationSet());
    assertEquals(new TimeCode(mp3Duration, TimeCodeRounding.DOWN).toFrameCount() * CD_BYTES_PER_FRAME, cueFile1.getSizeAndDuration().getSize()); // audio size "on CD", rounded DOWN by default
    assertEquals(mp3Duration, cueFile1.getSizeAndDuration().getDuration());
    assertEquals(mp3Duration.minus(indexTimeCode.toDuration()), cueFile1.getTrackDuration(0)); // index here not track number. Duration excludes the time before the first index


    CueFile cueFile2 = new CueFile(path.toFile(), Data.BINARY, context);
    assertFalse(cueFile2.isAudio());
    assertTrue(cueFile2.isSizeAndDurationSet());
    assertEquals(210469L, cueFile2.getSizeAndDuration().getSize()); // binary size
    assertNull(cueFile2.getSizeAndDuration().getDuration());

    cueFile2.addTrack(new CueTrack(TrackType.MODE2_2352, new CueIndex(TimeCode.ZERO_SECOND)));
    cueFile2.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(TimeCode.ONE_SECOND))); // result below should be the same no matter the track type or indexes, since the file type is binary
    assertNull(cueFile2.getTrackDuration(0)); // index here not track number
    assertNull(cueFile2.getTrackDuration(1)); // index here not track number

    deleteRecursive(tempDir);
  }

  @Test
  void should_initialize_with_empty_file() throws IllegalTrackTypeException, IndexNotFoundException, NegativeDurationException {
    CueFile file = new CueFile("somefile", FileType.Audio.WAVE);
    assertEquals("somefile", file.getFile());
    assertEquals(FileType.Audio.WAVE, file.getType());

    assertEquals(0, file.getTrackCount());
    assertEquals(0, file.getTracks().size());

    assertNull(file.getFirstTrack());
    assertNull(file.getLastTrack());
    assertThrows(TrackNotFoundException.class, () -> file.getTrackDuration(0));
    assertEquals(0, file.getTracksDurations().size());

    assertEquals(0, file.getIndexCount());
    assertEquals(0, file.getIndexes().size());
  }

  @Test
  void should_get_tracks() {
    CueFile file = new CueFile("file", FileType.Audio.WAVE);
    CueTrack track1 = file.addTrack(new CueTrack(TrackType.AUDIO, "performer1", "title1"));
    CueTrack track2 = file.addTrack(new CueTrack(TrackType.AUDIO, "performer2", "title2"));

    assertEquals(2, file.getTrackCount());
    assertEquals(List.of(track1, track2), file.getTracks());

    assertSame(track1, file.getFirstTrack());
    assertSame(track2, file.getLastTrack());
  }

  @Test
  void should_deep_copy() {
    CueTrack track1 = new CueTrack(TrackType.AUDIO, "performer1", "title1");
    CueTrack track2 = new CueTrack(TrackType.AUDIO, "performer2", "title2");
    List<CueTrack> tracks = List.of(track1, track2);
    CueFile file = new CueFile("file", FileType.Audio.WAVE, tracks);

    CueFile fileCopy = file.deepCopy();

    assertNotSame(fileCopy, file);
    assertEquals(file.getTrackCount(), fileCopy.getTrackCount());

    List<CueTrack> trackCopies = fileCopy.getTracks();

    assertNotSame(tracks, trackCopies);
    assertEquals(tracks, trackCopies);

    CueTrack track1Copy = trackCopies.get(0);
    assertNotSame(track1, track1Copy);
    assertEquals(track1, track1Copy);

    CueTrack track2Copy = trackCopies.get(1);
    assertNotSame(track2, track2Copy);
    assertEquals(track2, track2Copy);
  }

  @Test
  void should_remove_track() {
    CueFile file = new CueFile("file", FileType.Audio.WAVE);
    CueTrack track1 = file.addTrack(new CueTrack(TrackType.AUDIO, "performer1", "title1"));
    CueTrack track2 = file.addTrack(new CueTrack(TrackType.AUDIO, "performer2", "title2"));

    assertEquals(2, file.getTrackCount());

    CueTrack removedTrack = file.removeTrack(0);
    assertSame(track1, removedTrack);

    assertEquals(1, file.getTrackCount());
    assertEquals(List.of(track2), file.getTracks());
    assertSame(track2, file.getFirstTrack());
    assertSame(track2, file.getLastTrack());
  }

  @Test
  void should_clear_tracks() {
    CueFile file = new CueFile("file", FileType.Audio.WAVE, List.of(
      new CueTrack(TrackType.AUDIO, "performer1", "title1"),
      new CueTrack(TrackType.AUDIO, "performer2", "title2")
    ));

    assertEquals(2, file.getTrackCount());

    file.clearTracks();
    assertEquals(0, file.getTrackCount());
  }

  @Test
  void should_not_add_twice_the_same_track() {
    CueTrack track1 = new CueTrack(TrackType.AUDIO, "performer1", "title1");
    CueTrack track2 = new CueTrack(TrackType.AUDIO, "performer2", "title2");
    CueFile file = new CueFile("file", FileType.Audio.WAVE, track1, track2);

    for (CueTrack track : file) {
      assertThrows(IllegalArgumentException.class, () -> file.addTrack(track));
    }
  }

  @Test
  void should_stream() {
    CueFile file = new CueFile("file", FileType.Audio.WAVE);
    assertEquals(0, file.stream().count());

    file.addTrack(new CueTrack(TrackType.AUDIO, "performer1", "title1"));
    file.addTrack(new CueTrack(TrackType.AUDIO, "performer2", "title2"));
    assertEquals(2, file.stream().count());
  }

  @Test
  void should_give_iterator() {
    CueFile file = new CueFile("file", FileType.Audio.WAVE);
    CueIterator<CueTrack> it1 = file.iterator();
    assertFalse(it1.hasNext());

    CueTrack track = new CueTrack(TrackType.AUDIO, "performer1", "title1");
    file.addTrack(track);

    CueIterator<CueTrack> it2 = file.iterator();
    assertTrue(it2.hasNext());
    assertEquals(0, it2.nextIndex());
    assertEquals(track, it2.next());

    // for coverage
    assertTrue(it2.hasPrevious());
    assertEquals(0, it2.previousIndex());
    assertEquals(track, it2.previous());

    assertThrows(UnsupportedOperationException.class, it2::remove);
    assertThrows(UnsupportedOperationException.class, () -> it2.set(track));
    assertThrows(UnsupportedOperationException.class, () -> it2.add(track));
  }

  @Test
  void should_get_track_duration() throws IllegalTrackTypeException, IndexNotFoundException, NegativeDurationException {
    TimeCode timeCode1 = new TimeCode(10, 20, 30);
    TimeCode timeCode2 = new TimeCode(20, 30, 40);
    CueFile file = new CueFile("file", FileType.Audio.WAVE,
      new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_TRACK_START, timeCode1)),
      new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_TRACK_START, timeCode2))
    );

    file.setSizeAndDuration(new SizeAndDuration(Duration.ofMinutes(30), TimeCodeRounding.DOWN));
    assertEquals(Duration.ofMillis(610133L), file.getTrackDuration(0)); // duration is calculated from the frames diff, hence the 0.133s instead of 0.134s calculated from millis.
    assertEquals(Duration.ofMillis(569466L), file.getTrackDuration(1));
  }

  @Test
  void should_get_tracks_durations() throws IllegalTrackTypeException, IndexNotFoundException, NegativeDurationException {
    TimeCode timeCode1 = new TimeCode(10, 20, 30);
    TimeCode timeCode2 = new TimeCode(20, 30, 40);
    CueFile file = new CueFile("file", FileType.Audio.WAVE);
    CueTrack track1 = file.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_TRACK_START, timeCode1)));
    CueTrack track2 = file.addTrack(new CueTrack(TrackType.AUDIO, new CueIndex(CueIndex.INDEX_TRACK_START, timeCode2)));
    track2.setPreGap(TimeCode.TWO_SECONDS);
    track2.setPostGap(TimeCode.ONE_SECOND);

    file.setSizeAndDuration(new SizeAndDuration(Duration.ofMinutes(30), TimeCodeRounding.DOWN));
    Map<CueTrack, Duration> tracksDurations = file.getTracksDurations();
    assertEquals(2, tracksDurations.size());

    Iterator<Entry<CueTrack, Duration>> it = tracksDurations.entrySet().iterator();
    Entry<CueTrack, Duration> entry1 = it.next();
    Entry<CueTrack, Duration> entry2 = it.next();

    assertSame(track1, entry1.getKey());
    assertEquals(Duration.ofMillis(610133L), entry1.getValue()); // duration is calculated from the frames diff, hence the 0.133s instead of 0.134s calculated from millis.

    assertSame(track2, entry2.getKey());
    assertEquals(Duration.ofMillis(572466L), entry2.getValue());
  }

  @Test
  void should_give_tostring() {
    CueFile file = new CueFile("My File.WAV", FileType.Audio.WAVE);
    assertEquals("FILE \"My File.WAV\" WAVE", file.toString());
  }
}
