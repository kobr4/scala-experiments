package com.kobr4.polo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest}
import akka.http.scaladsl.server.Directives.onSuccess
import akka.http.scaladsl.unmarshalling.Unmarshal
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsObject, Json}
import akka.stream.ActorMaterializer

import scala.collection.parallel.mutable
import scala.collection.parallel.mutable.ParHashMap
import scala.concurrent.{ExecutionContext, Future}

case class PairPrice(
                      name: String,
                      last: String,
                      lowestAsk: String,
                      highestBid: String,
                      percentChange: String,
                      baseVolume: String,
                      quoteVolume: String,
                    )


object PairPrice {
  implicit def pairPriceReads(implicit name: String): Reads[PairPrice] = {
    (
      Reads.pure(name) and
        (JsPath \ "last").read[String] and
        (JsPath \ "lowestAsk").read[String] and
        (JsPath \ "highestBid").read[String] and
        (JsPath \ "percentChange").read[String] and
        (JsPath \ "baseVolume").read[String] and
        (JsPath \ "quoteVolume").read[String]
      ) (PairPrice.apply _)
  }

  def fromString(body: String): Seq[PairPrice] = {
    Json.parse(body).as[JsObject].fields.map { case (s, v) =>
      implicit val n: String = s
      v.as[PairPrice]
    }
  }
}

case class User(name: String)

case class Asset(name: String, quantity: Float)

case class Portfolio(assets: List[Asset]) {
  def value()(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[Float] = {
    PoloQuote.getPair().map { pairs =>
        assets.map(asset =>
          asset.quantity * pairs.find(p => p.name.contains(asset.name) && p.name.contains("USDT")).map(_.last.toFloat).getOrElse(0f)).sum
    }
  }
}


object PoloQuote {
  val portfolioMap: ParHashMap[User, Portfolio] = mutable.ParHashMap[User, Portfolio]()

  def value(user: User)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) : Future[String] = {
    portfolioMap.get(user).map {_.value().map(_.toString)}.getOrElse(Future.successful("No portfolio"))
  }

  def getPair()(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[Seq[PairPrice]] = {
    Http().singleRequest(HttpRequest(uri = "https://poloniex.com/public?command=returnTicker")) flatMap { response =>
      Unmarshal(response.entity).to[String].map { body =>
        PairPrice.fromString(body)
      }
    }
  }

  def searchQuote(searchString: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    getPair().map { pairs => pairs.filter(pair => pair.name.contains(searchString)).map(pair => s"${pair.name}:${pair.last}").mkString("\n") }
  }


  def createPortfolio(user: User, params: List[String]): Future[String] = {
    def getAssets(params: List[String], in: List[Asset]): List[Asset] = {
      params match {
        case name :: quantity :: tail => getAssets(tail, Asset(name, quantity.toFloat) :: in)
        case _ => in
      }
    }

    portfolioMap.put(user, Portfolio(getAssets(params, List[Asset]())))

    Future.successful("Created")
  }
}