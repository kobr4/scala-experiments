package com.kobr4.tradebot.scheduler

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{ ExchangeApi, Kraken }
import com.kobr4.tradebot.model.Asset

import scala.concurrent.ExecutionContext

class KrakenDailyJob extends TradeBotDailyJob {

  override def getExchangeInterface()(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): ExchangeApi =
    ExchangeApi(Kraken)

  override def getAssetMap(): Map[Asset, BigDecimal] = KrakenDailyJob.assetMap

}

object KrakenDailyJob {

  val assetMap: Map[Asset, BigDecimal] = Map(Asset.Btc -> BigDecimal(0.4), Asset.Eth -> BigDecimal(0.3),
    Asset.Xmr -> BigDecimal(0.3))
}
