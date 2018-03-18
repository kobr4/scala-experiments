package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.{ExecutionContext, Future}


class PoloApi(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) {

  def returnBalances: Future[Map[Asset, Quantity]] =
    PoloApi.httpRequest(PoloApi.returnBalances).map { message =>
      val fvalueList = Json.parse(message).as[JsObject].fields.map { case (s, v) => (s.toUpperCase, BigDecimal(v.as[Double])) }
        .flatMap { case (s, v) => s match {
          case "ETH" => Option((Asset.Eth, Quantity(v)))
          case "BTC" => Option((Asset.Btc, Quantity(v)))
          case "USD" => Option((Asset.Usd, Quantity(v)))
          case _ => None
        }}

      fvalueList.toMap
    }


  def returnCompleteBalances = ???

  def returnDepositAddresses = ???

  def returnOpenOrders = ???

  def buy = ???

  def sell = ???
}


object PoloApi {

  val rootUrl = "https://poloniex.com/"

  val tradingApi = "tradingApi"

  val returnBalances = "returnBalances"

  val returnCompleteBalances = "returnCompleteBalances"

  val returnDepositAddresses = "returnDepositAddresses"

  val buy = "buy"

  val sell = "sell"


  def httpRequest(command: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    Http().singleRequest(HttpRequest(uri = "https://poloniex.com/tradingApi")).flatMap { response =>
      Unmarshal(response.entity).to[String]
    }
  }
}