package com.kobr4.tradebot.routes

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.QuickstartServer
import com.kobr4.tradebot.api._
import com.kobr4.tradebot.engine.Strategy
import com.kobr4.tradebot.model.{ Asset, Order, Quantity }
import com.kobr4.tradebot.routes.stub.Foo
import com.kobr4.tradebot.scheduler.{ KrakenDailyJob, TradeBotDailyJob }
import com.kobr4.tradebot.services._
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.concurrent.{ ExecutionContext, Future }

case class ExchangeCreds(exchange: SupportedExchange, apiKey: String, apiSecret: String)

object ExchangeCreds {

  implicit val supportedExchangeReads: Reads[SupportedExchange] = JsPath.read[String].map(SupportedExchange.fromString)

  implicit val supportedExchangeWrites: Writes[SupportedExchange] = (ex: SupportedExchange) => JsString(ex.toString)

  implicit val exchangeCredsFormat: Format[ExchangeCreds] = Json.format[ExchangeCreds]
}

case class LoginPassword(email: String, password: String)

object LoginPassword {

  implicit val loginPasswordFormat: Format[LoginPassword] = Json.format[LoginPassword]
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

package object stub {
  case class Foo(bar: String)
}

case object UnsupportedStrategyException extends RuntimeException

case class Balances(valuation: BigDecimal, assetList: List[(Asset, Quantity)])

trait TradingBotRoutes extends PlayJsonSupport with PriceApiRoutes with TradeJobsRoutes {

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  implicit def am: ActorMaterializer

  implicit def ec: ExecutionContext

  //private lazy val log = Logging(system, classOf[TradingBotRoutes])

  private val stringToZonedDateTime = Unmarshaller.strict[String, ZonedDateTime](ZonedDateTime.parse)

  private val stringToBigDecimal = Unmarshaller.strict[String, BigDecimal](BigDecimal.apply)

  private val stringToAsset = Unmarshaller.strict[String, Asset](s => Asset.fromString(s))

  private val stringToStrategy = Unmarshaller.strict[String, Strategy](s => Strategy.fromString(s).getOrElse(throw UnsupportedStrategyException))

  private val stringToSupportedExchange = Unmarshaller.strict[String, SupportedExchange](s => SupportedExchange.fromString(s))

  private val stringToCurrencyPair = Unmarshaller.strict[String, CurrencyPair](s => s.toUpperCase.split('_').map(s => Asset.fromString(s)).toList match {
    case a :: b :: Nil => CurrencyPair(a, b)
  })

  implicit val assetQuantityWrites: Writes[(Asset, Quantity)] = (
    (JsPath \ "asset").write[Asset] and
    (JsPath \ "quantity").write[String]) { a: (Asset, Quantity) => (a._1, a._2.quantity.underlying().toPlainString) }

  implicit val balancesWrites: Writes[Balances] = Json.writes[Balances]

  implicit val poloOrderWrites: Writes[PoloOrder] = (
    (JsPath \ "pair").write[CurrencyPair] and
    (JsPath \ "orderNumber").write[String] and
    (JsPath \ "rate").write[BigDecimal] and
    (JsPath \ "amount").write[BigDecimal])(unlift(PoloOrder.unapply))

  implicit val strategyyWrites: Writes[Strategy] = (o: Strategy) => {
    JsString(o.toString)
  }

  implicit val runMultipleReportWrites: Writes[RunMultipleReport] = Json.writes[RunMultipleReport]

  lazy val tradingBotRoutes: Route = pathPrefix("public") {
    getFromResourceDirectory("public")
  } ~ pathPrefix("api") {
    getFromResource("public/api.html")
  } ~ pathPrefix("price_api") {
    priceApiRoutes
  } ~ pathPrefix("trade_bot") {
    path("run") {
      get {
        parameters('asset.as(stringToAsset), 'start.as(stringToZonedDateTime), 'end.as(stringToZonedDateTime),
          'initial.as(stringToBigDecimal), 'fees.as(stringToBigDecimal), 'strategy.as(stringToStrategy), 'pair.as(stringToCurrencyPair).?) { (asset, start, end, initial, fees, strategy, maybePair) =>
            onSuccess(PriceService.getPricesOrPair(maybePair.getOrElse(CurrencyPair(Asset.Usd, asset)), start, end).map(pdata => TradeBotService.runPair(maybePair.getOrElse(CurrencyPair(Asset.Usd, asset)), start, initial, pdata, fees, strategy))) { orderList =>
              complete(orderList)
            }
          }
      }
    } ~ path("search") {
      get {
        parameters('asset.as(stringToAsset), 'start.as(stringToZonedDateTime), 'end.as(stringToZonedDateTime),
          'initial.as(stringToBigDecimal), 'fees.as(stringToBigDecimal), 'strategy.as(stringToStrategy), 'pair.as(stringToCurrencyPair).?) { (asset, start, end, initial, fees, strategy, maybePair) =>
            {
              val assetWeight: Map[Asset, BigDecimal] = Map(maybePair.getOrElse(CurrencyPair(Asset.Usd, asset)).right -> BigDecimal(1.0))
              onSuccess(TradeBotService.bestRun(assetWeight, initial, fees, start, end)) { runReport =>
                complete(runReport)
              }
            }
          }
      }
    }
  } ~ pathPrefix("trading_api") {
    path("balances") {
      post {
        entity(as[ExchangeCreds]) { creds =>
          val exchangeApi = ExchangeApi(creds.exchange, creds.apiKey, creds.apiSecret)
          val eventualBalancePrice = for {
            a <- exchangeApi.returnBalances.map(_.toList)
            b <- exchangeApi.returnTicker()
          } yield {
            (a, b)
          }
          onSuccess(eventualBalancePrice) { (assetQuantityList, quoteList) =>
            val usdValue = assetQuantityList.map(assetQ =>
              quoteList.find(p => (p.pair.left == Asset.Usd || p.pair.left == Asset.Tether) && p.pair.right == assetQ._1)
                .map(_.last).getOrElse(BigDecimal(1)) * assetQ._2.quantity).sum
            complete(Balances(usdValue, assetQuantityList))
          }
        }
      }
    } ~ path("open_orders") {
      post {
        entity(as[ExchangeCreds]) { creds =>
          val exchangeApi = ExchangeApi(creds.exchange, creds.apiKey, creds.apiSecret)
          onSuccess(exchangeApi.returnOpenOrders()) { openOrdersList =>
            complete(openOrdersList)
          }
        }
      }
    }
  } ~ pathPrefix("inhouse") {
    path("balances") {
      get {
        parameters('exchange.as(stringToSupportedExchange)) { exchange =>
          val exchangeApi = ExchangeApi(exchange)
          val eventualBalancePrice = for {
            a <- exchangeApi.returnBalances.map(_.toList)
            b <- exchangeApi.returnTicker()
          } yield {
            (a, b)
          }
          onSuccess(eventualBalancePrice) { (assetQuantityList, quoteList) =>
            val usdValue = assetQuantityList.map(assetQ =>
              quoteList.find(p => (p.pair.left == Asset.Usd || p.pair.left == Asset.Tether) && p.pair.right == assetQ._1)
                .map(_.last).getOrElse(BigDecimal(1)) * assetQ._2.quantity).sum
            complete(Balances(usdValue, assetQuantityList))
          }
        }
      }
    } ~ path("open_orders") {
      get {
        parameters('exchange.as(stringToSupportedExchange)) { exchange =>
          val exchangeApi = ExchangeApi(exchange)
          onSuccess(exchangeApi.returnOpenOrders()) { openOrdersList =>
            complete(openOrdersList)
          }
        }
      }
    } ~ path("trade_history") {
      get {
        parameters('exchange.as(stringToSupportedExchange)) { exchange =>
          val exchangeApi = ExchangeApi(exchange)
          onSuccess(exchangeApi.returnTradeHistory()) { tradeHistoryList =>
            complete(tradeHistoryList)
          }
        }
      }
    } ~ path("currently_trading") {
      get {
        parameters('exchange.as(stringToSupportedExchange)) {
          case Poloniex => complete(TradeBotDailyJob.assetMap)
          case Kraken => complete(KrakenDailyJob.assetMap)
        }
      }
    }
  } ~ pathPrefix("auth") {
    path("login") {
      post {
        entity(as[LoginPassword]) { (loginPassword) =>
          val verif = UserService.verify(loginPassword.email, loginPassword.password)
          onSuccess(verif) {
            case Some(userId) => complete(AuthService.issueToken(userId, loginPassword.email))
            case _ => complete((StatusCodes.Forbidden, "Not allowed"))
          }
        }
      }
    }
  } ~ pathPrefix("user") {
    path("signup") {
      post {
        entity(as[LoginPassword]) { (loginPassword) =>
          val maybeId = UserService.signUp(loginPassword.email, loginPassword.password)
          onSuccess(maybeId) {
            case Some(_) => complete("OK")
            case _ => complete((StatusCodes.Forbidden, "Not allowed"))
          }
        }
      }
    } ~ path("activation") {
      get {
        parameters("token") { token =>
          AuthService.verifyToken(token).map { appToken =>
            onSuccess(UserService.activate(appToken.login)) {
              case i if i > 0 => complete("OK")
              case _ => complete((StatusCodes.BadRequest, "Could not proceed"))
            }
          }.getOrElse(complete((StatusCodes.BadRequest, "Could not proceed")))
        }
      }
    }
  } ~ tradeJobsRoutes ~ path("about") {
    import akkahttptwirl.TwirlSupport._

    get {
      complete {
        html.ModernBusiness.render(List())
      }
    }
  } ~ tradeJobsRoutes ~ path("prices") {
    import akkahttptwirl.TwirlSupport._

    get {

      val eventualTicker = Future.sequence(Seq(ExchangeApi(Poloniex).returnTicker(), ExchangeApi(Kraken).returnTicker()))
      onSuccess(eventualTicker) {
        case poloQuote :: krakenQuote :: Nil => complete(html.Prices.render(poloQuote, krakenQuote))
      }

    }
  } ~ pathSingleSlash {
    import akkahttptwirl.TwirlSupport._
    get {
      val eventualOrders = Future.sequence(Seq(
        TradeBotCachedService.run(Asset.Btc),
        TradeBotCachedService.run(Asset.Eth),
        TradeBotCachedService.run(Asset.Xmr),
        TradeBotCachedService.run(Asset.Xrp),
        TradeBotCachedService.run(Asset.Ltc)))
      onSuccess(eventualOrders.map(_.map(_.last).toList)) { orders: List[Order] =>
        complete {
          html.ModernBusiness.render(orders)
        }
      }
    }
  } ~ get {
    getFromResource("public/api.html")
  }
}
