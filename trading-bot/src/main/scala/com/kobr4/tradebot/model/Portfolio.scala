package com.kobr4.tradebot.model

import java.time.ZonedDateTime

import com.kobr4.tradebot.model.Asset.Usd

import scala.collection.mutable

case class Portfolio(assets: mutable.Map[Asset, Quantity], orderList: mutable.ListBuffer[Order], prices: Map[Asset, PairPrices]) {

  def update(order: Order, fee: BigDecimal): Order = {
    order match {
      case Buy(asset, price, quantity) =>
        assets(asset) = Quantity(assets(asset).quantity + quantity - quantity * fee / BigDecimal(100))
        assets(Usd) = Quantity(assets(Usd).quantity - quantity * price)
      case Sell(asset, price, quantity) =>
        assets(asset) = Quantity(assets(asset).quantity - quantity)
        assets(Usd) = Quantity(assets(Usd).quantity + (quantity - quantity * fee / BigDecimal(100)) * price)
    }
    orderList.append(order)
    order
  }

  def balance(currentDate: ZonedDateTime): BigDecimal = {
    assets.keySet.map {
      case k @ Asset.Usd => assets(k).quantity
      case k => assets(k).quantity * prices(k).currentPrice(currentDate)

    }.sum
  }

  def balance(currentAsset: Asset, currentDate: ZonedDateTime): BigDecimal = {
    assets(currentAsset).quantity * prices(currentAsset).currentPrice(currentDate)
  }
}

object Portfolio {
  def create(priceMap: Map[Asset, PairPrices]) = Portfolio(
    mutable.Map(priceMap.keys.toList.map(asset => asset -> Quantity(0)):::List(Asset.Usd -> Quantity(0)) : _*),
    mutable.ListBuffer.empty[Order],
    priceMap)
}