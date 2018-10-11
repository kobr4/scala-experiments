package com.kobr4.tradebot.engine

import java.time.ZonedDateTime

import com.kobr4.tradebot.model._
import play.api.libs.json.{ JsPath, Reads }

import scala.math.BigDecimal.RoundingMode

trait Strategy {

  val minOrderValue = BigDecimal(5)

  def runStrategy(asset: Asset, current: ZonedDateTime, priceData: PairPrices, portfolio: Portfolio, weight: BigDecimal = BigDecimal(1)): Option[Order]

  def getQuantity(current: ZonedDateTime, weight: BigDecimal, asset: Asset, assetPrice: BigDecimal)(implicit portfolio: Portfolio): BigDecimal = {
    (((portfolio.balance(current) * weight) - portfolio.balance(asset, current)).max(0).min(portfolio.assets(Asset.Usd).quantity) / assetPrice).setScale(4, RoundingMode.DOWN)
  }

  def MaybeBuy(asset: Asset, quantity: BigDecimal, assetPrice: BigDecimal, current: ZonedDateTime): Option[Buy] =
    if (quantity * assetPrice > minOrderValue) Option(Buy(asset, assetPrice, quantity, current)) else None
}

object Strategy {

  def fromString(strategy: String): Option[Strategy] = strategy match {
    case "safe" => Some(SafeStrategy)
    case "custom" => Some(AlternativeStrategy)
    case _ => None
  }

  implicit val strategyReads: Reads[Strategy] = JsPath.read[String].map(fromString(_).getOrElse(throw new RuntimeException("Invalid strategy")))
}

object SafeStrategy extends Strategy {

  import com.kobr4.tradebot.engine.Rule.Condition._

  /* buy if abpve 30 days moving average */
  def buyStrategy(asset: Asset, portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices, weight: BigDecimal = BigDecimal(1)): Option[Buy] = {

    val assetPrice = priceData.currentPrice(current)

    implicit val port = portfolio

    val quantity = getQuantity(current, weight, asset, assetPrice)

    // Sexy DSL ! <3
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
    /*
      .whenLastBuyingPrice(asset, (buyPrice) => {
        buyPrice + buyPrice * 20 / 100 < currentPrice || buyPrice - buyPrice * 10 / 100 > currentPrice
      })
*/

  }

  def runStrategy(asset: Asset, current: ZonedDateTime, priceData: PairPrices, portfolio: Portfolio, weight: BigDecimal = BigDecimal(1)): Option[Order] = {
    buyStrategy(asset, portfolio, current, priceData, weight).orElse(
      sellStrategy(asset, portfolio, current, priceData))
  }
}