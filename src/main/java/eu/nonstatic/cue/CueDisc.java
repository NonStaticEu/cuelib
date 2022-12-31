package eu.nonstatic.cue;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * https://en.wikipedia.org/wiki/Cue_sheet_(computing)
 * https://wiki.hydrogenaud.io/index.php?title=Cue_sheet
 * https://wiki.hydrogenaud.io/index.php?title=EAC_and_Cue_Sheets
 * Regards to Jeff Arnold
 */
@Getter @Setter
@EqualsAndHashCode
public class CueDisc implements CueIterable<CueFile> {

  private static final String UPC_EAN_REGEXP = "\\d{12,13}";
  private static final Pattern UPC_EAN_PATTERN = Pattern.compile(UPC_EAN_REGEXP);

  private final String path;
  private final Charset charset;

  private String title;
  private String performer;
  private String songwriter;
  private String catalog; // MCN/UPC
  private String cdTextFile;

  private final List<CueFile> files;
  private final List<CueRemark> remarks;
  private final List<CueOther> others;


  public CueDisc(String path, Charset charset) {
    this.path = path;
    this.charset = charset;
    this.files = new ArrayList<>();
    this.remarks = new ArrayList<>();
    this.others = new ArrayList<>();
  }

  public void setCatalog(String catalog) {
    if (!UPC_EAN_PATTERN.matcher(catalog).matches()) {
      throw new IllegalArgumentException("UPC/EAN/MCN must be [12-13] digits");
    }
    if (catalog.length() == 12) {
      catalog = '0' + catalog;
    }
    this.catalog = catalog;
  }

  public List<CueFile> getFiles() {
    return Collections.unmodifiableList(files);
  }

  /**
   * @return the first track on this file. It may not be track 1 on a cue sheet
   */
  public Optional<CueFile> getFirstFile() {
    return files.isEmpty() ? Optional.empty() : Optional.of(files.get(0));
  }

  public Optional<CueFile> getLastFile() {
    return files.isEmpty() ? Optional.empty() : Optional.of(files.get(files.size() - 1));
  }

  public int getFileCount() {
    return (int) files.stream().map(CueFile::getFile).distinct().count();
  }

  @Override
  public CueIterator<CueFile> iterator() {
    return new CueIterator<>(files);
  }

  public CueFile addFile(CueFile newFile) {
    return addFile(newFile, false);
  }


  public synchronized CueFile addFile(CueFile newFile, boolean renumber) {
    if(files.contains(newFile)) {
      throw new IllegalArgumentException("The disc already contains this file");
    }

    int nextTrackNumber = getNextTrackNumber();
    Optional<CueTrack> firstNewTrack = newFile.getFirstTrack();
    if (renumber || firstNewTrack.isEmpty() || firstNewTrack.get().getNumber() == nextTrackNumber) {
      CueFile newFileCopy = new CueFile(newFile.getFileAndFormat());
      for (CueTrack newTrack : newFile) {
        newFileCopy.addTrack(newTrack, renumber);
      }
      files.add(newFileCopy);
      return newFileCopy;
    } else {
      String latestTrackNumber = getLastFile().flatMap(CueFile::getLastTrack).map(t -> t.getNumber().toString()).orElse("<START>");
      throw new IllegalStateException("New file's first track " + firstNewTrack.get().getNumber() + " doesn't chain with the latest disc's track " + latestTrackNumber);
    }
  }

  public void clearFiles() {
    files.clear();
  }

  //TODO add Track i.s.o (or on top of) from CueFile

  public List<CueRemark> getRemarks() {
    return Collections.unmodifiableList(remarks);
  }

  public void addRemark(CueRemark remark) {
    remarks.add(remark);
  }

  public void clearRemarks() {
    remarks.clear();
  }

  public List<CueFile> splitFiles() {
    return files.stream().flatMap(file -> file.split().stream()).collect(Collectors.toList());
  }

  /**
   * Groups tracks per file, in case the file was split per track whereas it shouldn't have been It's assuming tracks are in the right order
   */
  public Collection<CueFile> groupTracks() {
    Map<FileAndFormat, CueFile> map = new LinkedHashMap<>();
    for (CueFile file : files) {
      CueFile groupFile = map.computeIfAbsent(file.getFileAndFormat(), CueFile::new);
      file.getTracks().forEach(groupFile::addTrack);
    }
    return map.values();
  }

  public boolean hasHiddenTrack() {
    return !files.isEmpty() && files.get(0).hasHiddenTrack();
  }

  public Optional<CueHiddenTrack> getHiddenTrack() {
    return files.stream().findFirst()
        .filter(CueFile::hasHiddenTrack) // ensures we have at least track 1 and index 0 here
        .map(file -> {
          CueTrack trackOne = file.getNumberOneTrack().get();
          CueIndex preGapIndex = trackOne.getPreGapIndex().get();
          CueIndex trackStartIndex = trackOne.getStartIndex()
              .orElseThrow(() -> new IllegalStateException(
                  "No index " + CueIndex.INDEX_TRACK_START + " on track " + CueTrack.TRACK_ONE + " to calculate hidden track duration."));
          Duration hiddenTrackDuration = preGapIndex.until(trackStartIndex);
          return new CueHiddenTrack(file, hiddenTrackDuration);
        });
  }


  public List<CueTrack> getTracks() {
    return files.stream().flatMap(file -> file.getTracks().stream()).collect(Collectors.toList());
  }

  public int getTrackCount() {
    return files.stream().mapToInt(CueFile::getTrackCount).sum();
  }

  /**
   * @return track 1
   */
  public Optional<CueTrack> getNumberOneTrack() {
    return getTrack(CueTrack.TRACK_ONE);
  }

  public Optional<CueTrack> getTrack(int number) {
    return files.stream().map(file -> file.getTrack(number))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  /**
   * @return first track of the disc. That is, track 1 if it exists.
   */
  public Optional<CueTrack> getFirstTrack() {
    return getNumberOneTrack();
  }

  public Optional<CueTrack> getLastTrack() {
    return files.stream().map(CueFile::getLastTrack)
      .filter(Optional::isPresent)
      .map(Optional::get)
        .findFirst();
  }


  /**
   * In the end the file inside a CueFile may be used several times across the disc.
   * Not sure this is legit in the cue format, but we allow it.
   */
  public CueTrack moveTrackBefore(int movingNumber, int beforeNumber) {
    int trackCount = getTrackCount();
    checkTrackRange("Moving number", movingNumber, trackCount);
    checkTrackRange("Before number", beforeNumber, trackCount);

    // What if we don't want to move?
    if(movingNumber == beforeNumber || movingNumber == beforeNumber-1) {
      return getTrack(movingNumber).get();
    }

    List<FileAndTrack> resorterList = new ArrayList<>(getTrackCount());
    for (CueFile cueFile : this) {
      FileAndFormat ff = cueFile.getFileAndFormat();
      for (CueTrack track : cueFile) {
        if(track.number != movingNumber) {
          if(track.number == beforeNumber) {
            FileAndTrack movingFileAndTrack = getFileAndTrack(movingNumber).get();
            resorterList.add(movingFileAndTrack);
          }
          resorterList.add(new FileAndTrack(ff, track));
        }
      }
    }

    return resetFiles(resorterList, movingNumber);
  }


  /**
   * In the end the file inside a CueFile may be used several times across the disc.
   * Not sure this is legit in the cue format, but we allow it.
   */
  public synchronized CueTrack moveTrackAfter(int movingNumber, int afterNumber) {
    int trackCount = getTrackCount();
    checkTrackRange("Moving number", movingNumber, trackCount);
    checkTrackRange("After number", afterNumber, trackCount);

    // What if we don't want to move?
    if(movingNumber == afterNumber || movingNumber == afterNumber+1) {
      return getTrack(movingNumber).get();
    }

    List<FileAndTrack> ftList = new ArrayList<>(getTrackCount());
    for (CueFile file : this) {
      FileAndFormat ff = file.getFileAndFormat();
      for (CueTrack track : file) {
        if(track.number != movingNumber) {
          ftList.add(new FileAndTrack(ff, track));
          if(track.number == afterNumber) {
            FileAndTrack movingFileAndTrack = getFileAndTrack(movingNumber).get();
            ftList.add(movingFileAndTrack);
          }
        }
      }
    }

    return resetFiles(ftList, movingNumber);
  }

  private CueTrack resetFiles(List<FileAndTrack> ftList, int movingNumber) {
    files.clear();
    CueTrack movedTrack = null;
    CueFile currentFile = null;
    FileAndFormat currentFf = null;
    for (FileAndTrack fileAndTrack : ftList) {
      if(!fileAndTrack.ff.equals(currentFf)) {
        currentFf = fileAndTrack.ff;
        currentFile = new CueFile(currentFf);
        files.add(currentFile);
      }
      CueTrack addedTrack = currentFile.addTrack(fileAndTrack.track, true);
      if(movedTrack == null && fileAndTrack.track.getNumber() == movingNumber) {
        movedTrack = addedTrack;
      }
    }
    return movedTrack;
  }



  private static void checkTrackRange(String varName, int trackNumber, int trackCount) {
    if(trackNumber < CueTrack.TRACK_ONE || trackNumber > trackCount) {
      throw new IllegalArgumentException(varName + ' ' + trackNumber + " is out of range [" + CueTrack.TRACK_ONE + "," + trackCount + "]");
    }
  }

  public int getNextTrackNumber() {
    return getLastFile()
        .map(cueTracks -> cueTracks.getNextTrackNumber().orElse(CueTrack.TRACK_ONE))
        .orElse(CueTrack.TRACK_ONE);
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


  @AllArgsConstructor
  private static class FileAndTrack {
    FileAndFormat ff;
    CueTrack track;
  }

  private Optional<FileAndTrack> getFileAndTrack(int number) {
    for (CueFile cueFile : this) {
      FileAndFormat ff = cueFile.getFileAndFormat();
      for (CueTrack cueTrack : cueFile) {
        if (cueTrack.number == number) {
          return Optional.of(new FileAndTrack(ff, cueTrack));
        }
      }
    }
    return Optional.empty();
  }
}
