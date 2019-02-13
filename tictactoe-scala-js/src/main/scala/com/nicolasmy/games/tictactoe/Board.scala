package com.nicolasmy.games.tictactoe

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

