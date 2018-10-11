package com.kobr4.tradebot.routes

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.SupportedExchange
import com.kobr4.tradebot.model.Asset
import com.kobr4.tradebot.services.PriceService

import scala.concurrent.ExecutionContext

trait PriceApiRoutes extends PlayJsonSupport {

  implicit def system: ActorSystem

  implicit def am: ActorMaterializer

  implicit def ec: ExecutionContext

  private val stringToZonedDateTime = Unmarshaller.strict[String, ZonedDateTime](ZonedDateTime.parse)

  private val stringToAsset = Unmarshaller.strict[String, Asset](s => Asset.fromString(s).getOrElse(Asset.Btc))

  private val stringToSupportedExchange = Unmarshaller.strict[String, SupportedExchange](s => SupportedExchange.fromString(s))

  lazy val priceApiRoutes: Route =
    path("price_history") {
      get {
        parameters('asset.as(stringToAsset), 'start.as(stringToZonedDateTime), 'end.as(stringToZonedDateTime)) { (asset, start, end) =>
          onSuccess(PriceService.getPriceHistory(asset, start, end)) { priceList =>
            complete(priceList)
          }
        }
      }
    } ~ path("price_at") {
      get {
        parameters('asset.as(stringToAsset), 'date.as(stringToZonedDateTime)) { (asset, date) =>
          onSuccess(PriceService.getPriceAt(asset, date)) { price =>
            complete(price)
          }
        }
      }
    } ~
      path("moving") {
        get {
          parameters('asset.as(stringToAsset), 'start.as(stringToZonedDateTime), 'end.as(stringToZonedDateTime), 'days.as[Int]) { (asset, start, end, days) =>
            onSuccess(PriceService.getMovingAverageHistory(asset, start, end, days)) { priceList =>
              complete(priceList)
            }
          }
        }
      } ~
      path("weighted_moving") {
        get {
          parameters('asset.as(stringToAsset), 'start.as(stringToZonedDateTime), 'end.as(stringToZonedDateTime), 'days.as[Int]) { (asset, start, end, days) =>
            onSuccess(PriceService.getWeightedMovingAverageHistory(asset, start, end, days)) { priceList =>
              complete(priceList)
            }
          }
        }
      } ~ path("ticker") {
        get {
          parameters('exchange.as(stringToSupportedExchange)) { exchange =>
            onSuccess(PriceService.priceTicker(exchange)) { quoteList =>
              complete(quoteList)
            }
          }
        }
      }
}
