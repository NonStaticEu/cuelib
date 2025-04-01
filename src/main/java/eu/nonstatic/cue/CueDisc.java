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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * https://en.wikipedia.org/wiki/Cue_sheet_(computing)
 * https://wiki.hydrogenaud.io/index.php?title=Cue_sheet
 * https://wiki.hydrogenaud.io/index.php?title=EAC_and_Cue_Sheets
 * https://www.gnu.org/software/ccd2cue/manual/html_node/CUE-sheet-format.html#CUE-sheet-format
 * https://github.com/libyal/libodraw/blob/main/documentation/CUE%20sheet%20format.asciidoc
 * Regards to Jeff Arnold
 */
@Getter @Setter
@EqualsAndHashCode
public class CueDisc implements CueIterable<CueFile> {

  public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
  private static final Pattern UPC_EAN_PATTERN = Pattern.compile("\\d{12,13}");

  // https://en.wikipedia.org/wiki/CD-ROM#Capacity
  // https://en.wikipedia.org/wiki/Mini_CD
  public static final Duration DURATION_8_CM = Duration.ofMinutes(21); // same density as 74 min CD
  public static final Duration DURATION_63_MIN = Duration.ofMinutes(63);
  public static final Duration DURATION_74_MIN = Duration.ofMinutes(74);
  public static final Duration DURATION_80_MIN = Duration.ofMinutes(80);
  public static final Duration DURATION_90_MIN = Duration.ofMinutes(90);
  public static final Duration DURATION_99_MIN = Duration.ofMinutes(99);

  protected static final Duration DURATION_SEEK_WINDOW = Duration.ofSeconds(2); // 150 frames
  public static final Duration DURATION_LEAD_IN  = DURATION_SEEK_WINDOW; // Durations above include this
  public static final Duration DURATION_LEAD_OUT = DURATION_SEEK_WINDOW; // Durations above don't include this

  private static final String RANGE_MESSAGE_TRACK_NUMBER = "Track number";


  private final String path;
  private Charset charset;

  private String title;
  private String performer;
  private String songwriter;
  private String catalog; // MCN/UPC
  private String cdTextFile;
  /**
   * First track number, may be > 1
   */
  private int firstTrackNumber;

  private final List<CueFile> files;
  private final List<CueRemark> remarks;
  private final List<CueOther> others;

  public CueDisc() {
    this(DEFAULT_CHARSET);
  }

  public CueDisc(Charset charset) {
    this(null, charset);
  }

  public CueDisc(String path, Charset charset) {
    this.path = path;
    this.charset = charset;
    this.files = new ArrayList<>(2);
    this.remarks = new ArrayList<>();
    this.others = new ArrayList<>();
    this.firstTrackNumber = CueTrack.TRACK_ONE;
  }

  public CueDisc(String path, Charset charset, CueFile... files) {
    this(path, charset, List.of(files));
  }

  public CueDisc(String path, Charset charset, Collection<? extends CueFile> files) {
    this(path, charset);
    files.forEach(this::addFile);
  }

  public void setTitle(String title) {
    CueTools.validateCdText("title", title);
    this.title = title;
  }

  public void setPerformer(String performer) {
    CueTools.validateCdText("performer", performer);
    this.performer = performer;
  }

  public void setSongwriter(String songwriter) {
    CueTools.validateCdText("songwriter", songwriter);
    this.songwriter = songwriter;
  }

  public void setCatalog(String catalog) {
    if(catalog != null) {
      catalog = catalog.replace(" ", "");

      if (!UPC_EAN_PATTERN.matcher(catalog).matches()) {
        throw new IllegalArgumentException("UPC/EAN/MCN must be [12-13] digits: https://en.wikipedia.org/wiki/Universal_Product_Code");
      }
      if (catalog.length() == 12) {
        catalog = '0' + catalog;
      }
    }

    this.catalog = catalog;
  }

  public void setFirstTrackNumber(int firstTrackNumber) {
    CueTools.validateTrackRange("firstTrackNumber", firstTrackNumber, CueTrack.TRACK_ONE, CueTrack.TRACK_MAX);
    this.firstTrackNumber = firstTrackNumber;
  }

  @Override
  public CueIterator<CueFile> iterator() {
    return new CueIterator<>(files);
  }

  public List<CueFile> getFiles() {
    return Collections.unmodifiableList(files);
  }

  /**
   * @return the first track on this file. It may not be track 1 on a cue sheet
   */
  public CueFile getFirstFile() {
    return files.isEmpty() ? null : files.get(0);
  }

  public CueFile getLastFile() {
    return files.isEmpty() ? null : files.get(files.size() - 1);
  }

  public int getFileCount() {
    return files.size();
  }

  public int getDistinctFileCount() {
    return files.stream().map(CueFile::getFile).collect(Collectors.toSet()).size();
  }

  public CueFile addFile(CueFile file) {
    return addFile(files.size(), file);
  }

  public synchronized CueFile addFile(int idx, CueFile file) {
    // no dupe checking, one may use several times the same file and several times the same tracks
    CueTools.validateRange("File index", idx, 0, getFileCount());

    CueFile fileCopy = new CueFile(file.fileReference);
    for (CueTrack track : file) {
      fileCopy.addTrack(track);
    }
    addFileUnsafe(idx, fileCopy);

    return fileCopy;
  }

  protected void addFileUnsafe(CueFile file) {
    files.add(file);
  }

  protected void addFileUnsafe(int idx, CueFile file) {
    files.add(idx, file);
  }

  public void clearFiles() {
    files.clear();
  }

  public List<CueRemark> getRemarks() {
    return Collections.unmodifiableList(remarks);
  }

  public void addRemark(CueRemark remark) {
    remarks.add(remark);
  }

  public void clearRemarks() {
    remarks.clear();
  }


  public boolean hasHiddenTrack() {
    CueTrack firstTrack = getFirstTrack();
    return firstTrack != null && firstTrack.hasPreGapIndex();
  }

  public CueHiddenTrack getHiddenTrack() throws IndexNotFoundException {
    if(hasHiddenTrack()) {
      FileAndTrack fileAndTrack = chunk(firstTrackNumber); // the concept of a hidden track when firstTrackNumber > 1 beats me but it seems possible

      CueTrack firstTrack = fileAndTrack.track;
      CueIndex firstTrackStartIndex = firstTrack.getStartIndex();
      if(firstTrackStartIndex == null) {
        throw new IndexNotFoundException("No index " + CueIndex.INDEX_TRACK_START + " on track " + firstTrackNumber + " to calculate hidden track duration", CueIndex.INDEX_TRACK_START);
      }
      CueIndex preGapIndex = firstTrack.getPreGapIndex();
      Duration duration = preGapIndex.until(firstTrackStartIndex);
      return new CueHiddenTrack(fileAndTrack, duration);
    } else {
      return null;
    }
  }

  public List<CueTrack> getTracks() {
    return renumberTracks();
  }

  public Map<Integer, CueTrack> getNumberedTracks() {
    return getTracks().stream().collect(Collectors.toMap(CueTrack::getNumber, Function.identity()));
  }

  public int getTrackCount() {
    return files.stream().mapToInt(CueFile::getTrackCount).sum();
  }

  public int getNextTrackNumber() {
    return getTrackCount()+1;
  }

  /**
   * @param trackNumber track number 1-based
   * @return
   */
  public CueTrack getTrack(int trackNumber) {
    CueTools.validateTrackRange(RANGE_MESSAGE_TRACK_NUMBER, trackNumber, firstTrackNumber, getTrackCount());

    return Optional.ofNullable(getTrackUnsafe(trackNumber))
        .orElseThrow(() -> new TrackNotFoundException(trackNumber));
  }

  /**
   * @return Convenience method to get track 1. Caution, the first track of a CD may be > 1
   * @throws IllegalArgumentException if there's no track one.
   */
  //FIXLE renalme and rework: should be getTrack(firstTrackNumber), even if we consent to keeping a methos with this name here
  public CueTrack getTrackNumberOne() {
    return getTrack(CueTrack.TRACK_ONE);
  }

  /**
   * @return first track of the disc. That is, track 1 if it exists, else null.
   */
  public CueTrack getFirstTrack() {
    return getTrackUnsafe(firstTrackNumber);
  }

  private CueTrack getTrackUnsafe(int trackNumber) {
    int number = firstTrackNumber;
    for (CueFile file : files) {
      for (CueTrack track : file) {
        track.number = number; // renumbering on the fly
        if(trackNumber == number) {
          return track;
        }
        number++;
      }
      file.renumberingNecessary = false;
    }
    return null;
  }

  public CueTrack getLastTrack() {
    List<CueTrack> tracks = renumberTracks(); // renumbering feels better than getting the last track of each file as long as it has tracks
    return tracks.isEmpty() ? null : tracks.get(tracks.size()-1);
  }

  public CueTrack removeTrack(int trackNumber) {
    CueTools.validateTrackRange(RANGE_MESSAGE_TRACK_NUMBER, trackNumber, firstTrackNumber, getTrackCount());

    CueTrack result = null;
    int fileStart = firstTrackNumber;
    for (CueFile file : files) {
      int trackCount = file.getTrackCount();
      if (fileStart + file.getTrackCount() > trackNumber) {
        result = file.removeTrack(trackNumber - fileStart);
        break;
      } else {
        fileStart += trackCount;
      }
    }
    renumberTracks(); // optional but then the disc is accurate
    return result;
  }

  /**
   * In the end the file inside a CueFile may be used several times across the disc.
   * Not sure this is legit in the cue format, but we allow it.
   */
  public CueTrack moveTrackBefore(int movingNumber, int beforeNumber) {
    Map<Integer, FileAndTrack> fileAndTracks = split();
    int trackCount = fileAndTracks.size();

    CueTools.validateTrackRange("Moving number", movingNumber, firstTrackNumber, trackCount);
    CueTools.validateTrackRange("Before number", beforeNumber, firstTrackNumber, trackCount);

    FileAndTrack movingFileAndTrack = fileAndTracks.get(movingNumber);
    // What if we don't want to move?
    if(movingNumber != beforeNumber && movingNumber != beforeNumber-1) {
      List<FileAndTrack> newFileAndTracks = new ArrayList<>(trackCount);
      fileAndTracks.forEach((number, fileAndTrack) -> {
        if (number != movingNumber) {
          if (number == beforeNumber) {
            newFileAndTracks.add(movingFileAndTrack);
          }
          newFileAndTracks.add(fileAndTrack);
        }
      });

      repackFiles(newFileAndTracks);
      renumberTracks();
    }
    return movingFileAndTrack.track;
  }

  /**
   * In the end the file inside a CueFile may be used several times across the disc.
   * Not sure this is legit in the cue format, but we allow it.
   */
  public synchronized CueTrack moveTrackAfter(int movingNumber, int afterNumber) {
    Map<Integer, FileAndTrack> fileAndTracks = split();
    int trackCount = fileAndTracks.size();

    CueTools.validateTrackRange("Moving number", movingNumber, firstTrackNumber, trackCount);
    CueTools.validateTrackRange("After number", afterNumber, firstTrackNumber, trackCount);

    FileAndTrack movingFileAndTrack = fileAndTracks.get(movingNumber);
    // What if we don't want to move?
    if(movingNumber != afterNumber && movingNumber != afterNumber+1) {
      List<FileAndTrack> newFileAndTracks = new ArrayList<>(trackCount);
      fileAndTracks.forEach((number, fileAndTrack) -> {
        if (number != movingNumber) {
          newFileAndTracks.add(fileAndTrack);
          if (number == afterNumber) {
            newFileAndTracks.add(movingFileAndTrack);
          }
        }
      });

      repackFiles(newFileAndTracks);
      renumberTracks();
    }
    return movingFileAndTrack.track;
  }

  protected FileAndTrack chunk(int trackNumber) throws TrackNotFoundException {
    CueTools.validateTrackRange(RANGE_MESSAGE_TRACK_NUMBER, trackNumber, firstTrackNumber, getTrackCount());

    int number = firstTrackNumber;
    for (CueFile file : files) {
      FileReference ft = file.fileReference;
      for (CueTrack track : file) {
        if (number == trackNumber) {
          return new FileAndTrack(ft, track);
        }
        number++;
      }
    }

    throw new TrackNotFoundException(trackNumber); // unreachable
  }

  /**
   * Groups tracks per file, in case the file was split per track whereas it shouldn't have been.
   * Tracks are also renumbered on the fly
   */
  public void repackFiles() {
    repackFiles(split().values()); // split does renumbering too
  }

  private void repackFiles(Collection<FileAndTrack> ftCollection) {
    this.files.clear();
    this.files.addAll(join(ftCollection));
  }

  public boolean isRenumberingNecessary() {
    return files.stream().anyMatch(file -> file.renumberingNecessary);
  }

  public List<CueTrack> renumberTracks() {
    var result = new ArrayList<CueTrack>();
    int number = firstTrackNumber;
    for (CueFile file : files) {
      for (CueTrack track : file) {
        track.number = number++;
        result.add(track);
      }
      file.renumberingNecessary = false;
    }
    return result;
  }

  /**
   * Note it also renumbers tracks due to its return layout
   */
  private Map<Integer, FileAndTrack> split() {
    var fileAndTracks = new LinkedHashMap<Integer, FileAndTrack>(); // keeping order in case we just want the values
    int number = firstTrackNumber;
    for (CueFile file : files) {
      for (FileAndTrack fileAndTrack : file.split()) {
        fileAndTrack.track.number = number; // to be homogenous with the map's keys
        fileAndTracks.put(number, fileAndTrack);
        number++;
      }
    }
    return fileAndTracks;
  }

  private synchronized List<CueFile> join(Collection<FileAndTrack> ftList) {
    List<CueFile> newFiles = new ArrayList<>();

    CueFile currentFile = null;
    FileReference currentFf = null;

    for (FileAndTrack fileAndTrack : ftList) {
      if(!fileAndTrack.fileReference.equals(currentFf)) {
        currentFf = fileAndTrack.fileReference;
        currentFile = new CueFile(currentFf);
        newFiles.add(currentFile);
      }
      CueTrack track = fileAndTrack.track;
      currentFile.addTrackUnsafe(track);
    }

    return newFiles;
  }


  public List<CueIndex> getIndexes() {
    return files.stream().flatMap(file -> file.getIndexes().stream()).collect(Collectors.toList());
  }

  public int getIndexCount() {
    return files.stream().mapToInt(CueFile::getIndexCount).sum();
  }

  public List<CueOther> getOthers() {
    return Collections.unmodifiableList(others);
  }

  public void addOther(CueOther other) {
    others.add(other);
  }

  public void clearOthers() {
    others.clear();
  }

  /**
   * Checks the disc consistency (before writing to file for instance)
   * Layout will be optimized if necessary
   * Tracks will be renumbered to avoid rack-related error message to be misleading
   * Note: timecodes are set within their respective files, no overall chaining check is necessary
   * @param options
   * @return
   */
  public CueIssues checkConsistency(CueWriteOptions options) {
    repackFiles(); // optimization + renumbering, else some track-related error messages might be misleading

    var issues = new CueIssues();

    try {
      int trackCount = getTrackCount();
      int minTrackCount = options.isNoTrackAllowed() ? 0 : 1;
      CueTools.validateRange("Tracks count", trackCount, minTrackCount, CueTrack.TRACK_MAX);
      CueTools.validateRange("Track max", firstTrackNumber + trackCount - 1, minTrackCount, CueTrack.TRACK_MAX);
    } catch (IllegalArgumentException e) {
      issues.add(e);
    }

    files.forEach(file -> issues.addAll(file.checkConsistency(options.isOrderedTimeCodes())));

    // check min track duration
    Duration minTrackDuration = options.getMinTrackDuration();
    if(minTrackDuration != null) {
      Map<CueTrack, Duration> tracksDurations = getTracksDurations();

      boolean seenFirst = false;
      for (Map.Entry<CueTrack, Duration> entry : tracksDurations.entrySet()) {
        // Adding lead-in time on first track. Originally the min track duration is meant to always have a seek window margin of 2 seconds,
        // even when rewinding to the first index of the first track (which is then 2 seconds minimum). Lead-in is there even if there's a hidden track.
        Duration trackDuration = entry.getValue();
        if(!seenFirst) {
          seenFirst = true;
          trackDuration = trackDuration.plus(CueDisc.DURATION_LEAD_IN);
        }

        if (trackDuration.compareTo(minTrackDuration) < 0) {
          Integer trackNumber = entry.getKey().getNumber();
          issues.add(String.format("Track %s duration %s is below %s", trackNumber, trackDuration, minTrackDuration));
        }
      }
    }

    // check disc size
    Long burningLimit = options.getBurningLimit();
    if(burningLimit != null) {
      long sizeOnDisc = getSizeOnDisc();
      if (sizeOnDisc > burningLimit) {
        issues.add(new TooMuchDataException(burningLimit, sizeOnDisc));
      }
    }

    return issues;
  }

  /**
   * Computes the tracks durations *without* lead-in/out
   * @return the track's duration.
   * if the file is not audio, there's no entries for its tracks, so even though tracks are ordered tey may not be sequential.
   * if a track's diff with the next one is negative, then the returned value is the diff with the end of the file instead
   * @throws IllegalTrackTypeException if at least one track is not audio despite being in an audio file
   * @throws IndexNotFoundException if the needed index(es) for the computations do(es)n't exist
   * @throws NegativeDurationException if the file's duration is not sufficient, making that last track's duration negative which is illogical (not enough data to play/burn)
   */
  public Map<CueTrack, Duration> getTracksDurations() {
    var tracksDurations = new LinkedHashMap<CueTrack, Duration>(); // linked to preserve order
    for (CueFile file : files) {
      tracksDurations.putAll(file.getTracksDurations());
    }
    return tracksDurations;
  }

  /**
   * @return bytes on disc including lead-in, excluding lead-out
   */
  public long getSizeOnDisc() {
    // Initial mandatory lead-in as per specification EVEN if there is a cuefile pregap or a hidden track
    // (the lead in is actually silence with the TOC as subcode)
    long totalSize = SizeAndDuration.getCompactDiscBytesFrom(DURATION_LEAD_IN, TimeCodeRounding.CLOSEST);

    for (CueFile file : files) {
      SizeAndDuration sizeAndDuration = file.getSizeAndDuration();
      if(sizeAndDuration != null) {
        totalSize += sizeAndDuration.size;
      } else {
        throw new NullPointerException(file.getFile() + ": missing size");
      }

      // Gaps (index 00 to index 01) are supposed to be stored in the files - they may be silence or not - but there's still those artificial gaps to account for.
      // You shouldn't have preGap and Index 00 together in a cue sheet, makes no sense. Consistency check makes sure of it.
      for (CueTrack cueTrack : file) {
        TimeCode preGap = cueTrack.getPreGap();
        if(preGap != null) {
          totalSize += SizeAndDuration.getCompactDiscBytesFrom(preGap);
        }
        TimeCode postGap = cueTrack.getPostGap();
        if(postGap != null) {
          totalSize += SizeAndDuration.getCompactDiscBytesFrom(postGap);
        }
      }
    }

    // Not entirely sure we should account for lead-out.

    return totalSize;
  }

  public Duration getDuration() {
    Duration totalDuration = Duration.ZERO;

    for (CueFile file : files) {
      if(file.isAudio()) {
        SizeAndDuration sizeAndDuration = file.getSizeAndDuration();
        if(sizeAndDuration != null && sizeAndDuration.duration != null) {
          totalDuration = totalDuration.plus(sizeAndDuration.duration);
        } else {
          throw new NullPointerException(file.getFile() + ": missing duration");
        }
      }
    }

    return totalDuration;
  }
}
