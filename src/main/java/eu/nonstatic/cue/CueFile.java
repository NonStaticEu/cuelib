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

import eu.nonstatic.cue.CueTrack.TimeCodeValidation;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
public class CueFile implements CueEntity, CueIterable<CueTrack> {

  public static final String KEYWORD = CueWords.FILE;

  private static final String RANGE_MESSAGE_TRACK_INDEX = "Track index";

  @Getter(AccessLevel.PACKAGE)
  private FileAndType fileAndType;

  private final ArrayList<CueTrack> tracks;
  protected boolean renumberingNecessary;

  // FileAndFormat isn't a first class citizen, hence ctor visibility
  CueFile(FileAndType fileAndType) {
    this.fileAndType = fileAndType;
    this.tracks = new ArrayList<>(12);
  }

  public CueFile(String file, String format) {
    this(new FileAndType(file, format));
  }

  public CueFile(String file, String format, CueTrack... tracks) {
    this(file, format, List.of(tracks));
  }

  public CueFile(String file, String format, Collection<? extends CueTrack> tracks) {
    this(file, format);
    tracks.forEach(this::addTrack);
  }

  public String getFile() {
    return fileAndType.getFile();
  }

  public String getFormat() {
    return fileAndType.getType();
  }

  public CueFile deepCopy() {
    CueFile fileCopy = new CueFile(fileAndType);
    tracks.forEach(track -> fileCopy.addTrackUnsafe(track.deepCopy()));
    return fileCopy;
  }

  @Override
  public CueIterator<CueTrack> iterator() {
    return new CueIterator<>(tracks);
  }

  public List<CueTrack> getTracks() {
    return Collections.unmodifiableList(tracks);
  }

  /**
   * @return the first track on this file. It may not be track 1 on a cue sheet
   */
  public CueTrack getFirstTrack() {
    return tracks.isEmpty() ? null : tracks.get(0);
  }

  public CueTrack getLastTrack() {
    return tracks.isEmpty() ? null : tracks.get(tracks.size() - 1);
  }

  public int getTrackCount() {
    return tracks.size();
  }

  public void clearTracks() {
    tracks.clear();
  }

  protected List<FileAndTrack> split() {
    return tracks.stream().map(track -> new FileAndTrack(fileAndType, track)).collect(Collectors.toList());
  }

  /**
   * It is not possible to check whether we have max 99 tracks, it has to be done at the disc level before writing
   * Not checking we have a pregap on 2 files for instance, that might be the case across several files anyway. Better check consistency at disc level.
   */
  public CueTrack addTrack(CueTrack track) {
    return addTrack(tracks.size(), track);
  }

  public CueTrack addTrack(int idx, CueTrack track) {
    if(tracks.contains(track)) {
      throw new IllegalArgumentException("The file already contains this track");
    }
    CueTools.validateRange(RANGE_MESSAGE_TRACK_INDEX, idx, 0, tracks.size());

    renumberingNecessary = true;
    CueTrack trackCopy = track.deepCopy();
    addTrackUnsafe(idx, trackCopy);
    return trackCopy;
  }

  protected void addTrackUnsafe(CueTrack track) {
    tracks.add(track);
  }

  protected void addTrackUnsafe(int idx, CueTrack track) {
    tracks.add(idx, track);
  }

  public CueTrack removeTrack(int idx) {
    CueTools.validateRange(RANGE_MESSAGE_TRACK_INDEX, idx, 0, tracks.size()-1);
    return tracks.remove(idx);
  }

  /**
   * The cue file doesn't give the last track's ending time-code, so we need to know its run length.
   * @param idx
   * @param fileDuration The cue file doesn't give the last track's ending time-code, so we need to know its run length.
   * @return the track's duration. if the track's diff with the next one is negative, then the returned value is the diff with the end of the file instead
   * @throws IllegalTrackTypeException if this track or the next one is not audio
   * @throws IndexNotFoundException if the needed index(es) for the computation do(es)n't exist
   * @throws NegativeDurationException if the last track 's duration is requested and the file's duration is not sufficient, making that last track's duration negative which is illogical (not enough data to play/burn)
   */
  public Duration getTrackDuration(int idx, Duration fileDuration) throws IllegalTrackTypeException, IndexNotFoundException, NegativeDurationException {
    return getTrackDuration(idx, fileDuration, true);
  }

  /**
   * @param idx
   * @param fileDuration The cue file doesn't give the last track's ending time-code, so we need to know its run length.
   * @param allowDisorderedTimeCodes
   * @return the track's duration
   * @throws IllegalTrackTypeException if this track or the next one is not audio
   * @throws IndexNotFoundException if the needed index(es) for the computation do(es)n't exist
   * @throws NegativeDurationException if allowDisorderedTimeCodes is false and the track's length is negative,
   * or if the last track 's duration is requested and the file's duration is not sufficient, making that last track's duration negative which is illogical (not enough data to play/burn)
   */
  public Duration getTrackDuration(int idx, Duration fileDuration, boolean allowDisorderedTimeCodes) throws IllegalTrackTypeException, IndexNotFoundException, NegativeDurationException {
    int trackCount = tracks.size();
    CueTools.validateRange(RANGE_MESSAGE_TRACK_INDEX, idx, 0, trackCount-1);

    CueTrack track = tracks.get(idx);
    if(idx < trackCount-1) {
      CueTrack nextTrack = tracks.get(idx+1);
      return track.until(nextTrack, fileDuration, allowDisorderedTimeCodes);
    } else { // last track
      return track.until(null, fileDuration);
    }
  }

  /**
   * Computes the tracks durations
   * @param fileDuration The cue file doesn't give the last track's ending time-code, so we need to know its run length.
   * @return the track's duration. if the track's diff with the next one is negative, then the returned value is the diff with the end of the file instead
   * @throws IllegalTrackTypeException if at least one track is not audio
   * @throws IndexNotFoundException if the needed index(es) for the computations do(es)n't exist
   * @throws NegativeDurationException if the file's duration is not sufficient, making that last track's duration negative which is illogical (not enough data to play/burn)
   */
  public Map<CueTrack, Duration> getTracksDurations(Duration fileDuration) throws IllegalTrackTypeException, IndexNotFoundException, NegativeDurationException {
    return getTracksDurations(fileDuration, true);
  }

  /**
   * Computes the tracks durations
   * @param fileDuration The cue file doesn't give the last track's ending time-code, so we need to know its run length.
   * @param allowDisorderedTimeCodes
   * @return the tracks' durations
   * @throws IllegalTrackTypeException if at least one track is not audio
   * @throws IndexNotFoundException if the needed index(es) for the computations do(es)n't exist
   * @throws NegativeDurationException if allowDisorderedTimeCodes is false and timecodes are inconsistent (meaning at least one track length turns out to be negative)
   */
  public Map<CueTrack, Duration> getTracksDurations(Duration fileDuration, boolean allowDisorderedTimeCodes) throws IllegalTrackTypeException, IndexNotFoundException, NegativeDurationException {
    if (tracks.isEmpty()) {
      return Map.of();
    } else {
      var map = new LinkedHashMap<CueTrack, Duration>();
      Iterator<CueTrack> tit = tracks.iterator();
      CueTrack currentTrack = tit.next();

      while (tit.hasNext()) {
        CueTrack nextTrack = tit.next();
        map.put(currentTrack, currentTrack.until(nextTrack, fileDuration, allowDisorderedTimeCodes));
        currentTrack = nextTrack;
      }

      map.put(currentTrack, currentTrack.until(null, fileDuration));
      return map;
    }
  }


  public List<CueIndex> getIndexes() {
    return tracks.stream().flatMap(track -> track.getIndexes().stream()).collect(Collectors.toList());
  }

  public int getIndexCount() {
    return tracks.stream().mapToInt(CueTrack::getIndexCount).sum();
  }

  /**
   * This method doesn't check tracks' numbers consistency, or whether there is at most one pregap. This can only be done at the disc level.
   * Note: in theory a cue may not respect timecodes ordering (next > previous)
   */
  public List<String> checkConsistency(boolean orderedTimeCodes) {
    var issues = new ArrayList<String>();
    tracks.forEach(track -> issues.addAll(track.checkConsistency(false)));

    if(orderedTimeCodes) {
      CueIndex latestIndex = null;
      for (CueTrack track : getTracks()) {
        TimeCodeValidation timeCodeValidation = track.checkTimeCodesChaining(latestIndex);
        issues.addAll(timeCodeValidation.issues);
        latestIndex = timeCodeValidation.latest;
      }
    }
    return issues;
  }

  @Override
  public String toSheetLine() {
    return String.format("%s \"%s\" %s", KEYWORD, getFile(), getFormat());
  }

  @Override
  public String toString() {
    return toSheetLine();
  }
}
