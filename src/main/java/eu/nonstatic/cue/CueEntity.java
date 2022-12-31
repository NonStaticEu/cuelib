package eu.nonstatic.cue;

public abstract class CueEntity {
  // maybe getLineNumber()

  public abstract String toSheetLine();

  @Override
  public String toString() {
    return toSheetLine();
  }
}
