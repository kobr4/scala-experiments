package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.PoloApi._
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

case class PoloOrder(orderNumber: Long, rate: BigDecimal, amount: BigDecimal)

case class CurrencyPair(left: Asset, right: Asset)

case class Quote(pair: CurrencyPair, last: BigDecimal, lowestAsk: BigDecimal, highestBid: BigDecimal, percentChange: BigDecimal,
                 baseVolume: BigDecimal, quoteVolume: BigDecimal)


class PoloApi(val poloUrl: String = PoloApi.rootUrl)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) {

  private val tradingUrl = s"$poloUrl/${PoloApi.tradingApi}"

  private val publicUrl = s"$poloUrl/public"

  import play.api.libs.functional.syntax._

  implicit val poloOrderReads: Reads[PoloOrder] = (
    (JsPath \ "orderNumber").read[String].map(s => s.toLong) and
      (JsPath \ "rate").read[BigDecimal] and
      (JsPath \ "amount").read[BigDecimal]
    ) (PoloOrder.apply _)

  def quoteReads(pair: CurrencyPair): Reads[Quote] = (
    Reads.pure(pair) and
      (JsPath \ "last").read[BigDecimal] and
      (JsPath \ "lowestAsk").read[BigDecimal] and
      (JsPath \ "highestBid").read[BigDecimal] and
      (JsPath \ "percentChange").read[BigDecimal] and
      (JsPath \ "baseVolume").read[BigDecimal] and
      (JsPath \ "quoteVolume").read[BigDecimal]
    ) (Quote.apply _)


  def returnBalances: Future[Map[Asset, Quantity]] =
    PoloApi.httpRequestPost(tradingUrl, PoloApi.ReturnBalances.ReturnBalances).map { message =>
      Json.parse(message).as[JsObject].fields.flatMap { case (s, v) => Asset.fromString(s.toUpperCase).map { asset =>
        (asset, Quantity(BigDecimal(v.as[String])))
      }
      }.toMap
    }

  def returnDepositAddresses: Future[Map[Asset, String]] =
    PoloApi.httpRequestPost(tradingUrl, ReturnDepositAddresses.build()).map { message =>
      Json.parse(message).as[JsObject].fields.flatMap { case (s, v) => Asset.fromString(s.toUpperCase).map { asset =>
        (asset, v.as[String])
      }
      }.toMap
    }

  def returnOpenOrders() =
    PoloApi.httpRequestPost(tradingUrl, ReturnOpenOrders.build()).map { message =>
      Json.parse(message).as[JsArray].value.map { order => order.as[PoloOrder] }.toList
    }

  def cancelOrder(orderNumber: Long) = {
    PoloApi.httpRequestPost(tradingUrl, CancelOrder.build()).map { message =>
      Json.parse(message).as[JsObject].value.get("success").exists(_.as[Int] match {
        case 1 => true
        case _ => false
      })
    }
  }

  def buy(currencyPair: String, rate: BigDecimal, amount: BigDecimal) =
    PoloApi.httpRequestPost(tradingUrl, BuySell.build(currencyPair, rate, amount, true))

  def sell(currencyPair: String, rate: BigDecimal, amount: BigDecimal) =
    PoloApi.httpRequestPost(tradingUrl, BuySell.build(currencyPair, rate, amount, false))

  def returnTicker(): Future[List[Quote]] = PoloApi.httpRequest(publicUrl, Public.returnTicker).map { message =>
    Json.parse(message).as[JsObject].fields.flatMap { case (s, v) =>
      s.toUpperCase.split('_').map(s => Asset.fromString(s)).toList match {
        case Some(a) :: Some(b) :: Nil => v.asOpt[Quote](quoteReads(CurrencyPair(a, b)))
        case _ => None
      }
    }.toList
  }
}


object PoloApi {

  val rootUrl = "https://poloniex.com"

  val tradingApi = "tradingApi"

  val Command = "command"

  object Public {

    val returnTicker = "returnTicker"
  }

  object BuySell {

    val currencyPair = "currencyPair"

    val rate = "rate"

    val amount = "amount"

    val buy = "buy"

    val sell = "sell"

    def build(currencyPair: String, rate: BigDecimal, amount: BigDecimal, isBuy: Boolean) = {
      akka.http.scaladsl.model.FormData(Map(
        BuySell.currencyPair -> currencyPair,
        BuySell.rate -> rate.toString(),
        BuySell.amount -> amount.toString(),
        PoloApi.Command -> (if (isBuy) BuySell.buy else BuySell.sell)
      )).toEntity(HttpCharsets.`UTF-8`)
    }
  }

  object ReturnBalances {

    val ReturnBalances = "returnBalances"

    def build(): RequestEntity = akka.http.scaladsl.model.FormData(Map(PoloApi.Command -> ReturnBalances)).toEntity(HttpCharsets.`UTF-8`)
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

    def build(): RequestEntity = akka.http.scaladsl.model.FormData(Map(PoloApi.Command -> ReturnOpenOrders)).toEntity(HttpCharsets.`UTF-8`)
  }

  object ReturnDepositAddresses {

    val ReturnDepositAddresses = "returnDepositAddresses"

    def build(): RequestEntity = akka.http.scaladsl.model.FormData(Map(PoloApi.Command -> ReturnDepositAddresses)).toEntity(HttpCharsets.`UTF-8`)
  }

  object CancelOrder {

    val CancelOrder = "cancelOrder"

    def build(): RequestEntity = akka.http.scaladsl.model.FormData(Map(PoloApi.Command -> CancelOrder)).toEntity(HttpCharsets.`UTF-8`)
  }

  def httpRequest(url: String, command: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    Http().singleRequest(HttpRequest(uri = s"$url?command=$command")).flatMap { response =>
      Unmarshal(response.entity).to[String]
    }
  }

  def httpRequestPost(url: String, body: RequestEntity)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    Http().singleRequest(HttpRequest(
      method = HttpMethods.POST,
      headers = AuthHeader.build("toto", "sign"),
      entity = body,
      uri = url)
    ).flatMap { response =>
      Unmarshal(response.entity).to[String]
    }
  }

}