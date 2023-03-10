package com.kobr4.tradebot

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{ aResponse, _ }
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.kobr4.tradebot.api.{ CurrencyPair, PoloApiV2, PoloOrder }
import com.kobr4.tradebot.model.{ Asset, Buy, Quantity }
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ BeforeAndAfterEach, FlatSpec, Matchers }

import scala.concurrent.duration._
import scala.util.Random

class PoloApiV2Test extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfterEach {

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

    wireMockServer.stubFor(get(urlEqualTo("/accounts/balances?accountType=SPOT"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody("""[
                       {
                         "accountId": "123",
                         "accountType": "SPOT",
                         "balances": [
                           {
                             "currencyId": "60001",
                             "currency": "XMR",
                             "available": "93640.421767943475",
                             "hold": "19.84382885"
                           },
                           {
                             "currencyId": "60002",
                             "currency": "BTC",
                             "available": "100037.9449",
                             "hold": "0.00"
                           },
                           {
                             "currencyId": "60003",
                             "currency": "DOGE",
                             "available": "78086.768609427831705",
                             "hold": "22577.045"
                           }
                         ]
                       }
                     ]""")))

    val api = new PoloApiV2(DefaultConfiguration.PoloApi.Key, DefaultConfiguration.PoloApi.Secret, poloUrl, poloUrl)
    val map = api.returnBalances.futureValue(Timeout(10 seconds))

    map(Asset.Xmr) shouldBe Quantity(BigDecimal("93640.421767943475"))
    map(Asset.Btc) shouldBe Quantity(BigDecimal("100037.9449"))
    map(Asset.Doge) shouldBe Quantity(BigDecimal("78086.768609427831705"))
  }

  it should "return deposit adresses" in {

    wireMockServer.stubFor(get(urlEqualTo("/wallets/addresses"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody("""{"BTC":"19YqztHmspv2egyD6jQM3yn81x5t5krVdJ","LTC":"LPgf9kjv9H1Vuh4XSaKhzBe8JHdou1WgUB"}""")))

    val api = new PoloApiV2(DefaultConfiguration.PoloApi.Key, DefaultConfiguration.PoloApi.Secret, poloUrl, poloUrl)
    val map = api.returnDepositAddresses.futureValue(Timeout(10 seconds))

    map(Asset.Btc) shouldBe "19YqztHmspv2egyD6jQM3yn81x5t5krVdJ"
  }

  it should "return open orders" in {

    wireMockServer.stubFor(get(urlEqualTo("/orders"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody(
          """
            |[
            |  {
            |    "id": "24993088082542592",
            |    "clientOrderId": "",
            |    "symbol": "BTC_USDT",
            |    "state": "NEW",
            |    "accountType": "SPOT",
            |    "side": "SELL",
            |    "type": "MARKET",
            |    "timeInForce": "GTC",
            |    "quantity": "1.00",
            |    "price": "0.00",
            |    "avgPrice": "0.00",
            |    "amount": "0.00",
            |    "filledQuantity": "0.00",
            |    "filledAmount": "0.00",
            |    "createTime": 1646925216548,
            |    "updateTime": 1646925216548,
            |    "orderSource": "API",
            |    "loan": false
            |  },
            |  {
            |    "id": "21934611974062080",
            |    "clientOrderId": "123",
            |    "symbol": "XMR_USDT",
            |    "state": "NEW",
            |    "accountType": "SPOT",
            |    "side": "SELL",
            |    "type": "LIMIT",
            |    "timeInForce": "GTC",
            |    "quantity": "1.00",
            |    "price": "10.00",
            |    "avgPrice": "0.00",
            |    "amount": "0.00",
            |    "filledQuantity": "0.00",
            |    "filledAmount": "0.00",
            |    "createTime": 1646196019020,
            |    "updateTime": 1646196019020,
            |    "orderSource": "API",
            |    "loan": true
            |  }
            |]""".stripMargin)))

    val api = new PoloApiV2(DefaultConfiguration.PoloApi.Key, DefaultConfiguration.PoloApi.Secret, poloUrl, poloUrl)
    val list = api.returnOpenOrders().futureValue(Timeout(10 seconds))

    list should contain(PoloOrder(CurrencyPair(Asset.Tether, Asset.Xmr), "21934611974062080", BigDecimal("10.0"), BigDecimal("1.0")))
  }

  it should "place a sell order" in {
    wireMockServer.stubFor(get(urlEqualTo("/markets/USD_BTC"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody("""[ {
                    "symbol" : "XRP_USDT",
                    "baseCurrencyName" : "XRP",
                    "quoteCurrencyName" : "USDT",
                    "displayName" : "XRP/USDT",
                    "state" : "NORMAL",
                    "visibleStartTime" : 1659018819871,
                    "tradableStartTime" : 1659018819871,
                    "symbolTradeLimit" : {
                      "symbol" : "XRP_USDT",
                      "priceScale" : 4,
                      "quantityScale" : 4,
                      "amountScale" : 4,
                      "minQuantity" : "0.0001",
                      "minAmount" : "1",
                      "highestBid" : "0",
                      "lowestAsk" : "0"
                    }
                  } ]""")))

    wireMockServer.stubFor(post(urlEqualTo("/orders"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody(
          """{
            |  "id": "29772698821328896",
            |  "clientOrderId": "1234Abc"
            |}
            |""".stripMargin)))

    val api = new PoloApiV2(DefaultConfiguration.PoloApi.Key, DefaultConfiguration.PoloApi.Secret, poloUrl, poloUrl)
    api.sell(CurrencyPair(Asset.Btc, Asset.Usd), BigDecimal("1"), BigDecimal("1")).futureValue(Timeout(10 seconds))
  }

  it should "place a buy order" in {

    wireMockServer.stubFor(get(urlEqualTo("/markets/USD_BTC"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody("""[ {
                    "symbol" : "XRP_USDT",
                    "baseCurrencyName" : "XRP",
                    "quoteCurrencyName" : "USDT",
                    "displayName" : "XRP/USDT",
                    "state" : "NORMAL",
                    "visibleStartTime" : 1659018819871,
                    "tradableStartTime" : 1659018819871,
                    "symbolTradeLimit" : {
                      "symbol" : "XRP_USDT",
                      "priceScale" : 4,
                      "quantityScale" : 4,
                      "amountScale" : 4,
                      "minQuantity" : "0.0001",
                      "minAmount" : "1",
                      "highestBid" : "0",
                      "lowestAsk" : "0"
                    }
                  } ]""")))

    wireMockServer.stubFor(post(urlEqualTo("/orders"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody(
            """
            |{
            |  "id": "29772698821328896",
            |  "clientOrderId": "1234Abc"
            |}""".stripMargin)))

    val api = new PoloApiV2(DefaultConfiguration.PoloApi.Key, DefaultConfiguration.PoloApi.Secret, poloUrl, poloUrl)
    val order = api.buy(CurrencyPair(Asset.Btc, Asset.Usd), BigDecimal("1"), BigDecimal("1")).futureValue(Timeout(10 seconds))

    order.typeStr shouldBe "BUY"
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

    val api = new PoloApiV2(DefaultConfiguration.PoloApi.Key, DefaultConfiguration.PoloApi.Secret, poloUrl, poloUrl)
    val quoteList = api.returnTicker().futureValue(Timeout(10 seconds))

    quoteList.head.pair.left shouldBe Asset.Btc
    quoteList.head.pair.right shouldBe Asset.Usd
    quoteList.head.last shouldBe BigDecimal("0.0251")
  }

  it should "return trade history" in {
    wireMockServer.stubFor(get(urlMatching("/trades?(.*)"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody(
          """
            |[
            |    {
            |        "id": "62561238",
            |        "symbol": "BTC_USDT",
            |        "accountType": "SPOT",
            |        "orderId": "32164923987566592",
            |        "side": "BUY",
            |        "type": "LIMIT",
            |        "matchRole": "TAKER",
            |        "createTime": 1648635115525,
            |        "price": "11",
            |        "quantity": "0.5",
            |        "amount": "5.5",
            |        "feeCurrency": "USDT",
            |        "feeAmount": "0.007975",
            |        "pageId": "32164924331503616",
            |        "clientOrderId": "myOwnId-321"
            |    }
            |]
          """.stripMargin)))

    val api = new PoloApiV2(DefaultConfiguration.PoloApi.Key, DefaultConfiguration.PoloApi.Secret, poloUrl, poloUrl)
    val orderList = api.returnTradeHistory().futureValue(Timeout(10 seconds))

    val order = orderList.head
    val buy = order match {
      case b: Buy => b
    }

    buy.pair.right shouldBe Asset.Btc
    buy.price shouldBe BigDecimal(11.0)
    buy.quantity shouldBe BigDecimal(0.5)
  }

  it should "return chart data" in {
    wireMockServer.stubFor(get(urlMatching("/public\\?command=returnChartData\\&.*"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody(
          """
            |[
            |  {
            |    "date": "1405699200",
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

    val api = new PoloApiV2(DefaultConfiguration.PoloApi.Key, DefaultConfiguration.PoloApi.Secret, poloUrl, poloUrl)
    val chartData = api.returnChartData(CurrencyPair(Asset.Btc, Asset.Eth), 300,
      ZonedDateTime.parse("2017-01-01T01:00:00.000Z"), ZonedDateTime.parse("2017-02-01T01:00:00.000Z")).futureValue(Timeout(10 seconds))

    chartData.prices.length shouldNot be(0)

  }
}
