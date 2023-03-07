package com.kobr4.tradebot.model

import java.time.format.DateTimeFormatter
import java.time._

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ ExecutionContext, Future }

case class PairPrice(date: ZonedDateTime, price: BigDecimal)

case class PairPrices(prices: List[PairPrice]) {

  def movingAverage(base: ZonedDateTime, days: Int): Option[BigDecimal] = {
    val minDate = base.minusDays(days)
    val priceSubset = prices.filter(price => price.date.isAfter(minDate) && price.date.isBefore(base))
    priceSubset.length match {
      case 0 => None
      case len => Some(priceSubset.map(_.price).sum / len)
    }
  }

  def weightedMovingAverage(base: ZonedDateTime, days: Int): Option[BigDecimal] = {
    val minDate = base.minusDays(days)
    val priceSubset = prices.filter(price => price.date.isAfter(minDate) && price.date.isBefore(base))
    priceSubset.length match {
      case 0 => None
      case len =>
        val divFactor = priceSubset.map(pairPrice => days - Period.between(minDate.toLocalDate, pairPrice.date.toLocalDate).getDays).sum
        Some(
          priceSubset.map(pairPrice =>
            (days - Period.between(minDate.toLocalDate, pairPrice.date.toLocalDate).getDays) * pairPrice.price).sum / divFactor)
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
    PairPrices(prices.map { price => PairPrice(price.date, movingAverage(price.date, days).getOrElse(BigDecimal(0))) })
  }

  def weightedMovingAverage(days: Int): PairPrices = {
    PairPrices(prices.map { price => PairPrice(price.date, weightedMovingAverage(price.date, days).getOrElse(BigDecimal(0))) })
  }

  def filter(startDate: ZonedDateTime, endDate: ZonedDateTime): PairPrices =
    filter(p => p.date.isAfter(startDate) && p.date.isBefore(endDate))

}

object PairPrice extends StrictLogging {
  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"))

  def fromUrl(s: String): PairPrices = {
    val bufferedSource = scala.io.Source.fromURL(s)

    val priceLines = bufferedSource.getLines.toList
    val priceLineId = priceLines.head.split(',').zipWithIndex.find(_._1 == "price(USD)").map(_._2).getOrElse(throw new RuntimeException("Invalid file"))
    val prices =
      for (line <- priceLines.tail) yield {
        val splitted = line.split(',')
        val date = LocalDate.parse(splitted(0), formatter)
        val time = LocalTime.MIDNIGHT
        PairPrice(
          ZonedDateTime.of(date, time, ZoneId.of("UTC")),
          if (splitted(priceLineId) != "") BigDecimal(splitted(priceLineId)) else BigDecimal(0))
      }
    PairPrices(prices)
  }

  private def httpGetRequest(url: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    Http().singleRequest(HttpRequest(uri = url)).flatMap { response =>
      Unmarshal(response.entity.withSizeLimit(20000000)).to[String]
    }
  }

  def fromString(csvString: String, priceColumn: String): PairPrices = {
    val priceLines = csvString.linesIterator.toList

    val priceLineId = priceLines.head.split(',').zipWithIndex.find(_._1 == priceColumn).map(_._2).getOrElse(throw new RuntimeException("Invalid file, price column not found : " + priceColumn))
    val prices =
      for (line <- priceLines.tail) yield {
        val splitted = line.split(',')
        val date = LocalDate.parse(splitted(0), formatter)
        val time = LocalTime.MIDNIGHT
        PairPrice(
          ZonedDateTime.of(date, time, ZoneId.of("UTC")),
          if (splitted.length >= priceLineId && splitted(priceLineId) != "" && splitted(priceLineId) != "null") BigDecimal(splitted(priceLineId)) else BigDecimal(0))
      }
    logger.info("Latest price : {}", prices.filter(_.price != BigDecimal(0)).last.date)
    PairPrices(prices.filter(_.price != BigDecimal(0)))
  }

  def fromUrlAsync(url: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[PairPrices] = {
    logger.info("Loading prices from url {}", url)
    httpGetRequest(url).map(fromString(_, "PriceUSD"))
  }
}