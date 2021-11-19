package strategygames.fairysf
package variant

import strategygames.GameFamily

import cats.implicits._

case object Xiangqi
    extends Variant(
      id = 2,
      key = "xiangqi",
      name = "Xiangqi",
      shortName = "Xiangqi",
      title = "Xiangqi (Chinese Chess)",
      standardInitialPosition = true,
      fairysfName=FairySFName("xiangqi"),
      boardSize = Board.Dim9x10
    ) {

  override def gameFamily: GameFamily = GameFamily.Xiangqi()

  def perfIcon: Char = 't'
  def perfId: Int = 201

  override def baseVariant: Boolean = true

  val kingPiece: Role = XiangqiKing

}
