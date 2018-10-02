package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{ CurrencyPair, PoloAPIInterface, PoloOrder, Quote }
import com.kobr4.tradebot.model.Quantity
import com.kobr4.tradebot.services.TradingOps
import org.mockito.Mockito._
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{ BeforeAndAfterEach, FlatSpec, Matchers }
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

    val apiMock = mock[PoloAPIInterface]
    when(apiMock.returnTicker()).thenReturn(Future.successful(List(btcUsd)))
    when(apiMock.buy(any[String], any[BigDecimal], any[BigDecimal])).thenReturn(Future.successful("success"))

    val tradingOps = new TradingOps(apiMock)

    tradingOps.buyAtMarketValue(pair, Asset.Btc, Quantity(1)).futureValue(Timeout(10 seconds))

    verify(apiMock).buy("USDT_BTC", BigDecimal("7890.55745487"), BigDecimal("7890.55745487"))
  }

  "TradingOps" should "place a sell order at market value" in {

    val apiMock = mock[PoloAPIInterface]
    when(apiMock.returnTicker()).thenReturn(Future.successful(List(btcUsd)))
    when(apiMock.sell(any[String], any[BigDecimal], any[BigDecimal])).thenReturn(Future.successful("success"))

    val tradingOps = new TradingOps(apiMock)

    tradingOps.sellAtMarketValue(pair, Asset.Btc, Quantity(1)).futureValue(Timeout(10 seconds))

    verify(apiMock).sell("USDT_BTC", 7890.55745487, 7890.55745487)
  }

  "TradingOps" should "cancel an order" in {

    val apiMock = mock[PoloAPIInterface]
    when(apiMock.returnOpenOrders()).thenReturn(Future.successful(List(PoloOrder(1, 1, 1))))
    when(apiMock.cancelOrder(any[Long])).thenReturn(Future.successful(true))

    val tradingOps = new TradingOps(apiMock)

    tradingOps.cancelAllOpenOrders().futureValue(Timeout(10 seconds))

    verify(apiMock).cancelOrder(1)
  }

  "TradingOps" should "load portfolio" in {

    val apiMock = mock[PoloAPIInterface]
    when(apiMock.returnBalances).thenReturn(Future.successful(Map[Asset, Quantity](Asset.Eth -> Quantity(1))))

    val tradingOps = new TradingOps(apiMock)

    val port = tradingOps.loadPortfolio().futureValue(Timeout(10 seconds))

    port.assets(Asset.Eth) shouldBe Quantity(1)
  }
}
