package com.kobr4.tradebot.api

import java.time.{ Instant, ZoneId, ZonedDateTime }
import java.util.Base64

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.DefaultConfiguration
import com.kobr4.tradebot.api.KrakenApi.Public
import com.kobr4.tradebot.model._
import com.typesafe.scalalogging.StrictLogging
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import play.api.libs.json._

import scala.concurrent.{ ExecutionContext, Future }

sealed trait SupportedExchange

case object Kraken extends SupportedExchange

case object Poloniex extends SupportedExchange

object UnsupportedExchangeException extends RuntimeException

object SupportedExchange {

  def fromString(input: String): SupportedExchange = input match {
    case "kraken" => Kraken
    case "poloniex" => Poloniex
    case _ => throw UnsupportedExchangeException
  }
}

object CurrencyPairHelper {
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
    (Asset.fromString(a), Asset.fromString(b)) match {
      case (Some(asset1), Some(asset2)) =>
        CurrencyPair(asset1, asset2)
    }
  }

  def toString(pair: CurrencyPair): String = pair match {
    case CurrencyPair(Asset.Usd, Asset.Ada) => s"ADAUSD"
    case CurrencyPair(Asset.Usd, Asset.Btc) => s"XXBTZUSD"
    case CurrencyPair(Asset.Usd, a: Asset) => s"X${a}ZUSD"
  }
}

case class KrakenTrade(ordertxid: String, pair: String, `type`: String, price: BigDecimal, vol: BigDecimal, time: Long) {

  def toOrder: Order = this.`type` match {
    case "buy" =>
      val currencyPair = CurrencyPairHelper.fromString(pair)
      Buy(
        if (currencyPair.left == Asset.Usd) currencyPair.right else currencyPair.left,
        price, vol, ZonedDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneId.of("UTC")))
    case "sell" =>
      val currencyPair = CurrencyPairHelper.fromString(pair)
      Sell(
        if (currencyPair.left == Asset.Usd) currencyPair.right else currencyPair.left,
        price, vol, ZonedDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneId.of("UTC")))
  }
}

object KrakenTrade {

  import play.api.libs.functional.syntax._

  implicit val krakenTradeReads: Reads[KrakenTrade] = (
    (JsPath \ "ordertxid").read[String] and
    (JsPath \ "pair").read[String] and
    (JsPath \ "type").read[String] and
    (JsPath \ "price").read[BigDecimal] and
    (JsPath \ "vol").read[BigDecimal] and
    (JsPath \ "time").read[Long])(KrakenTrade.apply _)
}

class KrakenApi(krakenUrl: String = KrakenApi.rootUrl, apiKey: String = DefaultConfiguration.KrakenApi.Key,
  apiSecret: String = DefaultConfiguration.KrakenApi.Secret)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) extends ExchangeApi {

  private val publicUrl = s"$krakenUrl/0/public"

  private val privateUrl = s"/0/private"

  private def nonce() = System.currentTimeMillis()

  //private def nonce() = 1538729057879L

  import play.api.libs.functional.syntax._

  def quoteReads(pair: CurrencyPair): Reads[Quote] = (
    Reads.pure(pair) and
    (JsPath \ "c")(0).read[BigDecimal] and
    (JsPath \ "a")(0).read[BigDecimal] and
    (JsPath \ "b")(0).read[BigDecimal] and
    Reads.pure(BigDecimal(0)) and
    (JsPath \ "v")(0).read[BigDecimal] and
    (JsPath \ "p")(0).read[BigDecimal])(Quote.apply _)

  def tradableAsset()(implicit ec: ExecutionContext): Future[List[String]] = KrakenApi.httpRequest(publicUrl, Public.tradableAsset).map { message =>
    Json.parse(message).as[JsObject].value("result").as[JsObject].fields.map(_._1).toList
  }

  def returnTicker()(implicit ec: ExecutionContext): Future[List[Quote]] =
    tradableAsset.map(_.filter(aList => aList.contains("ETH") || aList.contains("XMR") || aList.contains("XBT")).mkString(",")).flatMap { assetListParam =>
      {

        KrakenApi.httpRequest(publicUrl, Public.ticker(assetListParam)).map { message =>

          Json.parse(message).as[JsObject].value("result").as[JsObject].fields.flatMap {
            case (s, v) =>
              val pairString = s.toUpperCase
              val (a, b) = pairString.length match {
                case 6 =>
                  (pairString.substring(0, 3), pairString.substring(3))
                case 7 =>
                  (pairString.substring(0, 4), pairString.substring(4))
                case 8 =>
                  (pairString.substring(1, 4), pairString.substring(5))
              }
              (Asset.fromString(a), Asset.fromString(b)) match {
                case (Some(asset1), Some(asset2)) =>
                  v.asOpt[Quote](quoteReads(CurrencyPair(asset1, asset2)))
              }
          }.toList
        }
      }
    }

  def returnDepositMethods(asset: String): Future[List[String]] = {
    val reqNonce = nonce()
    KrakenApi.httpRequestPost(krakenUrl, s"$privateUrl/${KrakenApi.DepositMethods.DepositMethods}", reqNonce,
      KrakenApi.DepositMethods.build(reqNonce, asset), apiKey, apiSecret).map { message =>
        println(message)
        /*
      Json.parse(message).as[JsObject].value("result").as[JsObject].fields.flatMap {

      }
      */
        List()
      }
  }

  override def returnBalances: Future[Map[Asset, Quantity]] = {
    val reqNonce = nonce()
    KrakenApi.httpRequestPost(krakenUrl, s"$privateUrl/${KrakenApi.ReturnBalances.ReturnBalances}", reqNonce,
      KrakenApi.ReturnBalances.build(reqNonce), apiKey, apiSecret).map { message =>
        Json.parse(message).as[JsObject].value("result").as[JsObject].fields.flatMap {
          case (s, v) => Asset.fromString(s.toUpperCase).map { asset =>
            (asset, Quantity(BigDecimal(v.as[String])))
          }
        }.toMap
      }
  }

  override def returnDepositAddresses: Future[Map[Asset, String]] = {
    val reqNonce = nonce()
    KrakenApi.httpRequestPost(krakenUrl, s"$privateUrl/${KrakenApi.ReturnDepositAddresses.ReturnDepositAddresses}", reqNonce,
      KrakenApi.ReturnDepositAddresses.build(reqNonce), apiKey, apiSecret).map { message =>
        println(message)
        Map[Asset, String]()
      }
  }

  override def returnOpenOrders(): Future[List[PoloOrder]] = {
    val reqNonce = nonce()
    KrakenApi.httpRequestPost(krakenUrl, s"$privateUrl/${KrakenApi.ReturnOpenOrders.ReturnOpenOrders}", reqNonce,
      KrakenApi.ReturnOpenOrders.build(reqNonce), apiKey, apiSecret).map { message =>
        println(message)
        Json.parse(message).as[JsObject].value("result").as[JsObject].value("open").as[JsObject].value.toList.map {
          case (txid, jsOrder) =>
            val rate = jsOrder.as[JsObject].value("price").as[BigDecimal]
            val vol = jsOrder.as[JsObject].value("vol").as[BigDecimal]
            PoloOrder(txid, rate, vol)
        }
      }
  }

  override def cancelOrder(orderNumber: String): Future[Boolean] = {
    val reqNonce = nonce()
    KrakenApi.httpRequestPost(krakenUrl, s"$privateUrl/${KrakenApi.CancelOrder.CancelOrder}", reqNonce,
      KrakenApi.CancelOrder.build(reqNonce, orderNumber), apiKey, apiSecret).map { message =>
        if (Json.parse(message).as[JsObject].value("result").as[JsObject].value("count").as[JsNumber].as[Long] > 0) true
        else false
      }
  }

  override def buy(currencyPair: CurrencyPair, rate: BigDecimal, amount: BigDecimal): Future[String] = {
    val reqNonce = nonce()
    KrakenApi.httpRequestPost(krakenUrl, s"$privateUrl/${KrakenApi.BuySell.AddOrder}", reqNonce,
      KrakenApi.BuySell.build(reqNonce, CurrencyPairHelper.toString(currencyPair), rate, amount, isBuy = true), apiKey, apiSecret).map { message =>
        message
      }
  }

  override def sell(currencyPair: CurrencyPair, rate: BigDecimal, amount: BigDecimal): Future[String] = {
    val reqNonce = nonce()
    KrakenApi.httpRequestPost(krakenUrl, s"$privateUrl/${KrakenApi.BuySell.AddOrder}", reqNonce,
      KrakenApi.BuySell.build(reqNonce, CurrencyPairHelper.toString(currencyPair), rate, amount, isBuy = false), apiKey, apiSecret).map { message =>
        message
      }
  }

  override def returnTradeHistory(start: ZonedDateTime, end: ZonedDateTime): Future[List[Order]] = {
    val reqNonce = nonce()
    KrakenApi.httpRequestPost(krakenUrl, s"$privateUrl/${KrakenApi.ReturnTradesHistory.ReturnTradesHistory}", reqNonce,
      KrakenApi.ReturnTradesHistory.build(reqNonce, start.toEpochSecond, end.toEpochSecond), apiKey, apiSecret).map { message =>
        Json.parse(message).as[JsObject].value("result").as[JsObject].value("trades").as[JsObject].value.toList.map {
          case (txid, order) => order.as[KrakenTrade].toOrder
        }
      }
  }
}

object KrakenApi extends StrictLogging {

  val rootUrl = "https://api.kraken.com"

  val Asset = "asset"

  object Public {

    def ticker(pairList: String) = s"Ticker?pair=$pairList"

    val tradableAsset = "AssetPairs"
  }

  object ReturnBalances {

    val ReturnBalances = "Balance"

    def build(nonce: Long): FormData = akka.http.scaladsl.model.FormData(Map(PoloApi.Nonce -> nonce.toString))
  }

  object DepositMethods {

    val DepositMethods = "DepositMethods"

    def build(nonce: Long, asset: String): FormData = akka.http.scaladsl.model.FormData(Map(PoloApi.Nonce -> nonce.toString, KrakenApi.Asset -> asset))
  }

  object AuthHeader {

    val key = "API-Key"

    val sign = "API-Sign"

    def build(key: String, sign: String): List[HttpHeader] = {
      List(RawHeader(AuthHeader.key, key), RawHeader(AuthHeader.sign, sign))
    }
  }

  object ReturnOpenOrders {

    val ReturnOpenOrders = "OpenOrders"

    def build(nonce: Long): FormData = akka.http.scaladsl.model.FormData(Map(PoloApi.Nonce -> nonce.toString))
  }

  object CancelOrder {

    val CancelOrder = "CancelOrder"

    val TxId = "txid"

    def build(nonce: Long, txid: String): FormData = akka.http.scaladsl.model.FormData(Map(
      PoloApi.Nonce -> nonce.toString,
      KrakenApi.CancelOrder.TxId -> txid))
  }

  object BuySell {

    val AddOrder = "AddOrder"

    val price = "price"

    val volume = "volume"

    val pair = "pair"

    val buy = "buy"

    val sell = "sell"

    val `type` = "type"

    val orderType = "ordertype"

    def build(nonce: Long, currencyPair: String, rate: BigDecimal, amount: BigDecimal, isBuy: Boolean) = {
      akka.http.scaladsl.model.FormData(Map(
        BuySell.pair -> currencyPair,
        BuySell.price -> rate.toString(),
        BuySell.volume -> amount.toString(),
        BuySell.orderType -> "limit",
        BuySell.`type` -> (if (isBuy) BuySell.buy else BuySell.sell),
        PoloApi.Nonce -> nonce.toString))
    }
  }

  object ReturnDepositAddresses {

    val ReturnDepositAddresses = "DepositAddresses"

    def build(nonce: Long): FormData = akka.http.scaladsl.model.FormData(Map(PoloApi.Nonce -> nonce.toString))
  }

  object ReturnTradesHistory {

    val ReturnTradesHistory = "TradesHistory"

    val Start = "start"

    val End = "end"

    def build(nonce: Long, start: Long, end: Long): FormData = akka.http.scaladsl.model.FormData(Map(
      PoloApi.Nonce -> nonce.toString,
      KrakenApi.ReturnTradesHistory.Start -> start.toString, KrakenApi.ReturnTradesHistory.End -> end.toString))
  }

  private def httpRequest(url: String, command: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    Http().singleRequest(HttpRequest(uri = s"$url/$command")).flatMap { response =>
      Unmarshal(response.entity).to[String]
    }
  }

  private def httpRequestPost(krakenUrl: String, path: String, nonce: Long, body: FormData, apiKey: String, apiSecret: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    val url = s"$krakenUrl$path"
    logger.info(s"Sending post request to $url")
    logger.info(s"Body: ${body.fields.toString}")
    logger.info(s"Path $path")
    Http().singleRequest(HttpRequest(
      method = HttpMethods.POST,
      headers = AuthHeader.build(apiKey, generateHMAC512(apiSecret, path.getBytes ++ generateSha256(nonce.toString + body.fields.toString))),
      entity = body.toEntity(HttpCharsets.`UTF-8`),
      uri = url)).flatMap { response =>
      if (response.status == StatusCodes.OK)
        Unmarshal(response.entity).to[String]
      else
        throw new RuntimeException("Return code was " + response.status)
    }
  }

  private[tradebot] def generateSha256(originalString: String): Array[Byte] = {
    import java.security.MessageDigest
    val digest = MessageDigest.getInstance("SHA-256")
    import java.nio.charset.StandardCharsets
    val hash = digest.digest(originalString.getBytes(StandardCharsets.UTF_8))
    hash
  }

  private[tradebot] def generateHMAC512(sharedSecret: String, preHashData: Array[Byte]): String = {
    val secret = new SecretKeySpec(Base64.getDecoder.decode(sharedSecret), "HmacSHA512")
    val mac = Mac.getInstance("HmacSHA512")
    mac.init(secret)
    val hashString: Array[Byte] = mac.doFinal(preHashData)
    val hmacsign = Base64.getEncoder.encodeToString(hashString)
    logger.debug(s"HMAC sha512 signature: $hmacsign")
    hmacsign
  }

}
