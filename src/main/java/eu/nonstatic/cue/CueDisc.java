package eu.nonstatic.cue;

import lombok.Data;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * https://en.wikipedia.org/wiki/Cue_sheet_(computing) https://wiki.hydrogenaud.io/index.php?title=Cue_sheet
 * https://wiki.hydrogenaud.io/index.php?title=EAC_and_Cue_Sheets Regards to Jeff Arnold
 */
@Data
public class CueDisc implements CueEntity {

  private final String path;
  private final Charset charset;

  private String title;
  private String performer;
  private String songwriter;
  private String catalog; // MCN/UPC
  private String cdTextFile;

  private final List<CueFile> files = new ArrayList<>();
  private final List<CueRemark> remarks = new ArrayList<>();
  private final List<CueOther> others = new ArrayList<>();


  public CueDisc(String path, Charset charset) {
    this.path = path;
    this.charset = charset;
  }

  public List<CueFile> getFiles() {
    return Collections.unmodifiableList(files);
  }

  public void addFile(CueFile file) {
    files.add(file);
  }

  public List<CueRemark> getRemarks() {
    return Collections.unmodifiableList(remarks);
  }

  public void addRemark(CueRemark remark) {
    remarks.add(remark);
  }

  public List<CueTrack> getTracks() {
    return files.stream().flatMap(file -> file.getTracks().stream()).collect(Collectors.toList());
  }

  public List<CueIndex> getIndexes() {
    return files.stream().flatMap(file -> file.getIndexes().stream()).collect(Collectors.toList());
  }

  public List<CueOther> getOthers() {
    return Collections.unmodifiableList(others);
  }

  public void addOther(CueOther other) {
    others.add(other);
  }
}
