package com.kobr4.tradebot

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.common.cache.CacheBuilder
import scalacache.Entry
import scalacache.guava.GuavaCache

import scala.concurrent.{ ExecutionContext, Future }

object PriceService {

  val underlyingGuavaCache = CacheBuilder.newBuilder().maximumSize(10000L).build[String, Entry[PairPrices]]
  implicit val guavaCache = GuavaCache(underlyingGuavaCache)

  private val ethPricesUrl = "https://coinmetrics.io/data/eth.csv"
  private val btcPricesUrl = "https://coinmetrics.io/data/btc.csv"

  private def getPairPricesWithCache(url: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[PairPrices] = {
    import scalacache._
    import scalacache.modes.scalaFuture._

    import scala.concurrent.duration._
    cachingF(url)(ttl = Some(2 hours)) {
      PairPrice.fromUrlAsync(url)
    }
  }

  private def fetchPrice(url: String, startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[BigDecimal]] =
    getPairPricesWithCache(url).map { pairPrices =>
      pairPrices.filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)).groupByMonth.map(merge => merge._2.map(_.price).sum / merge._2.length)
    }

  private def fetchPriceAndFilter(url: String, startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) =
    getPairPricesWithCache(url).map { pairPrices => pairPrices.filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)) }

  def getPriceHistory(asset: Asset, startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[BigDecimal]] =
    asset match {
      case Asset.Btc => fetchPrice(btcPricesUrl, startDate, endDate)
      case Asset.Eth => fetchPrice(ethPricesUrl, startDate, endDate)
    }

  def getPriceAt(asset: Asset, date: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[BigDecimal] =
    (asset match {
      case Asset.Btc => getPairPricesWithCache(btcPricesUrl)
      case Asset.Eth => getPairPricesWithCache(ethPricesUrl)
    }).map { pairPrices =>
      pairPrices.filter(pairPrices => pairPrices.date.getYear == date.getYear && pairPrices.date.getDayOfYear == date.getDayOfYear).prices.headOption.getOrElse(
        pairPrices.filter(pairPrices => pairPrices.date.getYear == date.getYear && pairPrices.date.getDayOfYear == date.getDayOfYear - 1).prices.head).price
    }

  def getMovingAverageHistory(asset: Asset, startDate: ZonedDateTime, endDate: ZonedDateTime, days: Int)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[BigDecimal]] =
    asset match {
      case Asset.Btc => getPairPricesWithCache(btcPricesUrl).map(_.movingAverage(days).filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)).groupByMonth.map(merge => merge._2.map(_.price).sum / merge._2.length))
      case Asset.Eth => getPairPricesWithCache(ethPricesUrl).map(_.movingAverage(days).filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)).groupByMonth.map(merge => merge._2.map(_.price).sum / merge._2.length))
    }

  def getPriceData(asset: Asset, startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[PairPrices] =
    asset match {
      case Asset.Btc => fetchPriceAndFilter(btcPricesUrl, startDate, endDate)
      case Asset.Eth => fetchPriceAndFilter(ethPricesUrl, startDate, endDate)
    }

  def priceTicker()(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[Quote]] = {
    val api = new PoloApi()
    api.returnTicker()
  }
}
