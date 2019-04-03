package com.kobr4.tradebot

import java.time.{Instant, ZoneId, ZonedDateTime}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.kobr4.tradebot.api.{BinanceApi, CurrencyPair, PoloOrder, Quote}
import com.kobr4.tradebot.model.{Asset, Buy}
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.util.Random

class BinanceApiTest extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfterEach {


  val port = Math.abs(Random.nextInt()) % 4096 + 1024
  val wireMockServer = new WireMockServer(options().port(port))

  implicit val as = ActorSystem()
  implicit val am = ActorMaterializer()
  implicit val ec = as.dispatcher

  override def beforeEach {
    wireMockServer.start()
  }

  override def afterEach {
    wireMockServer.stop()
  }

  it should "return open orders" in {

    wireMockServer.stubFor(get(urlPathEqualTo("/api/v3/openOrders"))
      .withHeader("X-MBX-APIKEY",equalTo("vmPUZE6mv9SD5VNHk4HlWFsOr6aKE2zvsw0MuIgwCIPy6utIco14y7Ju91duEh8A"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody(
          """
            |[
            |  {
            |    "symbol": "LTCBTC",
            |    "orderId": 1,
            |    "clientOrderId": "myOrder1",
            |    "price": "0.1",
            |    "origQty": "1.0",
            |    "executedQty": "0.0",
            |    "cummulativeQuoteQty": "0.0",
            |    "status": "NEW",
            |    "timeInForce": "GTC",
            |    "type": "LIMIT",
            |    "side": "BUY",
            |    "stopPrice": "0.0",
            |    "icebergQty": "0.0",
            |    "time": 1499827319559,
            |    "updateTime": 1499827319559,
            |    "isWorking": true
            |  }
            |]
            |""".stripMargin)))

    val api = new BinanceApi("vmPUZE6mv9SD5VNHk4HlWFsOr6aKE2zvsw0MuIgwCIPy6utIco14y7Ju91duEh8A",
      "NhqPtmdSJYdKjVHjA7PZj4Mge3R5YNiP1e3UZjInClVN65XAbvqqM6A7H5fATj0j", s"http://127.0.0.1:$port/api/v3/")
    val list = api.returnOpenOrders().futureValue(Timeout(10 seconds))

    list should contain(PoloOrder(CurrencyPair(Asset.Btc, Asset.Ltc), "1", BigDecimal(0.1), BigDecimal(1.0)))
  }


  it should "return trades history" in {

    wireMockServer.stubFor(get(urlPathEqualTo("/api/v3/myTrades"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody(
          """
            |[
            |  {
            |    "symbol": "ETHBTC",
            |    "id": 28457,
            |    "orderId": 100234,
            |    "price": "4.000001",
            |    "qty": "12.0",
            |    "quoteQty": "48.000012",
            |    "commission": "10.10000000",
            |    "commissionAsset": "BNB",
            |    "time": 1499865549590,
            |    "isBuyer": true,
            |    "isMaker": false,
            |    "isBestMatch": true
            |  }
            |]
            |""".stripMargin)))

    val api = new BinanceApi(DefaultConfiguration.KrakenApi.Key, DefaultConfiguration.KrakenApi.Secret, s"http://127.0.0.1:$port/api/v3/")
    val list = api.returnTradeHistory().futureValue(Timeout(10 seconds))

    list should contain(Buy(CurrencyPair(Asset.Btc, Asset.Eth), BigDecimal(4.00000100), BigDecimal(12.00000000),
      ZonedDateTime.ofInstant(Instant.ofEpochSecond(1499865549590L), ZoneId.of("UTC"))))
  }

  it should "return ticker prices" in {

    wireMockServer.stubFor(get(urlEqualTo("/api/v3/ticker/price"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody(
          """
            |[
            |  {
            |    "symbol": "LTCBTC",
            |    "price": "4.00000200"
            |  },
            |  {
            |    "symbol": "ETHBTC",
            |    "price": "0.07946600"
            |  }
            |]
            |""".stripMargin)))

    val api = new BinanceApi("vmPUZE6mv9SD5VNHk4HlWFsOr6aKE2zvsw0MuIgwCIPy6utIco14y7Ju91duEh8A",
      "NhqPtmdSJYdKjVHjA7PZj4Mge3R5YNiP1e3UZjInClVN65XAbvqqM6A7H5fATj0j", s"http://127.0.0.1:$port/api/v3/")
    val list = api.returnTicker().futureValue(Timeout(10 seconds))
    list should contain(Quote(CurrencyPair(Asset.Btc, Asset.Ltc), BigDecimal(4.000002), BigDecimal(0), BigDecimal(0), BigDecimal(0), BigDecimal(0), BigDecimal(0)))
  }

  it should "provide HMAC-256 signature" in {

    val signature = BinanceApi.generateHMAC256("NhqPtmdSJYdKjVHjA7PZj4Mge3R5YNiP1e3UZjInClVN65XAbvqqM6A7H5fATj0j",
      "symbol=LTCBTC&side=BUY&type=LIMIT&timeInForce=GTC&quantity=1&price=0.1&recvWindow=5000&timestamp=1499827319559".getBytes())

    signature should be("c8db56825ae71d6d79447849e617115f4a920fa2acdcab2b053c4b2838bd6b71")
  }

}
