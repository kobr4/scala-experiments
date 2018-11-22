package com.kobr4.tradebot.scheduler

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{ ExchangeApi, Poloniex }
import com.kobr4.tradebot.engine._
import com.kobr4.tradebot.model.{ Asset, Order }
import com.kobr4.tradebot.services.{ TradeBotService, TradingOps }
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

class PoloniexBtcDailyJob extends SchedulerJobInterface with StrictLogging {

  def getExchangeInterface()(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): ExchangeApi =
    ExchangeApi(Poloniex)

  def getAssetMap(): Map[Asset, BigDecimal] = TradeBotDailyJob.assetMap

  override def run()(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[Order]] = {

    logger.info("Running Job: PoloniexBtcDailyJob")

    val exchangeApi = getExchangeInterface()

    val tradingOps = new TradingOps(exchangeApi)

    val strategy = GeneratedStrategy(
      List(WhenAboveMovingAverage(10), WhenHigh(20)),
      List(WhenAboveMovingAverage(30), WhenBelowMovingAverage(20)))

    val eventualResult = TradeBotService.runMapAndTrade(getAssetMap(), strategy, exchangeApi, tradingOps, Asset.Btc).map { orderList =>
      orderList.map(order => {
        logger.info(order.toString)
        order
      })
    }

    eventualResult.recover {
      case NonFatal(t) =>
        logger.error("Job failed with error: {}", t.getMessage)

    }

    eventualResult
  }
}

object PoloniexBtcDailyJob {
  val assetMap: Map[Asset, BigDecimal] = Map(
    Asset.Bch -> BigDecimal(0.2),
    Asset.Ltc -> BigDecimal(0.2),
    Asset.Dgb -> BigDecimal(0.2),
    Asset.Dash -> BigDecimal(0.2),
    Asset.Xem -> BigDecimal(0.2),
    Asset.Zec -> BigDecimal(0.2),
    Asset.Maid -> BigDecimal(0.2))

}
