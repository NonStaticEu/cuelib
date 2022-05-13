package eu.nonstatic.cue;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
public class CueTrack implements CueEntity {

  public static final String KEYWORD = CueWords.TRACK;

  private final int number;
  private final String type; // ex: AUDIO

  private String title;
  private String performer;
  private String artist; // nonstandard
  private String songwriter;
  private String isrc;

  private String pregap;
  private final List<CueIndex> indexes = new ArrayList<>();
  private String postgap;
  private List<String> flags;

  private final List<CueRemark> remarks = new ArrayList<>();

  public CueTrack(int number, String type) {
    this.number = number;
    this.type = type;
  }

  public List<CueIndex> getIndexes() {
    return Collections.unmodifiableList(indexes);
  }

  public void addIndex(CueIndex index) {
    indexes.add(index);
  }

  public List<CueRemark> getRemarks() {
    return Collections.unmodifiableList(remarks);
  }

  public void addRemark(CueRemark remark) {
    remarks.add(remark);
  }
}
