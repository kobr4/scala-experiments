package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.model.Asset
import com.kobr4.tradebot.services.PriceService
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class PriceServiceIT extends FlatSpec with ScalaFutures with Matchers {

  implicit val as = ActorSystem()
  implicit val am = ActorMaterializer()
  implicit val ec = as.dispatcher

  it should "get price" in {
    implicit val ec = ExecutionContext.global

    PriceService.getPriceData(Asset.Btc).futureValue(Timeout(30 seconds))
  }

}
