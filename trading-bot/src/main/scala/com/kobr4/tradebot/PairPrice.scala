package com.kobr4.tradebot

import java.time.{LocalDate, LocalTime, ZoneId, ZonedDateTime}

import com.kobr4.tradebot.App.formatter

sealed trait PairPrice {
  val date: ZonedDateTime
  val price: BigDecimal
}

case class EthUsd(date: ZonedDateTime, price: BigDecimal) extends PairPrice

case class PairPrices(prices: List[PairPrice]) {

  def movingAverage(base: ZonedDateTime, days: Int): Option[BigDecimal] = {
    val minDate = base.minusDays(days)
    val priceSubset = prices.filter(price => minDate.isBefore(price.date))
    priceSubset.length match {
      case 0 => None
      case len => Some(priceSubset.map(_.price).sum / len)
    }
  }

  def currentPrice(current: ZonedDateTime): BigDecimal = {
    prices.reduce((p1, p2) =>
      if (Math.abs(p1.date.toEpochSecond - current.toEpochSecond) < Math.abs(p2.date.toEpochSecond - current.toEpochSecond))
        p1
      else p2
    ).price
  }
}

object PairPrice {
  def fromUrl(s: String): PairPrices = {
    val bufferedSource = io.Source.fromURL(s)
    val prices =
      for (line <- bufferedSource.getLines.toList.tail) yield {
        val splitted = line.split(',')
        val date = LocalDate.parse(splitted(0), formatter)
        val time = LocalTime.MIDNIGHT
        EthUsd(ZonedDateTime.of(date, time, ZoneId.of("UTC")), BigDecimal(splitted(4)))
      }
    PairPrices(prices)
  }
}