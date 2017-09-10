package com.kobr4.snippets

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.kobr4.polo.PairPrice

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn // Async

object WebServer {
  def main(args: Array[String]) {

    implicit val system: ActorSystem = ActorSystem("my-system")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

//    SlackBot.start(args(0))

    val route =
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      } ~
      path("test") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, Page.helloWorld(Sample("toto", "titi"))))
        } ~
        post { formField('name,'mail) {(name,mail) =>
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, Page.helloWorld(Sample(name, mail))))
        }}
      }  ~
        path("quote" / Segment) { pairName =>
          get {
            onSuccess(Http().singleRequest(HttpRequest(uri = "https://poloniex.com/public?command=returnTicker"))) {
              response =>
                onSuccess(Unmarshal(response.entity).to[String]) { body =>
                  val httpResp = PairPrice.fromString(body).filter(pair => pair.name.contains(pairName)).map {
                    pair =>
                      HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>${pair.name}:${pair.last}</h1>")
                  }.headOption.getOrElse(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Pair not found</h1>"))

                  complete(httpResp)
                }
            }
          }
        }
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}