package strategygames.chess

import cats.syntax.option._

import strategygames.chess.format.FEN
import strategygames.chess.Pos._
import strategygames.chess.variant.Standard

class PromotionTest extends ChessTest {

  "pawn promotion" should {
    val board = """
  p
K      """
    val game  = Game(board, P2)
    "promote to a queen" in {
      game.playMove(C2, C1, Queen.some) must beGame("""

K q    """)
    }
    "promote to a queen by default" in {
      game.playMove(C2, C1) must beGame("""

K q    """)
    }
    "promote to a knight" in {
      game.playMove(C2, C1, Knight.some) must beGame("""

K n    """)
    }
    "promote to a queen by killing" in {
      Game(
        """
  p
K  R""",
        P2
      ).playMove(C2, D1) must beGame("""

K  q""")
    }
    "promote to a knight by killing" in {
      Game(
        """
  p
K  R""",
        P2
      ).playMove(C2, D1, Knight.some) must beGame("""

K  n""")
    }
    "promote to a p1knight" in {
      Game("""

P





K n    """).playMove(A7, A8, Knight.some) must beGame("""
N






K n    """)
    }

    "Not allow promotion to a king in a standard game " in {
      val fen  = FEN("8/1P6/8/8/8/8/7k/1K6 w - -")
      val game = fenToGame(fen, Standard)

      val failureGame = game flatMap (_.apply(Pos.B7, Pos.B8, Option(King))) map (_._1)

      failureGame must beInvalid
    }

  }
}
