package com.kobr4.tradebot

sealed trait Order

case class Buy(asset: Asset, price: BigDecimal, quantity: BigDecimal) extends Order

case class Sell(asset: Asset, price: BigDecimal, quantity: BigDecimal) extends Order

case class Quantity(quantity: BigDecimal)