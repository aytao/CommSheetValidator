package com.aytao.rubiks.comm;

import java.util.ArrayList;
import java.util.regex.Pattern;

import com.aytao.rubiks.cube.Move;
import com.aytao.rubiks.cube.Sequence;

public class Comm {

  public static class UnbalancedBracketsException extends IllegalArgumentException {
    private String commString;

    public UnbalancedBracketsException(String commString) {
      super();
      this.commString = commString;
    }

    @Override
    public String toString() {
      return "Comm string '" + this.commString + "' has unbalanced brackets";
    }

  }

  private Component root;
  private String originalString;
  private static final char L_BRACKET = '[';
  private static final char R_BRACKET = ']';

  private static final String DOUBLE_REGEX = "(?s)\\(.*\\)2";
  private static final Pattern DOUBLE_PATTERN = Pattern.compile(DOUBLE_REGEX);

  public Comm(String commStr) {
    this.originalString = commStr;
    this.root = parse(commStr);
  }

  public ArrayList<Move> toSequence() {
    return root.toSequence();
  }

  public String toString() {
    return originalString;
  }

  /*****************************************************************************
   * Parsing
   ****************************************************************************/
  private static boolean balanced(String commStr) {
    int count = 0;

    for (int i = 0; i < commStr.length(); i++) {
      if (commStr.charAt(i) == L_BRACKET) {
        count++;
      } else if (commStr.charAt(i) == R_BRACKET) {
        count--;
      }

      if (count < 0) {
        return false;
      }
    }

    return count == 0;
  }

  private static String stripBrackets(String commStr) {
    String trimmed = commStr.trim();
    int length = trimmed.length();

    if (length == 0) {
      return commStr;
    }
    if (trimmed.charAt(0) == L_BRACKET && trimmed.charAt(length - 1) == R_BRACKET) {
      String stripped = trimmed.substring(1, length - 1);
      // If stripped string is unbalanced, then brackets removed do not correspond to
      // each other
      if (balanced(stripped)) {
        return stripped;
      }
    }

    return trimmed;
  }

  private static boolean isSequential(String commStr) {
    char[] specialChars = { ',', ':', '[', ']', '(', ')', '/' };

    for (int i = 0; i < commStr.length(); i++) {
      for (int j = 0; j < specialChars.length; j++) {
        if (commStr.charAt(i) == specialChars[j])
          return false;
      }
    }

    return true;
  }

  private static boolean isDouble(String commStr) {
    return DOUBLE_PATTERN.matcher(commStr).matches();
  }

  /*
   * Finds and returns the first unbracketed appearance of c, or -1 if there is
   * no such appearance. Does not work for bracket characters ('[' or ']')
   */
  private static int findFirstUnbracketed(String commStr, char c) {
    int count = 0;

    for (int i = 0; i < commStr.length(); i++) {
      if (commStr.charAt(i) == L_BRACKET) {
        count++;
      } else if (commStr.charAt(i) == R_BRACKET) {
        count--;
      } else if (count == 0 && commStr.charAt(i) == c) {
        return i;
      }
    }

    return -1;
  }

  private static Component parse(String commStr) {
    if (!balanced(commStr)) {
      throw new UnbalancedBracketsException("String provided has unbalanced brackets");
    }

    commStr = stripBrackets(commStr);

    if (isSequential(commStr)) {
      return new SequenceComponent(commStr);
    }

    if (isDouble(commStr)) {
      return new DoubleComponent(parse(commStr.substring(1, commStr.length() - 2)));
    }

    // Conjugates
    int colonSplit = findFirstUnbracketed(commStr, ':');
    if (colonSplit >= 0) {
      Component setup = parse(commStr.substring(0, colonSplit));
      Component nested = parse(commStr.substring(colonSplit + 1));
      return new ConjugateComponent(setup, nested);
    }

    // Pure Comms
    int commaSplit = findFirstUnbracketed(commStr, ',');
    if (commaSplit >= 0) {
      Component first = parse(commStr.substring(0, commaSplit));
      Component second = parse(commStr.substring(commaSplit + 1));
      return new CommutatorComponent(first, second);
    }

    // Slashes
    int slashSplit = findFirstUnbracketed(commStr, '/');
    if (slashSplit >= 0) {
      Component setup = parse(commStr.substring(0, slashSplit));
      Component slice = parse(commStr.substring(slashSplit + 1));
      return new SlashComponent(setup, slice);
    }

    throw new IllegalArgumentException(commStr);
  }

  public static void main(String[] args) {
    String[] tests = {
        "R U R' U'",
        "[R, U]",
        "[Rw2, u]",
        // Conjugates should work regardless of bracket usage
        "[U' : [S , R' B R]]",
        "[R' : [U' R' U , M]]",
        "R' : [U' R' U , M]",
        "R' : U' R' U , M",
        // Pure comms should work regardless of bracket usage
        "[L F' L' , S]",
        "L F' L' , S",
        // Slash components should work even when nested in conjugates
        "U / M'",
        "M: U / M'",
        // Double sequences should work
        "M2' : (U M U M')2", };

    for (String test : tests) {
      Comm comm = new Comm(test);
      System.out.println(Sequence.toString(comm.toSequence()));
    }

  }
}
