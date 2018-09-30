package com.kobr4.tradebot

import java.time._
import java.time.format.DateTimeFormatter

import play.api.libs.json.{ JsString, Writes }

sealed trait Asset

object Asset {

  case object Eth extends Asset { override def toString: String = "ETH" }

  case object Btc extends Asset { override def toString: String = "BTC" }

  case object Xmr extends Asset { override def toString: String = "XMR" }

  case object Usd extends Asset { override def toString: String = "USDT" }

  case class Custom(code: String) extends Asset { override def toString: String = code }

  def fromString(s: String): Option[Asset] = s match {
    case "ETH" => Some(Asset.Eth)
    case "BTC" => Some(Asset.Btc)
    case "XMR" => Some(Asset.Xmr)
    case "USD" => Some(Asset.Usd)
    case "USDT" => Some(Asset.Usd)
    case code => Some(Custom(code))
  }

  implicit val assetWrites: Writes[Asset] = { a: Asset => JsString(a.toString) }
}

object AppNoRun {
  val ethPricesUrl = "https://coinmetrics.io/data/eth.csv"
  val btcPrices = "https://coinmetrics.io/data/btc.csv"

  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"))
  def mainNoRun(args: Array[String]): Unit = {

    val date = ZonedDateTime.parse("2018-02-01T01:00:00.000Z")
    /*
    val priceData = PairPrice.fromUrl(ethPricesUrl)
    priceData.prices.filter(_.date.isAfter(date)).foreach { p =>
      Strategy.runStrategy(p.date, priceData)
    }

    val holdBalance = 10000 / priceData.currentPrice(date) * priceData.prices.last.price
    println(s"balance: ${Strategy.portfolio.balance(priceData)} hold: $holdBalance")
*/
  }
}