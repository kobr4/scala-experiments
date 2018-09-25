package com.kobr4.tradebot

import java.time._
import java.time.format.DateTimeFormatter

import Asset.Usd
import com.fasterxml.jackson.annotation.JsonValue
import play.api.libs.json.{ Format, JsString, Json, Writes }

import scala.collection.mutable
import scala.util.parsing.json.JSONObject

sealed trait Asset

object Asset {

  case object Eth extends Asset { override def toString: String = "ETH" }

  case object Btc extends Asset { override def toString: String = "BTC" }

  case object Usd extends Asset { override def toString: String = "USDT" }

  def fromString(s: String) = s match {
    case "ETH" => Some(Asset.Eth)
    case "BTC" => Some(Asset.Btc)
    case "USD" => Some(Asset.Usd)
    case "USDT" => Some(Asset.Usd)
    case _ => None
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