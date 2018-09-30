package com.kobr4.tradebot

import com.kobr4.tradebot.Asset.Usd

import scala.collection.mutable

case class Portfolio(assets: mutable.HashMap[Asset, Quantity], orderList: mutable.ListBuffer[Order]) {

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

  def balance(priceData: PairPrices): BigDecimal = {
    assets.keySet.map {
      case k @ Asset.Eth => assets(k).quantity * priceData.prices.last.price
      case k => assets(k).quantity
    }.sum
  }
}

object Portfolio {
  def create(asset: Asset) = Portfolio(
    mutable.HashMap(asset -> Quantity(0), Asset.Usd -> Quantity(0)),
    mutable.ListBuffer.empty[Order])
}