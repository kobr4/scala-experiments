package com.kobr4.tradebot.services

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api._
import com.kobr4.tradebot.model.{ Asset, EthUsd, PairPrice, PairPrices }
import scalacache.Cache
import scalacache.guava.GuavaCache

import scala.concurrent.{ ExecutionContext, Future }

object PriceService {

  implicit val guavaCache: Cache[PairPrices] = GuavaCache[PairPrices]

  private def getCoinmetricsPricesWithCache(url: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[PairPrices] = {
    import scalacache._
    import scalacache.modes.scalaFuture._

    import scala.concurrent.duration._

    cachingF(url)(ttl = Some(2 hours)) {
      PairPrice.fromUrlAsync(url)
    }
  }

  private def getYahooPricesWithCache(code: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[PairPrices] = {
    import scalacache._
    import scalacache.modes.scalaFuture._

    import scala.concurrent.duration._

    cachingF(code)(ttl = Some(2 hours)) {
      YahooFinanceApi.fetchPriceData(code)
    }
  }

  private def getPricesOrPair(pair: CurrencyPair, startDate: ZonedDateTime, endDate: ZonedDateTime, withCache: Boolean = true)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[PairPrices] = {
    if (pair.left == Asset.Usd && withCache)
        getPricesWithCache(pair.right)
    else if (pair.left == Asset.Usd) {
      getPricesWithoutCache(pair.right)
    } else
      getPairPrice(pair, startDate, endDate, withCache)
  }

  private def getPricesWithCache(asset: Asset)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[PairPrices] = asset match {
    case Asset.Btc => getCoinmetricsPricesWithCache(CoinMetricsPriceUrl.btc)
    case Asset.Eth => getCoinmetricsPricesWithCache(CoinMetricsPriceUrl.eth)
    case Asset.Xmr => getCoinmetricsPricesWithCache(CoinMetricsPriceUrl.xmr)
    case Asset.Doge => getCoinmetricsPricesWithCache(CoinMetricsPriceUrl.doge)
    case Asset.Xem => getCoinmetricsPricesWithCache(CoinMetricsPriceUrl.xem)
    case Asset.Xrp => getCoinmetricsPricesWithCache(CoinMetricsPriceUrl.xrp)
    case Asset.Xlm => getCoinmetricsPricesWithCache(CoinMetricsPriceUrl.xlm)
    case Asset.Dgb => getCoinmetricsPricesWithCache(CoinMetricsPriceUrl.dgb)
    case Asset.Ada => getCoinmetricsPricesWithCache(CoinMetricsPriceUrl.ada)
    case Asset.Ltc => getCoinmetricsPricesWithCache(CoinMetricsPriceUrl.ltc)
    case Asset.Zec => getCoinmetricsPricesWithCache(CoinMetricsPriceUrl.zec)
    case Asset.Dash => getCoinmetricsPricesWithCache(CoinMetricsPriceUrl.dash)
    case Asset.Bch => getCoinmetricsPricesWithCache(CoinMetricsPriceUrl.bch)
    case Asset.Maid => getCoinmetricsPricesWithCache(CoinMetricsPriceUrl.maid)
    case other => getYahooPricesWithCache(other.toString)
  }

  private def getPricesWithoutCache(asset: Asset)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[PairPrices] = asset match {
    case Asset.Btc => PairPrice.fromUrlAsync(CoinMetricsPriceUrl.btc)
    case Asset.Eth => PairPrice.fromUrlAsync(CoinMetricsPriceUrl.eth)
    case Asset.Xmr => PairPrice.fromUrlAsync(CoinMetricsPriceUrl.xmr)
    case Asset.Doge => PairPrice.fromUrlAsync(CoinMetricsPriceUrl.doge)
    case Asset.Xem => PairPrice.fromUrlAsync(CoinMetricsPriceUrl.xem)
    case Asset.Xrp => PairPrice.fromUrlAsync(CoinMetricsPriceUrl.xrp)
    case Asset.Xlm => PairPrice.fromUrlAsync(CoinMetricsPriceUrl.xlm)
    case Asset.Dgb => PairPrice.fromUrlAsync(CoinMetricsPriceUrl.dgb)
    case Asset.Ada => PairPrice.fromUrlAsync(CoinMetricsPriceUrl.ada)
    case Asset.Ltc => PairPrice.fromUrlAsync(CoinMetricsPriceUrl.ltc)
    case Asset.Zec => PairPrice.fromUrlAsync(CoinMetricsPriceUrl.zec)
    case Asset.Dash => PairPrice.fromUrlAsync(CoinMetricsPriceUrl.dash)
    case Asset.Bch => PairPrice.fromUrlAsync(CoinMetricsPriceUrl.bch)
    case Asset.Maid => PairPrice.fromUrlAsync(CoinMetricsPriceUrl.maid)
    case other => getYahooPricesWithCache(other.toString)
  }

  private def groupAndFilter(pairPrices: PairPrices, startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): List[BigDecimal] =
    pairPrices.filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)).groupByMonth.map(merge => merge._2.map(_.price).sum / merge._2.length)

  private def filter(pairPrices: PairPrices, startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) =
    pairPrices.filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate))

  def getPriceHistory(pair: CurrencyPair, startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[BigDecimal]] =
    getPricesOrPair(pair, startDate, endDate).map { prices => groupAndFilter(prices, startDate, endDate) }

  def getPairPrice(pair: CurrencyPair, startDate: ZonedDateTime, endDate: ZonedDateTime, withCache: Boolean = true)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[PairPrices] = {
    for {
      leftPrices <- if (withCache) getPricesWithCache(pair.left) else getPricesWithoutCache(pair.left)
      rightPrices <- if (withCache) getPricesWithCache(pair.right) else getPricesWithoutCache(pair.right)
    } yield {
      val pairPriceList = leftPrices.filter(startDate, endDate).prices.zip(rightPrices.filter(startDate, endDate).prices).
        map(pairTuple => EthUsd(pairTuple._1.date, pairTuple._2.price / pairTuple._1.price))
      PairPrices(pairPriceList)
    }
  }

  def getPriceAt(asset: Asset, date: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[BigDecimal] =
    getPricesWithCache(asset).map { pairPrices =>
      pairPrices.prices.map(p => (Math.abs(p.date.toEpochSecond - date.toEpochSecond), p.price)).minBy(_._1)._2
    }

  def getMovingAverageHistory(pair: CurrencyPair, startDate: ZonedDateTime, endDate: ZonedDateTime, days: Int)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[BigDecimal]] =
    getPricesOrPair(pair, startDate, endDate).map(_.movingAverage(days).filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)).groupByMonth.map(merge => merge._2.map(_.price).sum / merge._2.length))

  def getWeightedMovingAverageHistory(pair: CurrencyPair, startDate: ZonedDateTime, endDate: ZonedDateTime, days: Int)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[BigDecimal]] =
    getPricesOrPair(pair, startDate, endDate).map(_.weightedMovingAverage(days).filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)).groupByMonth.map(merge => merge._2.map(_.price).sum / merge._2.length))

  def getPriceData(pair: CurrencyPair, startDate: ZonedDateTime, endDate: ZonedDateTime = ZonedDateTime.now())(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[PairPrices] =
    getPricesOrPair(pair, startDate, endDate)

  def getPriceData(asset: Asset)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[PairPrices] =
    getPricesWithCache(asset)

  def getPriceDataWithoutCache(asset: Asset)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[PairPrices] =
    getPricesWithoutCache(asset)

  def priceTicker(exchange: SupportedExchange)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[Quote]] = {
    ExchangeApi(exchange).returnTicker()
  }
}
