package com.kobr4.tradebot.engine

import java.time.ZonedDateTime

import com.kobr4.tradebot.api.CurrencyPair
import com.kobr4.tradebot.model._

object AlternativeStrategy extends Strategy {

  import com.kobr4.tradebot.engine.Rule.Condition._

  /* buy if abpve 30 days moving average */
  def buyStrategy(pair: CurrencyPair, portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices, weight: BigDecimal = BigDecimal(1)): Option[Buy] = {

    val assetPrice = priceData.currentPrice(current)
    implicit val port = portfolio

    val quantity = getQuantity(current, weight, pair, assetPrice)

    MaybeBuy(pair, quantity, assetPrice, current)
      .whenCashAvailable(pair.left)
      .whenAboveMovingAverage(current, assetPrice, priceData, 20)
      .whenAboveMovingAverage(current, assetPrice, priceData, 10)
  }

  /* sell if below moving average and if 20% gain or 10% loss */
  def sellStrategy(pair: CurrencyPair, portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices): Option[Sell] = {
    val currentPrice = priceData.currentPrice(current)
    implicit val port = portfolio

    val maybeSellAll = Option(Sell(pair, currentPrice, portfolio.assets(pair.right).quantity, current))

    maybeSellAll
      .when(portfolio.assets(pair.right).quantity > 0)
      //.whenStops(pair.right, 20, -20, currentPrice)
      .whenBelowMovingAverage(current, currentPrice, priceData, 20)
    //.or(in => in.whenBelowMovingAverge(current, currentPrice, priceData, 10).orElse(in.whenStops(pair.right, 20, -10, currentPrice)))
  }

  def runStrategy(pair: CurrencyPair, current: ZonedDateTime, priceData: PairPrices, portfolio: Portfolio, weight: BigDecimal = BigDecimal(1)): Option[Order] = {
    buyStrategy(pair, portfolio, current, priceData, weight).orElse(
      sellStrategy(pair, portfolio, current, priceData))
  }
}
