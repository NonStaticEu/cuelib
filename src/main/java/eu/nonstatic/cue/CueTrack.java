package eu.nonstatic.cue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
public class CueTrack implements CueEntity, Comparable<CueTrack> {

  public static final String KEYWORD = CueWords.TRACK;

  private final int number;
  private final String type; // ex: AUDIO

  private String title;
  private String performer;
  private String songwriter;
  private String isrc;

  private String pregap;
  private final List<CueIndex> indexes = new ArrayList<>();
  private String postgap;
  private List<CueFlag> flags;

  private final List<CueRemark> remarks = new ArrayList<>();
  private final List<CueOther> others = new ArrayList<>();

  public CueTrack(int number, String type) {
    this.number = number;
    this.type = type;
  }

  public List<CueIndex> getIndexes() {
    return Collections.unmodifiableList(indexes);
  }

  public void addIndex(CueIndex index) {
    // TODO check numbering
    indexes.add(index);
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
    return number == 1
        && !indexes.isEmpty()
        && indexes.get(0).getNumber() == 0;
  }

  @Override
  public int compareTo(CueTrack cueTrack) {
    return Integer.compare(number, cueTrack.number);
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
