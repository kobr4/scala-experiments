package com.kobr4.tradebot.api

import java.time.{Instant, ZoneId, ZoneOffset, ZonedDateTime}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.kobr4.tradebot._
import com.kobr4.tradebot.model._
import com.typesafe.scalalogging.StrictLogging
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode

case class PoloOrder(currencyPair: CurrencyPair, orderNumber: String, rate: BigDecimal, amount: BigDecimal)

case class PoloTrade(globalTradeID: String, tradeID: Long, date: ZonedDateTime, rate: BigDecimal, amount: BigDecimal,
  total: BigDecimal, fee: BigDecimal, orderNumber: Long, `type`: String, category: String) {

  def toOrder(pair: CurrencyPair): Order = this.`type` match {
    case "buy" => Buy(pair, rate, amount, date)
    case "sell" => Sell(pair, rate, amount, date)
  }
}

case class PoloMarket(symbol: String, baseCurrencyName: String, quoteCurrencyName: String, displayName: String,
                      state: String, visibleStartTime: Long, tradableStartTime: Long, symbolTradeLimit: PoloSymbolTradeLimit)

case class PoloSymbolTradeLimit(symbol:	String, priceScale: Int, quantityScale: Int, amountScale: Int,
                                minQuantity: String, minAmount: String, highestBid:	String, lowestAsk: String)

object PoloMarket {

  import play.api.libs.functional.syntax._

  implicit val poloSymbolTradeLimitReads: Reads[PoloSymbolTradeLimit] = (
    (JsPath \ "symbol").read[String] and
    (JsPath \ "priceScale").read[Int] and
    (JsPath \ "quantityScale").read[Int] and
    (JsPath \ "amountScale").read[Int] and
    (JsPath \ "minQuantity").read[String] and
    (JsPath \ "minAmount").read[String] and
    (JsPath \ "highestBid").read[String] and
    (JsPath \ "lowestAsk").read[String])(PoloSymbolTradeLimit.apply _)


  implicit val poloMarketReads: Reads[PoloMarket] = (
    (JsPath \ "symbol").read[String] and
      (JsPath \ "baseCurrencyName").read[String] and
      (JsPath \ "quoteCurrencyName").read[String] and
      (JsPath \ "displayName").read[String] and
      (JsPath \ "state").read[String] and
      (JsPath \ "visibleStartTime").read[Long] and
      (JsPath \ "tradableStartTime").read[Long] and
      (JsPath \ "symbolTradeLimit").read[PoloSymbolTradeLimit])(PoloMarket.apply _)
}


object PoloTrade {

  import java.time.format.DateTimeFormatter

  import play.api.libs.functional.syntax._

  val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"))

  implicit val poloTradeReads: Reads[PoloTrade] = (
    (JsPath \ "globalTradeID").read[String] and
    (JsPath \ "tradeID").read[String].map(_.toLong) and
    (JsPath \ "date").read[String].map(sDate => ZonedDateTime.parse(sDate, dateTimeFormatter)) and
    (JsPath \ "rate").read[BigDecimal] and
    (JsPath \ "amount").read[BigDecimal] and
    (JsPath \ "total").read[BigDecimal] and
    (JsPath \ "fee").read[BigDecimal] and
    (JsPath \ "orderNumber").read[String].map(s => s.toLong) and
    (JsPath \ "type").read[String] and
    (JsPath \ "category").read[String])(PoloTrade.apply _)
}

case class CurrencyPair(left: Asset, right: Asset) {
  override def toString: String = {
    s"${left.toString}_${right.toString}"
  }
}

object CurrencyPair {
  implicit val currencyPairWrites = Json.writes[CurrencyPair]
}

object PoloCurrencyPairHelper {

  def fromString(s: String): CurrencyPair = {
    val cP = s.toUpperCase.split('_').map(s => Asset.fromString(s)).toList
    CurrencyPair(cP.head, cP.last)
  }
}

case class Quote(pair: CurrencyPair, last: BigDecimal, lowestAsk: BigDecimal, highestBid: BigDecimal, percentChange: BigDecimal,
  baseVolume: BigDecimal, quoteVolume: BigDecimal)

object Quote {
  implicit val quoteWrites = Json.writes[Quote]
}

class PoloApi(
  val apiKey: String = DefaultConfiguration.PoloApi.Key,
  val apiSecret: String = DefaultConfiguration.PoloApi.Secret,
  val poloUrl: String = PoloApi.rootUrl, val apiUrl: String = "https://api.poloniex.com")(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) extends ExchangeApi {

  def nonce = System.currentTimeMillis()

  import PoloApi._

  private val tradingUrl = s"$poloUrl/${PoloApi.tradingApi}"

  private val publicUrl = s"$poloUrl/public"

  import play.api.libs.functional.syntax._

  implicit def poloOrderReads(pair: CurrencyPair): Reads[PoloOrder] = (
    Reads.pure(pair) and
    (JsPath \ "orderNumber").read[String] and
    (JsPath \ "rate").read[BigDecimal] and
    (JsPath \ "amount").read[BigDecimal])(PoloOrder.apply _)

  def quoteReads(pair: CurrencyPair): Reads[Quote] = (
    Reads.pure(pair) and
    (JsPath \ "last").read[BigDecimal] and
    (JsPath \ "lowestAsk").read[BigDecimal] and
    (JsPath \ "highestBid").read[BigDecimal] and
    (JsPath \ "percentChange").read[BigDecimal] and
    (JsPath \ "baseVolume").read[BigDecimal] and
    (JsPath \ "quoteVolume").read[BigDecimal])(Quote.apply _)

  implicit val pairPriceReads: Reads[PairPrice] = (
    (JsPath \ "date").read[String].map(t => ZonedDateTime.ofInstant(Instant.ofEpochSecond(t.toLong), ZoneOffset.UTC)) and
    (JsPath \ "close").read[BigDecimal])(PairPrice.apply _)

  override def returnBalances: Future[Map[Asset, Quantity]] =
    PoloApi.httpRequestPost(tradingUrl, PoloApi.ReturnBalances.build(nonce), apiKey, apiSecret).map { message =>
      Json.parse(message).as[JsObject].fields.map {
        case (s, v) => (Asset.fromString(s.toUpperCase), Quantity(BigDecimal(v.as[String])))
      }.toMap
    }

  override def returnDepositAddresses: Future[Map[Asset, String]] =
    PoloApi.httpRequestPost(tradingUrl, ReturnDepositAddresses.build(nonce), apiKey, apiSecret).map { message =>
      Json.parse(message).as[JsObject].fields.map {
        case (s, v) => (Asset.fromString(s.toUpperCase), v.as[String])
      }.toMap
    }

  override def returnOpenOrders(): Future[List[PoloOrder]] =
    PoloApi.httpRequestPost(tradingUrl, ReturnOpenOrders.build(nonce), apiKey, apiSecret).map { message =>
      Json.parse(message).as[JsObject].fields.toList.flatMap(t => t._2.as[JsArray].value.map { order => order.as[PoloOrder](PoloCurrencyPairHelper.fromString(t._1)) }.toList)
      //Json.parse(message).as[JsArray].value.map { order => order.as[PoloOrder] }.toList
    }

  override def cancelOrder(order: PoloOrder): Future[Boolean] = {
    PoloApi.httpRequestPost(tradingUrl, CancelOrder.build(nonce, order.orderNumber), apiKey, apiSecret).map { message =>
      Json.parse(message).as[JsObject].value.get("success").exists(_.as[Int] match {
        case 1 => true
        case _ => false
      })
    }
  }

  override def returnTradeHistory(
    start: ZonedDateTime = ZonedDateTime.now().minusMonths(1),
    end: ZonedDateTime = ZonedDateTime.now()): Future[List[Order]] = {
    PoloApi.httpRequestPost(tradingUrl, ReturnTradeHistory.build(nonce, start.toEpochSecond, end.toEpochSecond), apiKey, apiSecret).map { message =>
      Json.parse(message).as[JsObject].fields.flatMap {
        case (s, v) =>
          s.toUpperCase.split('_').map(s => Asset.fromString(s)).toList match {
            case a :: b :: Nil =>
              v.as[JsArray].value.toList.map { jsValue =>
                jsValue.as[PoloTrade].toOrder(CurrencyPair(a, b))
              }
            case _ => None
          }
      }.toList.sortBy(_.date.toEpochSecond)
    }
  }

  override def buy(currencyPair: CurrencyPair, rate: BigDecimal, amount: BigDecimal): Future[Order] =
    getMarket(currencyPair).flatMap { market =>
      PoloApi.httpRequestPost(tradingUrl, BuySell.build(nonce, currencyPair.toString, rate, amount.setScale(market.symbolTradeLimit.quantityScale, RoundingMode.DOWN), true), apiKey, apiSecret).map { _ =>
        Buy(currencyPair, rate, amount.setScale(market.symbolTradeLimit.quantityScale, RoundingMode.DOWN), ZonedDateTime.now())
      }
    }

  override def sell(currencyPair: CurrencyPair, rate: BigDecimal, amount: BigDecimal): Future[Order] =
    getMarket(currencyPair).flatMap { market =>
      PoloApi.httpRequestPost(tradingUrl, BuySell.build(nonce, currencyPair.toString, rate, amount.setScale(market.symbolTradeLimit.quantityScale, RoundingMode.DOWN), false), apiKey, apiSecret).map { _ =>
        Sell(currencyPair, rate, amount.setScale(market.symbolTradeLimit.quantityScale, RoundingMode.DOWN), ZonedDateTime.now())
      }
    }

  override def returnTicker()(implicit ec: ExecutionContext): Future[List[Quote]] = PoloApi.httpRequest(publicUrl, Public.returnTicker).map { message =>
    Json.parse(message).as[JsObject].fields.flatMap {
      case (s, v) => v.asOpt[Quote](quoteReads(PoloCurrencyPairHelper.fromString(s)))
      case _ => None
    }.toList
  }

  def returnChartData(currencyPair: CurrencyPair, period: Int, start: ZonedDateTime, end: ZonedDateTime): Future[PairPrices] =
    PoloApi.httpRequest(publicUrl, Public.returnChartData + "&" + ReturnChartData.build(currencyPair.toString, period, start.toEpochSecond, end.toEpochSecond).fields.toString).map {
      message => {
        PairPrices(Json.parse(message).as[JsArray].value.toList.map { item =>
          item.as[PairPrice]
        })
      }
    }


  def getMarket(currencyPair: CurrencyPair): Future[PoloMarket] =
    PoloApi.httpRequest(s"$apiUrl/markets/${currencyPair.right}_${currencyPair.left}").map { message =>
      Json.parse(message).as[JsArray].value.toList.map { item =>
        item.as[PoloMarket]
      }.head
    }
}

object PoloApi extends StrictLogging {

  val rootUrl = "https://poloniex.com"

  val tradingApi = "tradingApi"

  val Command = "command"

  val Nonce = "nonce"

  object Public {

    val returnTicker = "returnTicker"

    val returnChartData = "returnChartData"
  }

  object BuySell {

    val currencyPair = "currencyPair"

    val rate = "rate"

    val amount = "amount"

    val buy = "buy"

    val sell = "sell"

    def build(nonce: Long, currencyPair: String, rate: BigDecimal, amount: BigDecimal, isBuy: Boolean) = {
      akka.http.scaladsl.model.FormData(Map(
        BuySell.currencyPair -> currencyPair,
        BuySell.rate -> rate.underlying().toPlainString,
        BuySell.amount -> amount.underlying().toPlainString,
        PoloApi.Command -> (if (isBuy) BuySell.buy else BuySell.sell),
        PoloApi.Nonce -> nonce.toString))
    }
  }

  object ReturnBalances {

    val ReturnBalances = "returnBalances"

    def build(nonce: Long): FormData = akka.http.scaladsl.model.FormData(Map(PoloApi.Command -> ReturnBalances, PoloApi.Nonce -> nonce.toString))
  }

  object AuthHeader {

    val key = "Key"

    val sign = "Sign"

    def build(key: String, sign: String): List[HttpHeader] = {
      List(RawHeader(AuthHeader.key, key), RawHeader(AuthHeader.sign, sign))
    }
  }

  object ReturnOpenOrders {

    val ReturnOpenOrders = "returnOpenOrders"

    val CurrencyPair = "currencyPair"

    def build(nonce: Long): FormData = akka.http.scaladsl.model.FormData(Map(PoloApi.Command -> ReturnOpenOrders, PoloApi.Nonce -> nonce.toString, CurrencyPair -> "all"))
  }

  object ReturnDepositAddresses {

    val ReturnDepositAddresses = "returnDepositAddresses"

    def build(nonce: Long): FormData = akka.http.scaladsl.model.FormData(Map(PoloApi.Command -> ReturnDepositAddresses, PoloApi.Nonce -> nonce.toString))
  }

  object ReturnChartData {

    val CurrencyPair = "currencyPair"

    val Period = "period"

    val Start = "start"

    val End = "end"

    def build(currencyPair: String, period: Int, start: Long, end: Long): FormData = akka.http.scaladsl.model.FormData(
      Map(CurrencyPair -> currencyPair, Period -> period.toString, Start -> start.toString, End -> end.toString))
  }

  object CancelOrder {

    val CancelOrder = "cancelOrder"

    val OrderNumber = "orderNumber"

    def build(nonce: Long, orderNumber: String): FormData = akka.http.scaladsl.model.FormData(Map(
      PoloApi.Command -> CancelOrder,
      PoloApi.CancelOrder.OrderNumber -> orderNumber,
      PoloApi.Nonce -> nonce.toString))
  }

  object ReturnTradeHistory {

    val ReturnTradeHistory = "returnTradeHistory"

    val Start = "start"

    val End = "end"

    val CurrencyPair = "currencyPair"

    def build(nonce: Long, start: Long, end: Long): FormData = akka.http.scaladsl.model.FormData(Map(
      PoloApi.Command -> ReturnTradeHistory,
      PoloApi.ReturnTradeHistory.Start -> start.toString,
      PoloApi.ReturnTradeHistory.End -> end.toString,
      PoloApi.ReturnTradeHistory.CurrencyPair -> "all",
      PoloApi.Nonce -> nonce.toString))
  }

  private def httpRequest(url: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    logger.info(s"Sending post request to $url")
    Http().singleRequest(HttpRequest(uri = s"$url")).flatMap { response =>
      Unmarshal(response.entity).to[String]
    }
  }

  private def httpRequest(url: String, command: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {

    Http().singleRequest(HttpRequest(uri = s"$url?command=$command")).flatMap { response =>
      Unmarshal(response.entity).to[String]
    }
  }

  private def httpRequestPost(url: String, body: FormData, apiKey: String, apiSecret: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    logger.info(s"Sending post request to $url")
    logger.info(s"Body: ${body.fields.toString}")
    Http().singleRequest(HttpRequest(
      method = HttpMethods.POST,
      headers = AuthHeader.build(apiKey, generateHMAC512(apiSecret, body.fields.toString)),
      entity = body.toEntity(HttpCharsets.`UTF-8`),
      uri = url)).flatMap { response =>
      if (response.status == StatusCodes.OK)
        Unmarshal(response.entity).to[String]
      else {
        logger.error("Unexpected response code ({}) reason ({})",response.status.value, response.status.reason())
        //response.discardEntityBytes()
        Unmarshal(response.entity).to[String].map(body => logger.error(body))
        throw new RuntimeException("Return code was " + response.status)
      }
    }
  }

  private[tradebot] def generateHMAC512(sharedSecret: String, preHashString: String): String = {
    val secret = new SecretKeySpec(sharedSecret.getBytes, "HmacSHA512")
    val mac = Mac.getInstance("HmacSHA512")
    mac.init(secret)
    val hashString: Array[Byte] = mac.doFinal(preHashString.getBytes)
    val hmacsign = hashString.toList.map("%02x" format _).mkString
    logger.debug(s"HMAC sha512 signature: $hmacsign")
    hmacsign
  }

}