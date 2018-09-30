package com.kobr4.tradebot

import java.time.ZonedDateTime

import com.kobr4.tradebot.Asset.Usd

object Strategy {

  import Rule.Condition._

  /* buy if below 30 days moving average */
  def buyStrategy(asset: Asset, portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices): Option[Buy] = {

    val ethPrice = priceData.currentPrice(current)
    implicit val port = portfolio

    val maybeBuy = Option(Buy(asset, ethPrice, portfolio.assets(Asset.Usd).quantity / ethPrice))

    // Sexy DSL ! <3
    maybeBuy
      .whenCashAvailable
      .whenAboveMovingAverge(current, ethPrice, priceData)
  }

  /* sell if 20% gain or 10% loss */
  def sellStrategy(asset: Asset, portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices): Option[Sell] = {
    val currentPrice = priceData.currentPrice(current)
    implicit val port = portfolio

    val maybeSellAll = Option(Sell(asset, currentPrice, portfolio.assets(asset).quantity))

    val maybeFirst = maybeSellAll
      .when(portfolio.assets(asset).quantity > 0)
      .whenBelowMovingAverge(current, currentPrice, priceData)
      .whenLastBuyingPrice(asset, (buyPrice) => {
        buyPrice + buyPrice * 20 / 100 < currentPrice || buyPrice - buyPrice * 10 / 100 > currentPrice
      })

    maybeFirst.orElse(
      maybeSellAll
        .when(portfolio.assets(asset).quantity > 0)
        .whenAboveMovingAverge(current, currentPrice, priceData)
        .whenLastBuyingPrice(asset, (buyPrice) => {
          buyPrice + buyPrice * 20 / 100 < currentPrice //|| buyPrice - buyPrice * 10 / 100 > currentPrice
        }))

    maybeFirst
  }

  def runStrategy(asset: Asset, current: ZonedDateTime, priceData: PairPrices, portfolio: Portfolio): Option[(ZonedDateTime, Order)] = {
    buyStrategy(asset, portfolio, current, priceData).map { order =>
      println(s"[${current.getDayOfMonth}/${current.getMonthValue}/${current.getYear}] BUY: ${order.asset.toString.toUpperCase()} ${order.quantity} @ ${order.price}")
      (current, order)
    }.orElse(
      sellStrategy(asset, portfolio, current, priceData).map { order =>
        println(s"[${current.getDayOfMonth}/${current.getMonthValue}/${current.getYear}] SELL: ${order.asset.toString.toUpperCase()} ${order.quantity} @ ${order.price}")
        (current, order)
      })
  }
}

object TradeBotService {

  def run(asset: Asset, initialUsdAmount: BigDecimal, priceData: PairPrices, feesPercentage: BigDecimal): List[(ZonedDateTime, Order)] = {
    val portfolio = Portfolio.create
    portfolio.assets(Usd) = Quantity(initialUsdAmount)
    priceData.prices.flatMap(p => Strategy.runStrategy(asset, p.date, priceData, portfolio).map(t => (t._1, portfolio.update(t._2, feesPercentage))))
  }

}