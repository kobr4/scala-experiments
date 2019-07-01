package com.kobr4.tradebot.scheduler

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{ Binance, CurrencyPair, ExchangeApi }
import com.kobr4.tradebot.model.Asset

import scala.concurrent.ExecutionContext

class BinanceDailyJob extends TradeBotDailyJob {

  override def getExchangeInterface()(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): ExchangeApi =
    ExchangeApi(Binance)

  override def getBaseAsset() = BinanceDailyJob.baseAsset

  override def getAssetMap(): Map[Asset, BigDecimal] = BinanceDailyJob.assetMap

}

object BinanceDailyJob {

  val assetMap: Map[Asset, BigDecimal] = Map(Asset.Btc -> BigDecimal(0.4), Asset.Eth -> BigDecimal(0.3))

  val baseAsset = Asset.Tether

  def pairList = assetMap.keys.toList.map(asset => CurrencyPair(baseAsset, asset))
}
