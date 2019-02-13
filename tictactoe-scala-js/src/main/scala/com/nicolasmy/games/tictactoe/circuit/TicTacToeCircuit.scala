package com.nicolasmy.games.tictactoe.circuit

import com.nicolasmy.games.tictactoe.{Game, Score, Side}
import diode.ActionResult.ModelUpdate
import diode.{Action, Circuit}

case object NewGame extends Action

case object IaPlay extends Action

case class Play(index: Int) extends Action


class TicTacToeCircuit extends Circuit[Game] {
  override protected def initialModel: Game = Game.newGame(Score(0, 0))

  override protected def actionHandler: HandlerFunction =
    (model, action) => action match {
      case NewGame => Some(ModelUpdate(Game.newGame(model.score)))
      case IaPlay => Some(ModelUpdate(Game.iaPlay(model.side, model)))
      case Play(index) =>
        val game = Game.play(model.side, index, model)
        Some(ModelUpdate(Game.iaPlay(game.side, game)))
      case _ => None
    }
}
