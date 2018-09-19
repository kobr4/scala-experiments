package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.json._
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import scala.concurrent.{ ExecutionContext, Future }

case class PoloOrder(orderNumber: Long, rate: BigDecimal, amount: BigDecimal)

case class CurrencyPair(left: Asset, right: Asset) {
  override def toString: String = {
    s"${left.toString}_${right.toString}"
  }
}

case class Quote(pair: CurrencyPair, last: BigDecimal, lowestAsk: BigDecimal, highestBid: BigDecimal, percentChange: BigDecimal,
  baseVolume: BigDecimal, quoteVolume: BigDecimal)

class PoloApi(val poloUrl: String = PoloApi.rootUrl)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) extends PoloAPIInterface {

  def nonce = System.currentTimeMillis()

  import PoloApi._
  private val tradingUrl = s"$poloUrl/${PoloApi.tradingApi}"

  private val publicUrl = s"$poloUrl/public"

  import play.api.libs.functional.syntax._

  implicit val poloOrderReads: Reads[PoloOrder] = (
    (JsPath \ "orderNumber").read[String].map(s => s.toLong) and
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

  override def returnBalances: Future[Map[Asset, Quantity]] =
    PoloApi.httpRequestPost(tradingUrl, PoloApi.ReturnBalances.build(nonce)).map { message =>
      Json.parse(message).as[JsObject].fields.flatMap {
        case (s, v) => Asset.fromString(s.toUpperCase).map { asset =>
          (asset, Quantity(BigDecimal(v.as[String])))
        }
      }.toMap
    }

  override def returnDepositAddresses: Future[Map[Asset, String]] =
    PoloApi.httpRequestPost(tradingUrl, ReturnDepositAddresses.build(nonce)).map { message =>
      Json.parse(message).as[JsObject].fields.flatMap {
        case (s, v) => Asset.fromString(s.toUpperCase).map { asset =>
          (asset, v.as[String])
        }
      }.toMap
    }

  override def returnOpenOrders(): Future[List[PoloOrder]] =
    PoloApi.httpRequestPost(tradingUrl, ReturnOpenOrders.build(nonce)).map { message =>
      Json.parse(message).as[JsArray].value.map { order => order.as[PoloOrder] }.toList
    }

  override def cancelOrder(orderNumber: Long): Future[Boolean] = {
    PoloApi.httpRequestPost(tradingUrl, CancelOrder.build(nonce)).map { message =>
      Json.parse(message).as[JsObject].value.get("success").exists(_.as[Int] match {
        case 1 => true
        case _ => false
      })
    }
  }

  override def buy(currencyPair: String, rate: BigDecimal, amount: BigDecimal): Future[String] =
    PoloApi.httpRequestPost(tradingUrl, BuySell.build(nonce, currencyPair, rate, amount, true))

  override def sell(currencyPair: String, rate: BigDecimal, amount: BigDecimal): Future[String] =
    PoloApi.httpRequestPost(tradingUrl, BuySell.build(nonce, currencyPair, rate, amount, false))

  override def returnTicker()(implicit ec: ExecutionContext): Future[List[Quote]] = PoloApi.httpRequest(publicUrl, Public.returnTicker).map { message =>
    Json.parse(message).as[JsObject].fields.flatMap {
      case (s, v) =>
        s.toUpperCase.split('_').map(s => Asset.fromString(s)).toList match {
          case Some(a) :: Some(b) :: Nil => v.asOpt[Quote](quoteReads(CurrencyPair(a, b)))
          case _ => None
        }
    }.toList
  }
}

object PoloApi extends StrictLogging {

  val rootUrl = "https://poloniex.com"

  val tradingApi = "tradingApi"

  val Command = "command"

  val Nonce = "nonce"

  object Public {

    val returnTicker = "returnTicker"
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
        BuySell.rate -> rate.toString(),
        BuySell.amount -> amount.toString(),
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

    def build(nonce: Long): FormData = akka.http.scaladsl.model.FormData(Map(PoloApi.Command -> ReturnOpenOrders, PoloApi.Nonce -> nonce.toString))
  }

  object ReturnDepositAddresses {

    val ReturnDepositAddresses = "returnDepositAddresses"

    def build(nonce: Long): FormData = akka.http.scaladsl.model.FormData(Map(PoloApi.Command -> ReturnDepositAddresses, PoloApi.Nonce -> nonce.toString))
  }

  object CancelOrder {

    val CancelOrder = "cancelOrder"

    def build(nonce: Long): FormData = akka.http.scaladsl.model.FormData(Map(PoloApi.Command -> CancelOrder, PoloApi.Nonce -> nonce.toString))
  }

  private def httpRequest(url: String, command: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    Http().singleRequest(HttpRequest(uri = s"$url?command=$command")).flatMap { response =>
      Unmarshal(response.entity).to[String]
    }
  }

  private def httpRequestPost(url: String, body: FormData)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    logger.info(s"Sending post request to $url")
    logger.info(s"Body: ${body.fields.toString}")
    Http().singleRequest(HttpRequest(
      method = HttpMethods.POST,
      headers = AuthHeader.build(DefaultConfiguration.PoloApi.Key, generateHMAC512(DefaultConfiguration.PoloApi.Secret, body.fields.toString)),
      entity = body.toEntity(HttpCharsets.`UTF-8`),
      uri = url)).flatMap { response =>
      if (response.status == StatusCodes.OK)
        Unmarshal(response.entity).to[String]
      else
        throw new RuntimeException("Return code was " + response.status)
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