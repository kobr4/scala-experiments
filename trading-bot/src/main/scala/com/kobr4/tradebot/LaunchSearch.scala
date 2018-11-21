package com.kobr4.tradebot

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.CurrencyPair
import com.kobr4.tradebot.engine._
import com.kobr4.tradebot.model._
import com.kobr4.tradebot.services.{ PriceService, RunMultipleReport, RunPairReport, TradeBotService }
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

object LaunchSearch extends StrictLogging {

  val date = ZonedDateTime.parse("2017-01-01T01:00:00.000Z")
  val initialAmount = BigDecimal(10000)
  val fees = BigDecimal(0.2)
  //val pair = CurrencyPair(Asset.Usd, Asset.Custom("SOI.PA"))
  val pair = CurrencyPair(Asset.Usd, Asset.Btc)

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
      RunPairReport(pair, initialAmount, initialAmount, strategy))
  }

  def runMultipleAndReport(assetWeight: Map[Asset, BigDecimal], priceMap: Map[Asset, PairPrices], strategy: Strategy)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) = {
    val pdList = TradeBotService.runMapT(assetWeight, priceMap, date, Asset.Usd, initialAmount, fees, strategy)
    val portfolio = Portfolio.create(priceMap)
    portfolio.assets(Asset.Usd) = Quantity(initialAmount)
    pdList.foreach { tuple => portfolio.update(tuple, BigDecimal(0.1)) }
    RunMultipleReport(portfolio.balance(Asset.Usd, ZonedDateTime.now()), strategy)

  }

  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem = ActorSystem("helloAkkaHttpServer")
    implicit val am: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContext = system.dispatcher

    logger.info("Launching length: {}", RuleGenerator.getAll(2).combinations(2).toList.length)

    //PriceService.getPriceData(pair, date, ZonedDateTime.now()).map { pd =>

    //val assetWeight: Map[Asset, BigDecimal] = Map(Asset.Btc -> BigDecimal(0.3), Asset.Eth -> BigDecimal(0.3),
    //  Asset.Xmr -> BigDecimal(0.1), Asset.Xrp -> BigDecimal(0.1), Asset.Xlm -> BigDecimal(0.1), Asset.Doge -> BigDecimal(0.1))
    //val assetWeight: Map[Asset, BigDecimal] = Map(Asset.Eth -> BigDecimal(0.2), Asset.Ltc -> BigDecimal(0.2),
    //  Asset.Xmr -> BigDecimal(0.2), Asset.Dgb -> BigDecimal(0.1), Asset.Xrp -> BigDecimal(0.1), Asset.Xlm -> BigDecimal(0.1), Asset.Doge -> BigDecimal(0.1))
    val assetWeight: Map[Asset, BigDecimal] = Map(Asset.Tether -> BigDecimal(1.0))
    //val assetWeight: Map[Asset, BigDecimal]
    // = Map(Asset.Custom("GLE.PA") -> BigDecimal(0.2), Asset.Custom("BNP.PA") -> BigDecimal(0.3), Asset.Custom("FP.PA") -> BigDecimal(0.3), Asset.Custom("ENGI.PA") -> BigDecimal(0.2))
    val eventualPData = Future.sequence(assetWeight.keys.toList.map(asset => PriceService.getPricesOrPair(CurrencyPair(Asset.Usd, asset), date, ZonedDateTime.now()).map(asset -> _)))
    eventualPData.map { priceMap =>

      val reportList = RuleGenerator.getAll(2).combinations(2).toList.par.flatMap { buyList =>
        RuleGenerator.getAll(2).combinations(2).toList.par.map { sellList =>
          val strategy = GeneratedStrategy(buyList, sellList)
          //runPairAndReport(pair, strategy, pd)
          runMultipleAndReport(assetWeight, priceMap.toMap, strategy)
        }
      }
      reportList.maxBy(report => report.finalBalance).print()

      logger.info("Search ended")
    }.recover {
      case NonFatal(t) => logger.info("Failed with error : " + t.printStackTrace())
    }

  }

}
