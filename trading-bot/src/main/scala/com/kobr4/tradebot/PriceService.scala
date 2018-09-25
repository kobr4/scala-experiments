package com.kobr4.tradebot

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.{ ExecutionContext, Future }

object PriceService {

  private val ethPricesUrl = "https://coinmetrics.io/data/eth.csv"
  private val btcPricesUrl = "https://coinmetrics.io/data/btc.csv"

  private def fetchPrice(url: String, startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[BigDecimal]] =
    PairPrice.fromUrlAsync(url).map { pairPrices =>
      pairPrices.filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)).groupByMonth.map(merge => merge._2.map(_.price).sum / merge._2.length)
    }

  private def fetchPriceAndFilter(url: String, startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) =
    PairPrice.fromUrlAsync(url).map { pairPrices => pairPrices.filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)) }

  def getBtcPriceHistory(startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[BigDecimal]] =
    fetchPrice(btcPricesUrl, startDate, endDate)

  def getEthPriceHistory(startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[BigDecimal]] =
    fetchPrice(ethPricesUrl, startDate, endDate)

  def getBtcPriceData(startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[PairPrices] =
    fetchPriceAndFilter(btcPricesUrl, startDate, endDate)

  def priceTicker()(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[Quote]] = {
    val api = new PoloApi()
    api.returnTicker()
  }
}
