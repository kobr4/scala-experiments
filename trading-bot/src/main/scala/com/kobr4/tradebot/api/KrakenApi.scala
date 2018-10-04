package com.kobr4.tradebot.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.Asset
import com.kobr4.tradebot.api.KrakenApi.Public
import com.kobr4.tradebot.api.PoloApi.{ AuthHeader, generateHMAC512, logger }
import com.kobr4.tradebot.model.Quantity
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.json._

import scala.concurrent.{ ExecutionContext, Future }

sealed trait SupportedExchange

case object Kraken extends SupportedExchange

case object Poloniex extends SupportedExchange

object UnsupportedExchangeException extends RuntimeException

object SupportedExchange {

  def fromString(input: String): SupportedExchange = input match {
    case "kraken" => Kraken
    case "poloniex" => Poloniex
    case _ => throw UnsupportedExchangeException
  }
}

class KrakenApi(krakenUrl: String = KrakenApi.rootUrl)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) {

  private val publicUrl = s"$krakenUrl/public"

  private val privateUrl = s"$krakenUrl/private"

  private val apiKey = ""

  private val apiSecret = ""

  private def nonce = System.currentTimeMillis()

  import play.api.libs.functional.syntax._

  def quoteReads(pair: CurrencyPair): Reads[Quote] = (
    Reads.pure(pair) and
    (JsPath \ "c")(0).read[BigDecimal] and
    (JsPath \ "a")(0).read[BigDecimal] and
    (JsPath \ "b")(0).read[BigDecimal] and
    Reads.pure(BigDecimal(0)) and
    (JsPath \ "v")(0).read[BigDecimal] and
    (JsPath \ "p")(0).read[BigDecimal])(Quote.apply _)

  def tradableAsset()(implicit ec: ExecutionContext): Future[List[String]] = KrakenApi.httpRequest(publicUrl, Public.tradableAsset).map { message =>
    Json.parse(message).as[JsObject].value("result").as[JsObject].fields.map(_._1).toList
  }

  def returnTicker()(implicit ec: ExecutionContext): Future[List[Quote]] =
    tradableAsset.map(_.filter(aList => aList.contains("ETH") || aList.contains("XMR") || aList.contains("XBT")).mkString(",")).flatMap { assetListParam =>
      {

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

  def returnBalances: Future[Map[Asset, Quantity]] =
    KrakenApi.httpRequestPost(privateUrl, KrakenApi.ReturnBalances.build(nonce), apiKey, apiSecret).map { message =>
      Json.parse(message).as[JsObject].fields.flatMap {
        case (s, v) => Asset.fromString(s.toUpperCase).map { asset =>
          (asset, Quantity(BigDecimal(v.as[String])))
        }
      }.toMap
    }
}

object KrakenApi extends StrictLogging {

  val rootUrl = "https://api.kraken.com/0/"

  object Public {

    def ticker(pairList: String) = s"Ticker?pair=$pairList"

    val tradableAsset = "AssetPairs"
  }

  object ReturnBalances {

    val ReturnBalances = "Balance"

    def build(nonce: Long): FormData = akka.http.scaladsl.model.FormData(Map(PoloApi.Nonce -> nonce.toString))
  }

  object AuthHeader {

    val key = "API-Key"

    val sign = "API-Sign"

    def build(key: String, sign: String): List[HttpHeader] = {
      List(RawHeader(AuthHeader.key, key), RawHeader(AuthHeader.sign, sign))
    }
  }

  private def httpRequest(url: String, command: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    Http().singleRequest(HttpRequest(uri = s"$url/$command")).flatMap { response =>
      Unmarshal(response.entity).to[String]
    }
  }

  private def httpRequestPost(url: String, body: FormData, apiKey: String, apiSecret: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    logger.info(s"Sending post request to $url")
    logger.info(s"Body: ${body.fields.toString}")
    Http().singleRequest(HttpRequest(
      method = HttpMethods.POST,
      headers = AuthHeader.build(apiKey, generateHMAC512(apiSecret, body.fields.toString)),
      entity = body.toEntity(HttpCharsets.`UTF-8`),
      uri = url)).flatMap { response =>
      if (response.status == StatusCodes.OK)
        Unmarshal(response.entity).to[String]
      else
        throw new RuntimeException("Return code was " + response.status)
    }
  }

}
