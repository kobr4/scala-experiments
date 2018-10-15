package com.kobr4.tradebot.api

import java.time.ZonedDateTime

import com.kobr4.tradebot.model.{ Asset, Order, Quantity }

import scala.concurrent.{ ExecutionContext, Future }

trait PoloAPIInterface {

  def returnBalances: Future[Map[Asset, Quantity]]

  def returnDepositAddresses: Future[Map[Asset, String]]

  def returnOpenOrders(): Future[List[PoloOrder]]

  def cancelOrder(orderNumber: String): Future[Boolean]

  def returnTradeHistory(
    start: ZonedDateTime = ZonedDateTime.parse("2018-01-01T01:00:00.000Z"),
    end: ZonedDateTime = ZonedDateTime.now()): Future[List[Order]]

  def buy(currencyPair: String, rate: BigDecimal, amount: BigDecimal): Future[String]

  def sell(currencyPair: String, rate: BigDecimal, amount: BigDecimal): Future[String]

  def returnTicker()(implicit ec: ExecutionContext): Future[List[Quote]]
}
