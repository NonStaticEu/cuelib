package eu.nonstatic.cue;

import lombok.Data;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class CueDisc implements CueEntity {

  private final String path;
  private final Charset charset;

  private String title;
  private String performer;
  private String artist; // nonstandard
  private String songwriter;
  private String catalog; // MCN/UPC
  private String cdTextFile;

  private final List<CueFile> files = new ArrayList<>();

  private final List<CueRemark> remarks = new ArrayList<>();


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
}
