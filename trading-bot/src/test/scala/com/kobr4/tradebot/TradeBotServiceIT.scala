package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{PoloAPIInterface, PoloApi}
import com.kobr4.tradebot.engine.SafeStrategy
import com.kobr4.tradebot.model.Asset
import com.kobr4.tradebot.services.{PriceService, TradeBotService, TradingOps}
import org.scalatest.FlatSpec
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration._


class TradeBotServiceIT extends FlatSpec with ScalaFutures with MockitoSugar {

  implicit val as = ActorSystem()
  implicit val am = ActorMaterializer()
  implicit val ec = as.dispatcher

  it should "run and trade one asset" in {

    val poloApi = new PoloApi()
    val apiMock = mock[PoloAPIInterface]
    val tradingOps = new TradingOps(apiMock)

    val result = for {
     priceData <- PriceService.getPriceData(Asset.Btc)
     result <- TradeBotService.runAndTrade(Asset.Btc,priceData, SafeStrategy, poloApi, tradingOps)
    } yield result

    result.futureValue(Timeout(10 seconds))

  }

  it should "run and trade multiple asset" in {

    val poloApi = new PoloApi()
    val apiMock = mock[PoloAPIInterface]
    val tradingOps = new TradingOps(apiMock)

    val result = TradeBotService.runMapAndTrade(Map(Asset.Btc -> BigDecimal(0.5), Asset.Eth -> BigDecimal(0.5)), SafeStrategy, poloApi, tradingOps)

    result.futureValue(Timeout(10 seconds))

  }
}
