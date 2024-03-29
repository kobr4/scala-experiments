package com.kobr4.tradebot.api

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.model.{ Asset, Order, Quantity }

import scala.concurrent.{ ExecutionContext, Future }

trait ExchangeApi {

  def returnBalances: Future[Map[Asset, Quantity]]

  def returnDepositAddresses: Future[Map[Asset, String]]

  def returnOpenOrders(): Future[List[PoloOrder]]

  def cancelOrder(order: PoloOrder): Future[Boolean]

  def returnTradeHistory(
    start: ZonedDateTime = ZonedDateTime.parse("2018-01-01T01:00:00.000Z"),
    end: ZonedDateTime = ZonedDateTime.now()): Future[List[Order]]

  def buy(currencyPair: CurrencyPair, rate: BigDecimal, amount: BigDecimal): Future[Order]

  def sell(currencyPair: CurrencyPair, rate: BigDecimal, amount: BigDecimal): Future[Order]

  def returnTicker()(implicit ec: ExecutionContext): Future[List[Quote]]
}

object ExchangeApi {

  def apply(supportedExchange: SupportedExchange)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): ExchangeApi = supportedExchange match {
    case Kraken => new KrakenApi()
    case Poloniex => new PoloApiV2()
    case Binance => new BinanceApi()
  }

  def apply(supportedExchange: SupportedExchange, apiKey: String, apiSecret: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): ExchangeApi = supportedExchange match {
    case Kraken => new KrakenApi(apiKey, apiSecret)
    case Poloniex => new PoloApiV2(apiKey, apiSecret)
    case Binance => new BinanceApi(apiKey, apiSecret)
  }

}