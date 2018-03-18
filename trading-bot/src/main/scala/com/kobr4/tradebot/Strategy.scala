package com.kobr4.tradebot

import java.time.ZonedDateTime

import com.kobr4.tradebot.Asset.Usd

object Rule {

  trait Condition[T] {
    def portfolioCash100(implicit portfolio: Portfolio): T

    def belowMovingAverge(current: ZonedDateTime, currentPrice: BigDecimal, priceData: PairPrices): T

    def condition(v: Boolean): T

    def buyPriceCondition(asset: Asset, f: (BigDecimal) => Boolean)(implicit portfolio: Portfolio): T
  }

  object Condition {

    implicit class ConditionOrder[T <: Order](input: Option[T]) extends Condition[Option[T]] {

      override def portfolioCash100(implicit portfolio: Portfolio): Option[T] = input.filter(_ => portfolio.assets(Asset.Usd).quantity > 100)

      override def belowMovingAverge(current: ZonedDateTime, currentPrice: BigDecimal, priceData: PairPrices): Option[T] = priceData.movingAverage(current, 60).filter(_ > currentPrice).flatMap(_ => input)

      override def condition(v: Boolean): Option[T] = input.filter(_ => v)

      override def buyPriceCondition(asset: Asset, f: (BigDecimal) => Boolean)(implicit portfolio: Portfolio): Option[T] =
        portfolio.orderList.flatMap {
          case o@Buy(ass, _, _) if ass == asset => Some(o)
          case _ => None
        }.lastOption.filter( buy  => f(buy.price)).flatMap(_ => input)
    }

  }

}


object Strategy {

  import Rule.Condition._

  /* buy if below 30 days moving average */
  def buyStrategy(portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices): Option[Buy] = {

    val ethPrice = priceData.currentPrice(current)
    implicit val port = portfolio


    Option(Buy(Asset.Eth, ethPrice, portfolio.assets(Asset.Usd).quantity / ethPrice))
      .portfolioCash100
      .belowMovingAverge(current, ethPrice, priceData)
  }

  /* sell if 20% gain or 10% loss */
  def sellStrategy(portfolio: Portfolio, current: ZonedDateTime, priceData: PairPrices): Option[Sell] = {
    val currentPrice = priceData.currentPrice(current)
    implicit val port = portfolio
    Option(Sell(Asset.Eth, currentPrice, portfolio.assets(Asset.Eth).quantity))
      .condition(portfolio.assets(Asset.Eth).quantity > 0)
      .buyPriceCondition(Asset.Eth, (buyPrice) => { buyPrice + buyPrice * 20 / 100 < currentPrice || buyPrice - buyPrice * 10 / 100 > currentPrice} )
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