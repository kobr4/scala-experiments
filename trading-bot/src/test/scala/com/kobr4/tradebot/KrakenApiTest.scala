package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{ aResponse, post, urlEqualTo }
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.kobr4.tradebot.api.{ CurrencyPair, KrakenApi, PoloApi, PoloOrder }
import com.kobr4.tradebot.model.Asset
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.{ BeforeAndAfterEach, FlatSpec, Matchers }
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._
import scala.util.Random

class KrakenApiTest extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfterEach {

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

    wireMockServer.stubFor(post(urlEqualTo("/0/private/OpenOrders"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody(
          """{
            |   "error":[
            |
            |   ],
            |   "result":{
            |      "open":
            |         {
            |            "O7ICPO-F4CLJ-MVBLHC":{
            |               "refid":{
            |
            |               },
            |               "userref":{
            |
            |               },
            |               "status":"open",
            |               "opentm":1373750306.9819,
            |               "starttm":0,
            |               "expiretm":0,
            |               "descr":{
            |                  "pair" : "XBTUSD",
            |                  "order":"sell 3.00000000 XBTUSD @ limit 500.00000"
            |               },
            |               "vol":3.00000000,
            |               "vol_exec":0.00000000,
            |               "cost":0.00000,
            |               "fee":0.00000,
            |               "price":1.00000,
            |               "misc":{
            |
            |               },
            |               "oflags":{
            |
            |               }
            |            }
            |         }
            |
            |   }
            |}""".stripMargin)))

    val api = new KrakenApi(DefaultConfiguration.KrakenApi.Key, DefaultConfiguration.KrakenApi.Secret, s"http://127.0.0.1:$port")
    val list = api.returnOpenOrders().futureValue(Timeout(10 seconds))

    list should contain(PoloOrder(CurrencyPair(Asset.Usd, Asset.Btc), "O7ICPO-F4CLJ-MVBLHC", BigDecimal(1), BigDecimal(3)))
  }

  it should "return a valid form request" in {

    val body = KrakenApi.BuySell.build(
      1L,
      com.kobr4.tradebot.api.KrakenCurrencyPairHelper.toString(CurrencyPair(Asset.Usd, Asset.Btc)),
      BigDecimal(1.0), BigDecimal("0.0000001700"), false).fields.toString

    body should be("nonce=1&price=1.0&ordertype=limit&pair=XXBTZUSD&type=sell&volume=0.0000001700")
  }

}
