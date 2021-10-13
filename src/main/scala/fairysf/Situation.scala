package strategygames.fairysf

import strategygames.{ Color, Status }

import cats.data.Validated
import cats.implicits._

import strategygames.fairysf.format.Uci

case class Situation(board: Board, color: Color) {

  lazy val actors = board actorsOf color

  lazy val moves: Map[Pos, List[Move]] = board.variant.validMoves(this)

  //lazy val playerCanCapture: Boolean = moves exists (_._2 exists (_.captures))

  lazy val destinations: Map[Pos, List[Pos]] = moves.view.mapValues { _ map (_.dest) }.to(Map)

  def drops: Option[List[Pos]] =
    board.variant match {
      //case v: variant.Shogi.type => v possibleDrops this
      case _                     => None
    }

  //lazy val kingPos: Option[Pos] = board kingPosOf color

  //stub
  lazy val check: Boolean = false

  //stub
  def checkSquare = None

  def history = board.history

  def checkMate: Boolean = board.variant checkmate this

  private def staleMate: Boolean = board.variant staleMate this

  private def autoDraw: Boolean = board.autoDraw || board.variant.specialDraw(this)

  def opponentHasInsufficientMaterial: Boolean = board.variant.opponentHasInsufficientMaterial(this)

  lazy val threefoldRepetition: Boolean = board.history.threefoldRepetition

  private def variantEnd = board.variant specialEnd this

  def end: Boolean = checkMate || staleMate || autoDraw || variantEnd

  def winner: Option[Color] = board.variant.winner(this)

  def playable(strict: Boolean): Boolean =
    (board valid strict) && !end && !copy(color = !color).check

  lazy val status: Option[Status] =
    if (checkMate) Status.Mate.some
    else if (variantEnd) Status.VariantEnd.some
    else if (staleMate) Status.Stalemate.some
    else if (autoDraw) Status.Draw.some
    else none

  def move(from: Pos, to: Pos, promotion: Option[PromotableRole]): Validated[String, Move] =
    board.variant.move(this, from, to, promotion)

  def move(uci: Uci.Move): Validated[String, Move] =
    board.variant.move(this, uci.orig, uci.dest, uci.promotion)

  def drop(role: Role, pos: Pos): Validated[String, Drop] =
    board.variant.drop(this, role, pos)

  //def fixCastles = copy(board = board fixCastles)

  //def withHistory(history: History) =
  //  copy(
  //    board = board withHistory history
  //  )

  def withVariant(variant: strategygames.fairysf.variant.Variant) =
    copy(
      board = board withVariant variant
    )

  //def canCastle = board.history.canCastle _

  //stub
  def enPassantSquare: Option[Pos] = None//{
  //  // Before potentially expensive move generation, first ensure some basic
  //  // conditions are met.
  //  history.lastMove match {
  //    case Some(move: Uci.Move) =>
  //      if (
  //        move.dest.yDist(move.orig) == 2 &&
  //        board(move.dest).exists(_.is(Pawn)) &&
  //        List(
  //          move.dest.file.offset(-1),
  //          move.dest.file.offset(1)
  //        ).flatten
  //        .flatMap(board(_, Rank.passablePawnRank(color)))
  //        .exists(_ == Piece(color, Pawn))
  //      )
  //        moves.values.flatten.find(_.enpassant).map(_.dest)
  //      else None
  //    case _ => None
  //  }
  //}

  def unary_! = copy(color = !color)
}

object Situation {

  def apply(variant: strategygames.fairysf.variant.Variant): Situation = Situation(Board init variant, variant.startColor)
}
