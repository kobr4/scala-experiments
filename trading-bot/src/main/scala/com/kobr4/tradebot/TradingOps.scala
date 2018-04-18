package com.kobr4.tradebot

import scala.concurrent.{ExecutionContext, Future}

class TradingOps(val api: PoloAPIInterface)(implicit ec: ExecutionContext) {

  private def getAmount(currencyPair: CurrencyPair, asset: Asset, rate: BigDecimal, quantity: BigDecimal): BigDecimal = {
    if (currencyPair.left == asset)
      quantity / rate
    else
      quantity * rate
  }

  def buyAtMarketValue(currencyPair: CurrencyPair, asset: Asset, quantity: Quantity): Future[String] = {
    api.returnTicker().flatMap { quoteList =>
      quoteList.find(q => (q.pair.left == currencyPair.left)
        && (q.pair.right == currencyPair.right)).map { q =>
        api.buy(currencyPair.toString, q.last, getAmount(currencyPair, asset, q.last, quantity.quantity))
      }.getOrElse(Future.failed(new RuntimeException("Currency pair not found")))
    }
  }

  def sellAtMarketValue(currencyPair: CurrencyPair, asset: Asset, quantity: Quantity): Future[String] = {
    api.returnTicker().flatMap { quoteList =>
      quoteList.find(q => (q.pair.left == currencyPair.left)
        && (q.pair.right == currencyPair.right)).map { q =>
        api.sell(currencyPair.toString, q.last, getAmount(currencyPair, asset, q.last, quantity.quantity))
      }.getOrElse(Future.failed(new RuntimeException("Currency pair not found")))
    }
  }

  def cancelAllOpenOrders(): Unit = {
    api.returnOpenOrders().map { orderList => orderList.map { order => api.cancelOrder(order.orderNumber) }}
  }
}
