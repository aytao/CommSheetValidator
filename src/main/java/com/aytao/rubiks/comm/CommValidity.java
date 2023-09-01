package com.aytao.rubiks.comm;

public enum CommValidity {
  UNBALANCED_BRACKETS, ILLEGAL_FORMATTING, ILLEGAL_MOVE, PARSING_ERROR,
  DISRUPTS_OTHER_PIECES,
  INCORRECT_CYCLE,
  UNEXPECTED_EMPTY, SHOULD_BE_EMPTY,
  VALID;

  public static boolean isValid(CommValidity cv) {
    return cv == VALID;
  }
}
