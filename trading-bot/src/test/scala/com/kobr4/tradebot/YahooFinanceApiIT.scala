package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.YahooFinanceApi
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

class YahooFinanceApiIT extends FlatSpec with Matchers with ScalaFutures {

  implicit val as = ActorSystem()
  implicit val am = ActorMaterializer()
  implicit val ec = as.dispatcher

  "API" should "return btc historical data" in {

    val priceCsv = YahooFinanceApi.fetchPriceData("GOOG").futureValue(Timeout(10 seconds))

    priceCsv.prices should not be (empty)
  }

}
