package com.kobr4.tradebot.routes

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{ CurrencyPair, SupportedExchange }
import com.kobr4.tradebot.model.Asset
import com.kobr4.tradebot.services.PriceService

import scala.concurrent.ExecutionContext

trait PriceApiRoutes extends PlayJsonSupport {

  implicit def system: ActorSystem

  implicit def am: ActorMaterializer

  implicit def ec: ExecutionContext

  private val stringToZonedDateTime = Unmarshaller.strict[String, ZonedDateTime](ZonedDateTime.parse)

  private val stringToAsset = Unmarshaller.strict[String, Asset](s => Asset.fromString(s))

  private val stringToSupportedExchange = Unmarshaller.strict[String, SupportedExchange](s => SupportedExchange.fromString(s))

  private val stringToCurrencyPair = Unmarshaller.strict[String, CurrencyPair](s => s.toUpperCase.split('_').map(s => Asset.fromString(s)).toList match {
    case a :: b :: Nil => CurrencyPair(a, b)
  })

  lazy val priceApiRoutes: Route =
    path("price_history") {
      get {
        parameters('asset.as(stringToAsset), 'start.as(stringToZonedDateTime), 'end.as(stringToZonedDateTime), 'pair.as(stringToCurrencyPair).?) { (asset, start, end, maybePair) =>
          onSuccess(PriceService.getPriceHistory(maybePair.getOrElse(CurrencyPair(Asset.Usd, asset)), start, end)) { priceList =>
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
          parameters('asset.as(stringToAsset), 'start.as(stringToZonedDateTime), 'end.as(stringToZonedDateTime), 'days.as[Int], 'pair.as(stringToCurrencyPair).?) { (asset, start, end, days, maybePair) =>
            onSuccess(PriceService.getMovingAverageHistory(maybePair.getOrElse(CurrencyPair(Asset.Usd, asset)), start, end, days)) { priceList =>
              complete(priceList)
            }
          }
        }
      } ~
      path("weighted_moving") {
        get {
          parameters('asset.as(stringToAsset), 'start.as(stringToZonedDateTime), 'end.as(stringToZonedDateTime), 'days.as[Int], 'pair.as(stringToCurrencyPair).?) { (asset, start, end, days, maybePair) =>
            onSuccess(PriceService.getWeightedMovingAverageHistory(maybePair.getOrElse(CurrencyPair(Asset.Usd, asset)), start, end, days)) { priceList =>
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
