package com.kobr4.tradebot.api

import java.time.{Instant, ZoneId, ZonedDateTime}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpMethod
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.DefaultConfiguration
import com.kobr4.tradebot.api.KrakenApi.{AuthHeader, generateHMAC512, generateSha256, logger}
import com.kobr4.tradebot.model.Asset.Custom
import com.kobr4.tradebot.model._
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

object BinanceCurrencyPairHelper {
  def fromString(s: String): CurrencyPair = {
    val pairString = s.toUpperCase
    val (a, b) = pairString.length match {
      case 6 =>
        (pairString.substring(0, 3), pairString.substring(3))
      case 7 =>
        (pairString.substring(0, 4), pairString.substring(4))
      case 8 =>
        (pairString.substring(1, 4), pairString.substring(5))
    }

    CurrencyPair(Asset.fromString(b), Asset.fromString(a))
  }

  def toString(pair: CurrencyPair): String = pair match {
    case CurrencyPair(Asset.Usd, Asset.Ada) => s"ADAUSD"
    case CurrencyPair(Asset.Usd, Asset.Btc) => s"XXBTZUSD"
    case CurrencyPair(Asset.Usd, Asset.Tether) => s"USDTZUSD"
    case CurrencyPair(Asset.Usd, a: Asset) => s"X${a}ZUSD"
    case CurrencyPair(b: Asset, a: Asset) => s"$a$b"
  }
}

class BinanceApi(apiKey: String = DefaultConfiguration.BinanceApi.Key,
                 apiSecret: String = DefaultConfiguration.BinanceApi.Secret, binanceUrl: String = BinanceApi.url)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) extends ExchangeApi {
  private def nonce() = System.currentTimeMillis()

  import play.api.libs.functional.syntax._

  case class Trade(symbol: CurrencyPair, price: BigDecimal, quantity: BigDecimal, isBuyer: Boolean, date: ZonedDateTime) {
    def toOrder: Order = {
      isBuyer match {
        case true => Buy(symbol, price, quantity, date)
        case false => Sell(symbol, price, quantity, date)
      }
    }
  }

  implicit val tradeReads: Reads[Trade] = (
    (JsPath \ "symbol").read[String].map(BinanceCurrencyPairHelper.fromString) and
      (JsPath \ "price").read[BigDecimal] and
      (JsPath \ "qty").read[BigDecimal] and
      (JsPath \ "isBuyer").read[Boolean] and
      (JsPath \ "time").read[Long].map(t => ZonedDateTime.ofInstant(Instant.ofEpochSecond(t), ZoneId.of("UTC")))) (Trade.apply _)


  implicit val poloOrderReads: Reads[PoloOrder] = (
    (JsPath \ "symbol").read[String].map(BinanceCurrencyPairHelper.fromString) and
      (JsPath \ "orderId").read[Long].map(_.toString) and
      (JsPath \ "price").read[BigDecimal] and
      (JsPath \ "origQty").read[BigDecimal]) (PoloOrder.apply _)

  override def returnBalances: Future[Map[Asset, Quantity]] = {
    BinanceApi.httpGetRequest(binanceUrl, BinanceApi.ReturnBalances.Account).map { message =>
      Json.parse(message).as[JsObject].value("balances").as[JsArray].value.map { obj =>
        (Asset.fromString(obj.as[JsObject].value("asset").as[String]), Quantity(BigDecimal(obj.as[JsObject].value("free").as[String])))
      }.toMap
    }
  }

  override def returnDepositAddresses: Future[Map[Asset, String]] = ???

  override def returnOpenOrders(): Future[List[PoloOrder]] = {
    val reqNonce = nonce()
    BinanceApi.httpGetRequest(binanceUrl, BinanceApi.ReturnOpenOrders.OpenOrders).map { message =>
      Json.parse(message).as[JsArray].value.map(_.as[PoloOrder]).toList
    }
  }

  override def cancelOrder(orderNumber: String): Future[Boolean] = {
    val reqNonce = nonce()
    BinanceApi.httpRequest(binanceUrl, BinanceApi.CancelOrder.Order, reqNonce,
      BinanceApi.CancelOrder.build(reqNonce, orderNumber), apiKey, apiSecret, HttpMethods.DELETE).map { _ => true }
  }

  override def returnTradeHistory(start: ZonedDateTime, end: ZonedDateTime): Future[List[Order]] = {
    BinanceApi.httpGetRequest(binanceUrl, BinanceApi.ReturnTradesHistory.MyTrades).map { message =>
      Json.parse(message).as[JsArray].value.map(_.as[Trade].toOrder).toList
    }
  }

  override def buy(currencyPair: CurrencyPair, rate: BigDecimal, amount: BigDecimal): Future[String] = {
    val reqNonce = nonce()
    BinanceApi.httpRequest(binanceUrl, BinanceApi.BuySell.path, reqNonce, BinanceApi.BuySell.build(reqNonce,
      KrakenCurrencyPairHelper.toString(currencyPair), rate, amount, true), apiKey, apiSecret)
  }

  override def sell(currencyPair: CurrencyPair, rate: BigDecimal, amount: BigDecimal): Future[String] = {
    val reqNonce = nonce()
    BinanceApi.httpRequest(binanceUrl, BinanceApi.BuySell.path, reqNonce, BinanceApi.BuySell.build(reqNonce,
      KrakenCurrencyPairHelper.toString(currencyPair), rate, amount, false), apiKey, apiSecret)
  }

  override def returnTicker()(implicit ec: ExecutionContext): Future[List[Quote]] =
    BinanceApi.httpGetRequest(binanceUrl, BinanceApi.Public.returnTicker).map(message => {
      println(message)
      Json.parse(message).as[JsArray].value.map({ elt =>
        val obj = elt.as[JsObject]
        val pairString = obj.value("symbol").as[String]
        val (a, b) = pairString.length match {
          case _ if pairString == "USDTZUSD" =>
            ("USDT", "ZUSD")
          case 5 =>
            (pairString.substring(0, 2), pairString.substring(2))
          case 6 =>
            (pairString.substring(0, 3), pairString.substring(3))
          case 7 =>
            (pairString.substring(0, 3), pairString.substring(3))
          case 8 =>
            (pairString.substring(0, 4), pairString.substring(4))
          case 9 =>
            //println(pairString)
            (pairString.substring(0, 6), pairString.substring(6))
          case 10 =>
            (pairString.substring(0, 6), pairString.substring(6))


        }
        Quote(CurrencyPair(Asset.fromString(b), Asset.fromString(a)), obj.value("price").as[BigDecimal], 0, 0, 0, 0, 0)
      }).toList.filter(q => (q.pair.right, q.pair.left) match {
        case (_: Custom, _) => false
        case (_, _: Custom) => false
        case _ => true
      })

    })
}

object BinanceApi extends StrictLogging {

  val rootUrl = "https://api.binance.com"

  val timestamp = "timestamp"

  object Public {

    val returnTicker = "ticker/price"

  }

  val url = s"$rootUrl/api/v3/"

  object ReturnOpenOrders {

    val OpenOrders = "openOrders"

    def build(nonce: Long): FormData = akka.http.scaladsl.model.FormData(Map(BinanceApi.timestamp -> nonce.toString))
  }

  object BuySell {

    val path = "order"

    val price = "price"

    val quantity = "quantity"

    val symbol = "symbol"

    val buy = "BUY"

    val sell = "SELL"

    val `type` = "type"

    val side = "side"

    def build(nonce: Long, currencyPair: String, rate: BigDecimal, amount: BigDecimal, isBuy: Boolean) = {
      akka.http.scaladsl.model.FormData(Map(
        BuySell.symbol -> currencyPair,
        BuySell.price -> rate.underlying().toPlainString,
        BuySell.quantity -> amount.underlying().toPlainString,
        BuySell.`type` -> "LIMIT",
        BuySell.side -> (if (isBuy) BuySell.buy else BuySell.sell),
        BinanceApi.timestamp -> nonce.toString)
      )
    }
  }

  object ReturnTradesHistory {

    val MyTrades = "myTrades"

    val Start = "start"

    val End = "end"

    def build(nonce: Long, start: Long, end: Long): FormData = akka.http.scaladsl.model.FormData(Map(
      PoloApi.Nonce -> nonce.toString,
      KrakenApi.ReturnTradesHistory.Start -> start.toString, KrakenApi.ReturnTradesHistory.End -> end.toString))
  }

  object ReturnBalances {

    val Account = "account"

    def build(nonce: Long): FormData = akka.http.scaladsl.model.FormData(Map(PoloApi.Nonce -> nonce.toString))
  }

  object CancelOrder {

    val Order = "order"

    val OrderId = "orderId"

    def build(nonce: Long, txid: String): FormData = akka.http.scaladsl.model.FormData(Map(
      PoloApi.Nonce -> nonce.toString,
      BinanceApi.CancelOrder.OrderId -> txid))
  }

  private def httpGetRequest(url: String, path: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    Http().singleRequest(HttpRequest(uri = s"$url$path")).flatMap { response =>
      Unmarshal(response.entity).to[String]
    }
  }

  private def httpRequest(url: String, path: String, nonce: Long, body: FormData, apiKey: String, apiSecret: String,
                          method: HttpMethod = HttpMethods.POST)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    val fullUrl = s"$url$path"
    logger.info(s"Sending post request to $url")
    logger.info(s"Body: ${body.fields.toString}")
    logger.info(s"Path $path")
    Http().singleRequest(HttpRequest(
      method = method,
      headers = AuthHeader.build(apiKey, generateHMAC512(apiSecret, path.getBytes ++ generateSha256(nonce.toString + body.fields.toString))),
      entity = body.toEntity(HttpCharsets.`UTF-8`),
      uri = fullUrl)).flatMap { response =>
      if (response.status == StatusCodes.OK)
        Unmarshal(response.entity).to[String]
      else
        throw new RuntimeException("Return code was " + response.status)
    }
  }

}