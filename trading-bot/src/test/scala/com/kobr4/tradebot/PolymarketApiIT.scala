package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.PolymarketApi
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.duration._


class PolymarketApiIT extends FlatSpec with ScalaFutures with Matchers {

  implicit val as = ActorSystem()
  implicit val am = ActorMaterializer()
  implicit val ec = as.dispatcher

  it should "return available market" in {
    val api = new PolymarketApi()
    val markets = api.markets().futureValue(Timeout(10 seconds))

    println(markets.length)
  }

  it should "return available tags" in {
    val api = new PolymarketApi()
    val tags = api.allTags().futureValue(Timeout(10 seconds))

    println(tags.length)
  }

}
