sealed trait Side {
  def other: Side = this match {
    case O => X
    case X => O
  }
}

case object O extends Side

case object X extends Side

case class Board(turn: Side, current: Array[Side]) {

  def hasWinner: Boolean = {
    (for (i <- 0 to 3) yield {
      (current(0 + i * 3) == current(1 + i * 3)) && (current(1 + i * 3) == current(2 + i * 3))
    }).find(_ == true).getOrElse({
      for (i <- 0 to 3) yield {
        (current(i) == current(i + 3)) && (current(i + 3) == current(i * 6))
      }
    }.find(_ == true).getOrElse(false))
  }

}

case class Game(plays: List[Board]) {

  def side: Side = plays.headOption.map(_.turn.other).getOrElse(O)

  def hasWinner: Boolean = plays.headOption.exists(_.hasWinner)
}

object Game {

  def play(side: Side, row: Int, column: Int, game: Game): Game = {
    val board = game.plays.head.current.clone()
    board.update(row+column*3, side)
    Game(Board(side, board)::game.plays)
  }

  def newGame: Game = {
    Game(List(Board(O, new Array[Side](9))))
  }
}