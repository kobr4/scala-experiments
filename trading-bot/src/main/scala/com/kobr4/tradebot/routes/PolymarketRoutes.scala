package com.kobr4.tradebot.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{PolymarketApi, PolymarketMarket}
import play.api.libs.json.{Format, Json}

import scala.concurrent.ExecutionContext


trait PolymarketRoutes extends PlayJsonSupport {

  implicit def system: ActorSystem

  implicit def am: ActorMaterializer

  implicit def ec: ExecutionContext

  lazy val polymarketApi = new PolymarketApi()

  implicit val polymarketFormat: Format[PolymarketMarket] = Json.format[PolymarketMarket]


  lazy val polymarketRoutes: Route =
    path("markets") {
      get {
        onSuccess(polymarketApi.markets()) { markets => complete(markets) }
      }
    }

}
