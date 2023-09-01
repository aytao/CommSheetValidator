/* *****************************************************************************
 *  Author:       Andrew Tao
 *
 *  Description:  An enum representing all six faces in the Cube class.
 *
 **************************************************************************** */

package com.aytao.rubiks.cube;

enum Face {
  U(Cube.U),
  L(Cube.L),
  F(Cube.F),
  R(Cube.R),
  B(Cube.B),
  D(Cube.D);

  final int faceNum;

  /* Creates a new Face with the specified number associated with it */
  Face(int num) {
    this.faceNum = num;
  }
}
