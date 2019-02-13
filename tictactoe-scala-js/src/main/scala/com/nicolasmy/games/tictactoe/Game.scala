package com.nicolasmy.games.tictactoe

case class Game(plays: List[Board], score: Score) {

  def side: Side = plays.headOption.map(_.turn).getOrElse(O)

  def hasWinner: Boolean = plays.headOption.exists(b => b.hasWinner(O) || b.hasWinner(X))

  def winnerIs: Option[Side] = plays.headOption match {
    case Some(b) if b.hasWinner(O) => Some(O)
    case Some(b) if b.hasWinner(X) => Some(X)
    case _ => None
  }

  def isDraw: Boolean = !hasWinner && plays.headOption.exists(!_.current.exists(b => b.isEmpty))

  def updateScore = Game(this.plays, score.update(this))
}

case class Score(wins: Int, losses: Int) {

  def getString: String = s"W $wins - L $losses"

  def update(game: Game): Score = {
    game.winnerIs match {
      case Some(O) => Score(wins, losses + 1)
      case Some(X) => Score(wins + 1, losses)
      case None => this
    }
  }
}

object Game {

  def generate(board: Board): Stream[Board] =
    scala.util.Random.shuffle((0 to 8).flatMap(i =>
      if (board.current(i).isEmpty)
        Some(board.current.updated(i, Some(board.turn)))
      else None).map(b => Board(board.turn.other, b)).toStream)


  def minMaxReducer(side: Side, currentSide: Side, t1: (Int, List[Board]), t2: (Int, List[Board])) = {
    (side, currentSide) match {
      case (O, O) => if (t1._1 > t2._1) t1 else t2
      case (O, X) => if (t1._1 < t2._1) t1 else t2
      case (X, O) => if (t1._1 > t2._1) t1 else t2
      case (X, X) => if (t1._1 < t2._1) t1 else t2
    }
  }

  private def iaPlay(side: Side, inBoard: List[Board], cDepth: Int, maxDepth: Int): Option[(Int, List[Board])] = {
    generate(inBoard.last).flatMap(board =>
      board match {
        case _ if board.hasWinner(O) => Some(100, inBoard ::: board :: Nil)
        case _ if board.hasWinner(X) => Some(-100, inBoard ::: board :: Nil)
        case _ if cDepth == maxDepth => Some(0, inBoard ::: board :: Nil)
        case _ if board.current.count(_.isEmpty) == 0 => Some(0, inBoard ::: board :: Nil)
        case _ => iaPlay(side, inBoard ::: board :: Nil, cDepth + 1, maxDepth)
      }
    ).reduceLeftOption(minMaxReducer(side, inBoard.last.turn, _, _))
  }

  def iaPlay(side: Side, game: Game): Game = {
    if (!game.hasWinner) {
      iaPlay(side, List(game.plays.head), 0, 4).
        flatMap(t => t._2.tail.headOption.map(board => {
          Game(board :: game.plays, game.score).updateScore
        }
        )).getOrElse(game)
    } else game
  }


  def play(side: Side, index: Int, game: Game): Game = {
    if (!game.hasWinner && game.plays.head.current(index).isEmpty) {
      val board = game.plays.head.current.updated(index, Some(side))
      Game(Board(side.other, board) :: game.plays, game.score).updateScore
    } else game
  }

  def newGame(score: Score): Game = {
    Game(List(Board(X, Seq[Option[Side]](None, None, None, None, None, None, None, None, None))), score)
  }
}