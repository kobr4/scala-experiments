package com.kobr4.tradebot.api

import java.util.Base64

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.KrakenApi.Public
import com.kobr4.tradebot.model.Quantity
import com.kobr4.tradebot.{ Asset, DefaultConfiguration }
import com.typesafe.scalalogging.StrictLogging
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
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

class KrakenApi(krakenUrl: String = KrakenApi.rootUrl, apiKey: String = DefaultConfiguration.KrakenApi.Key,
  apiSecret: String = DefaultConfiguration.KrakenApi.Secret)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) extends PoloAPIInterface {

  private val publicUrl = s"$krakenUrl/0/public"

  private val privateUrl = s"/0/private"

  private def nonce() = System.currentTimeMillis()
  //private def nonce() = 1538729057879L

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

  def returnDepositMethods(asset: String): Future[List[String]] = {
    val reqNonce = nonce()
    KrakenApi.httpRequestPost(s"$privateUrl/${KrakenApi.DepositMethods.DepositMethods}", reqNonce, KrakenApi.DepositMethods.build(reqNonce, asset), apiKey, apiSecret).map { message =>
      println(message)
      /*
      Json.parse(message).as[JsObject].value("result").as[JsObject].fields.flatMap {

      }
      */
      List()
    }
  }

  override def returnBalances: Future[Map[Asset, Quantity]] = {
    val reqNonce = nonce()
    KrakenApi.httpRequestPost(s"$privateUrl/${KrakenApi.ReturnBalances.ReturnBalances}", reqNonce, KrakenApi.ReturnBalances.build(reqNonce), apiKey, apiSecret).map { message =>
      Json.parse(message).as[JsObject].value("result").as[JsObject].fields.flatMap {
        case (s, v) => Asset.fromString(s.toUpperCase).map { asset =>
          (asset, Quantity(BigDecimal(v.as[String])))
        }
      }.toMap
    }
  }

  override def returnDepositAddresses: Future[Map[Asset, String]] = {
    val reqNonce = nonce()
    KrakenApi.httpRequestPost(s"$privateUrl/${KrakenApi.ReturnDepositAddresses.ReturnDepositAddresses}", reqNonce,
      KrakenApi.ReturnDepositAddresses.build(reqNonce), apiKey, apiSecret).map { message =>
        println(message)
        Map[Asset, String]()
      }
  }

  override def returnOpenOrders(): Future[List[PoloOrder]] = ???

  override def cancelOrder(orderNumber: Long): Future[Boolean] = ???

  override def buy(currencyPair: String, rate: BigDecimal, amount: BigDecimal): Future[String] = ???

  override def sell(currencyPair: String, rate: BigDecimal, amount: BigDecimal): Future[String] = ???
}

object KrakenApi extends StrictLogging {

  val rootUrl = "https://api.kraken.com"

  val Asset = "asset"

  object Public {

    def ticker(pairList: String) = s"Ticker?pair=$pairList"

    val tradableAsset = "AssetPairs"
  }

  object ReturnBalances {

    val ReturnBalances = "Balance"

    def build(nonce: Long): FormData = akka.http.scaladsl.model.FormData(Map(PoloApi.Nonce -> nonce.toString))
  }

  object DepositMethods {

    val DepositMethods = "DepositMethods"

    def build(nonce: Long, asset: String): FormData = akka.http.scaladsl.model.FormData(Map(PoloApi.Nonce -> nonce.toString, KrakenApi.Asset -> asset))
  }

  object AuthHeader {

    val key = "API-Key"

    val sign = "API-Sign"

    def build(key: String, sign: String): List[HttpHeader] = {
      List(RawHeader(AuthHeader.key, key), RawHeader(AuthHeader.sign, sign))
    }
  }

  object ReturnOpenOrders {

    val ReturnOpenOrders = "OpenOrders"

    def build(nonce: Long): FormData = akka.http.scaladsl.model.FormData(Map(PoloApi.Nonce -> nonce.toString))
  }

  object ReturnDepositAddresses {

    val ReturnDepositAddresses = "DepositAddresses"

    def build(nonce: Long): FormData = akka.http.scaladsl.model.FormData(Map(PoloApi.Nonce -> nonce.toString))
  }

  private def httpRequest(url: String, command: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    Http().singleRequest(HttpRequest(uri = s"$url/$command")).flatMap { response =>
      Unmarshal(response.entity).to[String]
    }
  }

  private def httpRequestPost(path: String, nonce: Long, body: FormData, apiKey: String, apiSecret: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    val url = s"$rootUrl$path"
    logger.info(s"Sending post request to $url")
    logger.info(s"Body: ${body.fields.toString}")
    logger.info(s"Path $path")
    Http().singleRequest(HttpRequest(
      method = HttpMethods.POST,
      headers = AuthHeader.build(apiKey, generateHMAC512(apiSecret, path.getBytes ++ generateSha256(nonce.toString + body.fields.toString))),
      entity = body.toEntity(HttpCharsets.`UTF-8`),
      uri = url)).flatMap { response =>
      if (response.status == StatusCodes.OK)
        Unmarshal(response.entity).to[String]
      else
        throw new RuntimeException("Return code was " + response.status)
    }
  }

  private[tradebot] def generateSha256(originalString: String): Array[Byte] = {
    import java.security.MessageDigest
    val digest = MessageDigest.getInstance("SHA-256")
    import java.nio.charset.StandardCharsets
    val hash = digest.digest(originalString.getBytes(StandardCharsets.UTF_8))
    hash
  }

  private[tradebot] def generateHMAC512(sharedSecret: String, preHashData: Array[Byte]): String = {
    val secret = new SecretKeySpec(Base64.getDecoder.decode(sharedSecret), "HmacSHA512")
    val mac = Mac.getInstance("HmacSHA512")
    mac.init(secret)
    val hashString: Array[Byte] = mac.doFinal(preHashData)
    val hmacsign = Base64.getEncoder.encodeToString(hashString)
    logger.debug(s"HMAC sha512 signature: $hmacsign")
    hmacsign
  }

}
