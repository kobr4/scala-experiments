package com.kobr4.tradebot.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{PolymarketApi, PolymarketEvent, PolymarketEventsQuery, PolymarketMarket, PolymarketTag, PolymarketMarketsQuery, TagsQuery}
import play.api.libs.json.{Format, Json}

import scala.concurrent.ExecutionContext


trait PolymarketRoutes extends PlayJsonSupport {

  implicit def system: ActorSystem

  implicit def am: ActorMaterializer

  implicit def ec: ExecutionContext

  lazy val polymarketApi = new PolymarketApi()

  implicit val polymarketFormat: Format[PolymarketMarket] = Json.format[PolymarketMarket]
  implicit val polymarketTagFormat: Format[PolymarketTag] = Json.format[PolymarketTag]
  implicit val polymarketEventFormat: Format[PolymarketEvent] = Json.format[PolymarketEvent]


  lazy val polymarketRoutes: Route =
    path("markets") {
      get {
        parameters('limit.as[Int].?, 'offset.as[Int].?, 'active.as[Boolean].?, 'closed.as[Boolean].?, 'archived.as[Boolean].?) { (limitOpt, offsetOpt, activeOpt, closedOpt, archivedOpt) =>
          val query = PolymarketMarketsQuery(limit = limitOpt, offset = offsetOpt, active = activeOpt, closed = closedOpt, archived = archivedOpt)
          onSuccess(polymarketApi.markets(query)) { markets => complete(markets) }
        }
      }
    } ~
    path("events") {
      get {
        parameters('tag_id.as[Int].?, 'limit.as[Int].?, 'offset.as[Int].?, 'active.as[Boolean].?, 'closed.as[Boolean].?, 'archived.as[Boolean].?) { (tagIdOpt, limitOpt, offsetOpt, activeOpt, closedOpt, archivedOpt) =>
          val query = PolymarketEventsQuery(tagId = tagIdOpt, limit = limitOpt, offset = offsetOpt, active = activeOpt, closed = closedOpt, archived = archivedOpt)
          onSuccess(polymarketApi.events(query)) { events =>
            complete(events)
          }
        }
      }
    } ~
    path("tags") {
      get {
        parameters('limit.as[Int].?, 'offset.as[Int].?) { (limitOpt, offsetOpt) =>
          onSuccess(polymarketApi.tags(com.kobr4.tradebot.api.TagsQuery(limit = limitOpt, offset = offsetOpt))) { tags => complete(tags) }
        }
      }
    }

}
