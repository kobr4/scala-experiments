package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{ CurrencyPair, KrakenApi }
import com.kobr4.tradebot.model.Asset
import com.kobr4.tradebot.scheduler.KrakenDailyJob
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

class KrakenApiIT extends FlatSpec with ScalaFutures with Matchers {

  implicit val as = ActorSystem()
  implicit val am = ActorMaterializer()
  implicit val ec = as.dispatcher

  it should "return tradable pairs" in {

    val krakenApi = new KrakenApi()

    val tradableAssetList = krakenApi.tradableAsset().futureValue(Timeout(10 seconds))
    tradableAssetList should contain("ADAEUR")
    tradableAssetList should contain(com.kobr4.tradebot.api.KrakenCurrencyPairHelper.toString(CurrencyPair(Asset.Usd, Asset.Tether)))

  }

  it should "return ticker" in {

    val krakenApi = new KrakenApi()

    val quoteList = krakenApi.returnTicker().futureValue(Timeout(10 seconds))

    quoteList.map(_.pair) should contain allElementsOf KrakenDailyJob.assetMap.map(assetW => CurrencyPair(Asset.Usd, assetW._1)).toList

    println(quoteList)
  }

  it should "return balance" in {

    val krakenApi = new KrakenApi()

    val balanceMap = krakenApi.returnBalances.futureValue(Timeout(10 seconds))

    println(balanceMap)
  }

  it should "return deposit methods" in {

    val krakenApi = new KrakenApi()

    val balanceMap = krakenApi.returnDepositMethods("XETH").futureValue(Timeout(10 seconds))

    println(balanceMap)
  }

  it should "return deposit addresses" in {

    val krakenApi = new KrakenApi()

    val balanceMap = krakenApi.returnDepositAddresses.futureValue(Timeout(10 seconds))

    println(balanceMap)
  }

  it should "return open orders" in {

    val krakenApi = new KrakenApi()

    val orderList = krakenApi.returnOpenOrders().futureValue(Timeout(10 seconds))

    println(orderList)
  }

  it should "return trades history" in {

    val krakenApi = new KrakenApi()

    val orderList = krakenApi.returnTradeHistory().futureValue(Timeout(10 seconds))

    println(orderList)
  }
}
