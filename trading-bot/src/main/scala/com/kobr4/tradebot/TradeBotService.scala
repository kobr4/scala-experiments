package com.kobr4.tradebot

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.Asset.Usd

import scala.concurrent.ExecutionContext


object TradeBotService {

  def run(asset: Asset, initialUsdAmount: BigDecimal, priceData: PairPrices, feesPercentage: BigDecimal): List[(ZonedDateTime, Order)] = {
    val portfolio = Portfolio.create(asset)
    portfolio.assets(Usd) = Quantity(initialUsdAmount)
    priceData.prices.flatMap(p => Strategy.runStrategy(asset, p.date, priceData, portfolio).map(t => (t._1, portfolio.update(t._2, feesPercentage))))
  }

  def doTrade(asset: Asset, priceData: PairPrices)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) = {
    val poloApi = new PoloApi()
    val portfolio = Portfolio.create(asset)

    poloApi.returnBalances.map(balances => {
      portfolio.assets(asset) = Quantity(balances(asset).quantity)
      portfolio.assets(Asset.Usd) = Quantity(balances(Asset.Usd).quantity)
    }).map { _ =>
      Strategy.runStrategy(asset, ZonedDateTime.now(), priceData, portfolio).foreach(t => Order.process(t._2))
    }
  }

}
