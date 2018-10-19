package com.kobr4.tradebot.model

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{ CurrencyPair, PoloApi }
import com.kobr4.tradebot.services.TradingOps
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.json.{ JsObject, Json, Writes }

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

sealed trait Order {
  val date: ZonedDateTime
}

case class Buy(pair: CurrencyPair, price: BigDecimal, quantity: BigDecimal, date: ZonedDateTime) extends Order

case class Sell(pair: CurrencyPair, price: BigDecimal, quantity: BigDecimal, date: ZonedDateTime) extends Order

case class Quantity(quantity: BigDecimal)

object Order extends StrictLogging {

  def process(order: Order, tradingOps: TradingOps)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Order = {

    val eventualResult = order match {
      case b: Buy =>
        tradingOps.buyAtMarketValue(b.price, b.pair, b.pair.right, Quantity(b.quantity))
      case s: Sell =>
        tradingOps.sellAtMarketValue(s.price, s.pair, Quantity(s.quantity))
    }

    eventualResult.onComplete {
      case Success(v) =>
        logger.info("Order {} successfully processed with response: {}", order, v)
      case Failure(f) =>
        logger.error("Order {} was not processed with error: {}", order, f.getMessage)
    }

    order
  }

  implicit val buyWrites = new Writes[Buy] {
    def writes(buy: Buy): JsObject = Json.obj(
      "quantity" -> buy.quantity,
      "price" -> buy.price,
      "asset" -> buy.pair.right.toString,
      "date" -> buy.date.toOffsetDateTime.toString(),
      "type" -> "BUY")
  }

  implicit val sellWrites = new Writes[Sell] {
    def writes(sell: Sell): JsObject = Json.obj(
      "quantity" -> sell.quantity,
      "price" -> sell.price,
      "asset" -> sell.pair.right.toString,
      "date" -> sell.date.toOffsetDateTime.toString(),
      "type" -> "SELL")
  }

  implicit val orderWrites = new Writes[Order] {
    def writes(order: Order): JsObject = order match {
      case s: Sell => sellWrites.writes(s)
      case b: Buy => buyWrites.writes(b)
    }
  }
}
