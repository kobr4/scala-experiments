package com.kobr4.tradebot.services

import java.time.{ ZoneId, ZonedDateTime }

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{ CurrencyPair, ExchangeApi }
import com.kobr4.tradebot.engine.{ GeneratedStrategy, RuleGenerator, SafeStrategy, Strategy }
import com.kobr4.tradebot.model.Asset.Usd
import com.kobr4.tradebot.model._
import com.typesafe.scalalogging.StrictLogging
import scalacache.Cache
import scalacache.guava.GuavaCache

import scala.concurrent.{ ExecutionContext, Future }
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

object TradeBotService extends StrictLogging {

  val estimatedMarketFee = BigDecimal(0.25)

  def run(asset: Asset, startDate: ZonedDateTime, initialUsdAmount: BigDecimal, priceData: PairPrices, feesPercentage: BigDecimal, strategy: Strategy, endDate: ZonedDateTime = ZonedDateTime.now()): List[Order] = {
    val portfolio = Portfolio.create(Map(asset -> priceData))
    portfolio.assets(Usd) = Quantity(initialUsdAmount)
    priceData.prices.filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)).flatMap(p => strategy.runStrategy(CurrencyPair(Asset.Usd, asset), p.date, priceData, portfolio).map(order => portfolio.update(order, feesPercentage)))
  }

  def runPair(pair: CurrencyPair, startDate: ZonedDateTime, initialAmount: BigDecimal, priceData: PairPrices, feesPercentage: BigDecimal, strategy: Strategy, endDate: ZonedDateTime = ZonedDateTime.now()): List[Order] = {
    val portfolio = Portfolio.create(Map(pair.right -> priceData))
    portfolio.assets(pair.left) = Quantity(initialAmount)
    priceData.prices.filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)).flatMap(p => strategy.runStrategy(pair, p.date, priceData, portfolio).map(order => portfolio.update(order, feesPercentage)))
  }

  def runMapT(assetMap: Map[Asset, BigDecimal], priceMap: Map[Asset, PairPrices], startDate: ZonedDateTime,
    baseAsset: Asset, initialAmount: BigDecimal, feesPercentage: BigDecimal, strategy: Strategy, endDate: ZonedDateTime = ZonedDateTime.now())(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): List[Order] = {
    val portfolio = Portfolio.create(priceMap)
    portfolio.assets(baseAsset) = Quantity(initialAmount)
    priceMap.keys.toList.flatMap(asset => priceMap(asset).prices.filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)).map((asset, _))).sortBy(_._2.date.toEpochSecond).flatMap(assetData =>
      strategy.runStrategy(CurrencyPair(baseAsset, assetData._1), assetData._2.date, priceMap(assetData._1), portfolio, assetMap(assetData._1)).map(order => portfolio.update(order, feesPercentage)))

  }

  def runMap(assetMap: Map[Asset, BigDecimal], startDate: ZonedDateTime, initialUsdAmount: BigDecimal, feesPercentage: BigDecimal, strategy: Strategy, endDate: ZonedDateTime = ZonedDateTime.now())(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[Order]] = {
    val eventualPData = Future.sequence(assetMap.keys.toList.map(asset => PriceService.getPriceData(asset).map(asset -> _)))
    eventualPData.map(_.toMap).map { pDataMap =>
      val portfolio = Portfolio.create(pDataMap)
      portfolio.assets(Usd) = Quantity(initialUsdAmount)
      pDataMap.keys.toList.flatMap(asset => pDataMap(asset).prices.filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)).map((asset, _))).sortBy(_._2.date.toEpochSecond).flatMap(assetData =>
        strategy.runStrategy(CurrencyPair(Asset.Usd, assetData._1), assetData._2.date, pDataMap(assetData._1), portfolio, assetMap(assetData._1)).map(order => portfolio.update(order, feesPercentage)))
    }
  }

  def runMapAndTrade(assetMap: Map[Asset, BigDecimal], strategy: Strategy, poloApi: ExchangeApi, tradingsOps: TradingOps, baseAsset: Asset)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[Order]] = {
    val eventualPData = Future.sequence(assetMap.keys.toList.map(asset => PriceService.getPriceDataWithoutCache(asset).map(asset -> _)))
    eventualPData.map(_.toMap).flatMap { pDataMap =>
      Portfolio.fromApi(poloApi, pDataMap).map { portfolio =>
        assetMap.keys.toList.flatMap { asset =>
          strategy.runStrategy(CurrencyPair(baseAsset, asset), ZonedDateTime.now(), pDataMap(asset), portfolio, assetMap(asset)).map(order => {
            Order.process(order, tradingsOps)
            //In order to make the available cash relevent, we update the portfolio with the sell order if any
            portfolio.updateBuyOnly(order, estimatedMarketFee)
          })
        }
      }
    }
  }

  private def runMultipleAndReport(assetWeight: Map[Asset, BigDecimal], priceMap: Map[Asset, PairPrices],
    strategy: Strategy, initialUsdAmount: BigDecimal, feesPercentage: BigDecimal, startDate: ZonedDateTime,
    endDate: ZonedDateTime = ZonedDateTime.now())(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): RunMultipleReport = {
    val pdList = TradeBotService.runMapT(assetWeight, priceMap, startDate, Usd, initialUsdAmount, feesPercentage, strategy, endDate)
    val portfolio = Portfolio.create(priceMap)
    portfolio.assets(Asset.Usd) = Quantity(initialUsdAmount)
    pdList.foreach { tuple => portfolio.update(tuple, BigDecimal(0.1)) }
    RunMultipleReport(portfolio.balance(Asset.Usd, ZonedDateTime.now()), strategy)

  }

  def bestRun(assetWeight: Map[Asset, BigDecimal], initialUsdAmount: BigDecimal,
    feesPercentage: BigDecimal, startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[RunMultipleReport] = {
    logger.info("Search started")
    val eventualPData = Future.sequence(assetWeight.keys.toList.map(asset => PriceService.getPriceData(asset).map(asset -> _)))
    eventualPData.map { priceMap =>
      val reportList = RuleGenerator.getAll(2).combinations(2).toList.flatMap { buyList =>
        RuleGenerator.getAll(2).combinations(2).toList.par.map { sellList =>
          val strategy = GeneratedStrategy(buyList, sellList)
          runMultipleAndReport(assetWeight, priceMap.toMap, strategy, initialUsdAmount, feesPercentage, startDate, endDate)
        }
      }
      logger.info("Search ended")
      reportList.maxBy(report => report.finalBalance)
    }
  }.recover {
    case NonFatal(t) =>
      logger.error("Error occured {}", t.getMessage)
      throw t
  }

}

object TradeBotCachedService extends StrictLogging {

  implicit val guavaCache: Cache[List[Order]] = GuavaCache[List[Order]]

  private val startDate = ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))

  def run(asset: Asset)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[Order]] = {
    import scalacache._

    import scalacache.modes.scalaFuture._

    import scala.concurrent.duration._

    cachingF(asset)(ttl = Some(1 hours)) {

      TradeBotService.runMap(Map(asset -> BigDecimal(1.0)), startDate, 1000, 0.2, SafeStrategy)

    }
  }

}
