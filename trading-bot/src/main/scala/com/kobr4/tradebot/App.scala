package com.kobr4.tradebot

import java.time._
import java.time.format.DateTimeFormatter

import Asset.Usd

import scala.collection.mutable

sealed trait PairPrice {
  val date: ZonedDateTime
  val price: BigDecimal
}

object PairPrice {
  def movingAverage(base: ZonedDateTime, days: Int, prices: List[PairPrice]): Option[BigDecimal] = {
    val minDate = base.minusDays(days)
    val priceSubset = prices.filter(price => minDate.isBefore(price.date))
    priceSubset.length match {
      case 0 => None
      case len => Some(priceSubset.map(_.price).sum / len)
    }
  }

  def currentPrice(current: ZonedDateTime, prices: List[PairPrice]): BigDecimal = {
    prices.reduce((p1, p2) =>
      if (Math.abs(p1.date.toEpochSecond - current.toEpochSecond) < Math.abs(p2.date.toEpochSecond - current.toEpochSecond))
        p1
      else p2
    ).price
  }
}

case class EthUsd(date: ZonedDateTime, price: BigDecimal) extends PairPrice


sealed trait Asset

object Asset {

  case object Eth extends Asset

  case object Btc extends Asset

  case object Usd extends Asset

}

sealed trait Order

case class Buy(asset: Asset, price: BigDecimal, quantity: BigDecimal) extends Order

case class Sell(asset: Asset, price: BigDecimal, quantity: BigDecimal) extends Order

case class Quantity(quantity: BigDecimal)

case class Portfolio(assets: mutable.HashMap[Asset, Quantity], orderList: mutable.ListBuffer[Order]) {

  def update(order: Order): Unit = {
    order match {
      case Buy(asset, price, quantity) =>
        assets(asset) = Quantity(assets(asset).quantity + quantity)
        assets(Usd) = Quantity(assets(Usd).quantity - quantity * price)
      case Sell(asset, price, quantity) =>
        assets(asset) = Quantity(assets(asset).quantity - quantity)
        assets(Usd) = Quantity(assets(Usd).quantity + quantity * price)
    }
    orderList.append(order)
  }

  def balance(priceData: List[PairPrice]): BigDecimal = {
    assets.keySet.map {
      case k@Asset.Eth => assets(k).quantity * priceData.last.price
      case k => assets(k).quantity
    }.sum
  }
}

object Portfolio {
  def create = Portfolio(
    mutable.HashMap(Asset.Eth -> Quantity(0), Asset.Usd -> Quantity(0), Asset.Btc -> Quantity(0)),
    mutable.ListBuffer.empty[Order]
  )
}


object Strategy {
  /* buy if below 30 days moving average */
  def buyStrategy(portfolio: Portfolio, current: ZonedDateTime, priceData: List[PairPrice]): Option[Buy] = {
    if (portfolio.assets(Asset.Usd).quantity > 100) {
      val ethPrice = PairPrice.currentPrice(current, priceData)
      PairPrice.movingAverage(current, 60, priceData).filter(ethPrice < _).map(_ => Buy(Asset.Eth,ethPrice, portfolio.assets(Asset.Usd).quantity / ethPrice))
    } else None
  }

  /* sell if 20% gain or 10% loss */
  def sellStrategy(portfolio: Portfolio, current: ZonedDateTime, priceData: List[PairPrice]): Option[Sell] = {
    val currentPrice = PairPrice.currentPrice(current, priceData)
    if (portfolio.assets(Asset.Eth).quantity > 0) {
      portfolio.orderList.flatMap {
        case o@Buy(Asset.Eth, _, _) => Some(o)
        case _ => None
      }.lastOption.filter(buy => buy.price + buy.price * 20 / 100 < currentPrice || buy.price - buy.price * 10 / 100 > currentPrice).map { _=>
        Sell(Asset.Eth,currentPrice, portfolio.assets(Asset.Eth).quantity)
      }
    } else None
  }


  val portfolio = Portfolio.create
  portfolio.assets(Usd) = Quantity(BigDecimal(10000L))

  def runStrategy(current: ZonedDateTime, priceData: List[PairPrice]): Unit = {
    buyStrategy(portfolio, current, priceData).foreach{ order =>
      println(s"[${current.getDayOfMonth}/${current.getMonthValue}/${current.getYear}] BUY: ${order.asset.toString.toUpperCase()} ${order.quantity} @ ${order.price}")
      portfolio.update(order)
    }
    sellStrategy(portfolio, current, priceData).foreach { order =>
      println(s"[${current.getDayOfMonth}/${current.getMonthValue}/${current.getYear}] SELL: ${order.asset.toString.toUpperCase()} ${order.quantity} @ ${order.price}")
      portfolio.update(order)
    }
  }
}


object App {
  val ethPricesUrl = "https://coinmetrics.io/data/eth.csv"
  val btcPrices = "https://coinmetrics.io/data/btc.csv"

  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"))
  def main(args: Array[String]): Unit = {
    val bufferedSource = if (args.length > 0) io.Source.fromFile(args(0)) else io.Source.fromURL(ethPricesUrl)
    val prices =
      for (line <- bufferedSource.getLines.toList.tail) yield {
        val splitted = line.split(',')
        val date = LocalDate.parse(splitted(0), formatter)
        val time = LocalTime.MIDNIGHT
        EthUsd(ZonedDateTime.of(date, time, ZoneId.of("UTC")), BigDecimal(splitted(4)))
      }


    val date = ZonedDateTime.parse("2017-10-01T01:00:00.000Z")
    prices.filter(_.date.isAfter(date)).foreach { p =>
      Strategy.runStrategy(p.date, prices)
    }

    val holdBalance = 10000 / PairPrice.currentPrice(date, prices) * prices.last.price
    println(s"balance: ${Strategy.portfolio.balance(prices)} hold: $holdBalance")

  }
}