package eu.nonstatic.cue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * https://en.wikipedia.org/wiki/Cue_sheet_(computing)
 * https://wiki.hydrogenaud.io/index.php?title=Cue_sheet
 * https://wiki.hydrogenaud.io/index.php?title=EAC_and_Cue_Sheets
 * Regards to Jeff Arnold
 */
@Getter
@Setter
@EqualsAndHashCode
public class CueDisc implements CueIterable<CueFile> {

  public static final String UPC_EAN_REGEXP = "\\d{12,13}";
  private static final Pattern UPC_EAN_PATTERN = Pattern.compile(UPC_EAN_REGEXP);

  private final String path;
  private final Charset charset;

  private String title;
  private String performer;
  private String songwriter;
  private String catalog; // MCN/UPC
  private String cdTextFile;

  private final List<CueFile> files = new ArrayList<>();
  private final List<CueRemark> remarks = new ArrayList<>();
  private final List<CueOther> others = new ArrayList<>();


  public CueDisc(String path, Charset charset) {
    this.path = path;
    this.charset = charset;
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
    return new CueIterator(files);
  }

  public CueFile addFile(CueFile newFile) {
    return addFile(newFile, false);
  }


  public synchronized CueFile addFile(CueFile newFile, boolean renumber) {
    if(files.contains(newFile)) {
      throw new IllegalArgumentException("The file already contains this track");
    }

    int nextTrackNumber = getNextTrackNumber();
    Optional<CueTrack> firstNewTrack = newFile.getFirstTrack();
    if (renumber || firstNewTrack.isEmpty() || firstNewTrack.get().getNumber() == nextTrackNumber) {
      CueFile newFileCopy = new CueFile(newFile.toFileAndFormat());
      for (CueTrack newTrack : newFile) {
        newFileCopy.addTrack(newTrack, renumber);
      }
      files.add(newFileCopy);
      return newFileCopy;
    } else {
      String latestTrackNumber = getLastFile().flatMap(file -> file.getLastTrack()).map(t -> t.getNumber().toString()).orElse("<START>");
      throw new IllegalStateException("New file's first track " + firstNewTrack.get().getNumber() + " doesn't chain with the latest disc's track " + latestTrackNumber);
    }
  }


  public List<CueRemark> getRemarks() {
    return Collections.unmodifiableList(remarks);
  }

  public void addRemark(CueRemark remark) {
    remarks.add(remark);
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
      CueFile groupFile = map.computeIfAbsent(file.toFileAndFormat(), ff -> new CueFile(ff));
      file.getTracks().forEach(cueTrack -> groupFile.addTrack(cueTrack));
    }
    return map.values();
  }

  public boolean hasHiddenTrack() {
    return !files.isEmpty() && files.get(0).hasHiddenTrack();
  }

  public Optional<CueHiddenTrack> getHiddenTrack() {
    return files.stream().findFirst()
        .filter(file -> file.hasHiddenTrack()) // ensures we have at least track 1 and index 0 here
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
}
