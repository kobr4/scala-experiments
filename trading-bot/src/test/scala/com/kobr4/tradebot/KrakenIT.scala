package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.KrakenApi
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.duration._

class KrakenIT extends FlatSpec with ScalaFutures with Matchers {

  implicit val as = ActorSystem()
  implicit val am = ActorMaterializer()
  implicit val ec = as.dispatcher

  it should "return tradable pairs" in {

    val krakenApi = new KrakenApi()

    krakenApi.tradableAsset().futureValue(Timeout(10 seconds)) should contain("ADACAD")

  }

  it should "return ticker" in {

    val krakenApi = new KrakenApi()

    val quoteList = krakenApi.returnTicker().futureValue(Timeout(10 seconds))

    println(quoteList)
  }

}
