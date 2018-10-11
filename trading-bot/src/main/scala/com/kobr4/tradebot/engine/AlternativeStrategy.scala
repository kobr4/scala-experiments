package com.kobr4.tradebot.engine

import java.time.ZonedDateTime

import com.kobr4.tradebot.model._

import scala.math.BigDecimal.RoundingMode

object AlternativeStrategy extends Strategy {

  import com.kobr4.tradebot.engine.Rule.Condition._

  /* buy if abpve 30 days moving average */
  def buyStrategy(asset: Asset, portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices, weight: BigDecimal = BigDecimal(1)): Option[Buy] = {

    val assetPrice = priceData.currentPrice(current)
    implicit val port = portfolio

    val quantity = (((portfolio.balance(current) * weight) - portfolio.balance(asset, current)).max(0).min(portfolio.assets(Asset.Usd).quantity) / assetPrice).setScale(4, RoundingMode.DOWN)

    val maybeBuy = if (quantity * assetPrice > minOrderValue) Option(Buy(asset, assetPrice, quantity)) else None

    // Sexy DSL ! <3
    maybeBuy
      .whenCashAvailable
      .whenAboveMovingAverge(current, assetPrice, priceData)
  }

  /* sell if below moving average and if 20% gain or 10% loss */
  def sellStrategy(asset: Asset, portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices): Option[Sell] = {
    val currentPrice = priceData.currentPrice(current)
    implicit val port = portfolio

    val maybeSellAll = Option(Sell(asset, currentPrice, portfolio.assets(asset).quantity))

    maybeSellAll
      .when(portfolio.assets(asset).quantity > 0)
      .whenBelowMovingAverge(current, currentPrice, priceData)
      .whenLastBuyingPrice(asset, (buyPrice) => {
        buyPrice + buyPrice * 20 / 100 < currentPrice || buyPrice - buyPrice * 10 / 100 > currentPrice
      })
  }

  def runStrategy(asset: Asset, current: ZonedDateTime, priceData: PairPrices, portfolio: Portfolio, weight: BigDecimal = BigDecimal(1)): Option[(ZonedDateTime, Order)] = {
    buyStrategy(asset, portfolio, current, priceData, weight).map { order =>
      (current, order)
    }.orElse(
      sellStrategy(asset, portfolio, current, priceData).map { order =>
        (current, order)
      })
  }
}
