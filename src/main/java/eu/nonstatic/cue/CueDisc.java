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
  private static final String RANGE_MESSAGE_TRACK_NUMBER = "Track number";

  private final String path;
  private Charset charset;

  private String title;
  private String performer;
  private String songwriter;
  private String catalog; // MCN/UPC
  private String cdTextFile;

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

    CueFile fileCopy = new CueFile(file.getFileAndType());
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
    CueTrack trackOne = getFirstTrack();
    return trackOne != null && trackOne.hasPreGap();
  }

  public CueHiddenTrack getHiddenTrack() throws IndexNotFoundException {
    if(hasHiddenTrack()) {
      FileAndTrack fileAndTrack = chunk(CueTrack.TRACK_ONE);

      CueTrack trackOne = fileAndTrack.track;
      CueIndex trackOneStartIndex = trackOne.getStartIndex();
      if(trackOneStartIndex == null) {
        throw new IndexNotFoundException("No index " + CueIndex.INDEX_TRACK_START + " on track " + CueTrack.TRACK_ONE + " to calculate hidden track duration", CueIndex.INDEX_TRACK_START);
      }
      CueIndex preGapIndex = trackOne.getPreGapIndex();
      Duration duration = preGapIndex.until(trackOneStartIndex);
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
    CueTools.validateTrackRange(RANGE_MESSAGE_TRACK_NUMBER, trackNumber, getTrackCount());

    return Optional.ofNullable(getTrackUnsafe(trackNumber))
        .orElseThrow(() -> new IllegalArgumentException(Integer.toString(trackNumber)));
  }

  /**
   * @return track 1
   * @throws IllegalArgumentException if there's no track one.
   */
  public CueTrack getTrackNumberOne() {
    return getTrack(CueTrack.TRACK_ONE);
  }

  /**
   * @return first track of the disc. That is, track 1 if it exists, else null.
   */
  public CueTrack getFirstTrack() {
    return getTrackUnsafe(CueTrack.TRACK_ONE);
  }

  private CueTrack getTrackUnsafe(int trackNumber) {
    int number = CueTrack.TRACK_ONE;
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
    CueTools.validateTrackRange(RANGE_MESSAGE_TRACK_NUMBER, trackNumber, getTrackCount());

    CueTrack result = null;
    int fileStart = CueTrack.TRACK_ONE;
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

    CueTools.validateTrackRange("Moving number", movingNumber, trackCount);
    CueTools.validateTrackRange("Before number", beforeNumber, trackCount);

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

    CueTools.validateTrackRange("Moving number", movingNumber, trackCount);
    CueTools.validateTrackRange("After number", afterNumber, trackCount);

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


  protected FileAndTrack chunk(int trackNumber) {
    CueTools.validateTrackRange(RANGE_MESSAGE_TRACK_NUMBER, trackNumber, getTrackCount());

    int number = CueTrack.TRACK_ONE;
    for (CueFile file : files) {
      FileAndType ff = file.getFileAndType();
      for (CueTrack track : file) {
        if (number == trackNumber) {
          return new FileAndTrack(ff, track);
        }
        number++;
      }
    }

    throw new IllegalArgumentException(Integer.toString(trackNumber)); // unreachable
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
    int number = CueTrack.TRACK_ONE;
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
    int number = CueTrack.TRACK_ONE;
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
    FileAndType currentFf = null;

    for (FileAndTrack fileAndTrack : ftList) {
      if(!fileAndTrack.ff.equals(currentFf)) {
        currentFf = fileAndTrack.ff;
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
  public List<String> checkConsistency(CueSheetOptions options) {
    repackFiles(); // optimization + renumbering, else some track-related error messages might be misleading

    var issues = new ArrayList<String>();

    try {
      CueTools.validateRange("Tracks count", getTracks().size(), options.isNoTrackAllowed() ? 0 : 1, CueTrack.TRACK_MAX);
    } catch (IllegalArgumentException e) {
      issues.add(e.getMessage());
    }

    files.forEach(file -> issues.addAll(file.checkConsistency(options.isOrderedTimeCodes())));

    // checking whether there's at most one pregap, on track 1
    boolean firstSeen = false;
    for (CueTrack track : getTracks()) {
      if(!firstSeen) {
        firstSeen = true;
      } else if(track.hasPreGap()) {
        issues.add("Track " + track.number + " has a pregap. Only track " + CueTrack.TRACK_ONE + " may have one");
      }
    }
    return issues;
  }
}
