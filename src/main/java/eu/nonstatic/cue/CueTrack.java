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

import lombok.*;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter @Setter
@EqualsAndHashCode
public class CueTrack implements CueEntity, CueIterable<CueIndex> {

  private static final Pattern ISRC_PATTERN = Pattern.compile("([A-Z]{2})\\-?([A-Z0-9]{3})\\-?(\\d{2})\\-?(\\d{5})"); // dashes should be unnecessary but there's lots of cue oddities

  public static final String KEYWORD = CueWords.TRACK;
  public static final int TRACK_ONE = 1;
  public static final int TRACK_MAX = 99;
  public static final Duration DURATION_PREGAP_DEFAULT = CueDisc.DURATION_SEEK_WINDOW;
  public static final Duration DURATION_MIN = DURATION_PREGAP_DEFAULT.plus(CueDisc.DURATION_SEEK_WINDOW); // important at the time of the red book, not anymore

  public static final Comparator<Integer> COMPARATOR = Comparator.nullsFirst(Comparator.naturalOrder());


  @Setter(AccessLevel.NONE)
  protected Integer number; // holds the number READ FROM FILE. DO NOT rely on it to identify tracks. Else it's computed on the fly unless you use renumberTracks()

  private String type; // eg: AUDIO, MODE1/2352, etc

  private String title;
  private String performer;
  private String songwriter;
  private String isrc; // should have a CCOOOYYSSSSS format

  private TimeCode preGap;  // should be a duration but let's keep a timecode to avoid rounding errors
  private final List<CueIndex> indexes;
  private TimeCode postGap; // should be a duration but let's keep a timecode to avoid rounding errors
  private final Set<CueFlag> flags;

  private final List<CueRemark> remarks;
  private final List<CueOther> others;


  public CueTrack(String type) {
    this.type = Objects.requireNonNull(type, "type");
    this.indexes = new ArrayList<>(2);
    this.flags = new LinkedHashSet<>(0); // keeping order
    this.remarks = new ArrayList<>();
    this.others = new ArrayList<>();
  }

  public CueTrack(Integer number, String type) {
    this(type);
    this.number = number;
  }

  public CueTrack(String type, String performer, String title) {
    this(type);
    setPerformerAndTitle(performer, title);
  }

  public CueTrack(String type, CueIndex... indexes) {
    this(type, List.of(indexes));
  }

  public CueTrack(String type, Collection<? extends CueIndex> indexes) {
    this(type);
    indexes.forEach(this::addIndex);
  }

  public CueTrack deepCopy() {
    CueTrack trackCopy = new CueTrack(type);
    trackCopy.number = number;
    trackCopy.title = title;
    trackCopy.performer = performer;
    trackCopy.songwriter = songwriter;
    trackCopy.isrc = isrc;
    trackCopy.preGap = preGap;
    indexes.forEach(index -> trackCopy.addIndexUnsafe(index.deepCopy()));
    trackCopy.postGap = postGap;
    flags.forEach(trackCopy::addFlag);
    remarks.forEach(trackCopy::addRemark);
    trackCopy.others.addAll(others);
    return trackCopy;
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

  public void setPerformerAndTitle(String performer, String title) {
    setPerformer(performer);
    setTitle(title);
  }

  public void setIsrc(String isrc) {
    Matcher matcher = ISRC_PATTERN.matcher(isrc);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("ISRC must use the CCOOOYYSSSSS format: https://en.wikipedia.org/wiki/International_Standard_Recording_Code");
    }
    this.isrc = matcher.group(1) + matcher.group(2) + matcher.group(3) + matcher.group(4);
  }


  public void setPreGap(TimeCode preGap) {
    this.preGap = preGap;
  }

  public void setPreGap(Duration preGap, TimeCodeRounding rounding) {
    setPreGap(preGap != null ? new TimeCode(preGap, rounding) : null);
  }

  public void setPostGap(TimeCode postGap) {
    this.postGap = postGap;
  }

  public void setPostGap(Duration postGap, TimeCodeRounding rounding) {
    setPostGap(postGap != null ? new TimeCode(postGap, rounding) : null);
  }

  @Override
  public CueIterator<CueIndex> iterator() {
    return new CueIterator<>(indexes);
  }

  public List<CueIndex> getIndexes() {
    return Collections.unmodifiableList(indexes);
  }

  public CueIndex getIndex(int number) throws IndexNotFoundException {
    CueTools.validateRange("Index number", number,
        Optional.ofNullable(getFirstIndex()).map(CueIndex::getNumber).orElse(CueIndex.INDEX_TRACK_START),
        getIndexCount());

    return Optional.ofNullable(getIndexUnsafe(number))
        .orElseThrow(() -> new IndexNotFoundException(number));
  }

  private CueIndex getIndexUnsafe(int number) {
    for (CueIndex index : indexes) {
      int indexNumber = index.getNumber();
      if (indexNumber == number) {
        return index;
      } else if (indexNumber > number) {
        break;
      }
    }
    return null;
  }

  public CueIndex getFirstIndex() {
    return indexes.isEmpty() ? null : indexes.get(0);
  }

  public CueIndex getLastIndex() {
    return indexes.isEmpty() ? null : indexes.get(indexes.size() - 1);
  }

  public int getNextIndexNumber() {
    CueIndex lastIndex = getLastIndex();
    return lastIndex != null
      ? lastIndex.getNumber() + 1
      : CueIndex.INDEX_TRACK_START; // this is opinionated
  }

  private boolean isAudio() {
    return type.equals(TrackType.AUDIO);
  }

  public boolean hasPreGapIndex() {
    return getPreGapIndex() != null;
  }

  public boolean hasPreGap() {
    return hasPreGapIndex() || getPreGap() != null;
  }

  public boolean hasPostGap() {
    return getPostGap() != null;
  }

  public CueIndex getPreGapIndex() {
    return getIndexUnsafe(CueIndex.INDEX_PRE_GAP);
  }

  public CueIndex getStartIndex() {
    return getIndexUnsafe(CueIndex.INDEX_TRACK_START);
  }

  public CueIndex addIndex(CueIndex index) {
    return addIndex(index, false);
  }

  /**
   * It is not possible to check whether we have max 99 indexes, it has to be done at the track level before writing
   * It is not possible to check the timecodes consistency across several tracks, so it will have to be done at the track or file or disc level before writing
   */
  public synchronized CueIndex addIndex(CueIndex index, boolean renumber) {
    if(indexes.contains(index)) {
      throw new IllegalArgumentException("The track already contains this index");
    }

    if(index.number != null && renumber) {
      index = index.deepCopy(null);
    }

    Integer newNumber = index.number;
    if (newNumber != null) {
      if (indexes.isEmpty() && !CueIndex.isPreGapOrStart(newNumber)) {
        throw new IllegalArgumentException("Cannot start track with number " + newNumber + ", has to be [0,1]");
      }
      return insertIndex(index);
    } else {
      index.setNumberOnce(getNextIndexNumber());
      addIndexUnsafe(index); // it's OK to store it without copying because it's obviously not part of a file yet
      return index;
    }
  }

  private CueIndex insertIndex(CueIndex index) {
    Integer newNumber = index.number;
    int nextNumber = getNextIndexNumber();
    if (newNumber > nextNumber) {
      throw new IllegalArgumentException("Index number " + newNumber + " is out of range [" + CueIndex.INDEX_PRE_GAP + "," + nextNumber + "]");
    }

    CueIndex indexCopy = index.deepCopy();

    CueIndex firstIndex = getFirstIndex();
    if((newNumber == CueIndex.INDEX_PRE_GAP && firstIndex != null && firstIndex.getNumber() == CueIndex.INDEX_TRACK_START)) { // inserting index 0 after index 1
      addIndexUnsafe(0, indexCopy);
    } else if(newNumber == nextNumber) { // new index is directly chaining
      addIndexUnsafe(indexCopy);
    } else { // now we're sure 0 =< newNumber =< lastNumber
      int i = 0;
      int indexCount = getIndexCount();
      for (; i < indexCount; i++) {
        int currentNumber = indexes.get(i).getNumber();
        if (currentNumber >= newNumber) { // >= in case you insert index 0 after index 1
          break;
        }
      }

      addIndexUnsafe(i, indexCopy);
      indexCount++;

      // shift the remaining indexes
      while (++i < indexCount) {
        indexes.get(i).number++;
      }
    }
    return indexCopy;
  }

  protected void addIndexUnsafe(int idx, CueIndex index) {
    indexes.add(idx, index);
  }

  protected void addIndexUnsafe(CueIndex index) {
    indexes.add(index);
  }

  public CueIndex removeIndex(int number) {
    if (indexes.isEmpty()) {
      throw new IllegalArgumentException("Index list is empty");
    }

    CueTools.validateRange("Index number", number, getFirstIndex().getNumber(), getLastIndex().getNumber());

    CueIndex targetIndex = null;
    Iterator<CueIndex> it = indexes.iterator();
    while (it.hasNext()) {
      targetIndex = it.next();
      if (targetIndex.getNumber() == number) {
        it.remove();
        break;
      }
    }
    // shift the remaining indexes
    it.forEachRemaining(index -> index.number--);

    return targetIndex;
  }

  public int getIndexCount() {
    return indexes.size();
  }

  public void clearIndexes() {
    indexes.clear();
  }

  /**
   * This method doesn't check for bottom index (0 or 1) because this track number is unreliable. This can only be done at the disc level.
   * Note: in theory a cue may not respect timecodes ordering (next > previous)
   */
  public CueIssues checkConsistency(boolean withTimeCodes) {
    var issues = new CueIssues();

    // check at least one index
    try {
      CueTools.validateRange("Track " + number + " index count", getIndexCount(), 1, CueIndex.INDEX_MAX);

      // indexed chaining is already ensured by the insertIndex method
      // addIndex and insertIndex method also make sure the first index is 0 or 1
      // but we need to make sure index 1 is present in a track
      if(getIndexUnsafe(CueIndex.INDEX_TRACK_START) == null) {
        issues.add(String.format("Track %d doesn't have mandatory index %d", number, CueIndex.INDEX_TRACK_START));
      }
    } catch(IllegalArgumentException e) {
      issues.add(e);
    }

    // check for double pregap
    if(preGap != null && hasPreGapIndex()) {
      issues.add(String.format("Track %d has both a pregap duration and a pregap index", number)); // yet this would happen with a hidden track (first track's index 0) as there's always a 2s pregap
    }

    if(withTimeCodes) {
      issues.add(checkTimeCodesChaining(null).issue);
    }
    return issues;
  }

  /**
   * In theory a cue may not respect timecodes ordering (next > previous)
   * but we can also assume unordered timecodes are actually a mistake
   */
  TimeCodeValidation checkTimeCodesChaining(CueIndex latestIndex) {
    String issue = null;
    for (CueIndex index : indexes) {
      if(latestIndex != null && index.getTimeCode().compareTo(latestIndex.getTimeCode()) < 0) {
        issue = String.format("Track %d index %d timecode %s is before its predecessor %s", number, index.number, index.getTimeCode(), latestIndex.getTimeCode());
      }
      latestIndex = index;
    }
    return new TimeCodeValidation(latestIndex, issue);
  }

  @AllArgsConstructor
  static final class TimeCodeValidation {
    CueIndex latest;
    String issue;
  }

  public synchronized Set<CueFlag> getFlags() {
    return Collections.unmodifiableSet(flags);
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

  /**
   * @param nextTrack
   * @param fileDuration
   * @param allowDisorderedTimeCodes
   * @return time until the pregap -if it exists-, or start of another track, else till the end of the file
   * @throws IllegalTrackTypeException if this track or the other track is not audio
   * @throws IndexNotFoundException if the needed index(es) for the computation do(es)n't exist
   * @throws NegativeDurationException if the last track 's duration is requested and the file's duration is not sufficient, making that last track's duration negative which is illogical (not enough data to play/burn)
   */
  Duration until(CueTrack nextTrack, Duration fileDuration, boolean allowDisorderedTimeCodes) throws IllegalTrackTypeException, IndexNotFoundException, NegativeDurationException {
    if(allowDisorderedTimeCodes) {
      try {
        return until(nextTrack, null);
      } catch (NegativeDurationException e) { // then the only option is to run till the end of the file like some players do
        return until(null, fileDuration);
      }
    } else {
      return until(nextTrack, null);
    }
  }

  /**
   * @param otherTrack
   * @param fileDuration
   * @return time until the pregap -if it exists-, or start of another track, else till the end of the file
   * @throws IllegalTrackTypeException if this track or the other track is not audio
   * @throws IndexNotFoundException if the needed index(es) for the computation do(es)n't exist
   * @throws NegativeDurationException
   */
  Duration until(CueTrack otherTrack, Duration fileDuration) throws IllegalTrackTypeException, IndexNotFoundException, NegativeDurationException {
    if(!isAudio()) {
      throw new IllegalTrackTypeException(type, TrackType.AUDIO);
    }

    Duration trackDuration;

    CueIndex firstIndex = getFirstIndex();
    if(firstIndex == null) {
      throw new IndexNotFoundException(CueIndex.INDEX_TRACK_START);
    }
    if (otherTrack != null) {
      if(!otherTrack.isAudio()) {
        throw new IllegalTrackTypeException(type, TrackType.AUDIO);
      } else if(otherTrack.getIndexCount() > 0) {
        CueIndex otherStartIndex = otherTrack.getFirstIndex();
        trackDuration = firstIndex.until(otherStartIndex);
        if (trackDuration.isNegative()) {
          throw new NegativeDurationException(firstIndex.getTimeCode(), otherStartIndex.getTimeCode());
        }
      } else {
        throw new IndexNotFoundException(CueIndex.INDEX_PRE_GAP, CueIndex.INDEX_TRACK_START);
      }
    } else if(fileDuration != null) { // we can't but count till the end of the file
      trackDuration = fileDuration.minusMillis(firstIndex.getTimeMillis());
      if (trackDuration.isNegative()) {
        throw new NegativeDurationException(firstIndex.getTimeCode(), fileDuration);
      }
    } else {
      throw new NullPointerException("fileDuration");
    }

    if(preGap != null) {
      trackDuration = trackDuration.plus(preGap.toDuration());
    }
    if(postGap != null) {
      trackDuration = trackDuration.plus(postGap.toDuration());
    }

    return trackDuration;
  }

  @Override
  public String toSheetLine(CueSheetOptions options) {
    return String.format("%s %02d %s", KEYWORD, number, type);
  }

  @Override
  public String toString() {
    return toSheetLine(null);
  }
}
