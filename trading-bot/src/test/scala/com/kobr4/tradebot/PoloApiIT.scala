package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.FlatSpec
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

class PoloApiIT extends FlatSpec with ScalaFutures {

  implicit val as = ActorSystem()
  implicit val am = ActorMaterializer()
  implicit val ec = as.dispatcher

  "API" should "return balances" in {
    val api = new PoloApi
    val assetMAp = api.returnBalances.futureValue(Timeout(10 seconds))

    println(assetMAp)
  }

}
