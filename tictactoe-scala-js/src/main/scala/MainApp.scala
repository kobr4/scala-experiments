import org.scalajs.dom
import sri.web.ReactDOM

import scala.scalajs.js.annotation.JSExport
//import sri.mobile.ReactNative
//import sri.mobile.all._

import scala.scalajs.js.JSApp

object MainApp extends JSApp {
  /*
    def main(): Unit = {
      println("Starting 'tictactoe-scala-js'...")

      val p = document.createElement("p")
      val text = document.createTextNode("Hello!")
      p.appendChild(text)
      document.body.appendChild(p)

      val root = createReactNativeRoot(HelloNative.apply())
      ReactNative.AppRegistry.registerComponent("SriMobile", () => root)
    }
  */
  @JSExport
  override def main(): Unit = {
    //AppStyles.load()
    ReactDOM.render(TicTacToeView.apply(), dom.document.getElementById("app"))
  }

}
