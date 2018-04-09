package com.kobr4.tradebot

import java.time._
import java.time.format.DateTimeFormatter

import Asset.Usd

import scala.collection.mutable

sealed trait Asset

object Asset {

  case object Eth extends Asset

  case object Btc extends Asset

  case object Usd extends Asset

  def fromString(s: String) = s match {
    case "ETH" => Some(Asset.Eth)
    case "BTC" => Some(Asset.Btc)
    case "USD" => Some(Asset.Usd)
    case "USDT" => Some(Asset.Usd)
    case _ => None
  }
}

object App {
  val ethPricesUrl = "https://coinmetrics.io/data/eth.csv"
  val btcPrices = "https://coinmetrics.io/data/btc.csv"

  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"))
  def main(args: Array[String]): Unit = {


    val date = ZonedDateTime.parse("2017-10-01T01:00:00.000Z")

    val priceData = PairPrice.fromUrl(ethPricesUrl)
    priceData.prices.filter(_.date.isAfter(date)).foreach { p =>
      Strategy.runStrategy(p.date, priceData)
    }

    val holdBalance = 10000 / priceData.currentPrice(date) * priceData.prices.last.price
    println(s"balance: ${Strategy.portfolio.balance(priceData)} hold: $holdBalance")

  }
}