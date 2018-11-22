package com.kobr4.tradebot.model

import play.api.libs.json.{ JsPath, JsString, Reads, Writes }

sealed trait Asset {

  val code: String

  override def toString: String = { code }
}

object Asset {

  case object Eth extends Asset { val code = "ETH" }

  case object Ltc extends Asset { val code = "LTC" }

  case object Btc extends Asset { val code = "BTC" }

  case object Xmr extends Asset { val code = "XMR" }

  case object Doge extends Asset { val code = "DOGE" }

  case object Xrp extends Asset { val code = "XRP" }

  case object Xem extends Asset { val code = "XEM" }

  case object Xlm extends Asset { val code = "STR" }

  case object Usd extends Asset { val code = "USD" }

  case object Dgb extends Asset { val code = "DGB" }

  case object Ada extends Asset { val code = "ADA" }

  case object Bch extends Asset { val code = "BCH" }

  case object Dash extends Asset { val code = "DASH" }

  case object Zec extends Asset { val code = "ZEC" }

  case object Maid extends Asset { val code = "MAID" }

  case object Tether extends Asset { val code = "USDT" }

  case class Custom(code: String) extends Asset

  def fromStringExact(s: String): Option[Asset] = s match {
    case "ETH" => Some(Asset.Eth)
    case "BTC" => Some(Asset.Btc)
    case "XBT" => Some(Asset.Btc)
    case "XMR" => Some(Asset.Xmr)
    case "XRP" => Some(Asset.Xrp)
    case "XEM" => Some(Asset.Xem)
    case "DOGE" => Some(Asset.Doge)
    case "DGB" => Some(Asset.Dgb)
    case "XLM" => Some(Asset.Xlm)
    case "USD" => Some(Asset.Usd)
    case "USDT" => Some(Asset.Tether)
    case "ZUSD" => Some(Asset.Usd)
    case "ADA" => Some(Asset.Ada)
    case "STR" => Some(Asset.Xlm)
    case "LTC" => Some(Asset.Ltc)
    case "BCH" => Some(Asset.Bch)
    case "DASH" => Some(Asset.Dash)
    case "ZEC" => Some(Asset.Zec)
    case "MAID" => Some(Asset.Maid)
    case "TETHER" => Some(Asset.Tether)
    case xs if xs != "XUSD" && xs.startsWith("X") && xs.length == 4 => fromStringExact(xs.substring(1))
    case other => None
  }

  def fromString(s: String): Asset = fromStringExact(s).getOrElse(Custom(s))

  implicit val assetWrites: Writes[Asset] = { a: Asset => JsString(a.toString) }

  implicit val assetReads: Reads[Asset] = JsPath.read[String].map(fromString)
}