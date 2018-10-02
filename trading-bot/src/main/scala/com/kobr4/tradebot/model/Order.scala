package com.kobr4.tradebot.model

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.Asset
import com.kobr4.tradebot.api.PoloApi
import play.api.libs.json.{ JsObject, Json, Writes }

import scala.concurrent.ExecutionContext

sealed trait Order

case class Buy(asset: Asset, price: BigDecimal, quantity: BigDecimal) extends Order

case class Sell(asset: Asset, price: BigDecimal, quantity: BigDecimal) extends Order

case class Quantity(quantity: BigDecimal)

object Order {

  private def getCurrencyPair(asset: Asset): String = {
    asset match {
      case Asset.Btc => "BTC_USD"
      case Asset.Eth => "ETH_USD"
    }
  }

  def process(order: Order)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Unit = {
    val api = new PoloApi()
    order match {
      case b: Buy => api.buy(getCurrencyPair(b.asset), b.price, b.quantity)
      case s: Sell => api.sell(getCurrencyPair(s.asset), s.price, s.quantity)
    }
  }

  implicit val buyWrites = new Writes[Buy] {
    def writes(buy: Buy): JsObject = Json.obj(
      "quantity" -> buy.quantity,
      "price" -> buy.price,
      "asset" -> buy.asset.toString,
      "type" -> "BUY")
  }

  implicit val sellWrites = new Writes[Sell] {
    def writes(sell: Sell): JsObject = Json.obj(
      "quantity" -> sell.quantity,
      "price" -> sell.price,
      "asset" -> sell.asset.toString,
      "type" -> "SELL")
  }

  implicit val orderWrites = new Writes[Order] {
    def writes(order: Order): JsObject = order match {
      case s: Sell => sellWrites.writes(s)
      case b: Buy => buyWrites.writes(b)
    }
  }
}
