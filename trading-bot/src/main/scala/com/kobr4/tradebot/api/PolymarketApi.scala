package com.kobr4.tradebot.api

import java.net.URLEncoder

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpEntity, HttpRequest, StatusCodes, HttpResponse }
import akka.http.scaladsl.model.headers.{ `Accept-Encoding`, `Content-Encoding`, HttpEncoding, HttpEncodings }
import akka.http.scaladsl.coding.Coders
import akka.util.ByteString
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

case class NextCursor(nextCursor: String)

object NextCursor {

  implicit val nextCursorReads: Reads[NextCursor] = Reads { json =>
    PolymarketJson
      .stringField(json, "next_cursor")
      .orElse(PolymarketJson.stringField(json, "nextCursor"))
      .map(v => JsSuccess(NextCursor(v)))
      .getOrElse(JsError("Missing next_cursor"))
  }
}

case class PolymarketTag(
  id: Int,
  label: String,
  slug: Option[String])

object PolymarketTag {

  implicit val polymarketTagReads: Reads[PolymarketTag] = Reads { json =>
    val idOpt: Option[Int] = (json \\ "id").headOption.flatMap(_.asOpt[Int])
      .orElse((json \\ "id").headOption.flatMap(_.asOpt[String]).flatMap(v => Try(v.toInt).toOption))
      .orElse(PolymarketJson.stringField(json, "id").flatMap(v => Try(v.toInt).toOption))

    val nameOpt: Option[String] = PolymarketJson.stringField(json, "label")
      .orElse(PolymarketJson.stringField(json, "title"))

    (idOpt, nameOpt) match {
      case (Some(id), Some(label)) =>
        JsSuccess(PolymarketTag(id, label, PolymarketJson.stringField(json, "slug")))
      case _ => JsError("Missing id or name for PolymarketTag")
    }
  }
}

case class PolymarketMarketsQuery(
  limit: Option[Int] = Some(100),
  offset: Option[Int] = None,
  active: Option[Boolean] = None,
  closed: Option[Boolean] = None,
  archived: Option[Boolean] = None,
  slug: Option[String] = None,
  id: Option[String] = None,
  nextCursor: Option[String] = None) {

  def toQueryParams: Map[String, String] = Map(
    "limit" -> limit.map(_.toString),
    "offset" -> offset.map(_.toString),
    "active" -> active.map(_.toString),
    "closed" -> closed.map(_.toString),
    "archived" -> archived.map(_.toString),
    "slug" -> slug,
    "id" -> id,
    "next_cursor" -> nextCursor.map(_.toString)).collect { case (key, Some(value)) => key -> value }
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

case class TagsQuery(
  limit: Option[Int] = Some(100),
  offset: Option[Int] = None) {

  def toQueryParams: Map[String, String] = Map(
    "limit" -> limit.map(_.toString),
    "offset" -> offset.map(_.toString)).collect { case (key, Some(value)) => key -> value }
}


class PolymarketApi(
  gammaUrl: String = PolymarketApi.gammaUrl,
  clobUrl: String = PolymarketApi.clobUrl)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) {

  def markets(query: PolymarketMarketsQuery = PolymarketMarketsQuery()): Future[List[PolymarketMarket]] = {
    PolymarketApi.httpGet(gammaUrl, PolymarketApi.Gamma.markets, query.toQueryParams).map { message =>
      Json.parse(message).as[List[PolymarketMarket]]
    }
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

  def tags(params: TagsQuery = TagsQuery()): Future[List[PolymarketTag]] =
    PolymarketApi.httpGet(gammaUrl, PolymarketApi.Gamma.tags, params.toQueryParams).map { message =>
      Json.parse(message).as[List[PolymarketTag]]
    }

  def allTags(query: TagsQuery = TagsQuery()): Future[List[PolymarketTag]] = {
    val tagList = tags(query)
    for {
      f1Res <- tagList
      f2Res <- if (f1Res.length == query.limit.get) allTags(TagsQuery(offset=Some(query.offset.getOrElse(0)+query.limit.getOrElse(0)))) else Future(List.empty[PolymarketTag])
    } yield (f1Res ::: f2Res)
  }
}

object PolymarketApi extends StrictLogging {

  val gammaUrl = "https://gamma-api.polymarket.com"

  val clobUrl = "https://clob.polymarket.com"

  object Gamma {
    val markets = "markets"
    val events = "events"
    val tags = "tags"
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

    val request = HttpRequest(uri = url).addHeader(`Accept-Encoding`(HttpEncodings.gzip))

    Http().singleRequest(request).flatMap { response =>
      decodeBody(response).flatMap { body =>
        if (response.status == StatusCodes.OK) Future.successful(body)
        else Future.failed(new RuntimeException(s"Polymarket API returned ${response.status}: $body"))
      }
    }
  }

  private def decodeBody(resp: HttpResponse)(implicit am: ActorMaterializer, ec: ExecutionContext): Future[String] = {
    import scala.concurrent.duration._
    // Prefer header to decide, but also sniff gzip magic in case header is missing
    val headerSaysGzip = resp.headers.exists {
      case `Content-Encoding`(enc) if enc == HttpEncodings.gzip => true
      case _                                                    => false
    }

    resp.entity.toStrict(30.seconds).map(_.data).map { data =>
      val bytes = data.toArray
      val sniffGzip = bytes.length >= 2 && (bytes(0) == 0x1f.toByte && bytes(1) == 0x8b.toByte)
      val shouldGunzip = headerSaysGzip || sniffGzip
      if (shouldGunzip) gunzip(bytes) else new String(bytes, "UTF-8")
    }
  }

  private def gunzip(bytes: Array[Byte]): String = {
    val bais = new java.io.ByteArrayInputStream(bytes)
    val gis = new java.util.zip.GZIPInputStream(bais)
    val baos = new java.io.ByteArrayOutputStream()
    val buffer = new Array[Byte](8192)
    var read = gis.read(buffer)
    while (read > -1) {
      baos.write(buffer, 0, read)
      read = gis.read(buffer)
    }
    gis.close()
    baos.toString("UTF-8")
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