package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

import scala.concurrent.duration._


class PoloApiTest extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfterEach {

  val wireMockServer = new WireMockServer(options().port(2345))

  implicit val as = ActorSystem()
  implicit val am = ActorMaterializer()
  implicit val ec = as.dispatcher

  override def beforeEach {
    wireMockServer.start()
  }

  override def afterEach {
    wireMockServer.stop()
  }


  "API" should "return balances" in {

    wireMockServer.stubFor(post(urlEqualTo("/tradingApi"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody("""{"BTC":"0.59098578","LTC":"3.31117268"}""")))

    val api = new PoloApi("http://127.0.0.1:2345")
    val map = api.returnBalances.futureValue(Timeout(10 seconds))

    map(Asset.Btc) shouldBe Quantity(BigDecimal("0.59098578"))
  }

  "API" should "return deposit adresses" in {

    wireMockServer.stubFor(post(urlEqualTo("/tradingApi"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody("""{"BTC":"19YqztHmspv2egyD6jQM3yn81x5t5krVdJ","LTC":"LPgf9kjv9H1Vuh4XSaKhzBe8JHdou1WgUB"}""")))

    val api = new PoloApi("http://127.0.0.1:2345")
    val map = api.returnDepositAddresses.futureValue(Timeout(10 seconds))

    map(Asset.Btc) shouldBe "19YqztHmspv2egyD6jQM3yn81x5t5krVdJ"
  }

  "API" should "return open orders" in {

    wireMockServer.stubFor(post(urlEqualTo("/tradingApi"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody(
          """[{"orderNumber":"120466","type":"sell","rate":"0.025","amount":"100","total":"2.5"},
            |{"orderNumber":"120467","type":"sell","rate":"0.04","amount":"100","total":"4"}]""".stripMargin)))

    val api = new PoloApi("http://127.0.0.1:2345")
    val list = api.returnOpenOrders().futureValue(Timeout(10 seconds))

    list should contain(PoloOrder(120466,BigDecimal("0.025"),BigDecimal("100")))
  }

  "API" should "place a sell order" in {

    wireMockServer.stubFor(post(urlEqualTo("/tradingApi"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody(
          """{"orderNumber":31226040,"resultingTrades":[{"amount":"338.8732","date":"2014-10-18 23:03:21"
            |,"rate":"0.00000173","total":"0.00058625","tradeID":"16164","type":"sell"}]}""".stripMargin)))

    val api = new PoloApi("http://127.0.0.1:2345")
    api.sell("BTC_USD", BigDecimal("1"), BigDecimal("1")).futureValue(Timeout(10 seconds))
  }

  "API" should "place a buy order" in {

    wireMockServer.stubFor(post(urlEqualTo("/tradingApi"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody(
          """{"orderNumber":31226040,"resultingTrades":[{"amount":"338.8732","date":"2014-10-18 23:03:21"
            |,"rate":"0.00000173","total":"0.00058625","tradeID":"16164","type":"buy"}]}""".stripMargin)))

    val api = new PoloApi("http://127.0.0.1:2345")
    api.buy("BTC_USD", BigDecimal("1"), BigDecimal("1")).futureValue(Timeout(10 seconds))
  }

  "API" should "return ticker" in {
    wireMockServer.stubFor(get(urlEqualTo("/public?command=returnTicker"))
      .willReturn(aResponse()
      .withHeader("Content-Type", "text/plain")
      .withBody(
        """
          | {"BTC_USD":{"last":"0.0251","lowestAsk":"0.02589999","highestBid":"0.0251","percentChange":"0.02390438",
          |"baseVolume":"6.16485315","quoteVolume":"245.82513926"},"BTC_NXT":{"last":"0.00005730","lowestAsk":"0.00005710",
          |"highestBid":"0.00004903","percentChange":"0.16701570","baseVolume":"0.45347489","quoteVolume":"9094"}}
        """.stripMargin
      )))

    val api = new PoloApi("http://127.0.0.1:2345")
    val quoteList = api.returnTicker().futureValue(Timeout(10 seconds))

    quoteList.head.pair.left shouldBe Asset.Btc
    quoteList.head.pair.right shouldBe Asset.Usd
    quoteList.head.last shouldBe BigDecimal("0.0251")
  }

  "API" should "provide HMAC-512 signature" in {
    val signature = PoloApi.generateHMAC512("toto","command=returnBalances")

    signature shouldBe "5e6ec0bd24181eeef34ef1c70eb65e116dcbfabd96f4c5409d64f2f028fffaac14295dd6f86e8876f31eee845913edca53e4052b121739497a19b46f5e49ca75"
  }
}
