package com.kobr4.tradebot.services

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{ CurrencyPair, ExchangeApi }
import com.kobr4.tradebot.model.{ Asset, Order, Portfolio, Quantity }
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ ExecutionContext, Future }
import scala.math.BigDecimal.RoundingMode

class TradingOps(val api: ExchangeApi)(implicit ec: ExecutionContext) extends StrictLogging {

  private def percentage(base: BigDecimal, pct: BigDecimal): BigDecimal = base * pct / BigDecimal(100)

  private val pricePctDeltaThreshold = BigDecimal(10)

  private def getAmount(currencyPair: CurrencyPair, asset: Asset, rate: BigDecimal, quantity: BigDecimal) = {
    if (currencyPair.right == asset)
      quantity / rate
    else
      quantity * rate
  }.setScale(8, RoundingMode.DOWN)

  def buyAtMarketValue(targetPrice: BigDecimal, currencyPair: CurrencyPair, asset: Asset, quantity: Quantity): Future[Option[Order]] = {
    api.returnTicker().flatMap { quoteList =>
      quoteList.find(q => (q.pair.left == currencyPair.left)
        && (q.pair.right == currencyPair.right)).map { q =>
        if ((q.last - targetPrice).abs < percentage(targetPrice, pricePctDeltaThreshold)) {
          val amount = getAmount(currencyPair, asset, q.last, targetPrice * quantity.quantity)
          logger.info("Will buy on pair : {} for {} amount: {}", currencyPair.toString, q.last, amount)
          api.buy(currencyPair, q.last, amount).map(Some(_))
        } else {
          logger.info("Buy could not be processed because spread was too high {} vs {}", q.last, targetPrice)
          Future(None)
        }
      }.getOrElse(Future.failed(new RuntimeException(s"Currency pair not found : $currencyPair")))
    }
  }

  def sellAtMarketValue(targetPrice: BigDecimal, currencyPair: CurrencyPair, quantity: Quantity): Future[Option[Order]] = {
    api.returnTicker().flatMap { quoteList =>
      quoteList.find(q => (q.pair.left == currencyPair.left)
        && (q.pair.right == currencyPair.right)).map { q =>
        if ((q.last - targetPrice).abs < percentage(targetPrice, pricePctDeltaThreshold)) {
          logger.info("Will sell on pair : {} for {} amount: {}", currencyPair.toString, q.last, quantity.quantity)
          api.sell(currencyPair, q.last, quantity.quantity).map(Some(_))
        } else {
          logger.info("Sell could not be processed because spread was too high {} vs {}", q.last, targetPrice)
          Future(None)
        }
      }.getOrElse(Future.failed(new RuntimeException(s"Currency pair not found : $currencyPair")))
    }
  }

  def cancelAllOpenOrders(): Future[Unit] = {
    api.returnOpenOrders().map { orderList => orderList.map { order => api.cancelOrder(order) } }
  }

  def sellAll(targetAsset: Asset): Future[List[Order]] = {
    val f = for {
      balanceMap <- api.returnBalances
      prices <- api.returnTicker()
    } yield {
      val s = balanceMap.toList.map { t =>
        prices.find(q => q.pair == CurrencyPair(targetAsset, t._1)).map(quote =>
          sellAtMarketValue(quote.last, quote.pair, t._2)).getOrElse(Future.successful(None))
      }
      Future.sequence(s)
    }

    f.flatten
  }.map(_.flatten)

  def buyAll(assetMap: Map[Asset, BigDecimal], baseAsset: Asset)(implicit arf: ActorSystem, am: ActorMaterializer): Future[List[Order]] = {
    for {
      pData <- Future.sequence(assetMap.keys.toList.map(asset => PriceService.getPriceData(asset).map(asset -> _)))
      prices <- api.returnTicker()
      p <- Portfolio.fromApi(api, pData.toMap)
    } yield {
      val total = p.balance(baseAsset, ZonedDateTime.now())

      val l = assetMap.toList.flatMap(t => {
        prices.find(_.pair == CurrencyPair(baseAsset, t._1)).map(quote => {
          val q = if (total * t._2 > p.balanceForAsset(t._1, ZonedDateTime.now()))
            total * t._2 - p.balanceForAsset(t._1, ZonedDateTime.now())
          else BigDecimal(0)
          val amount = getAmount(CurrencyPair(baseAsset, t._1), baseAsset, quote.last, q)
          api.buy(CurrencyPair(baseAsset, t._1), quote.last, amount)
        })
      })

      Future.sequence(l)
    }

  }.flatten

  @deprecated
  def loadPortfolio(): Future[Portfolio] = {
    api.returnBalances.map { assetMap =>
      val port = Portfolio.create(Map())
      assetMap.toList.map(kv => port.assets.put(kv._1, kv._2))
      port
    }
  }
}
