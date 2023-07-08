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

import static eu.nonstatic.audio.AudioTestBase.MP3_NAME;
import static eu.nonstatic.audio.AudioTestBase.WAVE_NAME;
import static eu.nonstatic.cue.CueFlag.DIGITAL_COPY_PERMITTED;
import static eu.nonstatic.cue.CueFlag.FOUR_CHANNEL_AUDIO;
import static eu.nonstatic.cue.CueFlag.PRE_EMPHASIS_ENABLED;
import static eu.nonstatic.cue.CueFlag.SERIAL_COPY_MANAGEMENT_SYSTEM;
import static eu.nonstatic.cue.CueSheetReader.MESSAGE_NOT_CUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.nonstatic.audio.AudioTestBase;
import eu.nonstatic.cue.FileType.Audio;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CueSheetReaderTest extends CueTestBase {

  static final String TMP_DIR = System.getProperty("java.io.tmpdir");
  static final String ASCII_BE_BOP_A_LULA = "Be Bop a Lula";
  static final String UNICODE_CHA_CHA_CHA = "cha cha cha àâéếùï すみませんでした"; //this is UTF-8
  static URL bomCueUrl = CueDiscTest.class.getResource("/Bom Test.cue");
  static URL tcCueUrl = CueDiscTest.class.getResource("/TC Test.cue");
  static URL isrcCueUrl = CueDiscTest.class.getResource("/ISRC Test.cue");

  @Test
  void should_tell_cue_file() throws IOException {
    assertThrows(NullPointerException.class, () -> CueSheetReader.isCueFile(null));
    assertFalse(CueSheetReader.isCueFile(Paths.get("/tmp/nonexistent.cue")));

    Path tempFile = Files.createTempFile(null, null);
    assertFalse(CueSheetReader.isCueFile(tempFile));

    Path cueFile = tempFile.resolveSibling("somefile.cue");
    Files.deleteIfExists(cueFile);
    Files.move(tempFile, cueFile);
    assertTrue(CueSheetReader.isCueFile(cueFile));

    Files.delete(cueFile);
    assertFalse(CueSheetReader.isCueFile(cueFile));
  }

  @Test
  void should_detect_ascii_but_does_not() throws IOException {
    checkCharset(ASCII_BE_BOP_A_LULA, StandardCharsets.US_ASCII, StandardCharsets.ISO_8859_1); // ICU can't make the difference
  }

  @Test
  void should_detect_iso_8859_1() throws IOException {
    checkCharset(UNICODE_CHA_CHA_CHA, StandardCharsets.ISO_8859_1, StandardCharsets.ISO_8859_1);
  }

  @Test
  void should_detect_utf8() throws IOException {
    checkCharset(UNICODE_CHA_CHA_CHA, StandardCharsets.UTF_8, StandardCharsets.UTF_8);
  }

  static void checkCharset(String line, Charset fileCharset, Charset expectedCharset) throws IOException {
    File tempFile = createFileWithCharset(line, fileCharset);

    Charset charset = new CueSheetReader().detectEncoding(tempFile);
    CueSheetContext context = new CueSheetContext(tempFile, new CueOptions(charset));
    assertTrue(context.getName().startsWith("test") && context.getName().endsWith(".tmp"));

    assertTrue(context.getPath().startsWith(TMP_DIR));
    assertEquals(tempFile.getAbsolutePath(), context.getPath());
    assertEquals(expectedCharset, charset);
    tempFile.delete();
  }

  @Test
  void should_infer_charset_from_bom() throws IOException {
    CueOptions options = CueOptions.builder().build();
    CueSheetReadout readout = new CueSheetReader().readCueSheet(bomCueUrl, options);
    CueDisc disc = readout.getDisc();
    assertFalse(readout.isErrors());
    assertEquals(StandardCharsets.UTF_8, disc.getCharset());
    assertEquals(StandardCharsets.UTF_8, options.getCharset());
  }

  @Test
  void should_correct_charset_from_bom() throws IOException {
    CueOptions options = new CueOptions(StandardCharsets.US_ASCII);
    CueSheetReadout readout = new CueSheetReader().readCueSheet(bomCueUrl, options);
    CueDisc disc = readout.getDisc();
    assertFalse(readout.isErrors());
    assertEquals(StandardCharsets.UTF_8, disc.getCharset());
    assertEquals(StandardCharsets.UTF_8, options.getCharset());
  }

  @Test
  void should_fallback_charset_when_icu_cannot_detect_it() {
    CueOptions options = CueOptions.builder().build();
    CueSheetContext context = new CueSheetContext("faulty.cue", options);
    ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6});

    CueSheetReader reader = new CueSheetReader(0, StandardCharsets.US_ASCII); // fallback
    assertThrows(IOException.class, () -> reader.readCueSheet(new FaultyStream(is, Bom.MAX_LENGTH_BYTES), context)); // dies on first line read after charset fallback
    assertEquals(1, context.getErrors().size());
    assertEquals("Fallback to US-ASCII for faulty.cue: reads: 4", context.getErrors().get(0));
    assertEquals(StandardCharsets.US_ASCII, options.getCharset()); // fallback is set
  }

  /**
   * @return file because most methods taking File cascade to Path, so it's better for coverage
   */
  private static File createFileWithCharset(String line, Charset fileCharset) throws IOException {
    Path tempFile = Files.createTempFile("test", null);
    try (PrintStream ps = new PrintStream(Files.newOutputStream(tempFile, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE), true, fileCharset)) {
      ps.println(line);
    }
    return tempFile.toFile();
  }

  @Test
  void should_detect_utf8_stream() throws IOException {
    File tempFile = createFileWithCharset(UNICODE_CHA_CHA_CHA, StandardCharsets.UTF_8);
    try(InputStream is = Files.newInputStream(tempFile.toPath())) {
      assertEquals(StandardCharsets.UTF_8, new CueSheetReader().detectEncoding(is));
      tempFile.delete();
    }
  }

  @Test
  void should_read_cuesheet_from_url() throws IOException {
    CueSheetReadout readout = new CueSheetReader().readCueSheet(myTestUrl, StandardCharsets.UTF_8);
    CueDisc disc = readout.getDisc();
    checkReadCueSheet(disc, myTestUrl.toExternalForm(), "", false);
    assertFalse(disc.isRenumberingNecessary());
    assertEquals(2, readout.getErrors().size());
    assertEquals(myTestUrl + "#12: Unknown disc line: SINGLEWORD", readout.getErrors().get(0));
    assertEquals(myTestUrl + "#15: Unknown disc line: UNKNOWN ThiNG", readout.getErrors().get(1));


    disc.getFirstFile().addTrack(new CueTrack(TrackType.AUDIO));
    assertTrue(disc.isRenumberingNecessary());
  }

  private static void checkReadCueSheet(CueDisc disc, String expectedCuePath, String expectedFilesDir, boolean expectedSizesDurationsSet) {
    checkReadDisc(disc, expectedCuePath);

    List<CueFile> files = disc.getFiles();
    CueFile file0 = files.get(0);
    assertEquals(joinPath(expectedFilesDir, "some file 1.mp3"), file0.getFile());
    assertEquals(Audio.MP3, file0.getType());
    assertEquals(expectedSizesDurationsSet, file0.isSizeAndDurationSet());

    assertEquals(4, file0.getTrackCount());
    List<CueTrack> file0Tracks = file0.getTracks();
    checkReadTrack01(file0Tracks.get(0));
    checkReadTrack02(file0Tracks.get(1));
    checkReadTrack03(file0Tracks.get(2));
    checkReadTrack04(file0Tracks.get(3));

    CueFile file1 = files.get(1);
    assertEquals(joinPath(expectedFilesDir, "some file 2.WAV"), file1.getFile());
    assertEquals(Audio.WAVE, file1.getType());
    assertEquals(expectedSizesDurationsSet, file1.isSizeAndDurationSet());
    assertEquals(5, file1.getTrackCount());
    List<CueTrack> file1Tracks = file1.getTracks();

    checkReadTrack05(file1Tracks.get(0));
    checkReadTrack06(file1Tracks.get(1));
    checkReadTrack07(file1Tracks.get(2));
    checkReadTrack08(file1Tracks.get(3));
    checkReadTrack09(file1Tracks.get(4));

    CueFile file2 = files.get(2);
    assertEquals(joinPath(expectedFilesDir, "some file 1.mp3"), file2.getFile());
    assertEquals(Audio.MP3, file2.getType());
    assertEquals(expectedSizesDurationsSet, file2.isSizeAndDurationSet());

    assertEquals(4, file2.getTrackCount());
    List<CueTrack> file2Tracks = file2.getTracks();

    checkReadTrack10(file2Tracks.get(0));
    checkReadTrack11(file2Tracks.get(1));
    checkReadTrack12(file2Tracks.get(2));
    checkReadTrack13(file2Tracks.get(3));
  }

  private static String joinPath(String dir, String file) {
    return dir != null ? Paths.get(dir).resolve(file).toString() : file;
  }

  private static void checkReadDisc(CueDisc disc, String expectedCuePath) {
    assertEquals(StandardCharsets.UTF_8, disc.getCharset());
    assertEquals(expectedCuePath, disc.getPath());
    assertEquals("Some title", disc.getTitle());
    assertEquals("Some performer", disc.getPerformer());
    assertEquals("Some writer", disc.getSongwriter());
    assertEquals("0696969424242", disc.getCatalog());
    assertEquals("cdtextfile", disc.getCdTextFile());

    assertEquals(2, disc.getOthers().size());
    CueOther other0 = disc.getOthers().get(0);
    assertEquals("SINGLEWORD", other0.getKeyword());
    assertNull(other0.getValue());
    CueOther other1 = disc.getOthers().get(1);
    assertEquals("UNKNOWN", other1.getKeyword());
    assertEquals("ThiNG", other1.getValue());

    List<CueRemark> remarks = disc.getRemarks();
    assertEquals(6, remarks.size());
    CueRemark remark0 = remarks.get(0);
    assertEquals("GENRE", remark0.getTag());
    assertEquals("Some genre", remark0.getValue());
    CueRemark remark1 = remarks.get(1);
    assertEquals("DATE", remark1.getTag());
    assertEquals("2000", remark1.getValue());
    CueRemark remark2 = remarks.get(2);
    assertEquals("COMPOSER", remark2.getTag());
    assertEquals("", remark2.getValue());
    CueRemark remark3 = remarks.get(3);
    assertEquals("DISCID", remark3.getTag());
    assertEquals("B12C345D", remark3.getValue());
    CueRemark remark4 = remarks.get(4);
    assertEquals("COMMENT", remark4.getTag());
    assertEquals("Some comment with é", remark4.getValue());
    CueRemark remark5 = remarks.get(5);
    assertNull(remark5.getTag());
    assertEquals("WHATEVER", remark5.getValue());
  }

  private static void checkReadTrack01(CueTrack track01) {
    assertEquals("Title 1", track01.getTitle());
    assertEquals("Performer 1", track01.getPerformer());
    assertNull(track01.getIsrc());
    assertEquals(Set.of(DIGITAL_COPY_PERMITTED), track01.getFlags());
    assertEquals(0, track01.getRemarks().size());
    assertEquals(1, track01.getIndexCount());
    CueIndex track01Index1 = track01.getIndexes().get(0);
    assertEquals(1, track01Index1.getNumber());
    assertEquals("00:00:00", track01Index1.toTimeCode());
  }
  private static void checkReadTrack02(CueTrack track02) {
    assertEquals("Title 2", track02.getTitle());
    assertEquals("Performer 2", track02.getPerformer());
    assertNull(track02.getIsrc());
    assertEquals(Set.of(FOUR_CHANNEL_AUDIO), track02.getFlags());
    assertEquals(0, track02.getRemarks().size());
    assertEquals(1, track02.getIndexCount());
    CueIndex track02Index1 = track02.getIndexes().get(0);
    assertEquals(1, track02Index1.getNumber());
    assertEquals("00:13:02", track02Index1.toTimeCode());
  }
  private static void checkReadTrack03(CueTrack track03) {
    assertEquals("Title 3", track03.getTitle());
    assertEquals("Performer 3", track03.getPerformer());
    assertNull(track03.getIsrc());
    assertEquals(0, track03.getFlags().size());
    List<CueRemark> t03Remarks = track03.getRemarks();
    assertEquals(1, t03Remarks.size());
    assertNull(t03Remarks.get(0).getTag());
    assertEquals("My Remark", t03Remarks.get(0).getValue());
    assertEquals(2, track03.getIndexCount());
    CueIndex track03Index1 = track03.getIndexes().get(0);
    assertEquals(1, track03Index1.getNumber());
    assertEquals("01:42:25", track03Index1.toTimeCode());
    CueIndex track03Index2 = track03.getIndexes().get(1);
    assertEquals(2, track03Index2.getNumber());
    assertEquals("03:18:55", track03Index2.toTimeCode());
  }
  private static void checkReadTrack04(CueTrack track04) {
    assertEquals("Title 4", track04.getTitle());
    assertEquals("Performer 4", track04.getPerformer());
    assertEquals("FRXXX4212345", track04.getIsrc());
    assertEquals(0, track04.getFlags().size());
    assertEquals(0, track04.getRemarks().size());
    assertEquals(1, track04.getIndexCount());
    CueIndex track04Index1 = track04.getIndexes().get(0);
    assertEquals(1, track04Index1.getNumber());
    assertEquals("08:00:18", track04Index1.toTimeCode());
  }
  private static void checkReadTrack05(CueTrack track05) {
    assertEquals("Title 5", track05.getTitle());
    assertEquals("Performer 5", track05.getPerformer());
    assertNull(track05.getIsrc());
    assertEquals(0, track05.getFlags().size());
    assertEquals(0, track05.getRemarks().size());
    assertEquals(2, track05.getIndexCount());
    CueIndex track05Index0 = track05.getIndexes().get(0);
    assertEquals(0, track05Index0.getNumber());
    assertEquals("00:00:00", track05Index0.toTimeCode());
    CueIndex track05Index1 = track05.getIndexes().get(1);
    assertEquals(1, track05Index1.getNumber());
    assertEquals("14:44:69", track05Index1.toTimeCode());
  }
  private static void checkReadTrack06(CueTrack track06) {
    assertEquals("Title 6", track06.getTitle());
    assertEquals("Performer 6", track06.getPerformer());
    assertEquals("Songwriter 6", track06.getSongwriter());
    assertNull(track06.getIsrc());
    assertEquals(Set.of(PRE_EMPHASIS_ENABLED), track06.getFlags());
    assertEquals(0, track06.getRemarks().size());
    assertEquals(1, track06.getIndexCount());
    CueIndex track06Index1 = track06.getIndexes().get(0);
    assertEquals(1, track06Index1.getNumber());
    assertEquals("14:58:15", track06Index1.toTimeCode());
  }

  private static void checkReadTrack07(CueTrack track07) {
    assertEquals("Title 7", track07.getTitle());
    assertEquals("Performer 7", track07.getPerformer());
    assertNull(track07.getIsrc());
    assertEquals(0, track07.getFlags().size());
    assertEquals(0, track07.getRemarks().size());
    assertEquals(1, track07.getIndexCount());
    assertEquals(new TimeCode(0, 0, 4), track07.getPreGap());
    assertEquals(new TimeCode(0, 1, 2), track07.getPostGap());
    CueIndex track07Index1 = track07.getIndexes().get(0);
    assertEquals(1, track07Index1.getNumber());
    assertEquals("22:31:60", track07Index1.toTimeCode());
  }
  private static void checkReadTrack08(CueTrack track08) {
    assertEquals("Title 8", track08.getTitle());
    assertEquals("Performer 8", track08.getPerformer());
    assertNull(track08.getIsrc());
    assertEquals(0, track08.getFlags().size());
    assertEquals(0, track08.getRemarks().size());
    assertEquals(3, track08.getIndexCount());
    CueIndex track08Index1 = track08.getIndexes().get(0);
    assertEquals(1, track08Index1.getNumber());
    assertEquals("23:55:42", track08Index1.toTimeCode());
    CueIndex track08Index2 = track08.getIndexes().get(1);
    assertEquals(2, track08Index2.getNumber());
    assertEquals("28:07:74", track08Index2.toTimeCode());
    CueIndex track08Index3 = track08.getIndexes().get(2);
    assertEquals(3, track08Index3.getNumber());
    assertEquals("30:33:21", track08Index3.toTimeCode());
  }
  private static void checkReadTrack09(CueTrack track09) {
    assertEquals("Title 9", track09.getTitle());
    assertEquals("Performer 9", track09.getPerformer());
    assertNull(track09.getIsrc());
    assertEquals(Set.of(DIGITAL_COPY_PERMITTED, SERIAL_COPY_MANAGEMENT_SYSTEM, PRE_EMPHASIS_ENABLED), track09.getFlags());
    assertEquals(0, track09.getRemarks().size());
    assertEquals(1, track09.getIndexCount());
    CueIndex track09Index1 = track09.getIndexes().get(0);
    assertEquals(1, track09Index1.getNumber());
    assertEquals("32:25:08", track09Index1.toTimeCode());
  }
  private static void checkReadTrack10(CueTrack track10) {
    assertEquals("Title 10", track10.getTitle());
    assertEquals("Performer 10", track10.getPerformer());
    assertNull(track10.getIsrc());
    assertEquals(Set.of(SERIAL_COPY_MANAGEMENT_SYSTEM, PRE_EMPHASIS_ENABLED), track10.getFlags());
    assertEquals(0, track10.getRemarks().size());
    assertEquals(2, track10.getIndexCount());
    CueIndex track10Index0 = track10.getIndexes().get(0);
    assertEquals(0, track10Index0.getNumber());
    assertEquals("00:00:00", track10Index0.toTimeCode());
    CueIndex track10Index1 = track10.getIndexes().get(1);
    assertEquals(1, track10Index1.getNumber());
    assertEquals("34:46:14", track10Index1.toTimeCode());
  }
  private static void checkReadTrack11(CueTrack track11) {
    assertEquals("Title 11", track11.getTitle());
    assertEquals("Performer 11", track11.getPerformer());
    assertNull(track11.getIsrc());
    assertEquals(Set.of(FOUR_CHANNEL_AUDIO, DIGITAL_COPY_PERMITTED), track11.getFlags());
    assertEquals(0, track11.getRemarks().size());
    assertEquals(1, track11.getIndexCount());
    CueIndex track11Index1 = track11.getIndexes().get(0);
    assertEquals(1, track11Index1.getNumber());
    assertEquals("48:11:13", track11Index1.toTimeCode());
  }
  private static void checkReadTrack12(CueTrack track12) {
    assertEquals("Title 12", track12.getTitle());
    assertEquals("Performer 12", track12.getPerformer());
    assertNull(track12.getIsrc());
    assertEquals(0, track12.getFlags().size());
    assertEquals(0, track12.getRemarks().size());
    assertEquals(1, track12.getIndexCount());
    CueIndex track12Index1 = track12.getIndexes().get(0);
    assertEquals(1, track12Index1.getNumber());
    assertEquals("58:58:57", track12Index1.toTimeCode());
  }
  private static void checkReadTrack13(CueTrack track13) {
    assertEquals("Title 13", track13.getTitle());
    assertEquals("Performer 13", track13.getPerformer());
    assertNull(track13.getIsrc());
    assertEquals(0, track13.getFlags().size());
    assertEquals(0, track13.getRemarks().size());
    assertEquals(1, track13.getIndexCount());
    CueIndex track13Index1 = track13.getIndexes().get(0);
    assertEquals(1, track13Index1.getNumber());
    assertEquals("61:16:61", track13Index1.toTimeCode());
  }

  @Test
  void should_read_cuesheet_from_file() throws IOException {
    Path tempDir = Files.createTempDirectory("should_read_cuesheet_from_file");
    Path tempFile = Files.createTempFile(tempDir, "test", ".cue");
    Charset charset = StandardCharsets.UTF_8;
    List<String> lines = readLines(myTestUrl, charset);
    writeLines(tempFile, lines, charset);

    copyFileContents(AudioTestBase.class.getResource(MP3_NAME), tempDir, "some file 1.mp3");
    copyFileContents(AudioTestBase.class.getResource(WAVE_NAME), tempDir, "some file 2.WAV");

    try {
      CueSheetReader cueSheetReader = new CueSheetReader();
      CueSheetReadout readout1 = cueSheetReader.readCueSheet(tempFile.toFile());
      CueDisc disc1 = readout1.getDisc();
      checkReadCueSheet(disc1, tempFile.toString(), tempDir.toString(), true);
      assertEquals(2, readout1.getErrors().size());
      assertEquals(tempFile + "#12: Unknown disc line: SINGLEWORD", readout1.getErrors().get(0));
      assertEquals(tempFile + "#15: Unknown disc line: UNKNOWN ThiNG", readout1.getErrors().get(1));

      CueSheetReadout readout2 = cueSheetReader.readCueSheet(tempFile.toFile(), charset);
      CueDisc disc2 = readout2.getDisc();
      checkReadCueSheet(disc2, tempFile.toString(), tempDir.toString(), true);
      assertEquals(2, readout2.getErrors().size());
      assertEquals(tempFile + "#12: Unknown disc line: SINGLEWORD", readout2.getErrors().get(0));
      assertEquals(tempFile + "#15: Unknown disc line: UNKNOWN ThiNG", readout2.getErrors().get(1));
    } finally {
      deleteRecursive(tempDir);
    }
  }

  @Test
  void should_skip_non_cue_file() throws IOException {
    Path tempFile = Files.createTempFile("test", ".xyz");
    CueSheetReader cueSheetReader = new CueSheetReader();
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> cueSheetReader.readCueSheet(tempFile));
    String expectedMessage = MESSAGE_NOT_CUE + tempFile.toAbsolutePath();
    assertEquals(expectedMessage, ex.getMessage());
  }

  @Test
  void should_read_cuesheet_from_lines() throws IOException {
    String expectedCuePath = myTestUrl.toExternalForm();
    CueOptions options = new CueOptions(StandardCharsets.UTF_8);
    CueSheetContext context = new CueSheetContext(expectedCuePath, options);

    CueSheetReader cueSheetReader = new CueSheetReader();
    List<String> lines = readLines(myTestUrl, StandardCharsets.UTF_8);
    CueDisc disc1 = cueSheetReader.readCueSheet(lines, context);
    checkReadCueSheet(disc1, expectedCuePath, "", false);

    CueDisc disc2 = cueSheetReader.readCueSheet(lines.toArray(String[]::new), context);
    checkReadCueSheet(disc2, expectedCuePath, "", false);
  }

  @Test
  void should_read_cuesheet_from_reader() throws IOException {
    CueOptions options = new CueOptions(StandardCharsets.UTF_8);
    CueSheetContext context = new CueSheetContext(myTestUrl.toExternalForm(), options);
    try(InputStreamReader isr = new InputStreamReader(myTestUrl.openStream(), options.getCharset())) {
      CueDisc disc = new CueSheetReader().readCueSheet(isr, context);
      checkReadCueSheet(disc, myTestUrl.toExternalForm(), "", false);
    }
  }

  @Test
  void should_not_be_lenient_on_timecodes() {
    CueSheetReader cueSheetReader = new CueSheetReader();
    CueOptions options = new CueOptions(StandardCharsets.UTF_8);
    IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> cueSheetReader.readCueSheet(tcCueUrl, options));
    assertEquals("frames must be in the [0-74] range", iae.getMessage());
  }

  @Test
  void should_be_lenient_on_timecodes() throws IOException {
    CueSheetReadout readout = new CueSheetReader().readCueSheet(tcCueUrl, CueOptions.builder().timeCodeLeniency(true).build());
    CueFile file = readout.getDisc().getFirstFile();
    assertEquals(2, file.getTrackCount());
    List<CueIndex> t1Indexes = file.getFirstTrack().getIndexes();
    List<CueIndex> t2Indexes = file.getLastTrack().getIndexes();
    assertEquals(TimeCode.ZERO_SECOND, t1Indexes.get(0).getTimeCode());
    assertEquals(new TimeCode(1, 7, 45), t1Indexes.get(1).getTimeCode());

    assertEquals(new TimeCode(12, 13, 62), t2Indexes.get(0).getTimeCode());
    assertEquals(new TimeCode(33, 15, 35), t2Indexes.get(1).getTimeCode());
  }

  @Test
  void should_not_be_lenient_on_isrc() {
    CueSheetReader cueSheetReader = new CueSheetReader();
    CueOptions options = new CueOptions(StandardCharsets.UTF_8);
    IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> cueSheetReader.readCueSheet(isrcCueUrl, options));
    assertEquals("ISRC read: WHATEVER. Must use the CCOOOYYSSSSS format: https://en.wikipedia.org/wiki/International_Standard_Recording_Code", iae.getMessage());
  }

  @Test
  void should_be_lenient_on_isrc() throws IOException {
    CueSheetReadout readout = new CueSheetReader().readCueSheet(isrcCueUrl, CueOptions.builder().isrcLeniency(true).build());
    CueFile file = readout.getDisc().getFirstFile();
    assertEquals(2, file.getTrackCount());
    assertEquals("WHATEVER", file.getFirstTrack().getIsrc());
    assertNull(file.getLastTrack().getIsrc());
  }
}
