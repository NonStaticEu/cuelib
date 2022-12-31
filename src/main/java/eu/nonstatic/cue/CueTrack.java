package eu.nonstatic.cue;

import lombok.*;

import java.time.Duration;
import java.util.*;

@Getter
@Setter
@EqualsAndHashCode
public class CueTrack implements CueEntity, Comparable<CueTrack>, CueIterable<CueIndex> {

  public static final String KEYWORD = CueWords.TRACK;
  public static final int TRACK_ONE = 1;
  public static final String TYPE_AUDIO = "AUDIO"; //TODO other modes (MODE1, MODE2 ?)
  public static final Comparator<Integer> COMPARATOR = Comparator.naturalOrder();


  private Integer number;
  private String type; // ex: AUDIO

  private String title;
  private String performer;
  private String songwriter;
  private String isrc;

  private TimeCode pregap;
  private final ArrayList<CueIndex> indexes;
  private TimeCode postgap;
  private List<CueFlag> flags;

  private final List<CueRemark> remarks = new ArrayList<>();
  private final List<CueOther> others = new ArrayList<>();

  public CueTrack(Integer number, String type) {
    if (number != null) {
      setNumber(number);
    }
    this.type = type;
    this.indexes = new ArrayList<>(2);
  }

  public CueTrack(Integer number, String type, CueIndex index) {
    this(number, type, List.of(index));
  }

  public CueTrack(Integer number, String type, Collection<? extends CueIndex> indexes) {
    this(number, type);
    this.indexes.addAll(indexes);
  }


  public boolean hasNumber() {
    return number != null;
  }

  public void setNumber(int number) {
    if (number <= 0) {
      throw new IllegalArgumentException("Track number must be strictly positive");
    } else if (this.number != null) {
      throw new IllegalStateException("Track number already set to " + this.number);
    } else {
      this.number = number;
    }
  }

  void incrNumberUnsafe() {
    number++;
  }


  public List<CueIndex> getIndexes() {
    return Collections.unmodifiableList(indexes);
  }

  public Optional<CueIndex> getIndex(int number) {
    for (CueIndex index : indexes) {
      int indexNumber = index.getNumber();
      if (indexNumber == number) {
        return Optional.of(index);
      } else if (indexNumber > number) {
        break;
      }
    }
    return Optional.empty();
  }

  public Optional<CueIndex> getFirstIndex() {
    return indexes.isEmpty() ? Optional.empty() : Optional.of(indexes.get(0));
  }

  public Optional<CueIndex> getLastIndex() {
    return indexes.isEmpty() ? Optional.empty() : Optional.of(indexes.get(indexes.size() - 1));
  }

  public int getNextIndexNumber() {
    return getLastIndex()
        .map(track -> track.getNumber() + 1)
        .orElse(CueIndex.INDEX_TRACK_START); // this is opinionated
  }

  public Optional<CueIndex> getPreGapIndex() {
    return getIndex(CueIndex.INDEX_PRE_GAP);
  }

  public Optional<CueIndex> getStartIndex() {
    return getIndex(CueIndex.INDEX_TRACK_START);
  }


  public synchronized void addIndex(CueIndex newIndex) {
    int nextNumber = getNextIndexNumber();

    if (newIndex.hasNumber()) {
      int newNumber = newIndex.getNumber();
      if (indexes.isEmpty() && newNumber != CueIndex.INDEX_PRE_GAP && newNumber != CueIndex.INDEX_TRACK_START) {
        throw new IllegalArgumentException("Cannot start track with number " + newNumber + ", has to be [0,1]");
      }

      if (newNumber == nextNumber) {
        indexes.add(newIndex);
      } else if (newNumber > nextNumber) {
        throw new IllegalArgumentException("Index number " + newNumber + " is out of range [" + CueIndex.INDEX_PRE_GAP + "," + nextNumber + "]");
      } else { // now we're sure 1 =< newNumber =< lastNumber
        int i = 0;
        for (; i < indexes.size(); i++) {
          int currentNumber = indexes.get(i).getNumber();
          if (currentNumber == newNumber) {
            break;
          }
        }

        indexes.add(i, newIndex);

        // shift the remaining tracks
        while (++i < indexes.size()) {
          indexes.get(i).incrNumberUnsafe();
        }
      }
    } else {
      newIndex.setNumber(nextNumber);
      indexes.add(newIndex);
    }
  }

  //TODO remove index

  @Override
  public CueIterator<CueIndex> iterator() {
    return new CueIterator<>(indexes);
  }

  public int getIndexCount() {
    return indexes.size();
  }

  public List<CueRemark> getRemarks() {
    return Collections.unmodifiableList(remarks);
  }

  public void addRemark(CueRemark remark) {
    remarks.add(remark);
  }


  public List<CueOther> getOthers() {
    return Collections.unmodifiableList(others);
  }

  public void addOther(CueOther other) {
    others.add(other);
  }

  public boolean hasHiddenTrack() {
    return number != null && number == TRACK_ONE && getPreGapIndex().isPresent();
  }

  @Override
  public int compareTo(CueTrack otherTrack) {
    return COMPARATOR.compare(number, otherTrack.number);
  }


  /**
   * @return time until the start of another track, else till the end of the file
   */
  Duration until(CueTrack otherTrack, Duration fileDuration) {
    Duration trackDuration;

    CueIndex trackStart = getStartIndex()
        .orElseThrow(() -> new IllegalStateException("No index " + CueIndex.INDEX_TRACK_START + " on track " + number));
    if (otherTrack != null) {
      CueIndex otherStart = otherTrack.getPreGapIndex()
          .or(() -> otherTrack.getStartIndex())
          .orElseThrow(() -> new IllegalStateException(
              "No index " + CueIndex.INDEX_PRE_GAP + " or " + CueIndex.INDEX_TRACK_START + " on track " + otherTrack.getNumber()));

      trackDuration = trackStart.until(otherStart);
      if (trackDuration.isNegative()) {
        throw new IllegalStateException(
            "Difference between track " + number + " (" + trackStart + ") and track " + otherTrack.number + " (" + otherStart + ") is negative.");
      }
    } else {
      trackDuration = fileDuration.minusMillis(trackStart.getTimeMillis());
      if (trackDuration.isNegative()) {
        throw new IllegalArgumentException("fileDuration was too short " + fileDuration + " for track " + number);
      }
    }
    return trackDuration;
  }

  @Override
  public String toSheetLine() {
    return String.format("%s %02d %s", KEYWORD, number, type);
  }

  @Override
  public String toString() {
    return toSheetLine();
  }
}
