package com.kobr4.tradebot

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.LaunchReport.date
import com.kobr4.tradebot.api.CurrencyPair
import com.kobr4.tradebot.engine.{AlternativeStrategy, SafeStrategy, Strategy}
import com.kobr4.tradebot.model._
import com.kobr4.tradebot.services.{PriceService, TradeBotService}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure
import scala.util.control.NonFatal

case class RunReport(asset: Asset, finalBalance: BigDecimal, buyAndHold: BigDecimal, strategy: Strategy) {
  def print(): Unit = {
    println(this)
  }
}

case class RunPairReport(pair: CurrencyPair, finalBalance: BigDecimal, buyAndHold: BigDecimal, strategy: Strategy) {
  def print(): Unit = {
    println(this)
  }
}

case class RunMultipleReport(finalBalance: BigDecimal, strategy: Strategy) {
  def print(): Unit = {
    println(this)
  }
}

object LaunchReport {

  val date = ZonedDateTime.parse("2018-01-01T01:00:00.000Z")
  val initialAmount = BigDecimal(10000)
  val fees = BigDecimal(0.1)
  val strategy = SafeStrategy

  def runPairAndReport(pair: CurrencyPair)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[RunPairReport] = {
    PriceService.getPairPrice(pair, date, ZonedDateTime.now()).map { pd =>
      val orderList = TradeBotService.runPair(pair, date, initialAmount, pd, fees, strategy)
      val lastOrder = orderList.last
      val (assetOut, price, quantity, orderDate) = lastOrder match {
        case Buy(asset, price, quantity, date) => (asset, price, quantity, date)
        case Sell(asset, price, quantity, date) => (asset, price, quantity, date)
      }

      RunPairReport(pair, price * quantity, initialAmount / pd.currentPrice(date) * pd.currentPrice(ZonedDateTime.now()), strategy)
    }
  }

  def runAndReport(asset: Asset)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[RunReport] = {
    PriceService.getPriceData(asset).map { pd =>
      val orderList = TradeBotService.run(asset, date, initialAmount, pd, fees, strategy)
      val lastOrder = orderList.last
      val (assetOut, price, quantity, orderDate) = lastOrder match {
        case Buy(asset, price, quantity, date) => (asset, price, quantity, date)
        case Sell(asset, price, quantity, date) => (asset, price, quantity, date)
      }

      RunReport(asset, price * quantity, initialAmount / pd.currentPrice(date) * pd.currentPrice(ZonedDateTime.now()), strategy)
    }
  }

  def runMultipleAndReport(assetWeight: Map[Asset, BigDecimal])(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) = {
    val eventualRun = TradeBotService.runMap(assetWeight, date, initialAmount, fees, strategy)
    eventualRun.flatMap { pdList =>
      Portfolio.pricesMap(assetWeight.keys.toList).map(pricesMap => {
        val portfolio = Portfolio.create(pricesMap.toMap)
        portfolio.assets(Asset.Usd) = Quantity(initialAmount)
        pdList.foreach { tuple => portfolio.update(tuple, BigDecimal(0.1)) }

        RunMultipleReport(portfolio.balance(Asset.Usd, ZonedDateTime.now()), strategy)
      })
    }
  }

  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem = ActorSystem("helloAkkaHttpServer")
    implicit val am: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContext = system.dispatcher

    runPairAndReport(CurrencyPair(Asset.Btc, Asset.Eth)).map(_.print()).recover {
      case NonFatal(t) => println("Failed with error : "+t.printStackTrace())
    }

    val assetList = List(Asset.Btc, Asset.Eth, Asset.Xmr, Asset.Xlm, Asset.Doge)
    Future.sequence(assetList.map(asset => runAndReport(asset))).map { reportList => reportList.foreach(_.print()) }

    val assetWeight: Map[Asset, BigDecimal] = Map(Asset.Btc -> BigDecimal(0.3), Asset.Eth -> BigDecimal(0.3), Asset.Xmr -> BigDecimal(0.2), Asset.Xlm -> BigDecimal(0.1), Asset.Doge -> BigDecimal(0.1))
    val eventualRun = runMultipleAndReport(assetWeight).map(_.print())

    eventualRun.onComplete {
      case Failure(f) =>
        println(f)
        f.printStackTrace()
      case _ =>
    }
  }
}