package eu.nonstatic.cue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@EqualsAndHashCode
public class CueTrack implements CueEntity, Comparable<CueTrack>, CueIterable<CueIndex> {

  public static final String KEYWORD = CueWords.TRACK;
  public static final int TRACK_ONE = 1;
  public static final String TYPE_AUDIO = "AUDIO"; //TODO are there other modes (MODE1, MODE2 ?)
  public static final Comparator<Integer> COMPARATOR = Comparator.nullsFirst(Comparator.naturalOrder());


  protected Integer number;
  private String type; // ex: AUDIO

  private String title;
  private String performer;
  private String songwriter;
  private String isrc;

  private TimeCode pregap;
  private final List<CueIndex> indexes;
  private TimeCode postgap;
  private final List<CueFlag> flags;

  private final List<CueRemark> remarks;
  private final List<CueOther> others;

  public CueTrack(Integer number, String type) {
    if (number != null) {
      setNumberOnce(number);
    }
    this.type = type;

    this.indexes = new ArrayList<>(2);
    this.flags = new ArrayList<>(0);
    this.remarks = new ArrayList<>();
    this.others = new ArrayList<>();
  }

  public CueTrack(Integer number, String type, CueIndex index) {
    this(number, type, List.of(index));
  }

  public CueTrack(Integer number, String type, Collection<? extends CueIndex> indexes) {
    this(number, type);
    this.indexes.addAll(indexes);
  }

  public CueTrack deepCopy() {
    return deepCopy(number);
  }

  public CueTrack deepCopy(Integer newNumber) {
    CueTrack newTrack = new CueTrack(newNumber, type);
    newTrack.title = title;
    newTrack.performer = performer;
    newTrack.songwriter = songwriter;
    newTrack.isrc = isrc;
    newTrack.pregap = pregap;
    indexes.forEach(index -> newTrack.indexes.add(index.deepCopy()));
    newTrack.postgap = postgap;
    flags.forEach(newTrack::addFlag);
    remarks.forEach(newTrack::addRemark);
    newTrack.others.addAll(others);
    return newTrack;
  }

  public boolean hasNumber() {
    return number != null;
  }

  public void setNumberOnce(int number) {
    if (number <= 0) {
      throw new IllegalArgumentException("Track number must be strictly positive");
    } else if (this.number != null) {
      throw new IllegalStateException("Track number already set to " + this.number);
    } else {
      this.number = number;
    }
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

  public CueIndex addIndex(CueIndex newIndex) {
    return addIndex(newIndex, false);
  }

  public synchronized CueIndex addIndex(CueIndex newIndex, boolean renumber) {
    if(indexes.contains(newIndex)) {
      throw new IllegalArgumentException("The track already contains this index");
    }

    if(newIndex.hasNumber() && renumber) {
      newIndex = newIndex.deepCopy(null);
    }

    int nextIndexNumber = getNextIndexNumber();

    if (newIndex.hasNumber()) {
      int newNumber = newIndex.getNumber();
      if (indexes.isEmpty() && newNumber != CueIndex.INDEX_PRE_GAP && newNumber != CueIndex.INDEX_TRACK_START) {
        throw new IllegalArgumentException("Cannot start track with number " + newNumber + ", has to be [0,1]");
      }

      if (newNumber > nextIndexNumber) {
        throw new IllegalArgumentException("Index number " + newNumber + " is out of range [" + CueIndex.INDEX_PRE_GAP + "," + nextIndexNumber + "]");
      } else {
        CueIndex newIndexCopy = newIndex.deepCopy();
        if (newNumber == nextIndexNumber) { // it's chaining
          indexes.add(newIndexCopy); // fast-track version of the else clause below
        } else { // now we're sure 0 =< newNumber =< lastNumber
          int i = 0;
          for (; i < indexes.size(); i++) {
            int currentNumber = indexes.get(i).getNumber();
            if (currentNumber == newNumber) {
              break;
            }
          }

          indexes.add(i, newIndexCopy);

          // shift the remaining tracks
          while (++i < indexes.size()) {
            indexes.get(i).number++;
          }
        }
        return newIndexCopy;
      }
    } else {
      newIndex.setNumberOnce(nextIndexNumber);
      indexes.add(newIndex); // it's OK to store it without copying because it's obviously not part of a file yet
      return newIndex;
    }
  }

  public CueIndex removeIndex(int number) {
    if (indexes.isEmpty()) {
      throw new IllegalArgumentException("Index list is empty");
    }

    int firstNumber = getFirstIndex().get().getNumber();
    int lastNumber = getLastIndex().get().getNumber();
    if (number < firstNumber || number > lastNumber) {
      throw new IllegalArgumentException("Index number " + number + " is out of range [" + firstNumber + "," + lastNumber + "]");
    } else {
      CueIndex targetIndex = null;

      Iterator<CueIndex> it = indexes.iterator();
      while (it.hasNext()) {
        targetIndex = it.next();
        if (targetIndex.getNumber() == number) {
          it.remove();
          break;
        }
      }
      // shift the remaining tracks
      it.forEachRemaining(index -> index.number--);

      return targetIndex;
    }
  }

  @Override
  public CueIterator<CueIndex> iterator() {
    return new CueIterator<>(indexes);
  }

  public int getIndexCount() {
    return indexes.size();
  }

  public void clearIndexes() {
    indexes.clear();
  }

  public synchronized List<CueFlag> getFlags() {
    return Collections.unmodifiableList(flags);
  }

  public synchronized void setFlags(Collection<CueFlag> flags) {
    clearFlags();
    this.flags.addAll(flags);
  }

  public boolean addFlag(CueFlag cueFlag) {
    return flags.add(cueFlag);
  }

  public void clearFlags() {
    flags.clear();
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


  public List<CueOther> getOthers() {
    return Collections.unmodifiableList(others);
  }

  public void addOther(CueOther other) {
    others.add(other);
  }

  public void clearOthers() {
    others.clear();
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
          .or(otherTrack::getStartIndex)
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
