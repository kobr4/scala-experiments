package com.kobr4.tradebot

import java.time.ZonedDateTime

import com.kobr4.tradebot.Asset.Usd

object Strategy {

  import Rule.Condition._

  /* buy if below 30 days moving average */
  def buyStrategy(portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices): Option[Buy] = {

    val ethPrice = priceData.currentPrice(current)
    implicit val port = portfolio

    val maybeBuy = Option(Buy(Asset.Eth, ethPrice, portfolio.assets(Asset.Usd).quantity / ethPrice))

    // Sexy DSL ! <3
    maybeBuy
      .whenCashAvailable
      .whenBelowMovingAverge(current, ethPrice, priceData)
  }

  /* sell if 20% gain or 10% loss */
  def sellStrategy(portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices): Option[Sell] = {
    val currentPrice = priceData.currentPrice(current)
    implicit val port = portfolio

    val maybeSellAll = Option(Sell(Asset.Eth, currentPrice, portfolio.assets(Asset.Eth).quantity))

    val maybeFirst = maybeSellAll
      .when(portfolio.assets(Asset.Eth).quantity > 0)
      .whenAboveMovingAverge(current, currentPrice, priceData)
      .whenLastBuyingPrice(Asset.Eth, (buyPrice) => { buyPrice + buyPrice * 20 / 100 < currentPrice || buyPrice - buyPrice * 10 / 100 > currentPrice })

    maybeFirst.orElse(
      maybeSellAll
        .whenBelowMovingAverge(current, currentPrice, priceData)
        .whenLastBuyingPrice(Asset.Eth, (buyPrice) => { buyPrice + buyPrice * 20 / 100 < currentPrice || buyPrice - buyPrice * 10 / 100 > currentPrice }))
  }

  val portfolio = Portfolio.create
  portfolio.assets(Usd) = Quantity(BigDecimal(10000L))

  def runStrategy(current: ZonedDateTime, priceData: PairPrices): Unit = {
    buyStrategy(portfolio, current, priceData).foreach { order =>
      println(s"[${current.getDayOfMonth}/${current.getMonthValue}/${current.getYear}] BUY: ${order.asset.toString.toUpperCase()} ${order.quantity} @ ${order.price}")
      portfolio.update(order)
    }
    sellStrategy(portfolio, current, priceData).foreach { order =>
      println(s"[${current.getDayOfMonth}/${current.getMonthValue}/${current.getYear}] SELL: ${order.asset.toString.toUpperCase()} ${order.quantity} @ ${order.price}")
      portfolio.update(order)
    }
  }
}