package com.kobr4.tradebot.scheduler

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{ ExchangeApi, Poloniex }
import com.kobr4.tradebot.engine.SafeStrategy
import com.kobr4.tradebot.model.Asset
import com.kobr4.tradebot.services.{ TradeBotService, TradingOps }
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

class TradeBotDailyJob extends SchedulerJobInterface with StrictLogging {

  def getExchangeInterface()(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): ExchangeApi =
    ExchangeApi(Poloniex)

  def getAssetMap(): Map[Asset, BigDecimal] = TradeBotDailyJob.assetMap

  override def run()(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Unit = {

    logger.info("Running Job: TradeBotDailyJob")

    val exchangeApi = getExchangeInterface()

    val tradingOps = new TradingOps(exchangeApi)

    val eventualResult = TradeBotService.runMapAndTrade(getAssetMap(), SafeStrategy, exchangeApi, tradingOps).map { orderList =>
      orderList.foreach(order => logger.info(order.toString))
    }

    eventualResult.onComplete {
      case Success(v) =>
        logger.info("Job ran successfully")
      case Failure(f) =>
        logger.error("Job failed with error: {}", f.getMessage)
    }
  }
}

object TradeBotDailyJob {
  val assetMap: Map[Asset, BigDecimal] = Map(Asset.Btc -> BigDecimal(0.3), Asset.Eth -> BigDecimal(0.3),
    Asset.Xmr -> BigDecimal(0.2), Asset.Xlm -> BigDecimal(0.1), Asset.Doge -> BigDecimal(0.1))
}
