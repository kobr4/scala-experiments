package com.kobr4.tradebot.services

import com.kobr4.tradebot.api.{CurrencyPair, PoloAPIInterface}
import com.kobr4.tradebot.model.{Asset, Portfolio, Quantity}
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}

class TradingOps(val api: PoloAPIInterface)(implicit ec: ExecutionContext) extends StrictLogging {

  private def percentage(base: BigDecimal, pct: BigDecimal): BigDecimal = base * pct / BigDecimal(100)

  private val pricePctDeltaThreshold = BigDecimal(5)

  private def getAmount(currencyPair: CurrencyPair, asset: Asset, rate: BigDecimal, quantity: BigDecimal) = {
    if (currencyPair.right == asset)
      quantity / rate
    else
      quantity * rate
  }

  def buyAtMarketValue(targetPrice: BigDecimal, currencyPair: CurrencyPair, asset: Asset, quantity: Quantity): Future[String] = {
    api.returnTicker().flatMap { quoteList =>
      quoteList.find(q => (q.pair.left == currencyPair.left)
        && (q.pair.right == currencyPair.right)).map { q =>
        if ((q.last - targetPrice).abs < percentage(targetPrice, pricePctDeltaThreshold)) {
          val amount = getAmount(currencyPair, asset, q.last, targetPrice * quantity.quantity)
          logger.info("Will buy on pair : {} for {} amount: {}", currencyPair.toString, q.last, amount)
          api.buy(currencyPair.toString, q.last, amount)
        } else {
          logger.info("Buy could not be processed because spread was too high {} vs {}",q.last, targetPrice)
          Future("BUY could not processed")
        }
      }.getOrElse(Future.failed(new RuntimeException(s"Currency pair not found : $currencyPair")))
    }
  }

  def sellAtMarketValue(targetPrice: BigDecimal, currencyPair: CurrencyPair, asset: Asset, quantity: Quantity): Future[String] = {
    api.returnTicker().flatMap { quoteList =>
      quoteList.find(q => (q.pair.left == currencyPair.left)
        && (q.pair.right == currencyPair.right)).map { q =>
        if ((q.last - targetPrice).abs < percentage(targetPrice, pricePctDeltaThreshold)) {
          val amount = getAmount(currencyPair, asset, q.last, targetPrice * quantity.quantity)
          logger.info("Will sell on pair : {} for {} amount: {}", currencyPair.toString, q.last, amount)
          api.sell(currencyPair.toString, q.last, amount)
        } else {
          logger.info("Sell could not be processed because spread was too high {} vs {}",q.last, targetPrice)
          Future("SELL could not processed")
        }
      }.getOrElse(Future.failed(new RuntimeException(s"Currency pair not found : $currencyPair")))
    }
  }

  def cancelAllOpenOrders(): Future[Unit] = {
    api.returnOpenOrders().map { orderList => orderList.map { order => api.cancelOrder(order.orderNumber) } }
  }

  @deprecated
  def loadPortfolio(): Future[Portfolio] = {
    api.returnBalances.map { assetMap =>
      val port = Portfolio.create(Map())
      assetMap.toList.map(kv => port.assets.put(kv._1, kv._2))
      port
    }
  }
}
