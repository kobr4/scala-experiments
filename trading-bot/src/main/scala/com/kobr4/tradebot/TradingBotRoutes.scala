package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.RouteDirectives.complete


import scala.concurrent.ExecutionContext

trait TradingBotRoutes {
  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  implicit def am: ActorMaterializer

  private lazy val log = Logging(system, classOf[TradingBotRoutes])

  implicit def ec: ExecutionContext

  lazy val tradingBotRoutes : Route = {
    get {
      complete("Hello World")
    }

  }
}
