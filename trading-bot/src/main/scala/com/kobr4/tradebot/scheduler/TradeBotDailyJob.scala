package com.kobr4.tradebot.scheduler

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{ ExchangeApi, Poloniex }
import com.kobr4.tradebot.engine.{ SafeStrategy, Strategy }
import com.kobr4.tradebot.model.{ Asset, Order }
import com.kobr4.tradebot.services.{ TradeBotService, TradingOps }
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

class TradeBotDailyJob extends SchedulerJobInterface with StrictLogging {

  def getExchangeInterface()(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): ExchangeApi =
    ExchangeApi(Poloniex)

  def getAssetMap(): Map[Asset, BigDecimal] = TradeBotDailyJob.assetMap

  def getStrategy(): Strategy = SafeStrategy

  override def run()(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[Order]] = {

    logger.info("Running Job: TradeBotDailyJob")

    val exchangeApi = getExchangeInterface()

    val tradingOps = new TradingOps(exchangeApi)

    val eventualResult = for {
      _ <- tradingOps.cancelAllOpenOrders()
      orderList <- TradeBotService.runMapAndTrade(getAssetMap(), getStrategy(), exchangeApi, tradingOps, Asset.Usd)
    } yield {
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

object TradeBotDailyJob {
  val assetMap: Map[Asset, BigDecimal] = Map(
    Asset.Eth -> BigDecimal(0.4),
    Asset.Xmr -> BigDecimal(0.2), Asset.Xrp -> BigDecimal(0.2), Asset.Xlm -> BigDecimal(0.1), Asset.Doge -> BigDecimal(0.1))
}
