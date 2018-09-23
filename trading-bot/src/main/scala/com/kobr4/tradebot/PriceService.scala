package com.kobr4.tradebot

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.{ ExecutionContext, Future }

object PriceService {

  private val ethPricesUrl = "https://coinmetrics.io/data/eth.csv"
  private val btcPrices = "https://coinmetrics.io/data/btc.csv"

  def getBtcPriceHistory(startDate: ZonedDateTime, endDate: ZonedDateTime)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[BigDecimal]] =
    PairPrice.fromUrlAsync(btcPrices).map { pairPrices =>
      pairPrices.filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate)).groupByMonth.map(merge => merge._2.map(_.price).sum / merge._2.length)
    }

  def getEthPriceHistory()(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[List[BigDecimal]] =
    PairPrice.fromUrlAsync(ethPricesUrl).map(_.prices.map(_.price))

}
