package com.kobr4.tradebot.engine

import java.time.ZonedDateTime

import com.kobr4.tradebot.model._
import play.api.libs.json.{JsPath, Reads}

import scala.math.BigDecimal.RoundingMode

trait Strategy {

  def runStrategy(asset: Asset, current: ZonedDateTime, priceData: PairPrices, portfolio: Portfolio, weight: BigDecimal = BigDecimal(1)): Option[(ZonedDateTime, Order)]
}

object Strategy {

  def fromString(strategy: String): Option[Strategy] = strategy match {
    case "safe" => Some(SafeStrategy)
    case "custom" => Some(AggressiveStrategy)
    case _ => None
  }

  implicit val strategyReads: Reads[Strategy] = JsPath.read[String].map(fromString(_).getOrElse(throw new RuntimeException("Invalid strategy")))
}

object AggressiveStrategy extends Strategy {

  import com.kobr4.tradebot.engine.Rule.Condition._

  /* buy if abpve 30 days moving average */
  def buyStrategy(asset: Asset, portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices): Option[Buy] = {

    val assetPrice = priceData.currentPrice(current)
    implicit val port = portfolio

    val maybeBuy = Option(Buy(asset, assetPrice, portfolio.assets(Asset.Usd).quantity / assetPrice))

    // Sexy DSL ! <3
    maybeBuy
      .whenCashAvailable
  }

  /* sell if below moving average and if 20% gain or 10% loss */
  def sellStrategy(asset: Asset, portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices): Option[Sell] = {
    val currentPrice = priceData.currentPrice(current)
    implicit val port = portfolio

    val maybeSellAll = Option(Sell(asset, currentPrice, portfolio.assets(asset).quantity))

    maybeSellAll
      .when(portfolio.assets(asset).quantity > 0)
      .whenLastBuyingPrice(asset, (buyPrice) => {
        println("Buying price " + buyPrice)
        buyPrice + buyPrice * 20 / 100 < currentPrice || buyPrice - buyPrice * 10 / 100 > currentPrice
      })
  }

  def runStrategy(asset: Asset, current: ZonedDateTime, priceData: PairPrices, portfolio: Portfolio, weight: BigDecimal = BigDecimal(1)): Option[(ZonedDateTime, Order)] = {
    buyStrategy(asset, portfolio, current, priceData).map { order =>
      (current, order)
    }.orElse(
      sellStrategy(asset, portfolio, current, priceData).map { order =>
        (current, order)
      })
  }
}

object SafeStrategy extends Strategy {

  import com.kobr4.tradebot.engine.Rule.Condition._

  /* buy if abpve 30 days moving average */
  def buyStrategy(asset: Asset, portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices, weight: BigDecimal = BigDecimal(1)): Option[Buy] = {

    val assetPrice = priceData.currentPrice(current)
    implicit val port = portfolio

    val quantity = (((portfolio.balance(current) * weight) - portfolio.balance(asset, current)).max(0).min(portfolio.assets(Asset.Usd).quantity) / assetPrice).setScale(4, RoundingMode.DOWN)


    val maybeBuy = if (quantity > 0.1) Option(Buy(asset, assetPrice, quantity)) else None

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