package eu.nonstatic.cue;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class CueFile implements CueEntity {

  private final String file;
  private final String format; // MP3, AIFF, WAVE, FLAC, BIN

  private final List<CueTrack> tracks = new ArrayList<>(); // may remain empty, some files may be declared as extra resources

  public CueFile(String file, String format) {
    this.file = file;
    this.format = format;
  }

  public List<CueTrack> getTracks() {
    return Collections.unmodifiableList(tracks);
  }

  public void addTrack(CueTrack track) {
    tracks.add(track);
  }

  public List<CueIndex> getIndexes() {
    return tracks.stream().flatMap(track -> track.getIndexes().stream()).collect(Collectors.toList());
  }
}
