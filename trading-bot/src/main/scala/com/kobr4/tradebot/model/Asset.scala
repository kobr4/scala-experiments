package com.kobr4.tradebot.model

import play.api.libs.json.{ JsPath, JsString, Reads, Writes }

sealed trait Asset

object Asset {

  case object Eth extends Asset { override def toString: String = "ETH" }

  case object Btc extends Asset { override def toString: String = "BTC" }

  case object Xmr extends Asset { override def toString: String = "XMR" }

  case object Doge extends Asset { override def toString: String = "DOGE" }

  case object Xrp extends Asset { override def toString: String = "XRP" }

  case object Xem extends Asset { override def toString: String = "XEM" }

  case object Xlm extends Asset { override def toString: String = "STR" }

  case object Usd extends Asset { override def toString: String = "USDT" }

  case object Dgb extends Asset { override def toString: String = "DGB" }

  case object Ada extends Asset { override def toString: String = "ADA" }

  case class Custom(code: String) extends Asset { override def toString: String = code }

  def fromString(s: String): Option[Asset] = s match {
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
    case "USDT" => Some(Asset.Usd)
    case "ADA" => Some(Asset.Ada)
    case "STR" => Some(Asset.Xlm)
    case code => Some(Custom(code))
  }

  implicit val assetWrites: Writes[Asset] = { a: Asset => JsString(a.toString) }

  implicit val assetReads: Reads[Asset] = JsPath.read[String].map(fromString(_).getOrElse(throw new RuntimeException("Invalid asset")))
}