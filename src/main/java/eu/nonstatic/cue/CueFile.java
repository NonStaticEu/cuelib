package eu.nonstatic.cue;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class CueFile implements CueEntity {

  public static final String KEYWORD = CueWords.FILE;

  private final String file;
  private final String format; // MP3, AIFF, WAVE, FLAC, BIN

  private final List<CueTrack> tracks;

  CueFile(String file, String format) {
    this(file, format, new ArrayList<>()); // tracks may remain empty, some files may be declared as extra resources
  }

  public CueFile(String file, String format, CueTrack track) {
    this(file, format, List.of(track));
  }

  public CueFile(String file, String format, List<CueTrack> tracks) {
    this.file = file;
    this.format = format;
    this.tracks = new ArrayList<>(tracks);
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
    tracks.add(track);
  }

  public List<CueIndex> getIndexes() {
    return tracks.stream().flatMap(track -> track.getIndexes().stream()).collect(Collectors.toList());
  }

  public int getIndexCount() {
    return tracks.stream().mapToInt(CueTrack::getIndexCount).sum();
  }

  @Override
  public String toString() {
    return String.format("%s \"%s\" %s", KEYWORD, file, format);
  }
}
