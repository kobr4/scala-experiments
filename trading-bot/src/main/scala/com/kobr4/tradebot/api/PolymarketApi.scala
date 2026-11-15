package com.kobr4.tradebot.api

import java.net.URLEncoder

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, StatusCodes }
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.json._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

case class PolymarketMarket(
  id: Option[String],
  question: String,
  conditionId: Option[String],
  slug: Option[String],
  active: Boolean,
  closed: Boolean,
  archived: Boolean,
  volume: Option[BigDecimal],
  liquidity: Option[BigDecimal],
  outcomes: List[String],
  outcomePrices: List[BigDecimal],
  clobTokenIds: List[String])

object PolymarketMarket {

  implicit val polymarketMarketReads: Reads[PolymarketMarket] = Reads { json =>
    JsSuccess(PolymarketMarket(
      PolymarketJson.stringField(json, "id"),
      PolymarketJson.stringField(json, "question").getOrElse(""),
      PolymarketJson.stringField(json, "conditionId"),
      PolymarketJson.stringField(json, "slug"),
      PolymarketJson.booleanField(json, "active", default = false),
      PolymarketJson.booleanField(json, "closed", default = false),
      PolymarketJson.booleanField(json, "archived", default = false),
      PolymarketJson.bigDecimalField(json, "volume"),
      PolymarketJson.bigDecimalField(json, "liquidity"),
      PolymarketJson.stringListField(json, "outcomes"),
      PolymarketJson.bigDecimalListField(json, "outcomePrices"),
      PolymarketJson.stringListField(json, "clobTokenIds")))
  }
}

case class PolymarketEvent(
  id: Option[String],
  title: String,
  slug: Option[String],
  active: Boolean,
  closed: Boolean,
  archived: Boolean,
  markets: List[PolymarketMarket])

object PolymarketEvent {

  implicit val polymarketEventReads: Reads[PolymarketEvent] = Reads { json =>
    JsSuccess(PolymarketEvent(
      PolymarketJson.stringField(json, "id"),
      PolymarketJson.stringField(json, "title")
        .orElse(PolymarketJson.stringField(json, "question"))
        .getOrElse(""),
      PolymarketJson.stringField(json, "slug"),
      PolymarketJson.booleanField(json, "active", default = false),
      PolymarketJson.booleanField(json, "closed", default = false),
      PolymarketJson.booleanField(json, "archived", default = false),
      (json \ "markets").asOpt[List[PolymarketMarket]].getOrElse(List())))
  }
}

case class PolymarketOrderBookLevel(price: BigDecimal, size: BigDecimal)

object PolymarketOrderBookLevel {

  implicit val polymarketOrderBookLevelReads: Reads[PolymarketOrderBookLevel] = Reads { json =>
    for {
      price <- PolymarketJson.bigDecimalField(json, "price").map(JsSuccess(_)).getOrElse(JsError("Missing price"))
      size <- PolymarketJson.bigDecimalField(json, "size").map(JsSuccess(_)).getOrElse(JsError("Missing size"))
    } yield PolymarketOrderBookLevel(price, size)
  }
}

case class PolymarketOrderBook(
  market: Option[String],
  assetId: String,
  timestamp: Option[String],
  hash: Option[String],
  bids: List[PolymarketOrderBookLevel],
  asks: List[PolymarketOrderBookLevel])

object PolymarketOrderBook {

  implicit val polymarketOrderBookReads: Reads[PolymarketOrderBook] = Reads { json =>
    JsSuccess(PolymarketOrderBook(
      PolymarketJson.stringField(json, "market"),
      PolymarketJson.stringField(json, "asset_id")
        .orElse(PolymarketJson.stringField(json, "assetId"))
        .getOrElse(""),
      PolymarketJson.stringField(json, "timestamp"),
      PolymarketJson.stringField(json, "hash"),
      (json \ "bids").asOpt[List[PolymarketOrderBookLevel]].getOrElse(List()),
      (json \ "asks").asOpt[List[PolymarketOrderBookLevel]].getOrElse(List())))
  }
}

case class PolymarketMarketsQuery(
  limit: Option[Int] = Some(100),
  offset: Option[Int] = None,
  active: Option[Boolean] = None,
  closed: Option[Boolean] = None,
  archived: Option[Boolean] = None,
  slug: Option[String] = None,
  id: Option[String] = None) {

  def toQueryParams: Map[String, String] = Map(
    "limit" -> limit.map(_.toString),
    "offset" -> offset.map(_.toString),
    "active" -> active.map(_.toString),
    "closed" -> closed.map(_.toString),
    "archived" -> archived.map(_.toString),
    "slug" -> slug,
    "id" -> id).collect { case (key, Some(value)) => key -> value }
}

case class PolymarketEventsQuery(
  limit: Option[Int] = Some(100),
  offset: Option[Int] = None,
  active: Option[Boolean] = None,
  closed: Option[Boolean] = None,
  archived: Option[Boolean] = None,
  slug: Option[String] = None,
  tagId: Option[Int] = None) {

  def toQueryParams: Map[String, String] = Map(
    "limit" -> limit.map(_.toString),
    "offset" -> offset.map(_.toString),
    "active" -> active.map(_.toString),
    "closed" -> closed.map(_.toString),
    "archived" -> archived.map(_.toString),
    "slug" -> slug,
    "tag_id" -> tagId.map(_.toString)).collect { case (key, Some(value)) => key -> value }
}

class PolymarketApi(
  gammaUrl: String = PolymarketApi.gammaUrl,
  clobUrl: String = PolymarketApi.clobUrl)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) {

  def markets(query: PolymarketMarketsQuery = PolymarketMarketsQuery()): Future[List[PolymarketMarket]] =
    PolymarketApi.httpGet(gammaUrl, PolymarketApi.Gamma.markets, query.toQueryParams).map { message =>
      Json.parse(message).as[List[PolymarketMarket]]
    }

  def marketBySlug(slug: String): Future[Option[PolymarketMarket]] =
    markets(PolymarketMarketsQuery(slug = Some(slug), limit = Some(1))).map(_.headOption)

  def marketById(id: String): Future[Option[PolymarketMarket]] =
    markets(PolymarketMarketsQuery(id = Some(id), limit = Some(1))).map(_.headOption)

  def events(query: PolymarketEventsQuery = PolymarketEventsQuery()): Future[List[PolymarketEvent]] =
    PolymarketApi.httpGet(gammaUrl, PolymarketApi.Gamma.events, query.toQueryParams).map { message =>
      Json.parse(message).as[List[PolymarketEvent]]
    }

  def eventBySlug(slug: String): Future[Option[PolymarketEvent]] =
    events(PolymarketEventsQuery(slug = Some(slug), limit = Some(1))).map(_.headOption)

  def orderBook(tokenId: String): Future[PolymarketOrderBook] =
    PolymarketApi.httpGet(clobUrl, PolymarketApi.Clob.book, Map("token_id" -> tokenId)).map { message =>
      Json.parse(message).as[PolymarketOrderBook]
    }
}

object PolymarketApi extends StrictLogging {

  val gammaUrl = "https://gamma-api.polymarket.com"

  val clobUrl = "https://clob.polymarket.com"

  object Gamma {
    val markets = "markets"
    val events = "events"
  }

  object Clob {
    val book = "book"
  }

  private[tradebot] def buildUrl(rootUrl: String, path: String, queryParams: Map[String, String] = Map()): String = {
    val cleanRootUrl = rootUrl.stripSuffix("/")
    val cleanPath = path.stripPrefix("/")
    val queryString = if (queryParams.isEmpty) "" else {
      queryParams.toList.sortBy(_._1).map {
        case (key, value) => s"${encode(key)}=${encode(value)}"
      }.mkString("?", "&", "")
    }

    s"$cleanRootUrl/$cleanPath$queryString"
  }

  private def encode(value: String): String = URLEncoder.encode(value, "UTF-8")

  private def httpGet(rootUrl: String, path: String, queryParams: Map[String, String] = Map())(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    val url = buildUrl(rootUrl, path, queryParams)
    logger.info(s"Sending get request to $url")

    Http().singleRequest(HttpRequest(uri = url)).flatMap { response =>
      if (response.status == StatusCodes.OK) {
        Unmarshal(response.entity).to[String]
      } else {
        Unmarshal(response.entity).to[String].flatMap { body =>
          Future.failed(new RuntimeException(s"Polymarket API returned ${response.status}: $body"))
        }
      }
    }
  }
}

private object PolymarketJson {

  def stringField(json: JsValue, fieldName: String): Option[String] =
    (json \ fieldName).toOption.flatMap(asString)

  def booleanField(json: JsValue, fieldName: String, default: Boolean): Boolean =
    (json \ fieldName).toOption.flatMap(asBoolean).getOrElse(default)

  def bigDecimalField(json: JsValue, fieldName: String): Option[BigDecimal] =
    (json \ fieldName).toOption.flatMap(asBigDecimal)

  def stringListField(json: JsValue, fieldName: String): List[String] =
    (json \ fieldName).toOption.map(asStringList).getOrElse(List())

  def bigDecimalListField(json: JsValue, fieldName: String): List[BigDecimal] =
    (json \ fieldName).toOption.map(asBigDecimalList).getOrElse(List())

  private def asString(value: JsValue): Option[String] = value match {
    case JsString(value) => Some(value)
    case JsNumber(value) => Some(value.toString)
    case JsBoolean(value) => Some(value.toString)
    case _ => None
  }

  private def asBoolean(value: JsValue): Option[Boolean] = value match {
    case JsBoolean(value) => Some(value)
    case JsString(value) => Try(value.toBoolean).toOption
    case _ => None
  }

  private def asBigDecimal(value: JsValue): Option[BigDecimal] = value match {
    case JsNumber(value) => Some(value)
    case JsString(value) if value.nonEmpty => Try(BigDecimal(value)).toOption
    case _ => None
  }

  private def asStringList(value: JsValue): List[String] = parseArray(value).flatMap(asString).toList

  private def asBigDecimalList(value: JsValue): List[BigDecimal] = parseArray(value).flatMap(asBigDecimal).toList

  private def parseArray(value: JsValue): Seq[JsValue] = value match {
    case JsArray(values) => values
    case JsString(value) => Try(Json.parse(value)).toOption match {
      case Some(JsArray(values)) => values
      case _ => Seq()
    }
    case _ => Seq()
  }
}