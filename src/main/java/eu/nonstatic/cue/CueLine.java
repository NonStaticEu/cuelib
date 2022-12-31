package eu.nonstatic.cue;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
class CueLine {

  private final int lineNumber;
  private final String raw;
  private final String keyword;
  private final String tail;
  private final List<String> tailParts;

  private static final Pattern splitter = Pattern.compile("[ ]+");

  CueLine(int lineNumber, String line) {
    this.lineNumber = lineNumber;
    this.raw = line.trim();

    int sep = raw.indexOf(' ');
    if (sep >= 0) {
      this.keyword = this.raw.substring(0, sep).toUpperCase(Locale.ROOT);
      this.tail = this.raw.substring(sep + 1).trim();
      this.tailParts = splitter.splitAsStream(this.tail).collect(Collectors.toList());
    } else {
      this.keyword = this.raw;
      this.tail = null;
      this.tailParts = List.of();
    }
  }

  public String getTailWord(int i) {
    return this.tailParts.get(i);
  }

  public int words() {
    return 1 + tailParts.size();
  }

  public int length() {
    return raw.length();
  }

  public boolean isEmpty() {
    return raw.isEmpty();
  }
}