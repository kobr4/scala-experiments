package com.kobr4.tradebot.model

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.ExchangeApi
import com.kobr4.tradebot.services.PriceService

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

case class InvalidPortfolioState(order: Order) extends RuntimeException(s"Invalid state $order")

case class Portfolio(assets: mutable.Map[Asset, Quantity], orderList: mutable.ListBuffer[Order], prices: Map[Asset, PairPrices]) {

  def update(order: Order, fee: BigDecimal): Order = {
    order match {
      case Buy(pair, price, quantity, date) =>
        assets(pair.right) = Quantity(assets(pair.right).quantity + quantity - quantity * fee / BigDecimal(100))
        assets(pair.left) = Quantity(assets(pair.left).quantity - quantity * price)
        if (assets(pair.right).quantity < 0 || assets(pair.left).quantity < 0)
          throw InvalidPortfolioState(order)
      case Sell(pair, price, quantity, date) =>
        assets(pair.right) = Quantity(assets(pair.right).quantity - quantity)
        assets(pair.left) = Quantity(assets(pair.left).quantity + (quantity - quantity * fee / BigDecimal(100)) * price)
        if (assets(pair.right).quantity < 0 || assets(pair.left).quantity < 0) throw InvalidPortfolioState(order)
    }

    orderList.append(order)
    order
  }

  def updateBuyOnly(order: Order, fee: BigDecimal): Order = {
    order match {
      case b: Buy => update(b, fee)
      case _ => order
    }
  }

  def balance(baseAsset: Asset, currentDate: ZonedDateTime): BigDecimal = {
    assets.keySet.map {
      case k if k == baseAsset => assets(k).quantity
      case k => assets(k).quantity * prices.get(k).map(_.currentPrice(currentDate)).getOrElse(BigDecimal(0))

    }.sum
  }

  def balanceForAsset(currentAsset: Asset, currentDate: ZonedDateTime): BigDecimal = {
    assets(currentAsset).quantity * prices(currentAsset).currentPrice(currentDate)
  }

  def appendOrderList(orderListToAdd: List[Order]): Unit = {
    orderListToAdd.foreach { order =>
      orderList.append(order)
    }
  }
}

object Portfolio {
  def create(priceMap: Map[Asset, PairPrices]) = Portfolio(
    mutable.Map(priceMap.keys.toList.map(asset => asset -> Quantity(0)) ::: List(Asset.Usd -> Quantity(0)): _*),
    mutable.ListBuffer.empty[Order],
    priceMap)

  def fromApi(api: ExchangeApi, priceData: Map[Asset, PairPrices])(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[Portfolio] = {
    for {
      balancesMap <- api.returnBalances
      orderHistory <- api.returnTradeHistory()
    } yield {
      val port = Portfolio.create(priceData)
      balancesMap.toList.map(kv => port.assets.put(kv._1, kv._2))
      port.appendOrderList(orderHistory)
      port
    }
  }

  def pricesMap(assetList: List[Asset])(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[(Asset, PairPrices)]] =
    Future.sequence(assetList.map(asset => PriceService.getPriceData(asset).map(asset -> _)))
}