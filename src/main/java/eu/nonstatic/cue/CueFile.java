package eu.nonstatic.cue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@EqualsAndHashCode
public class CueFile extends FileAndFormat implements CueEntity {

  public static final String KEYWORD = CueWords.FILE;

  private final List<CueTrack> tracks;

  CueFile(FileAndFormat ff) {
    this(ff.file, ff.format, new ArrayList<>()); // tracks may remain empty, some files may be declared as extra resources
  }

  public CueFile(String file, String format, CueTrack track) {
    this(file, format, List.of(track));
  }

  public CueFile(String file, String format, List<CueTrack> tracks) {
    super(file, format);
    this.tracks = new ArrayList<>(tracks);
  }

  FileAndFormat getFileAndFormat() {
    return new FileAndFormat(file, format);
  }

  public List<CueTrack> getTracks() {
    return Collections.unmodifiableList(tracks);
  }

  public int getTrackCount() {
    return tracks.size();
  }

  public List<CueFile> splitTracks() {
    return tracks.stream().map(track -> new CueFile(file, format, track)).collect(Collectors.toList());
  }

  public void addTrack(CueTrack track) {
    // TODO check numbering
    tracks.add(track);
  }

  public boolean hasHiddenTrack() {
    return !tracks.isEmpty() && tracks.get(0).hasHiddenTrack();
  }


  public List<CueIndex> getIndexes() {
    return tracks.stream().flatMap(track -> track.getIndexes().stream()).collect(Collectors.toList());
  }

  public int getIndexCount() {
    return tracks.stream().mapToInt(CueTrack::getIndexCount).sum();
  }

  @Override
  public String toSheetLine() {
    return String.format("%s \"%s\" %s", KEYWORD, file, format);
  }

  @Override
  public String toString() {
    return toSheetLine();
  }
}
