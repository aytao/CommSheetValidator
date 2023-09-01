package com.aytao.rubiks.client;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.aytao.rubiks.comm.Comm;
import com.aytao.rubiks.comm.CommValidity;
import com.aytao.rubiks.comm.Comm.UnbalancedBracketsException;
import com.aytao.rubiks.cube.Cube;
import com.aytao.rubiks.cube.SpeffzUtils;
import com.aytao.rubiks.cube.Move.IllegalMoveException;
import com.aytao.rubiks.utils.ResourceHandler;

import au.com.bytecode.opencsv.CSVReader;

import org.apache.commons.cli.*;

public class CommSheetValidator {

  public enum PieceType {
    EDGE, CORNER;
  }

  private static class CommDescription {
    private final char target1;
    private final char target2;

    public CommDescription(char target1, char target2) {
      this.target1 = target1;
      this.target2 = target2;
    }
  }

  private static final String EMPTY_REGEX = "(\\s)*";
  private static final Pattern EMPTY_PATTERN = Pattern.compile(EMPTY_REGEX);

  private static void checkExpectedDims(List<String[]> commStrings, PieceType pieceType, boolean includesHeaders,
      boolean includesEmptyBufferGroups) {

    int expectedDim = SpeffzUtils.NUM_SPEFFZ_LETTERS;

    if (!includesEmptyBufferGroups) {
      if (pieceType.equals(PieceType.EDGE)) {
        expectedDim -= 2;
      } else {
        expectedDim -= 3;
      }
    }

    if (includesHeaders) {
      expectedDim++;
    }

    String flagSuggestion = " Consider setting flacs -e or -r. See usage help with -h for more details.";

    if (commStrings.size() != expectedDim) {
      String errorString = "Provided file has " + commStrings.size() + " rows, but " + expectedDim + " were expected.";
      errorString += flagSuggestion;
      throw new IllegalArgumentException(errorString);
    }

    for (int i = 0; i < commStrings.size(); i++) {
      if (includesHeaders && i == 0) {
        continue;
      }

      String[] arr = commStrings.get(i);
      if (arr.length != expectedDim) {
        String errorString = "Row " + i + " of provided file has " + commStrings.size() + " columns, but " + expectedDim
            + " were expected.";
        errorString += flagSuggestion;
        throw new IllegalArgumentException(errorString);
      }
    }
  }

  private static String[][] csvToTwoDimArray(String fileName, PieceType pieceType, boolean includesHeaders,
      boolean includesEmptyBufferGroups) {
    List<String[]> commStrings;
    try (CSVReader reader = new CSVReader(new FileReader(ResourceHandler.getFile(fileName)))) {
      commStrings = reader.readAll();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Error reading file '" + fileName + "'");
    }

    checkExpectedDims(commStrings, pieceType, includesHeaders, includesEmptyBufferGroups);

    if (includesHeaders) {
      commStrings.remove(0);
    }

    String[][] comms = new String[commStrings.size()][];
    int idx = 0;
    for (String[] arr : commStrings) {
      if (includesHeaders) {
        String[] commsRow = new String[arr.length - 1];
        System.arraycopy(arr, 1, commsRow, 0, comms.length);
        comms[idx] = commsRow;
      } else {
        comms[idx] = arr;
      }
      idx++;
    }

    return comms;
  }

  private static Set<Character> getRelatedStickersSet(PieceType pieceType, char sticker) {
    if (pieceType.equals(PieceType.EDGE)) {
      return SpeffzUtils.getRelatedEdgeStickersSet(sticker);
    } else {
      return SpeffzUtils.getRelatedCornerStickersSet(sticker);
    }
  }

  public CommValidity[][] checkValidity(String fileName, char buffer, PieceType pieceType, boolean includesHeaders,
      boolean includesEmptyBufferGroups) {
    Set<Character> bufferPieceSet = getRelatedStickersSet(pieceType, buffer);
    CommValidity[][] validities = new CommValidity[SpeffzUtils.NUM_SPEFFZ_LETTERS][SpeffzUtils.NUM_SPEFFZ_LETTERS];

    String[][] commStrings = csvToTwoDimArray(fileName, pieceType, includesHeaders, includesEmptyBufferGroups);
    int firstPieceIdx = 0;
    for (int i = 0; i < SpeffzUtils.NUM_SPEFFZ_LETTERS; i++) {
      if (bufferPieceSet.contains((char) (i + 'a'))) {
        if (includesEmptyBufferGroups) {
          firstPieceIdx++;
        }
        continue;
      }
      int secondPieceIdx = 0;
      for (int j = 0; j < SpeffzUtils.NUM_SPEFFZ_LETTERS; j++) {
        if (bufferPieceSet.contains((char) (j + 'a'))) {
          if (includesEmptyBufferGroups) {
            secondPieceIdx++;
          }
          continue;
        }
        validities[i][j] = getCommStringValidity(
            commStrings[secondPieceIdx][firstPieceIdx], pieceType, buffer, (char) (i + 'a'),
            (char) (j + 'a'));
        secondPieceIdx++;
      }
      firstPieceIdx++;
    }

    return validities;
  }

  private CommValidity getCommStringValidity(String commString, PieceType commPieceType, char buffer, char target1,
      char target2) {
    boolean targetsOnSamePiece = getRelatedStickersSet(commPieceType, target1).contains(target2);

    if (targetsOnSamePiece) {
      if (EMPTY_PATTERN.matcher(commString).matches()) {
        return CommValidity.VALID;
      } else {
        return CommValidity.SHOULD_BE_EMPTY;
      }
    } else {
      if (EMPTY_PATTERN.matcher(commString).matches()) {
        return CommValidity.UNEXPECTED_EMPTY;
      }
    }

    Comm comm;
    try {
      comm = new Comm(commString);
    } catch (IllegalMoveException e) {
      return CommValidity.ILLEGAL_MOVE;
    } catch (UnbalancedBracketsException e) {
      return CommValidity.UNBALANCED_BRACKETS;
    } catch (Exception e) {
      return CommValidity.PARSING_ERROR;
    }

    return checkComm(comm, commPieceType, buffer, target1, target2);
  }

  public static CommValidity checkComm(Comm comm, PieceType pieceType, char buffer, char target1, char target2) {
    Cube cube = new Cube();
    cube.execute(comm.toSequence());

    char[] otherPieceTypeReport;
    char[] relevantPieceTypeReport;
    if (pieceType.equals(PieceType.EDGE)) {
      otherPieceTypeReport = SpeffzUtils.cornerReport(cube);
      relevantPieceTypeReport = SpeffzUtils.edgeReport(cube);
    } else {
      otherPieceTypeReport = SpeffzUtils.edgeReport(cube);
      relevantPieceTypeReport = SpeffzUtils.cornerReport(cube);
    }

    if (!isAscendingOrder(otherPieceTypeReport)) {
      return CommValidity.DISRUPTS_OTHER_PIECES;
    }

    Set<Character> ignore = getRelatedStickersSet(pieceType, buffer);
    ignore.addAll(getRelatedStickersSet(pieceType, target1));
    ignore.addAll(getRelatedStickersSet(pieceType, target2));
    if (!otherStickersUndisturbed(relevantPieceTypeReport, ignore)) {
      return CommValidity.DISRUPTS_OTHER_PIECES;
    }

    if (!isCycle(relevantPieceTypeReport, buffer, target1, target2)) {
      return CommValidity.INCORRECT_CYCLE;
    }

    return CommValidity.VALID;
  }

  private static boolean otherStickersUndisturbed(char[] report, Set<Character> ignore) {
    for (int i = 0; i < SpeffzUtils.NUM_SPEFFZ_LETTERS; i++) {
      if (ignore.contains((char) ('a' + i))) {
        continue;
      }

      if (report[i] != i + 'a') {
        return false;
      }
    }

    return true;
  }

  private static boolean isReplaced(char[] report, char source, char target) {
    return report[target - 'a'] == source;
  }

  private static boolean isCycle(char[] report, char b, char t1, char t2) {
    return isReplaced(report, b, t1) &&
        isReplaced(report, t1, t2) &&
        isReplaced(report, t2, b);
  }

  private static boolean isAscendingOrder(char[] report) {
    for (int i = 0; i < report.length; i++) {
      if (report[i] != 'a' + i) {
        return false;
      }
    }

    return true;
  }

  private static void printErrorGroup(CommValidity cv, List<CommDescription> list) {
    String title = cv.name().replace("_", " ");
    String underline = new String(new char[title.length()]).replace('\0', '_');
    System.out.println(title);
    System.out.println(underline);

    for (CommDescription commDescription : list) {
      System.out.printf("%c%c\n", Character.toUpperCase(commDescription.target1),
          Character.toUpperCase(commDescription.target2));
    }
  }

  private static void detectAllErrors(String sheetName,
      char buffer,
      PieceType pieceType,
      boolean containsHeaders,
      boolean containsEmptyBufferGroups,
      boolean ignoreEmpty) {
    CommSheetValidator commSheetValidator = new CommSheetValidator();
    CommValidity[][] commValidities = commSheetValidator.checkValidity(
        sheetName, buffer, pieceType, containsHeaders, containsEmptyBufferGroups);

    Map<CommValidity, List<CommDescription>> map = new HashMap<>();
    for (int i = 0; i < commValidities.length; i++) {
      for (int j = 0; j < commValidities[i].length; j++) {
        if (commValidities[i][j] == null) {
          continue;
        }
        List<CommDescription> list = map.getOrDefault(commValidities[i][j], new ArrayList<>());
        list.add(new CommDescription((char) (i + 'a'), (char) (j + 'a')));
        map.put(commValidities[i][j], list);
      }
    }

    for (CommValidity cv : map.keySet()) {
      if (CommValidity.isValid(cv) || (ignoreEmpty && cv.equals(CommValidity.UNEXPECTED_EMPTY))) {
        continue;
      }
      printErrorGroup(cv, map.get(cv));
      System.out.println();
    }
  }

  public static void main(String[] args) {
    Options options = new Options();
    options.addOption("r", "headers", false, "Sheet includes row and column headers");
    options.addOption("e", "empty-buffer-groups", false, "Sheet has a empty rows and columns for buffer piece");
    options.addOption("b", "buffer", true, "Set the buffer (default is 'c')");
    options.addOption("i", "ignore-empty", false, "Ignore unexpectedly empty entries");
    options.addOption("h", "help", false, "Print usage help");

    options.addRequiredOption("t", "piece-type", true,
        "Specifies what piece type the sheet is for. Must be either 'e' for edges or 'c' for corners");

    CommandLineParser parser = new DefaultParser();
    try {
      CommandLine cmd = parser.parse(options, args);
      String[] anonymousArgs = cmd.getArgs();
      if (anonymousArgs.length != 1) {
        throw new ParseException("Only one anonymous arg expected");
      }
      String fileName = "Comms/" + anonymousArgs[0];

      if (cmd.hasOption("help")) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("check_comms.(bat|sh)", options);
        return;
      }

      boolean containsHeaders = cmd.hasOption("headers");
      boolean containsEmptyBufferGroups = cmd.hasOption("empty-buffer-groups");
      boolean ignoreEmpty = cmd.hasOption("ignore-empty");

      char buffer = 'c';
      if (cmd.hasOption("buffer")) {
        String bufferArg = cmd.getOptionValue("buffer");
        if (bufferArg.length() != 1 || !SpeffzUtils.isValidSpeffzLetter(bufferArg.charAt(0))) {
          throw new IllegalArgumentException("Illegal buffer: " + bufferArg);
        }
        buffer = bufferArg.charAt(0);
      }

      String pieceTypeArg = cmd.getOptionValue("piece-type");
      PieceType pieceType;
      if (pieceTypeArg.equals("e")) {
        pieceType = PieceType.EDGE;
      } else if (pieceTypeArg.equals("c")) {
        pieceType = PieceType.CORNER;
      } else {
        throw new IllegalArgumentException("Illegal piece type argument: " + pieceTypeArg);
      }

      detectAllErrors(fileName, buffer, pieceType, containsHeaders, containsEmptyBufferGroups, ignoreEmpty);
    } catch (ParseException e) {
      System.err.println("Error parsing command-line arguments: " + e.getMessage());
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("check_comms.(bat|sh)", options);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }
}
