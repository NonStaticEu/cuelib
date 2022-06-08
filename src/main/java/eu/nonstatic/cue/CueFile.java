package eu.nonstatic.cue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CueFile extends FileAndFormat implements CueEntity, CueIterable<CueTrack> {

  public static final String KEYWORD = CueWords.FILE;

  private final ArrayList<CueTrack> tracks;

  CueFile(FileAndFormat ff) {
    this(ff.file, ff.format); // tracks may remain empty, some files may be declared as extra resources
  }

  public CueFile(String file, String format) {
    super(file, format);
    this.tracks = new ArrayList<>(12);
  }

  public CueFile(String file, String format, CueTrack track) {
    this(file, format, List.of(track));
  }

  public CueFile(String file, String format, Collection<? extends CueTrack> tracks) {
    this(file, format);
    this.tracks.addAll(tracks);
  }

  FileAndFormat getFileAndFormat() {
    return new FileAndFormat(file, format);
  }

  public List<CueTrack> getTracks() {
    return Collections.unmodifiableList(tracks);
  }

  public Optional<CueTrack> getTrack(int number) {
    for (CueTrack track : tracks) {
      int trackNumber = track.getNumber();
      if (trackNumber == number) {
        return Optional.of(track);
      } else if (trackNumber > number) {
        break;
      }
    }
    return Optional.empty();
  }

  public Optional<CueTrack> getNumberOneTrack() {
    return getTrack(CueTrack.TRACK_ONE);
  }

  /**
   * @return the first track on this file. It may not be track 1 on a cue sheet
   */
  public Optional<CueTrack> getFirstTrack() {
    return tracks.isEmpty() ? Optional.empty() : Optional.of(tracks.get(0));
  }

  public Optional<CueTrack> getLastTrack() {
    return tracks.isEmpty() ? Optional.empty() : Optional.of(tracks.get(tracks.size() - 1));
  }

  public int getNextTrackNumber() {
    return getLastTrack()
        .map(track -> track.getNumber() + 1)
        .orElse(
            CueTrack.TRACK_ONE); // if tracks is empty we have no way to tell if there aren't other files/track before this one, thus if 1 is a legit number
  }

  public int getTrackCount() {
    return tracks.size();
  }

  /**
   * @return splits this CueFile into as many CueFiles as tracks
   */
  public List<CueFile> splitTracks() {
    return tracks.stream().map(track -> new CueFile(file, format, track)).collect(Collectors.toList());
  }

  public synchronized void addTrack(CueTrack newTrack) {
    int nextNumber = getNextTrackNumber();

    if (newTrack.hasNumber()) {
      int newNumber = newTrack.getNumber();
      if (newNumber == nextNumber) {
        tracks.add(newTrack);
      } else if (newNumber > nextNumber) {
        throw new IllegalArgumentException("Track number " + newNumber + " is out of range [" + CueTrack.TRACK_ONE + "," + nextNumber + "]");
      } else { // now we're sure 1 =< newNumber =< lastNumber
        int i = 0;
        for (; i < tracks.size(); i++) {
          int currentNumber = tracks.get(i).getNumber();
          if (currentNumber == newNumber) {
            break;
          }
        }

        tracks.add(i, newTrack);

        // shift the remaining tracks
        while (++i < tracks.size()) {
          tracks.get(i).incrNumberUnsafe();
        }
      }
    } else {
      newTrack.setNumber(nextNumber);
      tracks.add(newTrack);
    }
  }

  public CueTrack removeTrack(int number) {
    if (tracks.isEmpty()) {
      throw new IllegalArgumentException("Track list is empty");
    }

    int lastNumber = getLastTrack().get().getNumber();
    if (number < CueTrack.TRACK_ONE || number > lastNumber) {
      throw new IllegalArgumentException("Track number " + number + " is out of range [" + CueTrack.TRACK_ONE + "," + lastNumber + "]");
    } else {
      CueTrack track = null;

      Iterator<CueTrack> it = tracks.iterator();
      while (it.hasNext()) {
        track = it.next();
        if (track.getNumber() == number) {
          it.remove();
          break;
        }
      }
      // shift the remaining tracks
      it.forEachRemaining(CueTrack::decrNumberUnsafe);

      return track;
    }
  }

  @Override
  public CueIterator<CueTrack> iterator() {
    return new CueIterator<>(tracks);
  }

  /**
   * @return true if we have a track 1 with index 0
   */
  public boolean hasHiddenTrack() {
    return getNumberOneTrack().map(CueTrack::hasHiddenTrack).orElse(false);
  }

  public Map<CueTrack, Duration> getTracksDurations(Duration fileDuration) {
    var map = new LinkedHashMap<CueTrack, Duration>();

    if (!tracks.isEmpty()) {
      java.util.Iterator<CueTrack> tit = tracks.iterator();
      CueTrack currentTrack = tit.next();

      while (tit.hasNext()) {
        CueTrack nextTrack = tit.next();
        map.put(currentTrack, currentTrack.until(nextTrack, null));
        currentTrack = nextTrack;
      }

      map.put(currentTrack, currentTrack.until(null, fileDuration));
    }
    return map;
  }

  public Optional<Duration> getTrackDuration(int number, Duration fileDuration) {
    java.util.Iterator<CueTrack> tit = tracks.iterator();
    while (tit.hasNext()) {
      CueTrack track = tit.next();
      if (track.getNumber() == number) {
        CueTrack nextTrack = tit.hasNext() ? tit.next() : null;
        return Optional.of(track.until(nextTrack, fileDuration));
      } else if (track.getNumber() > number) {
        break;
      }
    }
    return Optional.empty();
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
