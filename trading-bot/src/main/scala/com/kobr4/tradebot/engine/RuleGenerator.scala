package com.kobr4.tradebot.engine

import java.time.ZonedDateTime

import com.kobr4.tradebot.api.CurrencyPair
import com.kobr4.tradebot.engine.Rule.Condition._
import com.kobr4.tradebot.model._
import play.api.libs.json._

trait ConditionObject {
  def when[T <: Order](in: Option[T], asset: Asset, current: ZonedDateTime, assetPrice: BigDecimal, priceData: PairPrices)(implicit portfolio: Portfolio): Option[T]
}

object ConditionObject {

  import play.api.libs.functional.syntax._

  implicit val conditionObjectReads: Reads[ConditionObject] =
    (JsPath \ "method").read[String].flatMap {
      case "whenAboveMovingAverage" => (JsPath \ "days").read[Int].map(WhenAboveMovingAverage)
      case "whenBelowMovingAverage" => (JsPath \ "days").read[Int].map(WhenBelowMovingAverage)
      case "whenStops" => ((JsPath \ "high").read[Int] and (JsPath \ "low").read[Int]).tupled.map(t => WhenStops(t._1, t._2))
      case "whenHigh" => (JsPath \ "days").read[Int].map(WhenHigh)
      case "whenLow" => (JsPath \ "days").read[Int].map(WhenLow)
      case "whenNoOp" => Reads.pure(WhenNoOp())
      case "whenAbove" => (JsPath \ "threshold").read[BigDecimal].map(WhenAbove)
      case "whenBelow" => (JsPath \ "threshold").read[BigDecimal].map(WhenBelow)
    }

  implicit val conditionObjectWrites: Writes[ConditionObject] = {
    case WhenAboveMovingAverage(days: Int) =>
      (JsPath \ "method").write[String].writes("whenAboveMovingAverage") ++ (JsPath \ "days").write[Int].writes(days)
    case WhenBelowMovingAverage(days: Int) =>
      (JsPath \ "method").write[String].writes("whenBelowMovingAverage") ++ (JsPath \ "days").write[Int].writes(days)
    case WhenStops(high: Int, low: Int) =>
      (JsPath \ "method").write[String].writes("whenStops") ++ (JsPath \ "high").write[Int].writes(high) ++ (JsPath \ "low").write[Int].writes(low)
    case WhenHigh(days: Int) =>
      (JsPath \ "method").write[String].writes("whenHigh") ++ (JsPath \ "days").write[Int].writes(days)
    case WhenLow(days: Int) =>
      (JsPath \ "method").write[String].writes("whenLow") ++ (JsPath \ "days").write[Int].writes(days)
    case WhenNoOp() =>
      (JsPath \ "method").write[String].writes("whenNoOp")
    case WhenAbove(threshold: BigDecimal) =>
      (JsPath \ "method").write[String].writes("whenAbove") ++ (JsPath \ "threshold").write[BigDecimal].writes(threshold)
    case WhenBelow(threshold: BigDecimal) =>
      (JsPath \ "method").write[String].writes("whenBelow") ++ (JsPath \ "threshold").write[BigDecimal].writes(threshold)
  }
}

case class WhenAboveMovingAverage(days: Int) extends ConditionObject {

  def when[T <: Order](in: Option[T], asset: Asset, current: ZonedDateTime, assetPrice: BigDecimal, priceData: PairPrices)(implicit portfolio: Portfolio): Option[T] = {
    in.whenAboveMovingAverage(current, assetPrice, priceData, days)
  }
}

case class WhenBelowMovingAverage(days: Int) extends ConditionObject {

  def when[T <: Order](in: Option[T], asset: Asset, current: ZonedDateTime, assetPrice: BigDecimal, priceData: PairPrices)(implicit portfolio: Portfolio): Option[T] = {
    in.whenBelowMovingAverage(current, assetPrice, priceData, days)
  }
}

case class WhenStops(high: Int, low: Int) extends ConditionObject {

  def when[T <: Order](in: Option[T], asset: Asset, current: ZonedDateTime, assetPrice: BigDecimal, priceData: PairPrices)(implicit portfolio: Portfolio): Option[T] = {
    in.whenStops(asset, high, low, assetPrice)
  }
}

case class WhenHigh(days: Int) extends ConditionObject {

  def when[T <: Order](in: Option[T], asset: Asset, current: ZonedDateTime, assetPrice: BigDecimal, priceData: PairPrices)(implicit portfolio: Portfolio): Option[T] = {
    in.whenHigh(current, assetPrice, priceData, days)
  }
}

case class WhenLow(days: Int) extends ConditionObject {

  def when[T <: Order](in: Option[T], asset: Asset, current: ZonedDateTime, assetPrice: BigDecimal, priceData: PairPrices)(implicit portfolio: Portfolio): Option[T] = {
    in.whenLow(current, assetPrice, priceData, days)
  }
}

case class WhenNoOp() extends ConditionObject {

  def when[T <: Order](in: Option[T], asset: Asset, current: ZonedDateTime, assetPrice: BigDecimal, priceData: PairPrices)(implicit portfolio: Portfolio): Option[T] = {
    in
  }
}

case class WhenAbove(thresholdPrice: BigDecimal) extends ConditionObject {

  def when[T <: Order](in: Option[T], asset: Asset, current: ZonedDateTime, assetPrice: BigDecimal, priceData: PairPrices)(implicit portfolio: Portfolio): Option[T] = {
    in.whenAbove(assetPrice, thresholdPrice)
  }
}

case class WhenBelow(thresholdPrice: BigDecimal) extends ConditionObject {

  def when[T <: Order](in: Option[T], asset: Asset, current: ZonedDateTime, assetPrice: BigDecimal, priceData: PairPrices)(implicit portfolio: Portfolio): Option[T] = {
    in.whenBelow(assetPrice, thresholdPrice)
  }
}

object RuleGenerator {

  private def getNoop(n: Int) = (for (a <- 1 to n) yield WhenNoOp()).toList

  def getAllWhenHigh = List(WhenHigh(10), WhenHigh(20), WhenHigh(30))

  def getAllWhenLow = List(WhenLow(10), WhenLow(20), WhenLow(30))

  def getAllWhenStops = List(WhenStops(10, -10), WhenStops(20, -20), WhenStops(30, -30))

  def getAllWhenAboveMovingAverage = List(
    WhenAboveMovingAverage(10),
    WhenAboveMovingAverage(20),
    WhenAboveMovingAverage(30),
    WhenAboveMovingAverage(50))

  def getAllWhenBelowMovingAverage = List(
    WhenBelowMovingAverage(10),
    WhenBelowMovingAverage(20),
    WhenBelowMovingAverage(30),
    WhenBelowMovingAverage(50))

  def getAll(n: Int): List[ConditionObject] = getAllWhenAboveMovingAverage ::: getAllWhenBelowMovingAverage ::: getAllWhenStops ::: getAllWhenHigh ::: getAllWhenLow ::: getNoop(n)
}

case class GeneratedStrategy(buyList: List[ConditionObject], sellList: List[ConditionObject]) extends Strategy {

  def runOrderList[T <: Order](input: Option[T], buyList: List[ConditionObject], asset: Asset, current: ZonedDateTime, assetPrice: BigDecimal, priceData: PairPrices)(implicit portfolio: Portfolio): Option[T] = (input, buyList) match {
    case (None, _) => None
    case (other, Nil) => other
    case (other, cond :: tail) => runOrderList(cond.when(other, asset, current, assetPrice, priceData), tail, asset, current, assetPrice, priceData)
  }

  def buyStrategy(pair: CurrencyPair, portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices, weight: BigDecimal = BigDecimal(1)): Option[Buy] = {

    val assetPrice = priceData.currentPrice(current)
    implicit val port = portfolio

    val quantity = getQuantity(current, weight, pair, assetPrice)

    runOrderList(MaybeBuy(pair, quantity, assetPrice, current).whenCashAvailable(pair.left), buyList, pair.right, current, assetPrice, priceData)
  }

  def sellStrategy(pair: CurrencyPair, portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices): Option[Sell] = {
    val assetPrice = priceData.currentPrice(current)
    implicit val port = portfolio

    runOrderList(Option(Sell(pair, assetPrice, portfolio.assets(pair.right).quantity, current)).when(portfolio.assets(pair.right).quantity > 0), sellList, pair.right, current, assetPrice, priceData)
  }

  override def runStrategy(pair: CurrencyPair, current: ZonedDateTime, priceData: PairPrices, portfolio: Portfolio, weight: BigDecimal = BigDecimal(1)): Option[Order] = {
    buyStrategy(pair, portfolio, current, priceData, weight).orElse(
      sellStrategy(pair, portfolio, current, priceData))
  }
}

object GeneratedStrategy {

  implicit val generatedStrategyFormat: Format[GeneratedStrategy] = Json.format[GeneratedStrategy]
}

case class AggregatedStrategy(strategyList: List[GeneratedStrategy]) extends Strategy {

  def buyStrategy(pair: CurrencyPair, portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices, weight: BigDecimal = BigDecimal(1)): Option[Buy] = {
    strategyList.foldLeft(strategyList.head.buyStrategy(pair, portfolio, current, priceData, weight))((maybeBuy, strategy) =>
      maybeBuy.orElse(strategy.buyStrategy(pair, portfolio, current, priceData, weight)))
  }

  def sellStrategy(pair: CurrencyPair, portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices, weight: BigDecimal = BigDecimal(1)): Option[Sell] = {
    strategyList.foldLeft(strategyList.head.sellStrategy(pair, portfolio, current, priceData))((maybeSell, strategy) =>
      maybeSell.orElse(strategy.sellStrategy(pair, portfolio, current, priceData)))
  }

  override def runStrategy(pair: CurrencyPair, current: ZonedDateTime, priceData: PairPrices, portfolio: Portfolio, weight: BigDecimal = BigDecimal(1)): Option[Order] = {
    buyStrategy(pair, portfolio, current, priceData, weight).orElse(
      sellStrategy(pair, portfolio, current, priceData))
  }

}

object AggregatedStrategy {

  implicit val aggregatedStrategyFormat: Format[AggregatedStrategy] = Json.format[AggregatedStrategy]
}
