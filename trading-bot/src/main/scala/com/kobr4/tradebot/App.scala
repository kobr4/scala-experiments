package com.kobr4.tradebot

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.engine.{SafeStrategy, Strategy}
import com.kobr4.tradebot.model.{Asset, Buy, Sell}
import com.kobr4.tradebot.services.{PriceService, TradeBotService}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object App {

  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem = ActorSystem("helloAkkaHttpServer")
    implicit val am: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContext = system.dispatcher

    val date = ZonedDateTime.parse("2017-01-01T01:00:00.000Z")

    val initialAmount = BigDecimal(10000)
    val fees = BigDecimal(0.1)

    PriceService.getPriceData(Asset.Btc, date).map { pd =>
      val orderList = TradeBotService.run(Asset.Btc, initialAmount, pd, fees, SafeStrategy)
      val lastOrder = orderList.last._2
      val (asset, price, quantity) = lastOrder match {
        case Buy(asset, price, quantity) => (asset, price, quantity)
        case Sell(asset, price, quantity) => (asset, price, quantity)
      }

      println(s"balance:" + (price * quantity))
    }

    val eventualRun = TradeBotService.runMap(Map(Asset.Btc -> BigDecimal(0.4), Asset.Eth -> BigDecimal(0.3), Asset.Xmr -> BigDecimal(0.3) ), date, initialAmount, fees, SafeStrategy)

    eventualRun.map { pdList =>

      println(pdList.length)
      pdList.foreach { order =>
        print(order._1)
        println(order._2)
      }
    }

    eventualRun.onComplete {
      case Failure(f) => println(f)
      case _ =>
    }
  }
}