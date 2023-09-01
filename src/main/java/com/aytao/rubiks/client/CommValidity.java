/* *****************************************************************************
 *  Author:       Andrew Tao
 *
 *  Description:  An enum representing all possible comm validity states.
 *
 **************************************************************************** */

package com.aytao.rubiks.client;

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
