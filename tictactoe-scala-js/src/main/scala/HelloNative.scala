import sri.core._
import sri.core.ElementFactory._
import sri.core.ReactComponent
import sri.universal.components._
import sri.universal.styles.UniversalStyleSheet

import scala.scalajs.js.annotation.ScalaJSDefined

object HelloNative {
/*
  var game = Game.newGame
  def gamePlay(index: Int): Unit = {
    println("Game updated at ")
    game = Game.play(game.side, index, game)
  }
*/

  @ScalaJSDefined
  class ComponentBoard extends ReactComponent[Unit, ComponentBoard.State] {

    initialState(HelloNative.ComponentBoard.State(game = Game.newGame))

    def newGame() : Unit = {
      this.setState((state:HelloNative.ComponentBoard.State, Unit) => HelloNative.ComponentBoard.State(Game.newGame))
    }

    def iaPlay() : Unit = {
      this.setState((state:HelloNative.ComponentBoard.State, Unit) => HelloNative.ComponentBoard.State(Game.iaPlay(state.game.side, state.game)))
    }

    def updateGame(index: Int): Unit = {
      this.setState((state:HelloNative.ComponentBoard.State, Unit) => HelloNative.ComponentBoard.State(Game.play(state.game.side, index, state.game)))
      iaPlay()
    }

    def displayWinner(): Unit = {

    }

    def render() = {
/*
      View(style = styles.container)(
        Text(style = styles.text)(s"Welcome to Sri Web"),
        Image(style = styles.image, source = ImageSource(uri = "http://www.scala-js.org/images/scala-js-logo-256.png"))(),
        Text(style = styles.text)("Scala.js - Future of app development!")
      )
*/
      val array = state.game.plays.head.current
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
          state.game.hasWinner ?= View(style = styles.rowHiddenContainer)(
            Text()(s"Winner is ${state.game.winnerIs.getOrElse("")}"),
            Button(title="New Game", onPress=() => newGame(), style=styles.button)()
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
          Text()("Turn: "+state.game.side.toString),
          Button(title="New Game", onPress=() => newGame(), style=styles.button)()
        )
      )
    }
  }

  object ComponentBoard {

    case class State(game: Game)

    //def apply() = CreateElement[ComponentBoard]()
  }

  object styles extends UniversalStyleSheet {

    val container = style(flexOne,
      flexDirection := "row",
      backgroundColor := "rgb(175, 9, 119)",
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
    val square = style(
      width := 50,
      height := 50,
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

      //shadowOffset := json(height = 1, width = 0))
    //    val buttonCommon = style()

  }

  def apply() = makeElement[ComponentBoard]
}