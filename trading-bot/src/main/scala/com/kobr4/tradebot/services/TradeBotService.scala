package com.kobr4.tradebot.services

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{PoloAPIInterface, PoloApi}
import com.kobr4.tradebot.engine.Strategy
import com.kobr4.tradebot.model.Asset.Usd
import com.kobr4.tradebot.model._

import scala.concurrent.{ExecutionContext, Future}

object TradeBotService {

  def run(asset: Asset, startDate: ZonedDateTime, initialUsdAmount: BigDecimal, priceData: PairPrices, feesPercentage: BigDecimal, strategy: Strategy, endDate: ZonedDateTime = ZonedDateTime.now()): List[(ZonedDateTime, Order)] = {
    val portfolio = Portfolio.create(Map(asset -> priceData))
    portfolio.assets(Usd) = Quantity(initialUsdAmount)
    priceData.prices.filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)).flatMap(p => strategy.runStrategy(asset, p.date, priceData, portfolio).map(t => (t._1, portfolio.update(t._2, feesPercentage))))
  }

  def runMap(assetMap: Map[Asset, BigDecimal], startDate: ZonedDateTime, initialUsdAmount: BigDecimal, feesPercentage: BigDecimal, strategy: Strategy, endDate: ZonedDateTime = ZonedDateTime.now())(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[(ZonedDateTime, Order)]] = {
    val eventualPData = Future.sequence(assetMap.keys.toList.map(asset => PriceService.getPriceData(asset).map(asset -> _)))
    eventualPData.map(_.toMap).map { pDataMap =>
      val portfolio = Portfolio.create(pDataMap)
      portfolio.assets(Usd) = Quantity(initialUsdAmount)
      pDataMap.keys.toList.flatMap(asset => pDataMap(asset).prices.filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)).map((asset, _))).sortBy(_._2.date.toEpochSecond).flatMap(assetData =>
        strategy.runStrategy(assetData._1, assetData._2.date, pDataMap(assetData._1), portfolio, assetMap(assetData._1)).map(t => (t._1, portfolio.update(t._2, feesPercentage))))
    }
  }

  def runAndTrade(asset: Asset, priceData: PairPrices, strategy: Strategy, poloApi: PoloAPIInterface, tradingsOps: TradingOps)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) = {

    Portfolio.fromApi(poloApi, Map(asset -> priceData)).map { portfolio =>
      strategy.runStrategy(asset, ZonedDateTime.now(), priceData, portfolio).foreach(t => Order.process(t._2, tradingsOps))
    }
  }

  def runMapAndTrade(assetMap: Map[Asset, BigDecimal], strategy: Strategy, poloApi: PoloAPIInterface, tradingsOps: TradingOps)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[Unit]] = {
    val eventualPData = Future.sequence(assetMap.keys.toList.map(asset => PriceService.getPriceData(asset).map(asset -> _)))
    eventualPData.map(_.toMap).flatMap { pDataMap =>
      Portfolio.fromApi(poloApi, pDataMap).map { portfolio =>
        assetMap.keys.toList.map { asset =>
          strategy.runStrategy(asset, ZonedDateTime.now(), pDataMap(asset), portfolio, assetMap(asset)).foreach(t => Order.process(t._2, tradingsOps))
        }
      }
    }
  }

}
