package com.kobr4.tradebot.services

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{CurrencyPair, ExchangeApi, PoloApi}
import com.kobr4.tradebot.engine.Strategy
import com.kobr4.tradebot.model.Asset.Usd
import com.kobr4.tradebot.model._

import scala.concurrent.{ExecutionContext, Future}

object TradeBotService {

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

  def runMap(assetMap: Map[Asset, BigDecimal], startDate: ZonedDateTime, initialUsdAmount: BigDecimal, feesPercentage: BigDecimal, strategy: Strategy, endDate: ZonedDateTime = ZonedDateTime.now())(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[Order]] = {
    val eventualPData = Future.sequence(assetMap.keys.toList.map(asset => PriceService.getPriceData(asset).map(asset -> _)))
    eventualPData.map(_.toMap).map { pDataMap =>
      val portfolio = Portfolio.create(pDataMap)
      portfolio.assets(Usd) = Quantity(initialUsdAmount)
      pDataMap.keys.toList.flatMap(asset => pDataMap(asset).prices.filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)).map((asset, _))).sortBy(_._2.date.toEpochSecond).flatMap(assetData =>
        strategy.runStrategy(CurrencyPair(Asset.Usd, assetData._1), assetData._2.date, pDataMap(assetData._1), portfolio, assetMap(assetData._1)).map(order => portfolio.update(order, feesPercentage)))
    }
  }

  def runAndTrade(asset: Asset, priceData: PairPrices, strategy: Strategy, poloApi: ExchangeApi, tradingsOps: TradingOps)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[Option[Order]] = {
    Portfolio.fromApi(poloApi, Map(asset -> priceData)).map { portfolio =>
      strategy.runStrategy(CurrencyPair(Asset.Usd, asset), ZonedDateTime.now(), priceData, portfolio).map(order => Order.process(order, tradingsOps))
    }
  }

  def runMapAndTrade(assetMap: Map[Asset, BigDecimal], strategy: Strategy, poloApi: ExchangeApi, tradingsOps: TradingOps)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[Order]] = {
    val eventualPData = Future.sequence(assetMap.keys.toList.map(asset => PriceService.getPriceData(asset).map(asset -> _)))
    eventualPData.map(_.toMap).flatMap { pDataMap =>
      Portfolio.fromApi(poloApi, pDataMap).map { portfolio =>
        assetMap.keys.toList.flatMap { asset =>
          strategy.runStrategy(CurrencyPair(Asset.Usd, asset), ZonedDateTime.now(), pDataMap(asset), portfolio, assetMap(asset)).map(order => {
            Order.process(order, tradingsOps)
            //In order to make the available cash relevent, we update the portfolio with the sell order if any
            portfolio.updateBuyOnly(order, estimatedMarketFee)
          })
        }
      }
    }
  }

}
