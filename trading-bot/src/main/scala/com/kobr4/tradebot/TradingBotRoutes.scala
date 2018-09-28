package com.kobr4.tradebot

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.ActorMaterializer
import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, Writes }
import Quote._
import scala.concurrent.ExecutionContext

trait TradingBotRoutes extends PlayJsonSupport {

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  implicit def am: ActorMaterializer

  private lazy val log = Logging(system, classOf[TradingBotRoutes])

  implicit def ec: ExecutionContext

  private val stringToZonedDateTime = Unmarshaller.strict[String, ZonedDateTime](ZonedDateTime.parse)

  private val stringToBigDecimal = Unmarshaller.strict[String, BigDecimal](BigDecimal.apply)

  private val stringToAsset = Unmarshaller.strict[String, Asset](s => Asset.fromString(s).getOrElse(Asset.Btc))

  implicit val dateOrderWrites: Writes[(ZonedDateTime, Order)] = (
    (JsPath \ "date").write[ZonedDateTime] and
    (JsPath \ "order").write[Order]) { a: (ZonedDateTime, Order) => (a._1, a._2) }

  lazy val tradingBotRoutes: Route = {
    pathPrefix("public") {
      getFromResourceDirectory("public")
    } ~ pathPrefix("api") {
      getFromResource("public/api.html")
    } ~ path("btc_price") {
      getFromResource("public/api.html")
    } ~ path("eth_price") {
      getFromResource("public/api.html")
    } ~ pathPrefix("price_api") {
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
        }
    } ~ path("trade_bot") {
      get {
        parameters('asset.as(stringToAsset), 'start.as(stringToZonedDateTime), 'end.as(stringToZonedDateTime), 'initial.as(stringToBigDecimal)) { (asset, start, end, initial) =>
          onSuccess(PriceService.getPriceData(asset, start, end).map(pdata => TradeBotService.run(asset, initial, pdata))) { orderList =>
            complete(orderList)
          }
        }
      }
    } ~ path("ticker") {
      get {
        onSuccess(PriceService.priceTicker()) { quoteList =>
          complete(quoteList)
        }
      }
    } ~ get {
      complete("Hello World")
    }
  }
}
