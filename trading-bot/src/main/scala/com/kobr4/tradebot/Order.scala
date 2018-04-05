package com.kobr4.tradebot

sealed trait Order

case class Buy(asset: Asset, price: BigDecimal, quantity: BigDecimal) extends Order

case class Sell(asset: Asset, price: BigDecimal, quantity: BigDecimal) extends Order

case class Quantity(quantity: BigDecimal)

object Order {

  private def getCurrencyPair(asset: Asset): String = {
    asset match {
      case Asset.Btc => "BTC_USD"
      case Asset.Eth => "ETH_USD"
    }
  }

  def process(order: Order): Unit = {
    val api = new PoloApi()
    order match {
      case b: Buy => api.buy(getCurrencyPair(b.asset), b.price, b.quantity)
      case s: Sell => api.sell(getCurrencyPair(s.asset), s.price, s.quantity)
    }
  }
}
