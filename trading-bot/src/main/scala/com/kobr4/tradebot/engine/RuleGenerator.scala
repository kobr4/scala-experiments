package com.kobr4.tradebot.engine
import java.time.ZonedDateTime

import com.kobr4.tradebot.api.CurrencyPair
import com.kobr4.tradebot.model._
import com.kobr4.tradebot.engine.Rule.Condition._
trait ConditionObject {
  def when[T <: Order](in: Option[T], current: ZonedDateTime, assetPrice: BigDecimal, priceData: PairPrices) : Option[T]
}


case class WhenAboveMovingAverage(days: Int) extends ConditionObject {

  def when[T <: Order](in: Option[T], current: ZonedDateTime, assetPrice: BigDecimal, priceData: PairPrices): Option[T] = {
    in.whenAboveMovingAverage(current, assetPrice, priceData, days)
  }
}

case class WhenBelowMovingAverage(days: Int) extends ConditionObject {

  def when[T <: Order](in: Option[T], current: ZonedDateTime, assetPrice: BigDecimal, priceData: PairPrices): Option[T] = {
    in.whenBelowMovingAverage(current, assetPrice, priceData, days)
  }
}

case class WhenNoOp() extends ConditionObject {

  def when[T <: Order](in: Option[T], current: ZonedDateTime, assetPrice: BigDecimal, priceData: PairPrices): Option[T] = {
    in
  }
}

object RuleGenerator {

  private def getNoop(n : Int) = (for(a <- 1 to n) yield WhenNoOp()).toList

  def getAllWhenAboveMovingAverage = List(WhenAboveMovingAverage(5),WhenAboveMovingAverage(10), WhenAboveMovingAverage(15),
    WhenAboveMovingAverage(20), WhenAboveMovingAverage(25), WhenAboveMovingAverage(30))

  def getAllWhenBelowMovingAverage = List(WhenBelowMovingAverage(5),WhenBelowMovingAverage(10), WhenBelowMovingAverage(15),
    WhenBelowMovingAverage(20), WhenBelowMovingAverage(25), WhenBelowMovingAverage(30))

  def getAll(n : Int): List[ConditionObject] = getAllWhenAboveMovingAverage ::: getAllWhenBelowMovingAverage ::: getNoop(n)
}


case class GeneratedStrategy(buyList : List[ConditionObject], sellList : List[ConditionObject]) extends Strategy {

  def runOrderList[T <: Order](input: Option[T], buyList : List[ConditionObject], current: ZonedDateTime, assetPrice: BigDecimal, priceData: PairPrices): Option[T] = (input, buyList) match {
    case (None,_) => None
    case (other, Nil) => other
    case (other,cond::tail) =>  runOrderList(cond.when(other, current, assetPrice, priceData), tail, current, assetPrice, priceData)
  }


  def buyStrategy(pair: CurrencyPair, portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices, weight: BigDecimal = BigDecimal(1)): Option[Buy] = {

    val assetPrice = priceData.currentPrice(current)
    implicit val port = portfolio

    val quantity = getQuantity(current, weight, pair, assetPrice)

    runOrderList(MaybeBuy(pair, quantity, assetPrice, current).whenCashAvailable(pair.left), buyList, current, assetPrice, priceData)
  }

  def sellStrategy(pair: CurrencyPair, portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices): Option[Sell] = {
    val assetPrice = priceData.currentPrice(current)
    implicit val port = portfolio

    runOrderList(Option(Sell(pair, assetPrice, portfolio.assets(pair.right).quantity, current)).when(portfolio.assets(pair.right).quantity > 0), sellList, current, assetPrice, priceData)
  }

  override def runStrategy(pair: CurrencyPair, current: ZonedDateTime, priceData: PairPrices, portfolio: Portfolio, weight: BigDecimal = BigDecimal(1)): Option[Order] = {
    buyStrategy(pair, portfolio, current, priceData, weight).orElse(
      sellStrategy(pair, portfolio, current, priceData))
  }
}
