package eu.nonstatic.cue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
public class CueFile implements CueEntity, CueIterable<CueTrack> {

  public static final String KEYWORD = CueWords.FILE;

  @Getter(AccessLevel.PACKAGE)
  private FileAndFormat fileAndFormat;

  private final ArrayList<CueTrack> tracks;

  // FileAndFormat isn't a first class citizen, hence ctor visibility
  CueFile(FileAndFormat fileAndFormat) {
    this.fileAndFormat = fileAndFormat;
    this.tracks = new ArrayList<>(12);
  }

  public CueFile(String file, String format) {
    this(new FileAndFormat(file, format));
  }

  public CueFile(String file, String format, CueTrack track) {
    this(file, format, List.of(track));
  }

  public CueFile(String file, String format, Collection<? extends CueTrack> tracks) {
    this(file, format);
    this.tracks.addAll(tracks);
  }

  public String getFile() {
    return fileAndFormat.getFile();
  }

  public String getFormat() {
    return fileAndFormat.getFormat();
  }

  public CueFile deepCopy() {
    CueFile newFile = new CueFile(fileAndFormat);
    tracks.forEach(track -> newFile.tracks.add(track.deepCopy()));
    return newFile;
  }

  public List<CueTrack> getTracks() {
    return Collections.unmodifiableList(tracks);
  }

  public CueTrack getTrack(int number) {
    if(!tracks.isEmpty()) {
      int firstNumber = getFirstTrack().getNumber();
      int lastNumber = getLastTrack().getNumber();
      if (firstNumber <= number && number <= lastNumber) {
        for (CueTrack track : tracks) {
          if (track.getNumber() == number) {
            return track;
          }
        }
      }
      // else no exception because we may be calling from a CueDisc throughout all its CueFiles
    }
    return null;
  }

  public CueTrack getNumberOneTrack() {
    return getTrack(CueTrack.TRACK_ONE);
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

  /**
   * If tracks is empty we have no way to tell if there aren't other files/track before this one, thus can't tell whether 1 would be a legit number
   * @return the next track number (notwithstanding whichever other CueFile might be behind this one)
   */
  public OptionalInt getNextTrackNumber() {
    return getLastTrack()
        .stream()
        .mapToInt(track -> track.getNumber() + 1)
        .findAny();
  }

  public int getTrackCount() {
    return tracks.size();
  }

  public void clearTracks() {
    tracks.clear();
  }

  /**
   * @return splits this CueFile into as many CueFiles as tracks
   */
  public List<CueFile> split() {
    return tracks.stream().map(track -> new CueFile(getFile(), getFormat(), track)).collect(Collectors.toList());
  }

  public CueTrack addTrack(CueTrack newTrack) {
    return addTrack(newTrack, false);
  }


  public synchronized CueTrack addTrack(CueTrack newTrack, boolean renumber) {
    if(tracks.contains(newTrack)) {
      throw new IllegalArgumentException("The file already contains this track");
    }

    if(newTrack.hasNumber() && renumber) {
      newTrack = newTrack.deepCopy(null);
    }

    // TODO not satisfying, one may be able to inject several tracks 1 in several files
    int nextTrackNumber = getNextTrackNumber().orElse(CueTrack.TRACK_ONE);

    if (newTrack.hasNumber()) {
      int newNumber = newTrack.getNumber();
      if (newNumber > nextTrackNumber) {
        throw new IllegalArgumentException("Track number " + newNumber + " is out of range [" + CueTrack.TRACK_ONE + "," + nextTrackNumber + "]");
      } else {
        CueTrack newTrackCopy = newTrack.deepCopy();
        if (newNumber == nextTrackNumber) { // it's chaining
          tracks.add(newTrackCopy); // fast-track version of the else clause below
        } else { // now we're sure 1 =< newNumber =< lastNumber
          int t = 0;
          for (; t < tracks.size(); t++) {
            int currentNumber = tracks.get(t).getNumber();
            if (currentNumber == newNumber) {
              break;
            }
          }

          tracks.add(t, newTrackCopy);

          // shift the remaining tracks
          while (++t < tracks.size()) {
            tracks.get(t).number++;
          }
        }
        return newTrackCopy;
      }
    } else {
      newTrack.setNumberOnce(nextTrackNumber);
      tracks.add(newTrack); // it's OK to store it without copying because it's obviously not part of a file yet
      return newTrack;
    }
  }

  public CueTrack removeTrack(int number) {
    if (tracks.isEmpty()) {
      throw new IllegalArgumentException("Track list is empty");
    }

    int lastNumber = getLastTrack().getNumber();
    if (number < CueTrack.TRACK_ONE || number > lastNumber) {
      throw new IllegalArgumentException("Track number " + number + " is out of range [" + CueTrack.TRACK_ONE + "," + lastNumber + "]");
    } else {
      CueTrack targetTrack = null;

      Iterator<CueTrack> it = tracks.iterator();
      while (it.hasNext()) {
        targetTrack = it.next();
        if (targetTrack.getNumber() == number) {
          it.remove();
          break;
        }
      }
      // shift the remaining tracks
      it.forEachRemaining(track -> track.number--);

      return targetTrack;
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
    CueTrack numberOneTrack = getNumberOneTrack();
    return numberOneTrack != null && numberOneTrack.hasHiddenTrack();
  }

  public Map<CueTrack, Duration> getTracksDurations(Duration fileDuration) {
    var map = new LinkedHashMap<CueTrack, Duration>();

    if (!tracks.isEmpty()) {
      Iterator<CueTrack> tit = tracks.iterator();
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

  /**
   * The cue file doesn't give the last track's ending time-code, so we need to know its run length.
   * @param number
   * @param fileDuration for the last track
   * @return
   */
  public Duration getTrackDuration(int number, Duration fileDuration) {
    Iterator<CueTrack> tit = tracks.iterator();
    while (tit.hasNext()) {
      CueTrack track = tit.next();
      if (track.getNumber() == number) {
        CueTrack nextTrack = tit.hasNext() ? tit.next() : null;
        return track.until(nextTrack, fileDuration);
      } else if (track.getNumber() > number) { // number must be in a file preceding this one
        break;
      }
    } // number must be in a file following this one
    throw new IllegalArgumentException("Track " + number + " isn't part of the file " + fileAndFormat);
  }

  public List<CueIndex> getIndexes() {
    return tracks.stream().flatMap(track -> track.getIndexes().stream()).collect(Collectors.toList());
  }

  public int getIndexCount() {
    return tracks.stream().mapToInt(CueTrack::getIndexCount).sum();
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
