import com.nicolasmy.games.tictactoe.circuit.{IaPlay, NewGame, Play, TicTacToeCircuit}
import com.nicolasmy.games.tictactoe.{Game, O, Score, X}
import diode.{Dispatcher, ModelR, ModelRO}
import sri.core._
import sri.core.ElementFactory._
import sri.core.ReactComponent
import sri.universal.components.{Text, _}
import sri.universal.styles.UniversalStyleSheet

import scala.scalajs.js.annotation.ScalaJSDefined

object TicTacToeView {

  @ScalaJSDefined
  class ComponentBoard extends ReactComponent[Unit, ComponentBoard.State] {

    initialState ({
      val circuit  =  new TicTacToeCircuit()
      circuit.subscribe(circuit.zoom[Game](g=>g))(m =>
        this.setState((state: TicTacToeView.ComponentBoard.State, Unit) => TicTacToeView.ComponentBoard.State(m, circuit))
      )
      TicTacToeView.ComponentBoard.State(game = circuit.zoom[Game](g=>g), dispatcher = circuit)
    })

    def newGame(): Unit = {
      this.state.dispatcher.dispatch(NewGame)
    }

    def iaPlay(): Unit = {
      /*
      this.setState((state: TicTacToeView.ComponentBoard.State, Unit) =>
        if (state.game.side == O) TicTacToeView.ComponentBoard.State(Game.iaPlay(state.game.side, state.game), state.score) else state)
        */
      this.state.dispatcher.dispatch(IaPlay)
    }

    def updateGame(index: Int): Unit = {
      /*
      if (!state.game.hasWinner) {

        this.setState((state: TicTacToeView.ComponentBoard.State, Unit) =>
          TicTacToeView.ComponentBoard.State(Game.play(state.game.side, index, state.game), state.score))
        iaPlay()

        this.setState((state: TicTacToeView.ComponentBoard.State, Unit) =>
          TicTacToeView.ComponentBoard.State(state.game, state.score.update(state.game)))
      }
      */
      this.state.dispatcher.dispatch(Play(index))
    }

    def render() = {

      val array = state.game.value.plays.head.current
      View(style = styles.container)(
        View(style = styles.rowContainer)(
          TouchableHighlight(onPress = () => updateGame(0))(
            View(style = styles.square)(Text()(array(0).getOrElse("").toString))
          ),
          TouchableHighlight(onPress = () => updateGame(1))(
            View(style = styles.square)(Text()(array(1).getOrElse("").toString))
          ),
          TouchableHighlight(onPress = () => updateGame(2))(
            View(style = styles.square)(Text()(array(2).getOrElse("").toString))
          )
        ),
        View(style = styles.rowContainer)(
          TouchableHighlight(onPress = () => updateGame(3))(
            View(style = styles.square)(Text()(array(3).getOrElse("").toString))
          ),
          TouchableHighlight(onPress = () => updateGame(4))(
            View(style = styles.square)(Text()(array(4).getOrElse("").toString))
          ),
          TouchableHighlight(onPress = () => updateGame(5))(
            View(style = styles.square)(Text()(array(5).getOrElse("").toString))
          ),
          state.game.value.hasWinner ?= View(style = styles.rowHiddenContainer)(
            Text()(s"Winner is ${state.game.value.winnerIs.getOrElse("")}"),
            Button(title = "New Game", onPress = () => newGame(), style = styles.button)()
          )
        ),
        View(style = styles.rowContainer)(
          TouchableHighlight(onPress = () => updateGame(6))(
            View(style = styles.square)(Text()(array(6).getOrElse("").toString))
          ),
          TouchableHighlight(onPress = () => updateGame(7))(
            View(style = styles.square)(Text()(array(7).getOrElse("").toString))
          ),
          TouchableHighlight(onPress = () => updateGame(8))(
            View(style = styles.square)(Text()(array(8).getOrElse("").toString))
          )
        ),
        View(style = styles.rowContainer)(
          View(style = styles.panelContainer)(
            Text()("Turn: " + state.game.value.side.toString),
            Button(title = "New Game", onPress = () => newGame(), style = styles.button)(),
            Text()(state.game.value.score.getString)
          )
        )
      )
    }
  }

  object ComponentBoard {

    case class State(game: ModelRO[Game], dispatcher : Dispatcher)

  }

  object styles extends UniversalStyleSheet {

    val container = style(flexOne,
      flexDirection := "row",
      backgroundColor := "rgb(169,169,169)",
      justifyContent.center)

    val image = style(width := 256, height := 256, margin := 20)

    val text = style(fontWeight._500,
      fontSize := 18,
      color := "white")

    val rowHiddenContainer = style(
      flex := -1,
      flexDirection := "column",
      backgroundColor := "red",
      justifyContent.center,
      position.absolute
    )

    val rowContainer = style(
      flex := -1,
      flexDirection := "column",
      //backgroundColor := "steelblue",
      justifyContent.center
    )

    val panelContainer = style(
      flex := -1,
      padding := 20,
      flexDirection := "column",
      //backgroundColor := "rgb(130,130,130)",
      //backgroundColor := "steelblue",
      justifyContent.center,
      alignItems.center
    )

    val square = style(
      flex := -1,
      width := 50,
      height := 50,
      borderWidth := 1,
      borderColor := "black",
      backgroundColor := "powderblue",
      justifyContent.center,
      alignItems.center
    )

    val button = style(width := 150,
      height := 150,
      backgroundColor := "#E84254",
      shadowColor := "black",
      shadowOpacity := 0.2,
      shadowRadius := 2,
      justifyContent.center,
      alignItems.center)

  }

  def apply() = makeElement[ComponentBoard]
}