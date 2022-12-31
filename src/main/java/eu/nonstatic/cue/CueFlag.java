package eu.nonstatic.cue;

import lombok.Getter;

public enum CueFlag {

  DIGITAL_COPY_PERMITTED("DCP"), // Digital copy permitted.
  FOUR_CHANNEL_AURDIO("4CH"), // Four channel audio.
  PRE_EMPHASIS_ENABLED("PRE"), // Pre-emphasis enabled (audio tracks only).
  SERIAL_COPY_MANAGEMENT_SYSTEM("SCMS") // Serial Copy Management System (not supported by all recorders).
  ;

  @Getter
  private final String flag;

  CueFlag(String flag) {
    this.flag = flag;
  }


  public static CueFlag flagOf(String flag) {
    for (CueFlag value : values()) {
      if (value.flag.equals(flag)) {
        return value;
      }
    }
    throw new IllegalArgumentException("No enum constant " + CueFlag.class.getCanonicalName() + "(\"" + flag + "\")");
  }
}
