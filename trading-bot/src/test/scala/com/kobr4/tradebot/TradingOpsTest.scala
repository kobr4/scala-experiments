package com.kobr4.tradebot

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api._
import com.kobr4.tradebot.engine.SafeStrategy
import com.kobr4.tradebot.model.{Asset, Buy, Quantity}
import com.kobr4.tradebot.services.TradingOps
import org.mockito.Mockito._
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import org.mockito.Matchers.any

import scala.concurrent.Future
import scala.concurrent.duration._

class TradingOpsTest extends FlatSpec with Matchers with ScalaFutures with MockitoSugar with BeforeAndAfterEach {

  val pair = CurrencyPair(Asset.Usd, Asset.Btc)
  val btcUsd = Quote(pair, 7890.55745487, 7892.13246374, 7887.41896173, 0.14273589, 32600437.50359336, 4360.51881670)

  implicit val as = ActorSystem()
  implicit val am = ActorMaterializer()
  implicit val ec = as.dispatcher

  "TradingOps" should "place a buy order at market value" in {
    val dummyOrder = Buy(CurrencyPair(Asset.Usd, Asset.Btc), 1.0, 1.0, ZonedDateTime.now())

    val apiMock = mock[ExchangeApi]
    when(apiMock.returnTicker()).thenReturn(Future.successful(List(btcUsd)))
    when(apiMock.buy(any[CurrencyPair], any[BigDecimal], any[BigDecimal])).thenReturn(Future.successful(dummyOrder))

    val tradingOps = new TradingOps(apiMock)

    tradingOps.buyAtMarketValue(BigDecimal("7890.55745487"), pair, Asset.Btc, Quantity(1)).futureValue(Timeout(10 seconds))

    verify(apiMock).buy(CurrencyPair(Asset.Usd, Asset.Btc), BigDecimal("7890.55745487"), BigDecimal("1"))
  }

  "TradingOps" should "place a sell order at market value" in {
    val dummyOrder = Buy(CurrencyPair(Asset.Usd, Asset.Btc), 1.0, 1.0, ZonedDateTime.now())

    val apiMock = mock[ExchangeApi]
    when(apiMock.returnTicker()).thenReturn(Future.successful(List(btcUsd)))
    when(apiMock.sell(any[CurrencyPair], any[BigDecimal], any[BigDecimal])).thenReturn(Future.successful(dummyOrder))

    val tradingOps = new TradingOps(apiMock)

    tradingOps.sellAtMarketValue(BigDecimal("7890.55745487"), pair, Quantity(1)).futureValue(Timeout(10 seconds))

    verify(apiMock).sell(CurrencyPair(Asset.Usd, Asset.Btc), 7890.55745487, 1)
  }

  "TradingOps" should "sell all available assets at market value" in {
    val dummyOrder = Buy(CurrencyPair(Asset.Usd, Asset.Btc), 1.0, 1.0, ZonedDateTime.now())

    val apiMock = mock[ExchangeApi]
    when(apiMock.returnTicker()).thenReturn(Future.successful(List(btcUsd)))
    when(apiMock.returnBalances).thenReturn(Future.successful(Map[Asset, Quantity](Asset.Btc -> Quantity(2.5))))
    when(apiMock.sell(any[CurrencyPair], any[BigDecimal], any[BigDecimal])).thenReturn(Future.successful(dummyOrder))

    val tradingOps = new TradingOps(apiMock)

    tradingOps.sellAll(Asset.Usd).futureValue(Timeout(10 seconds))

    verify(apiMock).sell(CurrencyPair(Asset.Usd, Asset.Btc), 7890.55745487, 2.5)
  }

  "TradingOps" should "place sell USDT_STR at market value" in {
    val dummyOrder = Buy(CurrencyPair(Asset.Usd, Asset.Btc), 1.0, 1.0, ZonedDateTime.now())

    val apiMock = mock[ExchangeApi]
    val poloApi = new PoloApi()
    //val eventualTicker = poloApi.returnTicker()
    when(apiMock.returnTicker()).thenReturn(Future.successful(
      List(Quote(CurrencyPair(Asset.Tether, Asset.Xlm), 1.0, 1.0, 1.0, 1.0, 1.0, 1.0))))
    when(apiMock.sell(any[CurrencyPair], any[BigDecimal], any[BigDecimal])).thenReturn(Future.successful(dummyOrder))

    val tradingOps = new TradingOps(apiMock)

    val pair = CurrencyPair(Asset.Tether, Asset.Xlm)

    //val quote = eventualTicker.futureValue(Timeout(10 seconds)).filter(q => q.pair.left == pair.left && q.pair.right == pair.right).head

    tradingOps.sellAtMarketValue(1.0, pair, Quantity(1)).futureValue(Timeout(10 seconds))

    verify(apiMock).sell(CurrencyPair(Asset.Tether, Asset.Xlm), 1.0, 1)
  }

  "TradingOps" should "cancel an order" in {

    val order = PoloOrder(CurrencyPair(Asset.Usd, Asset.Btc), "1", 1, 1)
    val apiMock = mock[ExchangeApi]
    when(apiMock.returnOpenOrders()).thenReturn(Future.successful(List(order)))
    when(apiMock.cancelOrder(any[PoloOrder])).thenReturn(Future.successful(true))

    val tradingOps = new TradingOps(apiMock)

    tradingOps.cancelAllOpenOrders().futureValue(Timeout(10 seconds))

    verify(apiMock).cancelOrder(order)
  }

  "TradingOps" should "not return an order" in {

    SafeStrategy.MaybeBuy(CurrencyPair(Asset.Tether, Asset.Eth), BigDecimal("0.00000825"), BigDecimal("173.17579748685"), ZonedDateTime.now()) shouldBe None

    SafeStrategy.MaybeSell(CurrencyPair(Asset.Tether, Asset.Eth), BigDecimal("0.00000825"), BigDecimal("173.17579748685"), ZonedDateTime.now()) shouldBe None

    SafeStrategy.MaybeSell(CurrencyPair(Asset.Tether, Asset.Eth), BigDecimal("0.1"), BigDecimal("173.17579748685"), ZonedDateTime.now()).isDefined shouldBe true

    SafeStrategy.MaybeSell(CurrencyPair(Asset.Btc, Asset.Eth), BigDecimal("0.1"), BigDecimal("0.01763540"), ZonedDateTime.now()).isDefined shouldBe true
  }

  /*
  "TradingOps" should "load portfolio" in {

    val apiMock = mock[ExchangeApi]
    when(apiMock.returnBalances).thenReturn(Future.successful(Map[Asset, Quantity](Asset.Eth -> Quantity(1))))

    val tradingOps = new TradingOps(apiMock)

    val port = tradingOps.loadPortfolio().futureValue(Timeout(10 seconds))

    port.assets(Asset.Eth) shouldBe Quantity(1)
  }
*/
}
