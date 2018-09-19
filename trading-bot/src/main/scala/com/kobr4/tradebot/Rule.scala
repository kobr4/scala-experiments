package com.kobr4.tradebot

import java.time.ZonedDateTime

object Rule {

  trait Condition[T] {
    def whenCashAvailable(implicit portfolio: Portfolio): T

    def whenBelowMovingAverge(current: ZonedDateTime, currentPrice: BigDecimal, priceData: PairPrices): T

    def whenAboveMovingAverge(current: ZonedDateTime, currentPrice: BigDecimal, priceData: PairPrices): T

    def when(v: Boolean): T

    def whenLastBuyingPrice(asset: Asset, f: (BigDecimal) => Boolean)(implicit portfolio: Portfolio): T
  }

  object Condition {

    implicit class ConditionOrder[T <: Order](input: Option[T]) extends Condition[Option[T]] {

      override def whenCashAvailable(implicit portfolio: Portfolio): Option[T] = input.filter(_ => portfolio.assets(Asset.Usd).quantity > 100)

      override def whenBelowMovingAverge(current: ZonedDateTime, currentPrice: BigDecimal, priceData: PairPrices): Option[T] = priceData.movingAverage(current, 30).filter(_ > currentPrice).flatMap(_ => input)

      override def whenAboveMovingAverge(current: ZonedDateTime, currentPrice: BigDecimal, priceData: PairPrices): Option[T] = priceData.movingAverage(current, 30).filter(_ < currentPrice).flatMap(_ => input)

      override def when(v: Boolean): Option[T] = input.filter(_ => v)

      override def whenLastBuyingPrice(asset: Asset, f: (BigDecimal) => Boolean)(implicit portfolio: Portfolio): Option[T] =
        portfolio.orderList.flatMap {
          case o @ Buy(ass, _, _) if ass == asset => Some(o)
          case _ => None
        }.lastOption.filter(buy => f(buy.price)).flatMap(_ => input)
    }

  }

}

