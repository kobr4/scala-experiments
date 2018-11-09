package com.kobr4.tradebot

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.kobr4.tradebot.api.{CurrencyPair, PoloApi, PoloOrder}
import com.kobr4.tradebot.model.Asset.Usd
import com.kobr4.tradebot.model.{Asset, Buy, Quantity}
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

import scala.collection.immutable.Range
import scala.concurrent.duration._
import scala.util.Random

class PoloApiTest extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfterEach {

  val port = Math.abs(Random.nextInt()) % 4096 + 1024
  val poloUrl = s"http://127.0.0.1:$port"
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

  it should "return balances" in {

    wireMockServer.stubFor(post(urlEqualTo("/tradingApi"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody("""{"BTC":"0.59098578","LTC":"3.31117268"}""")))

    val api = new PoloApi(DefaultConfiguration.PoloApi.Key, DefaultConfiguration.PoloApi.Secret, poloUrl)
    val map = api.returnBalances.futureValue(Timeout(10 seconds))

    map(Asset.Btc) shouldBe Quantity(BigDecimal("0.59098578"))
  }

  it should "return deposit adresses" in {

    wireMockServer.stubFor(post(urlEqualTo("/tradingApi"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody("""{"BTC":"19YqztHmspv2egyD6jQM3yn81x5t5krVdJ","LTC":"LPgf9kjv9H1Vuh4XSaKhzBe8JHdou1WgUB"}""")))

    val api = new PoloApi(DefaultConfiguration.PoloApi.Key, DefaultConfiguration.PoloApi.Secret, poloUrl)
    val map = api.returnDepositAddresses.futureValue(Timeout(10 seconds))

    map(Asset.Btc) shouldBe "19YqztHmspv2egyD6jQM3yn81x5t5krVdJ"
  }

  it should "return open orders" in {

    wireMockServer.stubFor(post(urlEqualTo("/tradingApi"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody(
          """[{"orderNumber":"120466","type":"sell","rate":"0.025","amount":"100","total":"2.5"},
            |{"orderNumber":"120467","type":"sell","rate":"0.04","amount":"100","total":"4"}]""".stripMargin)))

    val api = new PoloApi(DefaultConfiguration.PoloApi.Key, DefaultConfiguration.PoloApi.Secret, poloUrl)
    val list = api.returnOpenOrders().futureValue(Timeout(10 seconds))

    list should contain(PoloOrder("120466", BigDecimal("0.025"), BigDecimal("100")))
  }

  it should "place a sell order" in {

    wireMockServer.stubFor(post(urlEqualTo("/tradingApi"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody(
          """{"orderNumber":31226040,"resultingTrades":[{"amount":"338.8732","date":"2014-10-18 23:03:21"
            |,"rate":"0.00000173","total":"0.00058625","tradeID":"16164","type":"sell"}]}""".stripMargin)))

    val api = new PoloApi(DefaultConfiguration.PoloApi.Key, DefaultConfiguration.PoloApi.Secret, poloUrl)
    api.sell(CurrencyPair(Asset.Btc, Asset.Usd), BigDecimal("1"), BigDecimal("1")).futureValue(Timeout(10 seconds))
  }

  it should "place a buy order" in {

    wireMockServer.stubFor(post(urlEqualTo("/tradingApi"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody(
          """{"orderNumber":31226040,"resultingTrades":[{"amount":"338.8732","date":"2014-10-18 23:03:21"
            |,"rate":"0.00000173","total":"0.00058625","tradeID":"16164","type":"buy"}]}""".stripMargin)))

    val api = new PoloApi(DefaultConfiguration.PoloApi.Key, DefaultConfiguration.PoloApi.Secret, poloUrl)
    api.buy(CurrencyPair(Asset.Btc, Asset.Usd), BigDecimal("1"), BigDecimal("1")).futureValue(Timeout(10 seconds))
  }

  it should "return ticker" in {
    wireMockServer.stubFor(get(urlEqualTo("/public?command=returnTicker"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody(
          """
          | {"BTC_USD":{"last":"0.0251","lowestAsk":"0.02589999","highestBid":"0.0251","percentChange":"0.02390438",
          |"baseVolume":"6.16485315","quoteVolume":"245.82513926"},"BTC_NXT":{"last":"0.00005730","lowestAsk":"0.00005710",
          |"highestBid":"0.00004903","percentChange":"0.16701570","baseVolume":"0.45347489","quoteVolume":"9094"}}
        """.stripMargin)))

    val api = new PoloApi(DefaultConfiguration.PoloApi.Key, DefaultConfiguration.PoloApi.Secret, poloUrl)
    val quoteList = api.returnTicker().futureValue(Timeout(10 seconds))

    quoteList.head.pair.left shouldBe Asset.Btc
    quoteList.head.pair.right shouldBe Asset.Usd
    quoteList.head.last shouldBe BigDecimal("0.0251")
  }

  it should "return trade history" in {
    wireMockServer.stubFor(post(urlEqualTo("/tradingApi"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody(
          """
            |  { "USDT_BTC": [
            |    {
            |      "globalTradeID": 29251512,
            |      "tradeID": "1385888",
            |      "date": "2016-05-03 01:29:55",
            |      "rate": "0.00014243",
            |      "amount": "353.74692925",
            |      "total": "0.05038417",
            |      "fee": "0.00200000",
            |      "orderNumber": "12603322113",
            |      "type": "buy",
            |      "category": "settlement"
            |    },
            |    {
            |      "globalTradeID": 29251511,
            |      "tradeID": "1385887",
            |      "date": "2016-05-03 01:29:55",
            |      "rate": "0.00014111",
            |      "amount": "311.24262497",
            |      "total": "0.04391944",
            |      "fee": "0.00200000",
            |      "orderNumber": "12603319116",
            |      "type": "sell",
            |      "category": "marginTrade"
            |    } ]
            |    }
          """.stripMargin)))

    val api = new PoloApi(DefaultConfiguration.PoloApi.Key, DefaultConfiguration.PoloApi.Secret, poloUrl)
    val orderList = api.returnTradeHistory().futureValue(Timeout(10 seconds))

    val order = orderList.head
    val buy = order match {
      case b: Buy => b
    }

    buy.pair.right shouldBe Asset.Btc
    buy.price shouldBe BigDecimal(0.00014243)
    buy.quantity shouldBe BigDecimal(353.74692925)
  }

  it should "provide HMAC-512 signature" in {
    val signature = PoloApi.generateHMAC512("toto", "command=returnBalances")

    signature shouldBe "5e6ec0bd24181eeef34ef1c70eb65e116dcbfabd96f4c5409d64f2f028fffaac14295dd6f86e8876f31eee845913edca53e4052b121739497a19b46f5e49ca75"
  }

  it should "return chart data" in {
    wireMockServer.stubFor(get(urlMatching("/public\\?command=returnChartData\\&.*"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody(
          """
            |[
            |  {
            |    "date": 1405699200,
            |    "high": 0.0045388,
            |    "low": 0.00403001,
            |    "open": 0.00404545,
            |    "close": 0.00427592,
            |    "volume": 44.11655644,
            |    "quoteVolume": 10259.29079097,
            |    "weightedAverage": 0.00430015
            |  }
            |]
          """.stripMargin)))

    val api = new PoloApi(DefaultConfiguration.PoloApi.Key, DefaultConfiguration.PoloApi.Secret, poloUrl)
    val chartData = api.returnChartData(CurrencyPair(Asset.Btc, Asset.Eth), 300,
      ZonedDateTime.parse("2017-01-01T01:00:00.000Z"), ZonedDateTime.parse("2017-02-01T01:00:00.000Z")).futureValue(Timeout(10 seconds))

    chartData.prices.length shouldNot be(0)

  }
}
