import sri.core.ElementFactory._
import sri.core.ReactComponent
import sri.universal.components.{Image, ImageSource, Text, View}
import sri.universal.styles.UniversalStyleSheet

import scala.scalajs.js.annotation.ScalaJSDefined

object HelloNative {


  @ScalaJSDefined
  class Component extends ReactComponent[Unit, Unit] {

    def render() = {
/*
      View(style = styles.container)(
        Text(style = styles.text)(s"Welcome to Sri Web"),
        Image(style = styles.image, source = ImageSource(uri = "http://www.scala-js.org/images/scala-js-logo-256.png"))(),
        Text(style = styles.text)("Scala.js - Future of app development!")
      )
*/
      View(style = styles.container)(
        View(style = styles.rowContainer)(
          View(style= styles.square)(),
          View(style= styles.square)(),
          View(style= styles.square)()
        ),
        View(style = styles.rowContainer)(
          View(style= styles.square)(),
          View(style= styles.square)(),
          View(style= styles.square)()
        ),
        View(style = styles.rowContainer)(
          View(style= styles.square)(),
          View(style= styles.square)(),
          View(style= styles.square)()
        )
      )
    }
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

    val rowContainer = style(
      flex := -1,
      flexDirection := "column",
      //backgroundColor := "steelblue",
      justifyContent.center
    )
    val square = style(
      width := 50,
      height := 50,
      backgroundColor := "powderblue"
    )

  }

  def apply() = makeElement[Component]
}