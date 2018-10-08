package com.kobr4.tradebot

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.engine.SafeStrategy
import com.kobr4.tradebot.model._
import com.kobr4.tradebot.services.{ PriceService, TradeBotService }

import scala.concurrent.ExecutionContext
import scala.util.Failure

object App {

  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem = ActorSystem("helloAkkaHttpServer")
    implicit val am: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContext = system.dispatcher

    val date = ZonedDateTime.parse("2017-01-01T01:00:00.000Z")

    val initialAmount = BigDecimal(10000)
    val fees = BigDecimal(0.1)
    val asset = Asset.Custom("CS.PA")

    PriceService.getPriceData(asset).map { pd =>
      val orderList = TradeBotService.run(Asset.Btc, date, initialAmount, pd, fees, SafeStrategy)
      val lastOrder = orderList.last._2
      val (asset, price, quantity) = lastOrder match {
        case Buy(asset, price, quantity) => (asset, price, quantity)
        case Sell(asset, price, quantity) => (asset, price, quantity)
      }

      println(s"Balance:" + (price * quantity)+" USD")
      println(s"Buy & Hold: "+initialAmount/pd.currentPrice(date)*pd.currentPrice(ZonedDateTime.now())+" USD")
    }

    val assetWeight: Map[Asset, BigDecimal] = Map(Asset.Btc -> BigDecimal(0.3), Asset.Eth -> BigDecimal(0.3), Asset.Xmr -> BigDecimal(0.2), Asset.Dgb -> BigDecimal(0.1), Asset.Doge -> BigDecimal(0.1))
    val eventualRun = TradeBotService.runMap(assetWeight, date, initialAmount, fees, SafeStrategy)

    eventualRun.map { pdList =>

      println(pdList.length)
      pdList.foreach { order =>
        print(order._1)
        println(order._2)
      }

      Portfolio.pricesMap(assetWeight.keys.toList).map(pricesMap => {
        val portfolio = Portfolio.create(pricesMap.toMap)
        portfolio.assets(Asset.Usd) = Quantity(initialAmount)
        pdList.foreach { tuple => portfolio.update(tuple._2, BigDecimal(0.1)) }

        println("Final balance: " + portfolio.balance(ZonedDateTime.now()))
      })
    }

    eventualRun.onComplete {
      case Failure(f) =>
        println(f)
        f.printStackTrace()
      case _ =>
    }
  }
}