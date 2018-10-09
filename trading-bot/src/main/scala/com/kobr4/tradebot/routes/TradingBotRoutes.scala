package com.kobr4.tradebot.routes

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.QuickstartServer
import com.kobr4.tradebot.api.{PoloApi, PoloOrder, SupportedExchange}
import com.kobr4.tradebot.engine.Strategy
import com.kobr4.tradebot.model.{Asset, Order, Quantity}
import com.kobr4.tradebot.services.{PriceService, TradeBotService, TradingOps}
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads, Writes}

import scala.concurrent.ExecutionContext

case class ExchangeCreds(exchange: String, apiKey: String, apiSecret: String)

object ExchangeCreds {

  implicit val exchangeCredsReads: Reads[ExchangeCreds] = Json.reads[ExchangeCreds]

  implicit val exchangeCredsWrites: Writes[ExchangeCreds] = Json.writes[ExchangeCreds]
}

case class ScheduledTradeBot(hour: Int, minutes: Int, asset: Asset, strategy: Strategy) {
  def toCronExpression = s"0 $minutes $hour * * ?"
}

object ScheduledTradeBot {

  import play.api.libs.functional.syntax._

  implicit val scheduledTradeBotReads: Reads[ScheduledTradeBot] = (
    (JsPath \ "hour").read[Int] and
    (JsPath \ "minutes").read[Int] and
    (JsPath \ "asset").read[Asset] and
    (JsPath \ "strategy").read[Strategy])(ScheduledTradeBot.apply _)
}

case object UnsupportedStrategyException extends RuntimeException

trait TradingBotRoutes extends PlayJsonSupport {

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  implicit def am: ActorMaterializer

  private lazy val log = Logging(system, classOf[TradingBotRoutes])

  implicit def ec: ExecutionContext

  private val stringToZonedDateTime = Unmarshaller.strict[String, ZonedDateTime](ZonedDateTime.parse)

  private val stringToBigDecimal = Unmarshaller.strict[String, BigDecimal](BigDecimal.apply)

  private val stringToAsset = Unmarshaller.strict[String, Asset](s => Asset.fromString(s).getOrElse(Asset.Btc))

  private val stringToStrategy = Unmarshaller.strict[String, Strategy](s => Strategy.fromString(s).getOrElse(throw UnsupportedStrategyException))

  private val stringToSupportedExchange = Unmarshaller.strict[String, SupportedExchange](s => SupportedExchange.fromString(s))

  implicit val dateOrderWrites: Writes[(ZonedDateTime, Order)] = (
    (JsPath \ "date").write[String] and
    (JsPath \ "order").write[Order]) { a: (ZonedDateTime, Order) => (a._1.toOffsetDateTime.toString, a._2) }

  implicit val assetQuantityWrites: Writes[(Asset, Quantity)] = (
    (JsPath \ "asset").write[Asset] and
    (JsPath \ "quantity").write[BigDecimal]) { a: (Asset, Quantity) => (a._1, a._2.quantity) }

  implicit val poloOrderWrites: Writes[PoloOrder] = (
    (JsPath \ "orderNumber").write[Long] and
    (JsPath \ "rate").write[BigDecimal] and
    (JsPath \ "amount").write[BigDecimal])(unlift(PoloOrder.unapply))

  lazy val tradingBotRoutes: Route = pathPrefix("public") {
    getFromResourceDirectory("public")
  } ~ pathPrefix("api") {
    getFromResource("public/api.html")
  } ~ path("btc_price") {
    getFromResource("public/api.html")
  } ~ path("eth_price") {
    getFromResource("public/api.html")
  } ~ path("xmr_price") {
    getFromResource("public/api.html")
  } ~ path("goog_price") {
    getFromResource("public/api.html")
  } ~ path("trading") {
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
      } ~
      path("weighted_moving") {
        get {
          parameters('asset.as(stringToAsset), 'start.as(stringToZonedDateTime), 'end.as(stringToZonedDateTime), 'days.as[Int]) { (asset, start, end, days) =>
            onSuccess(PriceService.getWeightedMovingAverageHistory(asset, start, end, days)) { priceList =>
              complete(priceList)
            }
          }
        }
      }
  } ~ path("trade_bot") {
    get {
      parameters('asset.as(stringToAsset), 'start.as(stringToZonedDateTime), 'end.as(stringToZonedDateTime),
        'initial.as(stringToBigDecimal), 'fees.as(stringToBigDecimal), 'strategy.as(stringToStrategy)) { (asset, start, end, initial, fees, strategy) =>
          onSuccess(PriceService.getPriceData(asset, start, end).map(pdata => TradeBotService.run(asset, start, initial, pdata, fees, strategy))) { orderList =>
            complete(orderList)
          }
        }
    }
  } ~ pathPrefix("trading_api") {
    path("balances") {
      post {
        entity(as[ExchangeCreds]) { creds =>
          val poloApi = new PoloApi(creds.apiKey, creds.apiSecret)
          onSuccess(poloApi.returnBalances.map(_.toList)) { assetQuantityList =>
            complete(assetQuantityList)
          }
        }
      }
    } ~ path("open_orders") {
      post {
        entity(as[ExchangeCreds]) { creds =>
          val poloApi = new PoloApi(creds.apiKey, creds.apiSecret)
          onSuccess(poloApi.returnOpenOrders()) { openOrdersList =>
            complete(openOrdersList)
          }
        }
      }
    } ~ path("schedule_daily") {
      post {
        entity(as[ExchangeCreds]) { creds =>
          entity(as[ScheduledTradeBot]) { scheduled =>
            {
              val poloApi = new PoloApi(creds.apiKey, creds.apiSecret)
              val tradingOps = new TradingOps(poloApi)
              val zd = ZonedDateTime.parse("2017-01-01T00:00:00-00:00")
              onSuccess(
                PriceService.getPriceData(scheduled.asset, zd).map { pData =>
                  QuickstartServer.schedulingService.schedule(
                    "toto",
                    scheduled.toCronExpression,
                    () => TradeBotService.runAndTrade(
                      scheduled.asset,
                      pData,
                      scheduled.strategy, poloApi, tradingOps))
                }) { result =>
                  complete("OK")
                }
            }

          }
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
  } ~ pathSingleSlash {
    getFromResource("public/api.html")
  } ~ get {
    complete("Hello World")
  }
}
