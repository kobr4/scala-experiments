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

  private def getPricesOrPair(pair: CurrencyPair, startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[PairPrices] = {
    if (pair.left == Asset.Usd)
      getPricesWithCache(pair.right)
    else
      getPairPrice(pair, startDate, endDate)
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
    case other => getYahooPricesWithCache(other.toString)
  }

  private def groupAndFilter(pairPrices: PairPrices, startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): List[BigDecimal] =
    pairPrices.filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)).groupByMonth.map(merge => merge._2.map(_.price).sum / merge._2.length)

  private def filter(pairPrices: PairPrices, startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) =
    pairPrices.filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate))

  def getPriceHistory(pair: CurrencyPair, startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[BigDecimal]] =
    getPricesOrPair(pair, startDate, endDate).map { prices => groupAndFilter(prices, startDate, endDate) }

  def getPairPrice(pair: CurrencyPair, startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[PairPrices] = {
    for {
      leftPrices <- getPricesWithCache(pair.left)
      rightPrices <- getPricesWithCache(pair.right)
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

  def priceTicker(exchange: SupportedExchange)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[Quote]] = {
    ExchangeApi(exchange).returnTicker()
  }
}
