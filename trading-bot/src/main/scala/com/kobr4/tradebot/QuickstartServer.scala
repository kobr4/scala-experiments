package com.kobr4.tradebot

//#quick-start-server
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.routes.TradingBotRoutes
import com.kobr4.tradebot.scheduler.SchedulerJob
import com.kobr4.tradebot.services.SchedulingService
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext }
import scala.util.{ Failure, Success }

//#main-class
object QuickstartServer extends App with StrictLogging with TradingBotRoutes {

  // set up ActorSystem and other dependencies here
  //#main-class
  //#server-bootstrapping
  implicit val system: ActorSystem = ActorSystem("helloAkkaHttpServer")
  implicit val am: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  //#server-bootstrapping

  //#main-class
  // from the UserRoutes trait
  lazy val routes: Route = tradingBotRoutes
  //#main-class

  lazy val schedulingService = new SchedulingService()
  SchedulerJob.loadConfiguration(DefaultConfiguration, schedulingService).foreach {
    case Failure(f) => logger.error("Failure to instantiable scheduling job: {}", f.getMessage)
    case Success(v) =>
  }

  //schedulingService.schedule("toto", "*/30 * * * * ?", () => println("Hello"))
  //#http-server
  Http().bindAndHandle(routes, "0.0.0.0", 8080)

  println(s"Server online at http://0.0.0.0:8080/")

  Await.result(system.whenTerminated, Duration.Inf)
  //#http-server
  //#main-class
}
//#main-class
//#quick-start-server
