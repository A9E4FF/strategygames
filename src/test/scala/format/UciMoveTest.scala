package strategygames.chess
package format

import cats.syntax.option._

class UciMoveTest extends ChessTest {

  "piotr encoding" should {
    "be reflexive" in {
      val move = Uci.Move("a2g7").get
      Uci.Move piotr move.piotr must_== move.some
    }
  }
}
