package com.kobr4.tradebot.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.Asset
import com.kobr4.tradebot.api.KrakenApi.Public
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

class KrakenApi(krakenUrl: String = KrakenApi.rootUrl)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) {

  private val publicUrl = s"$krakenUrl/public"

  import play.api.libs.functional.syntax._

  def quoteReads(pair: CurrencyPair): Reads[Quote] = (
    Reads.pure(pair) and
      (JsPath \ "c") (0).read[BigDecimal] and
      (JsPath \ "a") (0).read[BigDecimal] and
      (JsPath \ "b") (0).read[BigDecimal] and
      Reads.pure(BigDecimal(0)) and
      (JsPath \ "v") (0).read[BigDecimal] and
      (JsPath \ "p") (0).read[BigDecimal]) (Quote.apply _)


  def tradableAsset()(implicit ec: ExecutionContext): Future[List[String]] = KrakenApi.httpRequest(publicUrl, Public.tradableAsset).map { message =>
    Json.parse(message).as[JsObject].value("result").as[JsObject].fields.map(_._1).toList
  }

  def returnTicker()(implicit ec: ExecutionContext): Future[List[Quote]] =
    tradableAsset.map(_.filter(aList => aList.contains("ETH") || aList.contains("XMR") || aList.contains("XBT")).mkString(",")).flatMap { assetListParam => {

      KrakenApi.httpRequest(publicUrl, Public.ticker(assetListParam)).map { message =>

        Json.parse(message).as[JsObject].value("result").as[JsObject].fields.flatMap {
          case (s, v) =>
            println(s)
            val pairString = s.toUpperCase
            val (a, b) = pairString.length match {
              case 6 =>
                (pairString.substring(0, 3), pairString.substring(3))
              case 7 =>
                (pairString.substring(0, 4), pairString.substring(4))
              case 8 =>
                (pairString.substring(1, 4), pairString.substring(5))
            }
            (Asset.fromString(a), Asset.fromString(b)) match {
              case (Some(asset1), Some(asset2)) =>
                v.asOpt[Quote](quoteReads(CurrencyPair(asset1, asset2)))
            }
        }.toList
      }
    }
    }
}

object KrakenApi {

  val rootUrl = "https://api.kraken.com/0/"

  object Public {

    def ticker(pairList: String) = s"Ticker?pair=$pairList"

    val tradableAsset = "AssetPairs"
  }

  private def httpRequest(url: String, command: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    Http().singleRequest(HttpRequest(uri = s"$url/$command")).flatMap { response =>
      Unmarshal(response.entity).to[String]
    }
  }

}
