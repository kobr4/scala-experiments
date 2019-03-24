package com.kobr4.tradebot.model

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.CurrencyPair
import com.kobr4.tradebot.services.TradingOps
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.json.{ JsObject, Json, Writes }
import scalatags.Text
import scalatags.Text.all._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

sealed trait Order {
  val date: ZonedDateTime
  val pair: CurrencyPair
  val typeStr: String
  val price: BigDecimal
}

case class Buy(pair: CurrencyPair, price: BigDecimal, quantity: BigDecimal, date: ZonedDateTime) extends Order {
  val typeStr = "BUY"
  def toHtml: Text.TypedTag[String] = {
    table(
      tr(th("Type"), th("Currency Pair"), th("Price"), th("Quantity"), th("Date")),
      tr(td("BUY"), td(pair.toString), td(price.toString()), td(quantity.toString())))
  }
}

case class Sell(pair: CurrencyPair, price: BigDecimal, quantity: BigDecimal, date: ZonedDateTime) extends Order {
  val typeStr = "SELL"
  def toHtml: Text.TypedTag[String] = {
    table(
      tr(th("Type"), th("Currency Pair"), th("Price"), th("Quantity"), th("Date")),
      tr(td("SELL"), td(pair.toString), td(price.toString()), td(quantity.toString())))
  }
}

case class Quantity(quantity: BigDecimal)

object Order extends StrictLogging {

  def process(order: Order, tradingOps: TradingOps)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[(Order, String)] = {

    val eventualResult = order match {
      case b: Buy =>
        tradingOps.buyAtMarketValue(b.price, b.pair, b.pair.right, Quantity(b.quantity))
      case s: Sell =>
        tradingOps.sellAtMarketValue(s.price, s.pair, Quantity(s.quantity))
    }

    eventualResult.map { result =>
      logger.info("Order {} successfully processed with response: {}", order, result)
      (order, result)
    }.recover {
      case NonFatal(t) =>
        logger.error("Order {} was not processed with error: {}", order, t.getMessage)
        (order, t.getMessage)
    }
  }

  implicit val buyWrites = new Writes[Buy] {
    def writes(buy: Buy): JsObject = Json.obj(
      "quantity" -> buy.quantity,
      "price" -> buy.price.underlying().toPlainString,
      "asset" -> buy.pair.right.toString,
      "date" -> buy.date.toOffsetDateTime.toString(),
      "type" -> "BUY")
  }

  implicit val sellWrites = new Writes[Sell] {
    def writes(sell: Sell): JsObject = Json.obj(
      "quantity" -> sell.quantity,
      "price" -> sell.price.underlying().toPlainString,
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
