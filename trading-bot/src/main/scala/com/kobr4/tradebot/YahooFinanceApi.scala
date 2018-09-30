package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer

import scala.concurrent.{ ExecutionContext, Future }

object YahooFinanceApi {

  val url = "https://query1.finance.yahoo.com/v7/finance/download/BTC-USD?period1=1279317600&period2=1538258400&interval=1d&events=history"

  val cookierUrl = "https://uk.finance.yahoo.com/quote/BTC-USD/history"

  private val crumbExtractor = """[\s\S]*"CrumbStore"\:\{"crumb":"([a-zA-Z0-9]*)"\}[\s\S]*""".r

  private def buildCrumbAndCookieUrl(code: String) = s"https://uk.finance.yahoo.com/quote/$code/history"

  private def buildCsvRequestUrl(code: String) = s"https://query1.finance.yahoo.com/v7/finance/download/$code?period1=1279317600&period2=1538258400&interval=1d&events=history"

  private def httpRequestFromCrumbAndCookie(url: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[Option[(String, String)]] = {
    Http().singleRequest(HttpRequest(uri = s"$url")).flatMap { response =>
      val cookie = response.headers.filter(p => p.lowercaseName() == "set-cookie").head.value()
      Unmarshal(response.entity).to[String].map(body => (body, cookie))
    }.map { t =>
      t._1 match {
        case crumbExtractor(crumb) =>
          Some((crumb, t._2))
        case _ => None
      }
    }
  }

  private def httpRequest(url: String, cookie: String, crumb: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    Http().singleRequest(HttpRequest(uri = s"$url&crumb=$crumb").addHeader(RawHeader("cookie", cookie))).flatMap { response =>
      Unmarshal(response.entity).to[String]
    }
  }

  def fetchPriceData(code: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[PairPrices] = {
    httpRequestFromCrumbAndCookie(buildCrumbAndCookieUrl(code)).flatMap {
      case Some((crumb, cookie)) => httpRequest(buildCsvRequestUrl(code), cookie, crumb)
      case _ => Future.failed(new RuntimeException("Could not retrieve cookie and crumb"))
    }.map {
      PairPrice.fromString(_, "Close")
    }
  }
}
