package com.kobr4.tradebot.engine

import java.time.ZonedDateTime

import com.kobr4.tradebot.api.CurrencyPair
import com.kobr4.tradebot.model._
import play.api.libs.json.{ JsPath, Reads }

import scala.math.BigDecimal.RoundingMode

trait Strategy {

  val minOrderValue = BigDecimal(5)

  def runStrategy(pair: CurrencyPair, current: ZonedDateTime, priceData: PairPrices, portfolio: Portfolio, weight: BigDecimal = BigDecimal(1)): Option[Order]

  def getQuantity(current: ZonedDateTime, weight: BigDecimal, pair: CurrencyPair, assetPrice: BigDecimal)(implicit portfolio: Portfolio): BigDecimal = {
    (((portfolio.balance(pair.left, current) * weight) - portfolio.balanceForAsset(pair.right, current)).max(0).min(portfolio.assets(pair.left).quantity) / assetPrice).setScale(4, RoundingMode.DOWN)
  }

  def MaybeBuy(pair: CurrencyPair, quantity: BigDecimal, assetPrice: BigDecimal, current: ZonedDateTime): Option[Buy] =
    if (quantity * assetPrice > minOrderValue) Option(Buy(pair, assetPrice, quantity, current)) else None
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
  def buyStrategy(pair: CurrencyPair, portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices, weight: BigDecimal = BigDecimal(1)): Option[Buy] = {

    val assetPrice = priceData.currentPrice(current)

    implicit val port = portfolio

    val quantity = getQuantity(current, weight, pair, assetPrice)

    // Sexy DSL ! <3
    MaybeBuy(pair, quantity, assetPrice, current)
      .whenCashAvailable(pair.left)
      .whenAboveMovingAverage(current, assetPrice, priceData, 20)
  }

  /* sell if below moving average and if 20% gain or 10% loss */
  def sellStrategy(pair: CurrencyPair, portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices): Option[Sell] = {
    val currentPrice = priceData.currentPrice(current)
    implicit val port = portfolio

    val maybeSellAll = Option(Sell(pair, currentPrice, portfolio.assets(pair.right).quantity, current))

    maybeSellAll
      .when(portfolio.assets(pair.right).quantity > 0)
      .whenBelowMovingAverage(current, currentPrice, priceData, 20)
    /*
      .whenLastBuyingPrice(asset, (buyPrice) => {
        buyPrice + buyPrice * 20 / 100 < currentPrice || buyPrice - buyPrice * 10 / 100 > currentPrice
      })
*/

  }

  def runStrategy(pair: CurrencyPair, current: ZonedDateTime, priceData: PairPrices, portfolio: Portfolio, weight: BigDecimal = BigDecimal(1)): Option[Order] = {
    buyStrategy(pair, portfolio, current, priceData, weight).orElse(
      sellStrategy(pair, portfolio, current, priceData))
  }
}