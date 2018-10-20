package com.kobr4.tradebot

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.CurrencyPair
import com.kobr4.tradebot.engine._
import com.kobr4.tradebot.model.{Asset, Buy, PairPrices, Sell}
import com.kobr4.tradebot.services.{PriceService, TradeBotService}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object LaunchSearch {

  val date = ZonedDateTime.parse("2017-01-01T01:00:00.000Z")
  val initialAmount = BigDecimal(10)
  val fees = BigDecimal(0.1)

  def runPairAndReport(pair: CurrencyPair, strategy: Strategy, pd: PairPrices)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): RunPairReport = {

    val orderList = TradeBotService.runPair(pair, date, initialAmount, pd, fees, strategy)
    val maybeLastOrder = orderList.lastOption

    maybeLastOrder.map { lastOrder =>
      val (assetOut, price, quantity, orderDate) = lastOrder match {
        case Buy(asset, price, quantity, date) => (asset, price, quantity, date)
        case Sell(asset, price, quantity, date) => (asset, price, quantity, date)
      }

      RunPairReport(pair, price * quantity, initialAmount / pd.currentPrice(date) * pd.currentPrice(ZonedDateTime.now()), strategy)
    }.getOrElse(
      RunPairReport(pair, initialAmount, initialAmount, strategy)
    )
  }

  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem = ActorSystem("helloAkkaHttpServer")
    implicit val am: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContext = system.dispatcher

    println("Launching length: " + RuleGenerator.getAll(2).combinations(2).toList.length)
    PriceService.getPairPrice(CurrencyPair(Asset.Btc, Asset.Eth), date, ZonedDateTime.now()).map { pd =>

      val reportList = RuleGenerator.getAll(2).combinations(2).toList.flatMap { buyList =>
        RuleGenerator.getAll(2).combinations(2).toList.map { sellList =>
          val strategy = GeneratedStrategy(buyList, sellList)
          val report = runPairAndReport(CurrencyPair(Asset.Btc, Asset.Eth), strategy, pd)
          report
        }
      }

      reportList.maxBy(report => report.finalBalance).print()

      println("Search ended")
    }.recover {
      case NonFatal(t) => println("Failed with error : " + t.printStackTrace())
    }

  }

}
