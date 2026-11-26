package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{ aResponse, _ }
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.kobr4.tradebot.api.{ PolymarketApi, PolymarketEventsQuery, PolymarketMarketsQuery, PolymarketOrderBookLevel, PolymarketTag }
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ BeforeAndAfterEach, FlatSpec, Matchers }

import scala.concurrent.duration._

class PolymarketApiTest extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfterEach {

  val wireMockServer = new WireMockServer(options().dynamicPort())

  implicit val as = ActorSystem()
  implicit val am = ActorMaterializer()
  implicit val ec = as.dispatcher

  override def beforeEach {
    wireMockServer.start()
  }

  override def afterEach {
    wireMockServer.stop()
  }

  it should "build URLs with sorted and encoded query parameters" in {
    PolymarketApi.buildUrl(
      "https://gamma-api.polymarket.com/",
      "/markets",
      Map("slug" -> "bitcoin up/down", "active" -> "true", "limit" -> "1")) shouldBe
      "https://gamma-api.polymarket.com/markets?active=true&limit=1&slug=bitcoin+up%2Fdown"
  }

  it should "return markets" in {
    wireMockServer.stubFor(get(urlEqualTo("/markets?limit=1&slug=bitcoin+up%2Fdown"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody(
          """
            |[
            |  {
            |    "id": "1001",
            |    "question": "Will Bitcoin close above $100k?",
            |    "conditionId": "0xabc",
            |    "slug": "bitcoin up/down",
            |    "active": true,
            |    "closed": false,
            |    "archived": false,
            |    "volume": "1234.56",
            |    "liquidity": 789.01,
            |    "outcomes": "[\"Yes\",\"No\"]",
            |    "outcomePrices": ["0.42", "0.58"],
            |    "clobTokenIds": ["token-yes", "token-no"]
            |  }
            |]
            |""".stripMargin)))

    val api = new PolymarketApi(s"http://127.0.0.1:${wireMockServer.port()}", s"http://127.0.0.1:${wireMockServer.port()}")
    val market = api.marketBySlug("bitcoin up/down").futureValue(Timeout(10 seconds)).get

    market.id shouldBe Some("1001")
    market.question shouldBe "Will Bitcoin close above $100k?"
    market.conditionId shouldBe Some("0xabc")
    market.slug shouldBe Some("bitcoin up/down")
    market.active shouldBe true
    market.closed shouldBe false
    market.archived shouldBe false
    market.volume shouldBe Some(BigDecimal("1234.56"))
    market.liquidity shouldBe Some(BigDecimal("789.01"))
    market.outcomes shouldBe List("Yes", "No")
    market.outcomePrices shouldBe List(BigDecimal("0.42"), BigDecimal("0.58"))
    market.clobTokenIds shouldBe List("token-yes", "token-no")
  }

  it should "return events with nested markets" in {
    wireMockServer.stubFor(get(urlEqualTo("/events?limit=1&slug=bitcoin-event"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody(
          """
            |[
            |  {
            |    "id": "2001",
            |    "title": "Bitcoin event",
            |    "slug": "bitcoin-event",
            |    "active": true,
            |    "closed": false,
            |    "archived": false,
            |    "markets": [
            |      {
            |        "id": "1001",
            |        "question": "Will Bitcoin close above $100k?",
            |        "active": true,
            |        "closed": false,
            |        "archived": false,
            |        "outcomes": ["Yes", "No"],
            |        "outcomePrices": ["0.42", "0.58"],
            |        "clobTokenIds": ["token-yes", "token-no"]
            |      }
            |    ]
            |  }
            |]
            |""".stripMargin)))

    val api = new PolymarketApi(s"http://127.0.0.1:${wireMockServer.port()}", s"http://127.0.0.1:${wireMockServer.port()}")
    val event = api.eventBySlug("bitcoin-event").futureValue(Timeout(10 seconds)).get

    event.id shouldBe Some("2001")
    event.title shouldBe "Bitcoin event"
    event.slug shouldBe Some("bitcoin-event")
    event.active shouldBe true
    event.markets should have size 1
    event.markets.head.clobTokenIds shouldBe List("token-yes", "token-no")
  }

  it should "return an order book" in {
    wireMockServer.stubFor(get(urlEqualTo("/book?token_id=token-yes"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody(
          """
            |{
            |  "market": "0xabc",
            |  "asset_id": "token-yes",
            |  "timestamp": "1700000000",
            |  "hash": "0xhash",
            |  "bids": [
            |    { "price": "0.41", "size": "100" }
            |  ],
            |  "asks": [
            |    { "price": "0.43", "size": "80.5" }
            |  ]
            |}
            |""".stripMargin)))

    val api = new PolymarketApi(s"http://127.0.0.1:${wireMockServer.port()}", s"http://127.0.0.1:${wireMockServer.port()}")
    val orderBook = api.orderBook("token-yes").futureValue(Timeout(10 seconds))

    orderBook.market shouldBe Some("0xabc")
    orderBook.assetId shouldBe "token-yes"
    orderBook.timestamp shouldBe Some("1700000000")
    orderBook.hash shouldBe Some("0xhash")
    orderBook.bids shouldBe List(PolymarketOrderBookLevel(BigDecimal("0.41"), BigDecimal("100")))
    orderBook.asks shouldBe List(PolymarketOrderBookLevel(BigDecimal("0.43"), BigDecimal("80.5")))
  }

  it should "convert query objects to query parameters without empty values" in {
    PolymarketMarketsQuery(limit = Some(10), active = Some(true), slug = Some("slug")).toQueryParams shouldBe
      Map("limit" -> "10", "active" -> "true", "slug" -> "slug")

    PolymarketEventsQuery(limit = None, archived = Some(false), tagId = Some(12)).toQueryParams shouldBe
      Map("archived" -> "false", "tag_id" -> "12")
  }

  it should "return tags" in {
    wireMockServer.stubFor(get(urlEqualTo("/tags?limit=100"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody(
          """
            |[
            |  { "id": 1, "label": "Politics", "slug": "politics" },
            |  { "id": "2", "label": "Crypto" }
            |]
            |""".stripMargin)))

    val api = new PolymarketApi(s"http://127.0.0.1:${wireMockServer.port()}", s"http://127.0.0.1:${wireMockServer.port()}")
    val tags = api.tags().futureValue(Timeout(10 seconds))

    tags should contain (PolymarketTag(1, "Politics", Some("politics")))
    tags should contain (PolymarketTag(2, "Crypto", None))
  }
}