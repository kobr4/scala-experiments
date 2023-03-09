package com.kobr4.tradebot.api

import java.time.{ Instant, ZoneId, ZoneOffset, ZonedDateTime }

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

import scala.concurrent.{ ExecutionContext, Future }
import scala.math.BigDecimal.RoundingMode

object PoloV2CurrencyPairHelper {

  def fromString(s: String): CurrencyPair = {
    val cP = s.toUpperCase.split('_').map(s => Asset.fromString(s)).toList
    CurrencyPair(cP.last, cP.head)
  }
}

case class PoloV2Trade(
  id: String,
  symbol: String,
  accountType: String,
  orderId: String,
  side: String,
  `type`: String,
  matchRole: String,
  createTime: Long,
  price: BigDecimal,
  quantity: BigDecimal,
  amount: BigDecimal,
  feeCurrency: String,
  feeAmount: BigDecimal,
  pageId: String,
  clientOrderId: String) {

  def toOrder: Order = this.side match {
    case "BUY" => Buy(PoloV2CurrencyPairHelper.fromString(symbol), price, quantity, ZonedDateTime.ofInstant(Instant.ofEpochMilli(createTime), ZoneId.of("UTC")))
    case "SELL" => Sell(PoloV2CurrencyPairHelper.fromString(symbol), price, quantity, ZonedDateTime.ofInstant(Instant.ofEpochMilli(createTime), ZoneId.of("UTC")))
  }
}

object PoloV2Trade {

  import java.time.format.DateTimeFormatter

  import play.api.libs.functional.syntax._

  val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"))

  implicit val poloTradeReads: Reads[PoloV2Trade] = (
    (JsPath \ "id").read[String] and
    (JsPath \ "symbol").read[String] and
    (JsPath \ "accountType").read[String] and
    (JsPath \ "orderId").read[String] and
    (JsPath \ "side").read[String] and
    (JsPath \ "type").read[String] and
    (JsPath \ "matchRole").read[String] and
    (JsPath \ "createTime").read[Long] and
    (JsPath \ "price").read[BigDecimal] and
    (JsPath \ "quantity").read[BigDecimal] and
    (JsPath \ "amount").read[BigDecimal] and
    (JsPath \ "feeCurrency").read[String] and
    (JsPath \ "feeAmount").read[BigDecimal] and
    (JsPath \ "pageId").read[String] and
    (JsPath \ "clientOrderId").read[String])(PoloV2Trade.apply _)
}

case class PoloV2Order(
  id: String,
  clientOrderId: String,
  symbol: String,
  state: String,
  accountType: String,
  side: String,
  `type`: String,
  timeInForce: String,
  quantity: String,
  price: String,
  avgPrice: String,
  amount: String,
  filledQuantity: String,
  filledAmount: String,
  createTime: Long,
  updateTime: Long,
  orderSource: String,
  loan: Boolean) {

  def toPoloOrder: PoloOrder = {
    PoloOrder(PoloV2CurrencyPairHelper.fromString(symbol), id, BigDecimal(price), BigDecimal(quantity))
  }

}

object PoloV2Order {

  import play.api.libs.functional.syntax._

  implicit def poloOrderReads: Reads[PoloV2Order] = (
    (JsPath \ "id").read[String] and
    (JsPath \ "clientOrderId").read[String] and
    (JsPath \ "symbol").read[String] and
    (JsPath \ "state").read[String] and
    (JsPath \ "accountType").read[String] and
    (JsPath \ "side").read[String] and
    (JsPath \ "type").read[String] and
    (JsPath \ "timeInForce").read[String] and
    (JsPath \ "quantity").read[String] and
    (JsPath \ "price").read[String] and
    (JsPath \ "avgPrice").read[String] and
    (JsPath \ "amount").read[String] and
    (JsPath \ "filledQuantity").read[String] and
    (JsPath \ "filledAmount").read[String] and
    (JsPath \ "createTime").read[Long] and
    (JsPath \ "updateTime").read[Long] and
    (JsPath \ "orderSource").read[String] and
    (JsPath \ "loan").read[Boolean])(PoloV2Order.apply _)

}

class PoloApiV2(
  val apiKey: String = DefaultConfiguration.PoloApi.Key,
  val apiSecret: String = DefaultConfiguration.PoloApi.Secret,
  val poloUrl: String = PoloApi.rootUrl, val apiUrl: String = "https://api.poloniex.com")(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) extends ExchangeApi {

  def nonce = System.currentTimeMillis()

  import PoloApiV2._

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

  def balanceRead: Reads[(Asset, Quantity)] =
    ((JsPath \ "currency").read[String] and
      (JsPath \ "available").read[BigDecimal])((currency, available) => (Asset.fromString(currency), Quantity(available)))

  implicit val pairPriceReads: Reads[PairPrice] = (
    (JsPath \ "date").read[String].map(t => ZonedDateTime.ofInstant(Instant.ofEpochSecond(t.toLong), ZoneOffset.UTC)) and
    (JsPath \ "close").read[BigDecimal])(PairPrice.apply _)

  override def returnBalances: Future[Map[Asset, Quantity]] =
    PoloApiV2.httpRequestGet(s"$apiUrl", "accounts/balances", PoloApiV2.ReturnBalances.build(), apiKey, apiSecret).map { message =>
      (Json.parse(message) \\ "balances").head.as[JsArray].value
        .map(_.as[(Asset, Quantity)](balanceRead))
        .toMap
    }

  override def returnDepositAddresses: Future[Map[Asset, String]] =
    PoloApiV2.httpRequestGet(s"$apiUrl", "wallets/addresses", ReturnDepositAddresses.build(), apiKey, apiSecret).map { message =>
      Json.parse(message).as[JsObject].fields.map {
        case (s, v) => (Asset.fromString(s.toUpperCase), v.as[String])
      }.toMap
    }

  override def returnOpenOrders(): Future[List[PoloOrder]] =
    PoloApiV2.httpRequestGet(s"$apiUrl", "orders", ReturnOpenOrders.build(), apiKey, apiSecret).map { message =>
      print(message)
      Json.parse(message).as[JsArray].value.map(_.as[PoloV2Order].toPoloOrder).toList
    }

  override def cancelOrder(order: PoloOrder): Future[Boolean] = {
    PoloApiV2.httpRequestPost(s"$apiUrl", s"/orders/${order.orderNumber}", CancelOrder.build(nonce, order.orderNumber), apiKey, apiSecret).map { message =>
      Json.parse(message).as[JsObject].value.get("success").exists(_.as[Int] match {
        case 1 => true
        case _ => false
      })
    }
  }

  override def returnTradeHistory(
    start: ZonedDateTime = ZonedDateTime.now().minusMonths(1),
    end: ZonedDateTime = ZonedDateTime.now()): Future[List[Order]] = {
    PoloApiV2.httpRequestGet(s"$apiUrl", "trades", ReturnTradeHistory.build(start.toInstant.toEpochMilli, end.toInstant.toEpochMilli), apiKey, apiSecret).map { message =>
      Json.parse(message).as[JsArray].value.map(_.as[PoloV2Trade].toOrder).toList
    }
  }

  override def buy(currencyPair: CurrencyPair, rate: BigDecimal, amount: BigDecimal): Future[Order] =
    getMarket(currencyPair).flatMap { market =>
      PoloApiV2.httpRequestPost(s"$apiUrl", "orders", BuySell.build(currencyPair.toString, rate, amount.setScale(market.symbolTradeLimit.quantityScale, RoundingMode.DOWN), true), apiKey, apiSecret).map { _ =>
        Buy(currencyPair, rate, amount.setScale(market.symbolTradeLimit.quantityScale, RoundingMode.DOWN), ZonedDateTime.now())
      }
    }

  override def sell(currencyPair: CurrencyPair, rate: BigDecimal, amount: BigDecimal): Future[Order] =
    getMarket(currencyPair).flatMap { market =>
      PoloApiV2.httpRequestPost(s"$apiUrl", "orders", BuySell.build(currencyPair.toString, rate, amount.setScale(market.symbolTradeLimit.quantityScale, RoundingMode.DOWN), false), apiKey, apiSecret).map { _ =>
        Sell(currencyPair, rate, amount.setScale(market.symbolTradeLimit.quantityScale, RoundingMode.DOWN), ZonedDateTime.now())
      }
    }

  override def returnTicker()(implicit ec: ExecutionContext): Future[List[Quote]] = PoloApiV2.httpRequest(publicUrl, Public.returnTicker).map { message =>
    Json.parse(message).as[JsObject].fields.flatMap {
      case (s, v) => v.asOpt[Quote](quoteReads(PoloCurrencyPairHelper.fromString(s)))
      case _ => None
    }.toList
  }

  def returnChartData(currencyPair: CurrencyPair, period: Int, start: ZonedDateTime, end: ZonedDateTime): Future[PairPrices] =
    PoloApiV2.httpRequest(publicUrl, Public.returnChartData + "&" + toRequestString(ReturnChartData.build(currencyPair.toString, period, start.toEpochSecond, end.toEpochSecond)).toString).map {
      message =>
        {
          PairPrices(Json.parse(message).as[JsArray].value.toList.map { item =>
            item.as[PairPrice]
          })
        }
    }

  def getMarket(currencyPair: CurrencyPair): Future[PoloMarket] =
    PoloApiV2.httpRequest(s"$apiUrl/markets/${currencyPair.right}_${currencyPair.left}").map { message =>
      Json.parse(message).as[JsArray].value.toList.map { item =>
        item.as[PoloMarket]
      }.head
    }
}

object PoloApiV2 extends StrictLogging {

  val Command = "command"

  val SignTimestamp = "signTimestamp"

  object Public {

    val returnTicker = "returnTicker"

    val returnChartData = "returnChartData"
  }

  object BuySell {

    val symbol = "symbol"

    val price = "price"

    val quantity = "quantity"

    val buy = "BUY"

    val sell = "SELL"

    val `type` = "type"

    val side = "side"

    def build(currencyPair: String, price: BigDecimal, quantity: BigDecimal, isBuy: Boolean) = {
      Map(
        BuySell.symbol -> currencyPair,
        BuySell.price -> price.underlying().toPlainString,
        BuySell.quantity -> quantity.underlying().toPlainString,
        BuySell.side -> (if (isBuy) BuySell.buy else BuySell.sell),
        BuySell.`type` -> "LIMIT")
    }
  }

  object ReturnBalances {

    val ReturnBalances = "returnBalances"

    val AccountType = "accountType"

    def build(): Map[String, String] = Map(AccountType -> "SPOT")
  }

  object AuthHeader {

    val key = "Key"

    val signature = "signature"

    val signTimestamp = "signTimestamp"

    def build(signTimestamp: String, key: String, signature: String): List[HttpHeader] = {
      List(RawHeader(AuthHeader.signTimestamp, signTimestamp), RawHeader(AuthHeader.key, key), RawHeader(AuthHeader.signature, signature))
    }
  }

  object ReturnOpenOrders {

    val ReturnOpenOrders = "returnOpenOrders"

    val CurrencyPair = "currencyPair"

    def build(): Map[String, String] = Map()
  }

  object ReturnDepositAddresses {

    def build(): Map[String, String] = Map()
  }

  object ReturnChartData {

    val CurrencyPair = "currencyPair"

    val Period = "period"

    val Start = "start"

    val End = "end"

    def build(currencyPair: String, period: Int, start: Long, end: Long): Map[String, String] =
      Map(CurrencyPair -> currencyPair, Period -> period.toString, Start -> start.toString, End -> end.toString)
  }

  object CancelOrder {

    val CancelOrder = "cancelOrder"

    val OrderNumber = "orderNumber"

    def build(nonce: Long, orderNumber: String): Map[String, String] = Map(
      PoloApi.Command -> CancelOrder,
      PoloApi.CancelOrder.OrderNumber -> orderNumber,
      PoloApi.Nonce -> nonce.toString)
  }

  object ReturnTradeHistory {

    val StartTime = "startTime"

    val EndTime = "endTime"

    val Symbol = "symbol"

    val States = "states"

    val AccountType = "accountType"

    def build(start: Long, end: Long): Map[String, String] = Map(
      PoloApiV2.ReturnTradeHistory.EndTime -> end.toString,
      PoloApiV2.ReturnTradeHistory.StartTime -> start.toString)
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

  private def toRequestString(input: Map[String, String]): String = {
    input.toList.sortBy(_._1).map(e => e._1 + "=" + e._2).mkString("&")
  }

  private def httpRequestGet(url: String, method: String, body: Map[String, String], apiKey: String, apiSecret: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    val signTimestamp = System.currentTimeMillis()

    logger.info(s"Sending get request to $url/$method")
    logger.info(s"Body: ${toRequestString(body ++ Map("signTimestamp" -> signTimestamp.toString))}")

    val bodyToSign =
      "GET\n" +
        s"/$method\n" +
        s"${toRequestString(body ++ Map("signTimestamp" -> signTimestamp.toString))}"

    Http().singleRequest(HttpRequest(
      method = HttpMethods.GET,
      headers = AuthHeader.build(signTimestamp.toString, apiKey, generateHMAC256(apiSecret, bodyToSign)),
      entity = Json.toJson(body).toString,
      uri = s"$url/$method?${toRequestString(body)}")).flatMap { response =>
      if (response.status == StatusCodes.OK)
        Unmarshal(response.entity).to[String]
      else {
        logger.error("Unexpected response code ({}) reason ({})", response.status.value, response.status.reason())
        //response.discardEntityBytes()
        Unmarshal(response.entity).to[String].map(body => logger.error(body))
        throw new RuntimeException("Return code was " + response.status)
      }
    }
  }

  private def httpRequestPost(url: String, method: String, body: Map[String, String], apiKey: String, apiSecret: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    val signTimestamp = System.currentTimeMillis()

    logger.info(s"Sending post request to $url/$method")
    logger.info(s"Body: ${toRequestString(body ++ Map("signTimestamp" -> signTimestamp.toString))}")

    val bodyToSign =
      "POST\n" +
        s"/$method\n" +
        s"requestBody=${Json.toJson(body).toString}\nsignTimestamp=$signTimestamp"

    Http().singleRequest(HttpRequest(
      method = HttpMethods.POST,
      headers = AuthHeader.build(signTimestamp.toString, apiKey, generateHMAC256(apiSecret, bodyToSign)),
      entity = Json.toJson(body).toString,
      uri = s"$url/$method")).flatMap { response =>
      if (response.status == StatusCodes.OK)
        Unmarshal(response.entity).to[String]
      else {
        logger.error("Unexpected response code ({}) reason ({})", response.status.value, response.status.reason())
        //response.discardEntityBytes()
        Unmarshal(response.entity).to[String].map(body => logger.error(body))
        throw new RuntimeException("Return code was " + response.status)
      }
    }
  }

  private[tradebot] def generateHMAC256(sharedSecret: String, preHashString: String): String = {
    val secret = new SecretKeySpec(sharedSecret.getBytes, "HmacSHA256")
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(secret)
    val hashString: Array[Byte] = mac.doFinal(preHashString.getBytes)
    val b64 = new String(java.util.Base64.getEncoder.encode(hashString))
    logger.debug(s"HMAC sha256 signature: $b64")
    b64
  }

}