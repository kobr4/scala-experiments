package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext

trait TradingBotRoutes {
  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  implicit def am: ActorMaterializer

  private lazy val log = Logging(system, classOf[TradingBotRoutes])

  implicit def ec: ExecutionContext

  lazy val tradingBotRoutes: Route = {
    pathPrefix("public") {
      getFromResourceDirectory("public")
    } ~ pathPrefix("api") {
      getFromResource("public/api.html")
    } ~ pathPrefix("price_api") {
      path("btc_history") {
        get {
          onSuccess(PriceService.getBtcPriceHistory()) { priceList =>
            complete(Json.toJson(priceList).toString())
          }
        }
      }
    } ~ get {
      complete("Hello World")
    }

  }
}
