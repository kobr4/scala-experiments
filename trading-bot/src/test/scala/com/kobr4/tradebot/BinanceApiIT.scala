package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{BinanceApi, CurrencyPair}
import com.kobr4.tradebot.model.Asset
import com.kobr4.tradebot.scheduler.KrakenDailyJob
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._

class BinanceApiIT extends FlatSpec with ScalaFutures with Matchers {

  implicit val as = ActorSystem()
  implicit val am = ActorMaterializer()
  implicit val ec = as.dispatcher
/*
  it should "return ticker" in {

    val binanceApi = new BinanceApi()

    val quoteList = binanceApi.returnTicker().futureValue(Timeout(10 seconds))

    quoteList.map(_.pair) should contain allElementsOf KrakenDailyJob.assetMap.map(assetW => CurrencyPair(Asset.Tether, assetW._1)).toList

    println(quoteList)
  }

  it should "return open orders" in {

    val binanceApi = new BinanceApi(DefaultConfiguration.BinanceApi.Key, DefaultConfiguration.BinanceApi.Secret)

    binanceApi.returnOpenOrders().futureValue(Timeout(10 seconds))
  }

  it should "return balances" in {

    val binanceApi = new BinanceApi(DefaultConfiguration.BinanceApi.Key, DefaultConfiguration.BinanceApi.Secret)

    val assetMap = binanceApi.returnBalances.futureValue(Timeout(10 seconds))

    println(assetMap)
  }


  it should "return trade history" in {

    val binanceApi = new BinanceApi(DefaultConfiguration.BinanceApi.Key, DefaultConfiguration.BinanceApi.Secret)

    val tradeList = binanceApi.returnTradeHistory().futureValue(Timeout(10 seconds))

    println(tradeList)
  }
*/
  it should "return pair props" in {

    val binanceApi = new BinanceApi(DefaultConfiguration.BinanceApi.Key, DefaultConfiguration.BinanceApi.Secret)

    val pairProp = binanceApi.getPairProp().futureValue(Timeout(10 seconds))

    println(pairProp)
  }
}
