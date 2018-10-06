package com.kobr4.tradebot.services

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api._
import com.kobr4.tradebot.model.{ Asset, PairPrice, PairPrices }
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

  private def getPricesWithCache(asset: Asset)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[PairPrices] = asset match {
    case Asset.Btc => getCoinmetricsPricesWithCache(CoinMetricsPriceUrl.btc)
    case Asset.Eth => getCoinmetricsPricesWithCache(CoinMetricsPriceUrl.eth)
    case Asset.Xmr => getCoinmetricsPricesWithCache(CoinMetricsPriceUrl.xmr)
    case Asset.Doge => getCoinmetricsPricesWithCache(CoinMetricsPriceUrl.doge)
    case Asset.Xem => getCoinmetricsPricesWithCache(CoinMetricsPriceUrl.xem)
    case Asset.Xrp => getCoinmetricsPricesWithCache(CoinMetricsPriceUrl.xrp)
    case other => getYahooPricesWithCache(other.toString)
  }

  private def groupAndFilter(pairPrices: PairPrices, startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): List[BigDecimal] =
    pairPrices.filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)).groupByMonth.map(merge => merge._2.map(_.price).sum / merge._2.length)

  private def filter(pairPrices: PairPrices, startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) =
    pairPrices.filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate))

  def getPriceHistory(asset: Asset, startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[BigDecimal]] =
    getPricesWithCache(asset).map { prices => groupAndFilter(prices, startDate, endDate) }

  def getPriceAt(asset: Asset, date: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[BigDecimal] =
    getPricesWithCache(asset).map { pairPrices =>

      pairPrices.prices.map(p => (Math.abs(p.date.toEpochSecond - date.toEpochSecond), p.price)).minBy(_._1)._2
    }

  def getMovingAverageHistory(asset: Asset, startDate: ZonedDateTime, endDate: ZonedDateTime, days: Int)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[BigDecimal]] =
    getPricesWithCache(asset).map(_.movingAverage(days).filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)).groupByMonth.map(merge => merge._2.map(_.price).sum / merge._2.length))

  def getWheightedMovingAverageHistory(asset: Asset, startDate: ZonedDateTime, endDate: ZonedDateTime, days: Int)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[BigDecimal]] =
    getPricesWithCache(asset).map(_.weightedMovingAverage(days).filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)).groupByMonth.map(merge => merge._2.map(_.price).sum / merge._2.length))

  def getPriceData(asset: Asset, startDate: ZonedDateTime, endDate: ZonedDateTime = ZonedDateTime.now())(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[PairPrices] =
    getPricesWithCache(asset).map(filter(_, startDate, endDate))

  def priceTicker(exchange: SupportedExchange)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[Quote]] = {
    exchange match {
      case Poloniex =>
        val api = new PoloApi()
        api.returnTicker()
      case Kraken =>
        val api = new KrakenApi()
        api.returnTicker()
    }

  }
}
