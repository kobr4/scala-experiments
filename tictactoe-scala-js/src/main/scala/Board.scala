import scala.annotation.tailrec

sealed trait Side {
  def other: Side = this match {
    case O => X
    case X => O
  }
}

case object O extends Side { override def toString = "O"}

case object X extends Side { override def toString = "X"}

case class Board(turn: Side, current: Seq[Option[Side]]) {

  def hasWinner(side: Side): Boolean = {
    (for (i <- 0 to 2) yield {
      (current(0 + i * 3).contains(side) && current(0 + i * 3) == current(1 + i * 3)) && (current(1 + i * 3) == current(2 + i * 3))
    }).find(_ == true).getOrElse({
      for (i <- 0 to 2) yield {
        (current(i).contains(side) && current(i) == current(i + 3)) && (current(i + 3) == current(i + 6))
      }
    }.find(_ == true).getOrElse(
      (current(0).contains(side) && current(0) == current(4)) && (current(4) == current(8)) ||
        (current(2).contains(side) && current(2) == current(4)) && (current(4) == current(6))
    ))
  }


  def print() = {
    Console.println(s"${current(0).getOrElse(" ")}${current(1).getOrElse(" ")}${current(2).getOrElse(" ")}")
    Console.println(s"${current(3).getOrElse(" ")}${current(4).getOrElse(" ")}${current(5).getOrElse(" ")}")
    Console.println(s"${current(6).getOrElse(" ")}${current(7).getOrElse(" ")}${current(8).getOrElse(" ")}")
  }
}

case class Game(plays: List[Board]) {

  def side: Side = plays.headOption.map(_.turn).getOrElse(O)

  def hasWinner: Boolean = plays.headOption.exists(b => b.hasWinner(O) || b.hasWinner(X))

  def winnerIs: Option[Side] = plays.headOption match {
    case Some(b) if b.hasWinner(O) => Some(O)
    case Some(b) if b.hasWinner(X) => Some(X)
    case _ => None
  }
}

object Game {

  def generate(board: Board) : List[Board] =
    scala.util.Random.shuffle((0 to 8).flatMap(i =>
      if (board.current(i).isEmpty)
        Some(board.current.updated(i, Some(board.turn)))
      else None).map(b => Board(board.turn.other, b)).toList)


  def minMaxReducer(side: Side, currentSide: Side,t1: (Int, List[Board]), t2: (Int, List[Board])) = {
    (side, currentSide) match {
      case (O, O) => if (t1._1 > t2._1) t1 else t2
      case (O, X) => if (t1._1 < t2._1) t1 else t2
      case (X, O) => if (t1._1 > t2._1) t1 else t2
      case (X, X) => if (t1._1 < t2._1) t1 else t2
    }

  }

  private def iaPlay(side: Side, inBoard : List[Board], cDepth: Int, maxDepth: Int) : Option[(Int, List[Board])] = {
    generate(inBoard.last).flatMap(board =>

      if (board.hasWinner(O)) {
        Some(100, inBoard:::board::Nil)
      } else
      if (board.hasWinner(X)){
        Some(-100, inBoard:::board::Nil)
      } else
      if (cDepth == maxDepth) {
        Some(0, inBoard:::board::Nil)
      } else
      if (board.current.count(_.isEmpty) == 0) {
        Some(0, inBoard:::board::Nil)
      }
      else
      iaPlay(side, inBoard:::board::Nil, cDepth+1, maxDepth)
    ).map(t => {
      if (cDepth == 0) Console.println(s"D${cDepth} Score: ${t._1}")
      t
    }
    ).reduceLeftOption(minMaxReducer(side, inBoard.last.turn, _, _))
  }


  def iaPlay(side: Side, game: Game) : Game = {
    iaPlay(side, List(game.plays.head), 0, 4).flatMap(t => t._2.tail.headOption.map(board => {
      Game(board :: game.plays)
    }
    )).getOrElse(game)

  }


  def play(side: Side, index: Int, game: Game): Game = {
    val board = game.plays.head.current.updated(index, Some(side))
    Game(Board(side.other, board)::game.plays)
  }

  def newGame: Game = {
    Game(List(Board(X, Seq[Option[Side]](None, None, None, None, None, None, None, None, None))))
  }
}