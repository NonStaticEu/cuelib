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

import static eu.nonstatic.cue.CueTools.unquote;

import eu.nonstatic.cue.CueTrack.TimeCodeValidation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@EqualsAndHashCode
public class CueFile implements CueEntity, CueIterable<CueTrack>, FileReferable {

  public static final String KEYWORD = CueWords.FILE;

  private static final String RANGE_MESSAGE_TRACK_INDEX = "Track index";
  private static final CueWriteOptions TO_STRING_OPTIONS = CueWriteOptions.builder().fullPaths(true).build();

  protected FileReference fileReference;

  private final ArrayList<CueTrack> tracks;
  protected boolean renumberingNecessary;

  // FileAndFormat isn't a first class citizen, hence ctor visibility
  CueFile(FileReference fileReference) {
    this.fileReference = fileReference;
    this.tracks = new ArrayList<>(12);
  }

  public CueFile(String file, FileType type) {
    this(new FileReference(file, type));
  }

  public CueFile(String file, FileType type, CueTrack... tracks) {
    this(file, type, List.of(tracks));
  }

  public CueFile(String file, FileType type, Collection<? extends CueTrack> tracks) {
    this(file, type);
    tracks.forEach(this::addTrack);
  }

  public CueFile(File file, CueSheetContext context) throws IOException {
    this(file.toPath(), context);
  }

  public CueFile(File file, FileType.Data type, CueSheetContext context) throws IOException {
    this(file.toPath(), type, context);
  }

  public CueFile(Path file, CueSheetContext context) throws IOException {
    this(new FileReference(file, context));
  }

  public CueFile(Path file, FileType.Data type, CueSheetContext context) throws IOException {
    this(new FileReference(file, type, context));
  }


  @Override
  public SizeAndDuration getSizeAndDuration() {
    return fileReference.getSizeAndDuration();
  }

  public void setSizeAndDuration(SizeAndDuration sizeAndDuration) {
    fileReference.setSizeAndDuration(sizeAndDuration);
  }

  @Override
  public String getFile() {
    return fileReference.getFile();
  }

  @Override
  public FileType getType() {
    return fileReference.getType();
  }

  public CueFile deepCopy() {
    CueFile fileCopy = new CueFile(fileReference);
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
   * private not to be confusing between track numbers (in CueDisc) and indexes (here)
   */
  private CueTrack getTrack(int idx) {
    try {
      return tracks.get(idx);
    } catch(IndexOutOfBoundsException e) {
      throw new TrackNotFoundException(e);
    }
  }

  /**
   * @return the first track on this file. It won't be track 1 on a cue sheet if this is not the first file
   */
  public CueTrack getFirstTrack() {
    return tracks.isEmpty() ? null : getTrack(0);
  }

  public CueTrack getLastTrack() {
    return tracks.isEmpty() ? null : getTrack(tracks.size() - 1);
  }

  public int getTrackCount() {
    return tracks.size();
  }

  public void clearTracks() {
    tracks.clear();
  }

  protected List<FileAndTrack> split() {
    return tracks.stream().map(track -> new FileAndTrack(fileReference, track)).collect(Collectors.toList());
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
   * @return the track's duration. if the track's diff with the next one is negative, then the returned value is the diff with the end of the file instead
   * @throws IllegalTrackTypeException if this track or the next one is not audio
   * @throws IndexNotFoundException if the needed index(es) for the computation do(es)n't exist
   * @throws NegativeDurationException if the last track 's duration is requested and the file's duration is not sufficient, making that last track's duration negative which is illogical (not enough data to play/burn)
   */
  public Duration getTrackDuration(int idx) throws IllegalTrackTypeException, IndexNotFoundException, NegativeDurationException {
    return getTrackDuration(idx, true);
  }

  /**
   * @param idx
   * @param allowDisorderedTimeCodes
   * @return the track's duration
   * @throws IllegalTrackTypeException if this track or the next one is not audio
   * @throws IndexNotFoundException if the needed index(es) for the computation do(es)n't exist
   * @throws NegativeDurationException if allowDisorderedTimeCodes is false and the track's length is negative,
   * or if the last track 's duration is requested and the file's duration is not sufficient, making that last track's duration negative which is illogical (not enough data to play/burn)
   */
  public Duration getTrackDuration(int idx, boolean allowDisorderedTimeCodes) throws IllegalTrackTypeException, IndexNotFoundException, NegativeDurationException {
    CueTrack track = getTrack(idx); // first because I want range check

    if(isAudio()) {
      Duration fileDuration = Optional.ofNullable(fileReference.sizeAndDuration).map(sd -> sd.duration).orElse(null);
      int trackCount = tracks.size();
      CueTools.validateRange(RANGE_MESSAGE_TRACK_INDEX, idx, 0, trackCount - 1);

      if (idx < trackCount - 1) {
        CueTrack nextTrack = getTrack(idx + 1);
        return track.until(nextTrack, fileDuration, allowDisorderedTimeCodes);
      } else if (fileDuration != null) { // last track
        return track.until(null, fileDuration);
      } else {
        throw new IllegalArgumentException("No duration has been specified to get the last track's length for " + fileReference.file);
      }
    } else {
      return null;
    }
  }

  /**
   * Computes the tracks durations *without* lead-in/out
   * @return the track's duration.
   * if the file is not audio the returned map is empty
   * if the track's diff with the next one is negative, then the returned value is the diff with the end of the file instead
   * @throws IllegalTrackTypeException if at least one track is not audio despite being in an audio file
   * @throws IndexNotFoundException if the needed index(es) for the computations do(es)n't exist
   * @throws NegativeDurationException if the file's duration is not sufficient, making that last track's duration negative which is illogical (not enough data to play/burn)
   */
  public Map<CueTrack, Duration> getTracksDurations() throws IllegalTrackTypeException, IndexNotFoundException, NegativeDurationException {
    return getTracksDurations(true);
  }

  /**
   * Computes the tracks durations *without* lead-in/out
   * @param allowDisorderedTimeCodes  to allow unordered timecodes
   * @return the tracks' durations. if the file is not audio the returned map is empty
   * @throws IllegalTrackTypeException if at least one track is not audio despite being in an audio file
   * @throws IndexNotFoundException if the needed index(es) for the computations do(es)n't exist
   * @throws NegativeDurationException if allowDisorderedTimeCodes is false and timecodes are inconsistent (meaning at least one track length turns out to be negative)
   */
  public Map<CueTrack, Duration> getTracksDurations(boolean allowDisorderedTimeCodes) throws IllegalTrackTypeException, IndexNotFoundException, NegativeDurationException {
    if (!tracks.isEmpty() && isAudio()) { // tracks emptiness first in case fileAndType isn't set
      Duration fileDuration = Optional.ofNullable(fileReference.sizeAndDuration)
          .map(sd -> sd.duration)
          .orElseThrow(() -> new IllegalArgumentException("No duration has been specified to get the last track's length for " + fileReference.file));

      var tracksDurations = new LinkedHashMap<CueTrack, Duration>(); // linked to preserve order
      Iterator<CueTrack> tit = tracks.iterator();
      CueTrack currentTrack = tit.next();

      while (tit.hasNext()) {
        CueTrack nextTrack = tit.next();
        Duration trackDuration = currentTrack.until(nextTrack, fileDuration, allowDisorderedTimeCodes);
        tracksDurations.put(currentTrack, trackDuration);
        currentTrack = nextTrack;
      }

      tracksDurations.put(currentTrack, currentTrack.until(null, fileDuration));
      return tracksDurations;
    } else {
      return Map.of();
    }
  }

  public List<CueIndex> getIndexes() {
    return tracks.stream().flatMap(track -> track.getIndexes().stream()).collect(Collectors.toList());
  }

  public int getIndexCount() {
    return tracks.stream().mapToInt(CueTrack::getIndexCount).sum();
  }

  /**
   * This method doesn't check tracks' numbers consistency. This can only be done at the disc level.
   * Note: in theory a cue may not respect timecodes ordering (next > previous)
   */
  public CueIssues checkConsistency(boolean orderedTimeCodes) {
    var issues = new CueIssues();

    tracks.forEach(track -> issues.addAll(track.checkConsistency(false)));

    // Make sure the first index of the first track has timecode 00:00:00.
    Optional.ofNullable(getFirstTrack())
      .map(CueTrack::getFirstIndex) // tracks consistency made sure there's >= 1 index per track, just avoiding NPE
      .filter(firstIndex -> !TimeCode.ZERO_SECOND.equals(firstIndex.getTimeCode()))
      .ifPresent(cueIndex -> issues.add(String.format("File %s doesn't have %s as its first track's first index", fileReference.file, TimeCode.ZERO_SECOND)));


    if(orderedTimeCodes) {
      CueIndex latestIndex = null;
      for (CueTrack track : getTracks()) {
        TimeCodeValidation timeCodeValidation = track.checkTimeCodesChaining(latestIndex);
        issues.add(timeCodeValidation.issue);
        latestIndex = timeCodeValidation.latest;
      }
    }
    return issues;
  }

  public String toSheetLine(CueWriteOptions options) {
    String file = options.isFullPaths() ? getFile() : getFileName();
    return String.format("%s \"%s\" %s", KEYWORD, file, getType().getValue());
  }

  @Override
  public String toString() {
    return toSheetLine(TO_STRING_OPTIONS);
  }


  /**
   * @param fileAndFormat "\"my file.wav\" WAVE" if the cue sheet contains "FILE \"my file.wav\" WAVE"
   * @param context
   * @return
   */
  static FileReference parse(@NonNull String fileAndFormat, CueSheetContext context) throws IOException {
    String fileName;
    FileType fileType;
    int sep = fileAndFormat.lastIndexOf(' ');
    if (sep >= 0 && fileAndFormat.indexOf('"', sep) < 0) { // checking we aren't going to cut a part of the file name
      fileName = unquote(fileAndFormat.substring(0, sep).trim());
      String type = fileAndFormat.substring(sep + 1);
      fileType = FileType.of(type);
    } else {
      fileName = unquote(fileAndFormat.trim());
      fileType = FileReference.getTypeByFileName(fileName);
    }

    // parent may be null if we're loading the file from a stream or the network
    // fileName may be a filename or a complete path
    return fromParentDir(fileName, fileType, context);
  }

  private static FileReference fromParentDir(String fileOrFileName, FileType fileType, CueSheetContext context) throws IOException {
    Path dir = context.getParent();
    if(dir == null ) { // let's set what we can
      return new FileReference(fileOrFileName, fileType);
    }

    Path filePath = Paths.get(fileOrFileName);
    if(filePath.getNameCount() == 1) { // just a file name
      filePath = dir.resolve(fileOrFileName);
    }

    return new FileReference(filePath, fileType, context);
  }
}
