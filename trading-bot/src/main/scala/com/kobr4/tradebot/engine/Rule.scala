package com.kobr4.tradebot.engine

import java.time.ZonedDateTime

import com.kobr4.tradebot.model._

object Rule {

  trait Condition[T] {
    def whenCashAvailable(asset: Asset)(implicit portfolio: Portfolio): T

    def whenHigh(current: ZonedDateTime, currentPrice: BigDecimal, priceData: PairPrices, days: Int): T

    def whenLow(current: ZonedDateTime, currentPrice: BigDecimal, priceData: PairPrices, days: Int): T

    def whenBelowMovingAverage(current: ZonedDateTime, currentPrice: BigDecimal, priceData: PairPrices, days: Int): T

    def whenAboveMovingAverage(current: ZonedDateTime, currentPrice: BigDecimal, priceData: PairPrices, days: Int): T

    def whenBelowWeightedMovingAverage(current: ZonedDateTime, currentPrice: BigDecimal, priceData: PairPrices, days: Int): T

    def whenAboveWeightedMovingAverage(current: ZonedDateTime, currentPrice: BigDecimal, priceData: PairPrices, days: Int): T

    def when(v: Boolean): T

    def whenLastBuyingPrice(asset: Asset, f: (BigDecimal) => Boolean)(implicit portfolio: Portfolio): T

    def or(f: T => T): T

    //def whenStops(asset: Asset, highPercent: Int, lowPercent: Int, currentPrice: BigDecimal)(implicit portfolio: Portfolio): Option[T]
  }

  object Condition {

    implicit class ConditionOrder[T <: Order](input: Option[T]) extends Condition[Option[T]] {

      override def whenCashAvailable(asset: Asset)(implicit portfolio: Portfolio): Option[T] = input.filter(_ => portfolio.assets(asset).quantity > 0)

      override def whenHigh(current: ZonedDateTime, currentPrice: BigDecimal, priceData: PairPrices, days: Int): Option[T] = {
        if (priceData.prices.filter(p => p.date.isAfter(current.minusDays(days)) && p.date.isBefore(current)).map(_.price).max <= currentPrice) input else None
      }

      override def whenLow(current: ZonedDateTime, currentPrice: BigDecimal, priceData: PairPrices, days: Int): Option[T] = {
        if (priceData.prices.filter(p => p.date.isAfter(current.minusDays(days)) && p.date.isBefore(current)).map(_.price).min >= currentPrice) input else None
      }

      override def whenBelowMovingAverage(current: ZonedDateTime, currentPrice: BigDecimal, priceData: PairPrices, days: Int): Option[T] = priceData.movingAverage(current, days).filter(_ > currentPrice).flatMap(_ => input)

      override def whenBelowWeightedMovingAverage(current: ZonedDateTime, currentPrice: BigDecimal, priceData: PairPrices, days: Int): Option[T] = priceData.weightedMovingAverage(current, days).filter(_ > currentPrice).flatMap(_ => input)

      override def whenAboveMovingAverage(current: ZonedDateTime, currentPrice: BigDecimal, priceData: PairPrices, days: Int): Option[T] = priceData.movingAverage(current, days).filter(_ < currentPrice).flatMap(_ => input)

      override def whenAboveWeightedMovingAverage(current: ZonedDateTime, currentPrice: BigDecimal, priceData: PairPrices, days: Int): Option[T] = priceData.weightedMovingAverage(current, days).filter(_ < currentPrice).flatMap(_ => input)

      override def when(v: Boolean): Option[T] = input.filter(_ => v)

      override def whenLastBuyingPrice(asset: Asset, f: (BigDecimal) => Boolean)(implicit portfolio: Portfolio): Option[T] =
        portfolio.orderList.flatMap {
          case o @ Buy(pair, _, _, _) if pair.right == asset => Some(o)
          case _ => None
        }.lastOption.filter(buy => f(buy.price)).flatMap(_ => input)

      def whenStops(asset: Asset, highPercent: Int, lowPercent: Int, currentPrice: BigDecimal)(implicit portfolio: Portfolio): Option[T] = whenLastBuyingPrice(asset, (buyPrice) => {
        buyPrice + buyPrice * highPercent / 100 < currentPrice || buyPrice + buyPrice * lowPercent / 100 > currentPrice
      })

      def or(f: Option[T] => Option[T]): Option[T] = input.flatMap(in => f(Option(in)))

    }

  }

}

