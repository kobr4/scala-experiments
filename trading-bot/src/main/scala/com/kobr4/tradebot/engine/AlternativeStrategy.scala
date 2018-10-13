package com.kobr4.tradebot.engine

import java.time.ZonedDateTime

import com.kobr4.tradebot.model._

object AlternativeStrategy extends Strategy {

  import com.kobr4.tradebot.engine.Rule.Condition._

  /* buy if abpve 30 days moving average */
  def buyStrategy(asset: Asset, portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices, weight: BigDecimal = BigDecimal(1)): Option[Buy] = {

    val assetPrice = priceData.currentPrice(current)
    implicit val port = portfolio

    val quantity = getQuantity(current, weight, asset, assetPrice)

    MaybeBuy(asset, quantity, assetPrice, current)
      .whenCashAvailable
      .whenAboveMovingAverge(current, assetPrice, priceData)
  }

  /* sell if below moving average and if 20% gain or 10% loss */
  def sellStrategy(asset: Asset, portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices): Option[Sell] = {
    val currentPrice = priceData.currentPrice(current)
    implicit val port = portfolio

    val maybeSellAll = Option(Sell(asset, currentPrice, portfolio.assets(asset).quantity, current))

    maybeSellAll
      .when(portfolio.assets(asset).quantity > 0)
      .whenBelowMovingAverge(current, currentPrice, priceData)
      .whenLastBuyingPrice(asset, (buyPrice) => {
        buyPrice + buyPrice * 20 / 100 < currentPrice || buyPrice - buyPrice * 10 / 100 > currentPrice
      })
  }

  def runStrategy(asset: Asset, current: ZonedDateTime, priceData: PairPrices, portfolio: Portfolio, weight: BigDecimal = BigDecimal(1)): Option[Order] = {
    buyStrategy(asset, portfolio, current, priceData, weight).orElse(
      sellStrategy(asset, portfolio, current, priceData))
  }
}
