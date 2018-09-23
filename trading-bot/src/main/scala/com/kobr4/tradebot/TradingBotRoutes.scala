package com.kobr4.tradebot

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext

trait TradingBotRoutes extends PlayJsonSupport {
  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  implicit def am: ActorMaterializer

  private lazy val log = Logging(system, classOf[TradingBotRoutes])

  implicit def ec: ExecutionContext

  private val stringToZonedDateTime = Unmarshaller.strict[String, ZonedDateTime](ZonedDateTime.parse)

  lazy val tradingBotRoutes: Route = {
    pathPrefix("public") {
      getFromResourceDirectory("public")
    } ~ pathPrefix("api") {
      getFromResource("public/api.html")
    } ~ path("btc_price") {
      getFromResource("public/api.html")
    } ~ pathPrefix("price_api") {
      path("btc_history") {
        get {
          parameters('start.as(stringToZonedDateTime), 'end.as(stringToZonedDateTime)) { (start, end) =>
            onSuccess(PriceService.getBtcPriceHistory(start, end)) { priceList =>
              complete(priceList)
            }
          }
        }
      } ~ path("eth_history") {
        get {
          onSuccess(PriceService.getEthPriceHistory()) { priceList =>
            complete(priceList)
          }
        }
      }
    } ~ get {
      complete("Hello World")
    }

  }
}
