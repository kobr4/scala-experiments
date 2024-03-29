package com.kobr4.tradebot

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{ CurrencyPair, ExchangeApi, KrakenApi, PoloApi }
import com.kobr4.tradebot.engine._
import com.kobr4.tradebot.model.{ Asset, Buy, Order, Quantity }
import com.kobr4.tradebot.services.{ PriceService, TradeBotService, TradingOps }
import org.scalatest.FlatSpec
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers.any

import scala.concurrent.Future
import scala.concurrent.duration._

class TradeBotServiceIT extends FlatSpec with ScalaFutures with MockitoSugar {

  implicit val as = ActorSystem()
  implicit val am = ActorMaterializer()
  implicit val ec = as.dispatcher

  it should "run and trade multiple asset" in {
    val dummyOrder = Buy(CurrencyPair(Asset.Usd, Asset.Btc), 1.0, 1.0, ZonedDateTime.now())
    val poloApi = new PoloApi()
    val apiMock = mock[ExchangeApi]
    val tradingOps = new TradingOps(apiMock)
    when(apiMock.returnTicker()).thenReturn(poloApi.returnTicker())
    when(apiMock.buy(any(), any(), any())).thenReturn(Future.successful(dummyOrder))

    val result = TradeBotService.runMapAndTrade(Map(Asset.Btc -> BigDecimal(0.3), Asset.Eth -> BigDecimal(0.3),
      Asset.Xmr -> BigDecimal(0.2), Asset.Xlm -> BigDecimal(0.1), Asset.Doge -> BigDecimal(0.1)), SafeStrategy, poloApi, tradingOps, Asset.Tether)

    val orderList = result.futureValue(Timeout(10 seconds))

    orderList.foreach(order => println(order))

  }

  it should "run and trade one asset with Kraken API" in {
    val dummyOrder = Buy(CurrencyPair(Asset.Usd, Asset.Btc), 1.0, 1.0, ZonedDateTime.now())
    val krakenApi = new KrakenApi()
    val apiMock = mock[ExchangeApi]
    val tradingOps = new TradingOps(apiMock)
    when(apiMock.returnTicker()).thenReturn(krakenApi.returnTicker())
    when(apiMock.buy(any(), any(), any())).thenReturn(Future.successful(dummyOrder))

    val result = TradeBotService.runMapAndTrade(Map(Asset.Eth -> BigDecimal(1.0)), SafeStrategy, krakenApi, tradingOps, Asset.Usd)

    val order = result.futureValue(Timeout(10 seconds))

    println(order)

  }

  it should "run and trade multiple asset against btc" in {
    val dummyOrder = Buy(CurrencyPair(Asset.Usd, Asset.Btc), 1.0, 1.0, ZonedDateTime.now())
    val poloApi = new PoloApi()
    val apiMock = mock[ExchangeApi]
    val tradingOps = new TradingOps(apiMock)
    when(apiMock.returnTicker()).thenReturn(poloApi.returnTicker())
    when(apiMock.buy(any(), any(), any())).thenReturn(Future.successful(dummyOrder))

    val result = TradeBotService.runMapAndTrade(Map(
      Asset.Bch -> BigDecimal(0.2),
      Asset.Ltc -> BigDecimal(0.2),
      Asset.Dgb -> BigDecimal(0.2),
      Asset.Dash -> BigDecimal(0.2),
      Asset.Xem -> BigDecimal(0.2),
      Asset.Zec -> BigDecimal(0.2),
      Asset.Maid -> BigDecimal(0.2)), GeneratedStrategy(
      List(WhenAboveMovingAverage(10), WhenHigh(20)),
      List(WhenAboveMovingAverage(30), WhenBelowMovingAverage(20))), poloApi, tradingOps, Asset.Btc)

    val orderList = result.futureValue(Timeout(10 seconds))

    orderList.foreach(order => println(order))

  }
}
