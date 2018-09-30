package com.kobr4.tradebot

import java.time.{ LocalDate, LocalTime, ZoneId, ZonedDateTime }

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.AppNoRun.formatter

import scala.concurrent.{ ExecutionContext, Future }

sealed trait PairPrice {
  val date: ZonedDateTime
  val price: BigDecimal
}

case class EthUsd(date: ZonedDateTime, price: BigDecimal) extends PairPrice

case class PairPrices(prices: List[PairPrice]) {

  def movingAverage(base: ZonedDateTime, days: Int): Option[BigDecimal] = {
    val minDate = base.minusDays(days)
    val priceSubset = prices.filter(price => price.date.isAfter(minDate) && price.date.isBefore(base))
    priceSubset.length match {
      case 0 => None
      case len => Some(priceSubset.map(_.price).sum / len)
    }
  }

  def currentPrice(current: ZonedDateTime): BigDecimal = {
    prices.reduce((p1, p2) =>
      if (Math.abs(p1.date.toEpochSecond - current.toEpochSecond) < Math.abs(p2.date.toEpochSecond - current.toEpochSecond))
        p1
      else p2).price
  }

  def groupByYear: List[(Int, List[PairPrice])] = prices.groupBy(p => p.date.getYear).toList.sortBy(_._1)

  def groupByMonth: List[(Int, List[PairPrice])] = prices.groupBy(p => p.date.getYear * 100 + p.date.getMonthValue).toList.sortBy(_._1)

  def filter(f: PairPrice => Boolean): PairPrices = PairPrices(prices.filter(f))

  def movingAverage(days: Int): PairPrices = {
    PairPrices(prices.map { price => EthUsd(price.date, movingAverage(price.date, days).getOrElse(BigDecimal(0))) })
  }

  def filter(startDate: ZonedDateTime, endDate: ZonedDateTime): PairPrices =
    filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate))

}

object PairPrice {
  def fromUrl(s: String): PairPrices = {
    val bufferedSource = io.Source.fromURL(s)

    val priceLines = bufferedSource.getLines.toList
    val priceLineId = priceLines.head.split(',').zipWithIndex.find(_._1 == "price(USD)").map(_._2).getOrElse(throw new RuntimeException("Invalid file"))
    val prices =
      for (line <- priceLines.tail) yield {
        val splitted = line.split(',')
        val date = LocalDate.parse(splitted(0), formatter)
        val time = LocalTime.MIDNIGHT
        EthUsd(
          ZonedDateTime.of(date, time, ZoneId.of("UTC")),
          if (splitted(priceLineId) != "") BigDecimal(splitted(priceLineId)) else BigDecimal(0))
      }
    PairPrices(prices)
  }

  private def httpGetRequest(url: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    Http().singleRequest(HttpRequest(uri = url)).flatMap { response =>
      Unmarshal(response.entity).to[String]
    }
  }

  def fromString(csvString: String, priceColumn: String): PairPrices = {
    val priceLines = csvString.lines.toList
    val priceLineId = priceLines.head.split(',').zipWithIndex.find(_._1 == priceColumn).map(_._2).getOrElse(throw new RuntimeException("Invalid file"))
    val prices =
      for (line <- priceLines.tail) yield {
        val splitted = line.split(',')
        val date = LocalDate.parse(splitted(0), formatter)
        val time = LocalTime.MIDNIGHT
        EthUsd(
          ZonedDateTime.of(date, time, ZoneId.of("UTC")),
          if (splitted(priceLineId) != "") BigDecimal(splitted(priceLineId)) else BigDecimal(0))
      }
    PairPrices(prices)
  }

  def fromUrlAsync(url: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[PairPrices] = {
    httpGetRequest(url).map(fromString(_, "price(USD)"))
  }
}