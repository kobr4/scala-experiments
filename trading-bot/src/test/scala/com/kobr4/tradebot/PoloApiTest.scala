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
    //WireMock.configureFor(Host, Port)
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

}
