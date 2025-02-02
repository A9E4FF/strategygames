package strategygames.draughts
package variant

import cats.implicits._

case object Frisian
    extends Variant(
      id = 10,
      gameType = 40,
      key = "frisian",
      name = "Frisian",
      standardInitialPosition = true,
      boardSize = Board.D100
    ) {
  import Variant._

  def perfId: Int    = 111
  def perfIcon: Char = ''

  def pieces           = Standard.pieces
  def initialFen       = Standard.initialFen
  def startingPosition = Standard.startingPosition

  def moveDirsPlayer = Standard.moveDirsPlayer
  def moveDirsAll    = Standard.moveDirsAll

  val captureDirs: Directions = List(
    (UpLeft, _.moveUpLeft),
    (UpRight, _.moveUpRight),
    (Up, _.moveUp),
    (DownLeft, _.moveDownLeft),
    (DownRight, _.moveDownRight),
    (Down, _.moveDown),
    (Left, _.moveLeft),
    (Right, _.moveRight)
  )

  override def getCaptureValue(board: Board, taken: List[Pos]) = taken.foldLeft(0) { (t, p) =>
    t + getCaptureValue(board, p)
  }
  override def getCaptureValue(board: Board, taken: Pos)       =
    board(taken) match {
      case Some(piece) if piece.role == King => 199
      case Some(piece) if piece.role == Man  => 100
      case _                                 => 0
    }

  override def validMoves(situation: Situation, finalSquare: Boolean = false): Map[Pos, List[Move]] = {

    var bestLineValue = 0
    var captureMap    = Map[Pos, List[Move]]()
    var captureKing   = false
    for (actor <- situation.actors) {
      val capts = if (finalSquare) actor.capturesFinal else actor.captures
      if (capts.nonEmpty) {
        val lineValue = capts.head.taken.fold(0)(getCaptureValue(situation.board, _))
        if (lineValue > bestLineValue) {
          bestLineValue = lineValue
          captureMap = Map(actor.pos -> capts)
          captureKing = actor.piece.role == King
        } else if (lineValue == bestLineValue) {
          if (!captureKing && (actor.piece is King)) {
            captureMap = Map(actor.pos -> capts)
            captureKing = true
          } else if (captureKing == (actor.piece is King))
            captureMap = captureMap + (actor.pos -> capts)
        }
      }
    }

    if (captureMap.nonEmpty)
      captureMap
    else
      situation.actors
        .collect {
          case actor if actor.noncaptures.nonEmpty =>
            actor.pos -> actor.noncaptures
        }
        .to(Map)
  }

  override def finalizeBoard(
      board: Board,
      uci: format.Uci.Move,
      captured: Option[List[Piece]],
      situationBefore: Situation,
      finalSquare: Boolean
  ): Board = {
    val remainingCaptures =
      if (finalSquare) 0 else situationBefore.captureLengthFrom(uci.orig).getOrElse(0) - 1
    if (remainingCaptures > 0) board
    else
      board
        .actorAt(uci.dest)
        .fold(board) { act =>
          val tookLastMan  =
            captured.fold(false)(_.exists(_.role == Man)) && board.count(Man, !act.player) == 0
          val remainingMen = board.count(Man, act.player)
          if (remainingMen != 0)
            board updateHistory { h =>
              val kingmove      = act.piece.role == King && uci.promotion.isEmpty && captured.fold(true)(_.isEmpty)
              val differentKing = kingmove && act.player
                .fold(h.kingMoves.p1King, h.kingMoves.p2King)
                .fold(false)(_ != uci.orig)
              val hist          = if (differentKing) h.withKingMove(act.player, none, false) else h
              hist.withKingMove(act.player, uci.dest.some, kingmove, tookLastMan)
            }
          else {
            val promotedLastMan = uci.promotion.nonEmpty
            if (tookLastMan)
              board updateHistory { h =>
                val hist = if (promotedLastMan) h.withKingMove(act.player, none, false) else h
                h.withKingMove(!act.player, none, false)
              }
            else if (promotedLastMan)
              board updateHistory { _.withKingMove(act.player, none, false) }
            else
              board
          }
        } withoutGhosts
  }

  def maxDrawingMoves(board: Board): Option[Int] = {
    val remainingPieces = board.pieces.count(!_._2.isGhost)
    if (remainingPieces <= 3 && board.roleCount(Man) == 0) {
      if (remainingPieces == 3) Some(14)
      else Some(4)
    } else None
  }

  /** Update position hashes for frisian drawing rules:
    *   - When one player has two kings and the other one, the game is drawn after both players made 7 moves.
    *   - When bother players have one king left, the game is drawn after both players made 2 moves. The
    *     official rules state that the game is drawn immediately when both players have only one king left,
    *     unless either player can capture the other king immediately or will necessarily be able to do this
    *     next move. In absence of a good way to distinguish the positions that win by force from those that
    *     don't, this rule is implemented on lidraughts by always allowing 2 more moves to win the game.
    */
  def updatePositionHashes(
      board: Board,
      move: Move,
      hash: strategygames.draughts.PositionHash
  ): PositionHash = {
    val newHash = Hash(Situation(board, !move.piece.player))
    maxDrawingMoves(board) match {
      case Some(drawingMoves) =>
        if (move.captures || move.promotes)
          newHash         // 7 move rule resets only when another piece disappears, activating the "2-move rule"
        else
          newHash ++ hash // 2 move rule never resets once activated
      case _                  => newHash
    }
  }

}
