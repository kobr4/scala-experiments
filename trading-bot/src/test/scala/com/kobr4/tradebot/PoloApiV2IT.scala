package com.kobr4.tradebot

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{CurrencyPair, PoloApiV2}
import com.kobr4.tradebot.model.Asset
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._

class PoloApiV2IT extends FlatSpec with ScalaFutures with Matchers {

  implicit val as = ActorSystem()
  implicit val am = ActorMaterializer()
  implicit val ec = as.dispatcher

  it should "return balances" in {
    val api = new PoloApiV2
    val assetMAp = api.returnBalances.futureValue(Timeout(10 seconds))

    println(assetMAp)
  }

  it should "return trade history" in {
    val api = new PoloApiV2
    val tradeList = api.returnTradeHistory().futureValue(Timeout(10 seconds))

    println(tradeList)
  }

  it should "return charts data" in {
    val api = new PoloApiV2
    val chartData = api.returnChartData(CurrencyPair(Asset.Btc, Asset.Eth), 300,
      ZonedDateTime.parse("2017-01-01T01:00:00.000Z"), ZonedDateTime.parse("2017-02-01T01:00:00.000Z")).futureValue(Timeout(10 seconds))

    chartData.prices.length shouldNot be(0)
  }

  it should "return open orders" in {
    val api = new PoloApiV2
    val openOrderList = api.returnOpenOrders().futureValue(Timeout(10 seconds))

    println(openOrderList)
  }

  it should "return ticker" in {
    val api = new PoloApiV2
    val quotes = api.returnTicker().futureValue(Timeout(10 seconds))

    println(quotes.filter(_.pair.right == Asset.Xrp))
  }


  it should "return market" in {
    val api = new PoloApiV2
    val market = api.getMarket(CurrencyPair(Asset.Tether, Asset.Eth)).futureValue(Timeout(10 seconds))
    println(market)
  }

}
