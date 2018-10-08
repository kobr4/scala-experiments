package com.kobr4.tradebot.services

import com.kobr4.tradebot.api.{ CurrencyPair, PoloAPIInterface }
import com.kobr4.tradebot.model.{ Asset, Portfolio, Quantity }

import scala.concurrent.{ ExecutionContext, Future }

class TradingOps(val api: PoloAPIInterface)(implicit ec: ExecutionContext) {

  private def percentage(base: BigDecimal, pct: BigDecimal): BigDecimal = base * pct / BigDecimal(100)

  private val pricePctDeltaThreshold = BigDecimal(5)

  private def getAmount(currencyPair: CurrencyPair, asset: Asset, rate: BigDecimal, quantity: BigDecimal) = {
    if (currencyPair.left == asset)
      quantity / rate
    else
      quantity * rate
  }

  def buyAtMarketValue(targetPrice: BigDecimal, currencyPair: CurrencyPair, asset: Asset, quantity: Quantity): Future[String] = {
    api.returnTicker().flatMap { quoteList =>
      quoteList.find(q => (q.pair.left == currencyPair.left)
        && (q.pair.right == currencyPair.right)).map { q =>
        if ((q.last - targetPrice).abs < percentage(targetPrice, pricePctDeltaThreshold))
          api.buy(currencyPair.toString, q.last, getAmount(currencyPair, asset, q.last, quantity.quantity))
        else
          Future("BUY could not processed")
      }.getOrElse(Future.failed(new RuntimeException("Currency pair not found")))
    }
  }

  def sellAtMarketValue(targetPrice: BigDecimal, currencyPair: CurrencyPair, asset: Asset, quantity: Quantity): Future[String] = {
    api.returnTicker().flatMap { quoteList =>
      quoteList.find(q => (q.pair.left == currencyPair.left)
        && (q.pair.right == currencyPair.right)).map { q =>
        if ((q.last - targetPrice).abs < percentage(targetPrice, pricePctDeltaThreshold))
          api.sell(currencyPair.toString, q.last, getAmount(currencyPair, asset, q.last, quantity.quantity))
        else
          Future("SELL could not processed")
      }.getOrElse(Future.failed(new RuntimeException("Currency pair not found")))
    }
  }

  def cancelAllOpenOrders(): Future[Unit] = {
    api.returnOpenOrders().map { orderList => orderList.map { order => api.cancelOrder(order.orderNumber) } }
  }

  def loadPortfolio(): Future[Portfolio] = {
    api.returnBalances.map { assetMap =>
      val port = Portfolio.create(Map())
      assetMap.toList.map(kv => port.assets.put(kv._1, kv._2))
      port
    }
  }
}
