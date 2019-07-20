package com.kobr4.tradebot.api

import java.time.{ Instant, ZoneId, ZonedDateTime }

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ HttpMethod, _ }
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.DefaultConfiguration
import com.kobr4.tradebot.model.Asset.Custom
import com.kobr4.tradebot.model._
import com.kobr4.tradebot.scheduler.BinanceDailyJob
import com.typesafe.scalalogging.StrictLogging
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import play.api.libs.json._

import scala.concurrent.{ ExecutionContext, Future }

sealed trait TimeInForce

case object GTC extends TimeInForce {
  override def toString = "GTC"
}

case object IOC extends TimeInForce {
  override def toString = "IOC"
}

case object FOK extends TimeInForce {
  override def toString = "FOK"
}

case class LotSize(minQty: BigDecimal, maxQty: BigDecimal, stepSize: BigDecimal)

case class PairProp(baseAssetPrecision: BigDecimal, quotePrecision: BigDecimal, lotSize: LotSize)

object BinanceCurrencyPairHelper {
  def fromString(s: String): CurrencyPair = {
    val pairString = s.toUpperCase
    val (a, b) = pairString.length match {
      case 5 =>
        (pairString.substring(0, 2), pairString.substring(2))
      case 6 =>
        (pairString.substring(0, 3), pairString.substring(3))
      case 7 =>
        (pairString.substring(0, 3), pairString.substring(3))
      case 8 =>
        (pairString.substring(0, 4), pairString.substring(4))
      case 9 | 10 =>
        (pairString.substring(0, 6), pairString.substring(6))
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

class BinanceApi(
  apiKey: String = DefaultConfiguration.BinanceApi.Key,
  apiSecret: String = DefaultConfiguration.BinanceApi.Secret, binanceUrl: String = BinanceApi.url)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) extends ExchangeApi {
  private def nonce() = System.currentTimeMillis()

  private val pairProp = getPairProp()

  import play.api.libs.functional.syntax._

  case class Trade(symbol: CurrencyPair, price: BigDecimal, quantity: BigDecimal, isBuyer: Boolean, date: ZonedDateTime) {
    def toOrder: Order = {
      if (isBuyer) {
        Buy(symbol, price, quantity, date)
      } else {
        Sell(symbol, price, quantity, date)
      }
    }
  }

  implicit val lotSizeReads: Reads[LotSize] = (
    (JsPath \ "minQty").read[BigDecimal] and
    (JsPath \ "maxQty").read[BigDecimal] and
    (JsPath \ "stepSize").read[BigDecimal])(LotSize.apply _)

  implicit val tradeReads: Reads[Trade] = (
    (JsPath \ "symbol").read[String].map(BinanceCurrencyPairHelper.fromString) and
    (JsPath \ "price").read[BigDecimal] and
    (JsPath \ "qty").read[BigDecimal] and
    (JsPath \ "isBuyer").read[Boolean] and
    (JsPath \ "time").read[Long].map(t => ZonedDateTime.ofInstant(Instant.ofEpochSecond(t / 1000), ZoneId.of("UTC"))))(Trade.apply _)

  implicit val poloOrderReads: Reads[PoloOrder] = (
    (JsPath \ "symbol").read[String].map(BinanceCurrencyPairHelper.fromString) and
    (JsPath \ "orderId").read[Long].map(_.toString) and
    (JsPath \ "price").read[BigDecimal] and
    (JsPath \ "origQty").read[BigDecimal])(PoloOrder.apply _)

  def getPairProp(): Future[Map[CurrencyPair, PairProp]] = {
    BinanceApi.httpGetRequest(BinanceApi.exchangeInfoUrl, "").map { message =>
      Json.parse(message).as[JsObject].value("symbols").as[JsArray].value.map { obj =>
        val pair = BinanceCurrencyPairHelper.fromString(obj.as[JsObject].value("symbol").as[String])
        val baseAssetPrecision = obj.as[JsObject].value("baseAssetPrecision").as[Int]
        val quotePrecision = obj.as[JsObject].value("quotePrecision").as[Int]
        val lotSize = obj.as[JsObject].value("filters").as[JsArray].value.filter(_.as[JsObject].value("filterType").as[String] == "LOT_SIZE").map(_.as[LotSize]).head
        (pair, PairProp(baseAssetPrecision, quotePrecision, lotSize))
      }.filter(q => (q._1.right, q._1.left) match {
        case (_: Custom, _) => false
        case (_, _: Custom) => false
        case _ => true
      }).toMap
    }
  }

  override def returnBalances: Future[Map[Asset, Quantity]] = {
    BinanceApi.httpRequest(binanceUrl, BinanceApi.ReturnBalances.Account, BinanceApi.ReturnBalances.build(nonce()),
      apiKey, apiSecret, HttpMethods.GET).map { message =>
      Json.parse(message).as[JsObject].value("balances").as[JsArray].value.map { obj =>
        (Asset.fromString(obj.as[JsObject].value("asset").as[String]), Quantity(BigDecimal(obj.as[JsObject].value("free").as[String])))
      }.toMap
    }
  }

  override def returnDepositAddresses: Future[Map[Asset, String]] = ???

  override def returnOpenOrders(): Future[List[PoloOrder]] = {
    val reqNonce = nonce()
    BinanceApi.httpRequest(binanceUrl, BinanceApi.ReturnOpenOrders.OpenOrders, BinanceApi.ReturnOpenOrders.build(reqNonce),
      apiKey, apiSecret, HttpMethods.GET).map { message =>
      Json.parse(message).as[JsArray].value.map(_.as[PoloOrder]).toList
    }
  }

  override def cancelOrder(order: PoloOrder): Future[Boolean] = {
    BinanceApi.httpRequest(binanceUrl, BinanceApi.CancelOrder.Order,
      BinanceApi.CancelOrder.build(nonce(), order.orderNumber, BinanceCurrencyPairHelper.toString(order.currencyPair)),
      apiKey, apiSecret, HttpMethods.DELETE).map { _ => true }
  }

  override def returnTradeHistory(start: ZonedDateTime, end: ZonedDateTime): Future[List[Order]] = {
    val evList = BinanceDailyJob.pairList.map { pair =>
      BinanceApi.httpRequest(binanceUrl, BinanceApi.ReturnTradesHistory.MyTrades, BinanceApi.ReturnTradesHistory.build(
        nonce(),
        BinanceCurrencyPairHelper.toString(pair), start.toEpochSecond * 1000, end.toEpochSecond * 1000), apiKey, apiSecret, HttpMethods.GET).map { message =>
        Json.parse(message).as[JsArray].value.map(_.as[Trade].toOrder).toList
      }
    }

    Future.sequence(evList).map(_.flatten.sortBy(_.date.toEpochSecond))
  }

  override def buy(currencyPair: CurrencyPair, rate: BigDecimal, amount: BigDecimal): Future[Order] = {
    (for {
      lotAmount <- pairProp.map(pairPropMap => BinanceApi.lotSizeAmount(amount, pairPropMap(currencyPair).lotSize.stepSize))
    } yield {
      BinanceApi.httpRequest(binanceUrl, BinanceApi.BuySell.path, BinanceApi.BuySell.build(
        nonce(),
        KrakenCurrencyPairHelper.toString(currencyPair), rate, lotAmount, true), apiKey, apiSecret)
        .map(_ => Buy(currencyPair, rate, amount, ZonedDateTime.now()))
    }).flatten
  }

  override def sell(currencyPair: CurrencyPair, rate: BigDecimal, amount: BigDecimal): Future[Order] = {
    (for {
      lotAmount <- pairProp.map(pairPropMap => BinanceApi.lotSizeAmount(amount, pairPropMap(currencyPair).lotSize.stepSize))
    } yield {
      BinanceApi.httpRequest(binanceUrl, BinanceApi.BuySell.path, BinanceApi.BuySell.build(
        nonce(),
        KrakenCurrencyPairHelper.toString(currencyPair), rate, lotAmount, false), apiKey, apiSecret)
        .map(_ => Sell(currencyPair, rate, amount, ZonedDateTime.now()))
    }).flatten
  }

  override def returnTicker()(implicit ec: ExecutionContext): Future[List[Quote]] =
    BinanceApi.httpGetRequest(binanceUrl, BinanceApi.Public.returnTicker).map(message => {
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

  def lotSizeAmount(amount: BigDecimal, stepSize: BigDecimal): BigDecimal = (amount / stepSize).setScale(0, BigDecimal.RoundingMode.FLOOR) * stepSize

  object Public {

    val returnTicker = "ticker/price"

  }

  val url = s"$rootUrl/api/v3/"

  val exchangeInfoUrl = s"$rootUrl/api/v1/exchangeInfo"

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

    val TimeInForce = "timeInForce"

    def build(nonce: Long, currencyPair: String, rate: BigDecimal, amount: BigDecimal, isBuy: Boolean) = {
      akka.http.scaladsl.model.FormData(Map(
        BuySell.symbol -> currencyPair,
        BuySell.price -> rate.underlying().toPlainString,
        BuySell.quantity -> amount.underlying().toPlainString,
        BuySell.`type` -> "LIMIT",
        BuySell.side -> (if (isBuy) BuySell.buy else BuySell.sell),
        BuySell.TimeInForce -> GTC.toString,
        BinanceApi.timestamp -> nonce.toString))
    }
  }

  object ReturnTradesHistory {

    val MyTrades = "myTrades"

    val Start = "startTime"

    val End = "endTime"

    val Symbol = "symbol"

    def build(nonce: Long, currencyPair: String, start: Long, end: Long): FormData = akka.http.scaladsl.model.FormData(Map(
      BinanceApi.timestamp -> nonce.toString,
      BinanceApi.ReturnTradesHistory.Symbol -> currencyPair,
      BinanceApi.ReturnTradesHistory.Start -> start.toString /*,
      BinanceApi.ReturnTradesHistory.End -> end.toString */
    ))
  }

  object ReturnBalances {

    val Account = "account"

    def build(nonce: Long): FormData = akka.http.scaladsl.model.FormData(Map(BinanceApi.timestamp -> nonce.toString))
  }

  object CancelOrder {

    val Order = "order"

    val OrderId = "orderId"

    val Symbol = "symbol"

    def build(nonce: Long, txid: String, currencyPair: String): FormData = akka.http.scaladsl.model.FormData(Map(
      BinanceApi.timestamp -> nonce.toString,
      BinanceApi.CancelOrder.Symbol -> currencyPair,
      BinanceApi.CancelOrder.OrderId -> txid))
  }

  object AuthHeader {

    val key = "X-MBX-APIKEY"

    def build(key: String): List[HttpHeader] = {
      List(RawHeader(AuthHeader.key, key))
    }
  }

  private def httpGetRequest(url: String, path: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    Http().singleRequest(HttpRequest(uri = s"$url$path")).flatMap { response =>
      Unmarshal(response.entity).to[String]
    }
  }

  private[tradebot] def generateHMAC256(sharedSecret: String, preHashData: Array[Byte]): String = {
    val secret = new SecretKeySpec(sharedSecret.getBytes, "HmacSHA256")
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(secret)
    val hashString: Array[Byte] = mac.doFinal(preHashData)
    val hmacsign = hashString.toList.map("%02x" format _).mkString
    logger.debug(s"HMAC sha256 signature: $hmacsign")
    hmacsign
  }

  private def httpRequest(url: String, path: String, body: FormData, apiKey: String, apiSecret: String,
    method: HttpMethod = HttpMethods.POST)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {

    val reqBody = body.fields.toString + s"&signature=${generateHMAC256(apiSecret, body.fields.toString.getBytes)}"
    val fullUrl = s"$url$path${if (method == HttpMethods.GET) "?" + reqBody else ""}"
    logger.info(s"Sending post request to $fullUrl")
    logger.info(s"Body: ${body.fields.toString}")
    logger.info(s"Path $path")
    Http().singleRequest(HttpRequest(
      method = method,
      headers = AuthHeader.build(apiKey),
      entity = if (method != HttpMethods.GET) reqBody else "",
      uri = fullUrl)).flatMap { response =>
      if (response.status == StatusCodes.OK)
        Unmarshal(response.entity).to[String]
      else {
        Unmarshal(response.entity).to[String].map(println)
        throw new RuntimeException("Return code was " + response.status)
      }
    }
  }

}