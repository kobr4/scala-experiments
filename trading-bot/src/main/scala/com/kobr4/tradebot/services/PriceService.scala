package com.kobr4.tradebot.services

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot._
import com.kobr4.tradebot.api.{ PoloApi, Quote, YahooFinanceApi }
import com.kobr4.tradebot.model.{ PairPrice, PairPrices }
import scalacache.Cache
import scalacache.guava.GuavaCache

import scala.concurrent.{ ExecutionContext, Future }

object PriceService {

  implicit val guavaCache: Cache[PairPrices] = GuavaCache[PairPrices]

  private val ethPricesUrl = "https://coinmetrics.io/data/eth.csv"
  private val btcPricesUrl = "https://coinmetrics.io/data/btc.csv"
  private val xmrPricesUrl = "https://coinmetrics.io/data/xmr.csv"

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
    case Asset.Btc => getCoinmetricsPricesWithCache(btcPricesUrl)
    case Asset.Eth => getCoinmetricsPricesWithCache(ethPricesUrl)
    case Asset.Xmr => getCoinmetricsPricesWithCache(xmrPricesUrl)
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

  def getPriceData(asset: Asset, startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[PairPrices] =
    getPricesWithCache(asset).map(filter(_, startDate, endDate))

  def priceTicker()(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[Quote]] = {
    val api = new PoloApi()
    api.returnTicker()
  }
}
