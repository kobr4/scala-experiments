package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.{ExecutionContext, Future}


class PoloApi(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) {

  def returnBalances: Future[Map[Asset, Quantity]] =
    PoloApi.httpRequest(PoloApi.tradingUrl,PoloApi.ReturnBalances.ReturnBalances).map { message =>
      val fvalueList = Json.parse(message).as[JsObject].fields.map { case (s, v) => (s.toUpperCase, BigDecimal(v.as[Double])) }
        .flatMap { case (s, v) => s match {
          case "ETH" => Option((Asset.Eth, Quantity(v)))
          case "BTC" => Option((Asset.Btc, Quantity(v)))
          case "USD" => Option((Asset.Usd, Quantity(v)))
          case _ => None
        }
        }

      fvalueList.toMap
    }


  def returnCompleteBalances = ???

  def returnDepositAddresses = ???

  def returnOpenOrders = ???

  def buy = ???

  def sell = ???
}


object PoloApi {

  private val rootUrl = "https://poloniex.com"

  private val tradingApi = "tradingApi"

  val returnCompleteBalances = "returnCompleteBalances"

  val returnDepositAddresses = "returnDepositAddresses"

  val Command = "command"

  val tradingUrl = s"$rootUrl/$tradingApi"

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
      List(HttpHeader(AuthHeader.key,key), HttpHeader(AuthHeader.sign, sign))
    }
  }


  def httpRequest(url: String, command: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    Http().singleRequest(HttpRequest(uri = "https://poloniex.com/tradingApi")).flatMap { response =>
      Unmarshal(response.entity).to[String]
    }
  }

  def httpRequestPost(url: String, body: RequestEntity)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    Http().singleRequest(HttpRequest(
      method = HttpMethods.POST,
      headers = AuthHeader.build("toto","sign"),
      entity = body,
      uri = "https://poloniex.com/tradingApi")
    ).flatMap { response =>
      Unmarshal(response.entity).to[String]
    }
  }

}