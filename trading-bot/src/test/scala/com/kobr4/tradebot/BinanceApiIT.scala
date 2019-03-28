package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{BinanceApi, CurrencyPair, KrakenApi}
import com.kobr4.tradebot.model.Asset
import com.kobr4.tradebot.scheduler.KrakenDailyJob
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.duration._

class BinanceApiIT extends FlatSpec with ScalaFutures with Matchers {

  implicit val as = ActorSystem()
  implicit val am = ActorMaterializer()
  implicit val ec = as.dispatcher

  it should "return ticker" in {

    val binanceApi = new BinanceApi()

    val quoteList = binanceApi.returnTicker().futureValue(Timeout(10 seconds))

    quoteList.map(_.pair) should contain allElementsOf KrakenDailyJob.assetMap.map(assetW => CurrencyPair(Asset.Tether, assetW._1)).toList

    println(quoteList)
  }

}
