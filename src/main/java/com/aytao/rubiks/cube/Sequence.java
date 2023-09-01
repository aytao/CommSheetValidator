/* *****************************************************************************
 *  Author:    Andrew Tao
 *
 *  Description:  Provides methods for handling sequences of moves, which are
 *                also commonly (and misrepresentatively) referred to as
 *                "algorithms".
 *
 **************************************************************************** */

package com.aytao.rubiks.cube;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.Stack;
import com.aytao.rubiks.utils.ResourceHandler;

public class Sequence {
  /* Returns a string of each move seperated by spaces */
  public static String toString(ArrayList<Move> scramble) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < scramble.size(); i++) {
      if (i != 0) {
        sb.append(" ");
      }
      sb.append(scramble.get(i));
    }
    return sb.toString();
  }

  /*
   * Parses a given String for a sequence of a moves, and returns the moves as an
   * ArrayList of Moves.
   */
  public static ArrayList<Move> getSequence(String movesStr) {
    ArrayList<Move> moves = new ArrayList<>();
    if (movesStr == null || movesStr.equals("")) {
      return moves;
    }

    String[] stringMoves = movesStr.split(" ");

    for (String move : stringMoves) {
      moves.add(Move.move(move));
    }
    return moves;
  }

  /*
   * Gets all moves from a file, with the # character
   * marking the beginning of comments. Comments last until the
   * end of the line (terminated by the newline character).
   */
  public static ArrayList<Move> getSequenceFromFile(String fileName) {
    ArrayList<Move> moves = new ArrayList<>();

    try (Scanner in = new Scanner(ResourceHandler.getFile(fileName), "utf-8")) {
      while (in.hasNext()) {
        try {
          String s = in.next();
          if (s.charAt(0) == '#') {
            in.nextLine();
            continue;
          }
          moves.add(Move.move(s));
        } catch (Exception e) {
          System.out.println(e.getMessage());
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Error opening file: '" + fileName + "'");
    }

    return moves;
  }

  /* Returns the inverse of a given sequence of moves */
  public static ArrayList<Move> getInverse(ArrayList<Move> moves) {
    Stack<Move> stack = new Stack<>();
    ArrayList<Move> ret = new ArrayList<>();

    for (Move move : moves) {
      stack.push(move);
    }

    while (!stack.isEmpty()) {
      ret.add(Move.getInverse(stack.pop()));
    }

    return ret;
  }
}
