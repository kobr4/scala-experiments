package com.kobr4.tradebot.api

import com.kobr4.tradebot.Asset
import com.kobr4.tradebot.model.Quantity

import scala.concurrent.{ ExecutionContext, Future }

trait PoloAPIInterface {

  def returnBalances: Future[Map[Asset, Quantity]]

  def returnDepositAddresses: Future[Map[Asset, String]]

  def returnOpenOrders(): Future[List[PoloOrder]]

  def cancelOrder(orderNumber: Long): Future[Boolean]

  def buy(currencyPair: String, rate: BigDecimal, amount: BigDecimal): Future[String]

  def sell(currencyPair: String, rate: BigDecimal, amount: BigDecimal): Future[String]

  def returnTicker()(implicit ec: ExecutionContext): Future[List[Quote]]
}